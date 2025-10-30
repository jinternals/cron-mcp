package com.jinternals.cron.mcp.server.services.cron.impl;

import com.jinternals.cron.mcp.server.constants.OS;
import com.jinternals.cron.mcp.server.services.cron.CronStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.jinternals.cron.mcp.server.utils.Util.*;

@Component
@ConditionalOnProperty(name = "cron.mac.backend", havingValue = "launchd", matchIfMissing = true)
public class MacLaunchdStrategy implements CronStrategy {

    @Override
    public OS os() { return OS.MAC; }

    @Override
    public String userCrontab() throws Exception {
        Path agents = Path.of(System.getProperty("user.home"), "Library", "LaunchAgents");
        String body = readAllFilesUnder(agents, "# ~/Library/LaunchAgents/");
        return (body == null || body.isBlank()) ? "# (no LaunchAgents found)" : body;
    }

    @Override
    public String systemCron() throws Exception {
        StringBuilder sb = new StringBuilder();
        Path ld = Path.of("/Library/LaunchDaemons");
        Path sld = Path.of("/System/Library/LaunchDaemons");
        sb.append(readAllFilesUnder(ld, "# /Library/LaunchDaemons/"));
        sb.append(readAllFilesUnder(sld, "# /System/Library/LaunchDaemons/"));

        Path etc = Path.of("/etc/crontab");
        if (Files.exists(etc)) sb.append("# /etc/crontab\n").append(Files.readString(etc)).append("\n\n");

        return sb.length() == 0 ? "# (no LaunchDaemons or crontab found)" : sb.toString();
    }

    @Override
    public String cronLog() throws Exception {
        try {
            String out = run(List.of("log", "show", "--last", "1d",
                    "--predicate", "process == \"launchd\"",
                    "--style", "syslog"), null);
            String t = tailString(out, 300);
            return t.isBlank() ? "(no launchd logs in last 1d)" : t;
        } catch (Exception ex) {
            // fallback to system log files
            for (String c : List.of("/var/log/system.log", "/var/log/syslog")) {
                Path p = Path.of(c);
                if (Files.exists(p)) return tailFile(p, 300);
            }
            throw ex;
        }
    }

    @Override
    public String listJobs() throws Exception {
        try {
            return run(List.of("launchctl", "list"), null);
        } catch (Exception e) {
            return "# (launchctl list failed: " + e.getMessage() + ")";
        }
    }

    @Override
    public String addJob(String schedule, String command, String nameHint) throws Exception {
        String label = ("com.jinternals.cronmcp." + (nameHint == null ? String.valueOf(System.currentTimeMillis()) : nameHint))
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        String[] p = schedule.trim().split("\\s+");
        int minute = (p.length > 0 && p[0].matches("\\d+")) ? Integer.parseInt(p[0]) : 0;
        int hour = (p.length > 1 && p[1].matches("\\d+")) ? Integer.parseInt(p[1]) : 0;
        Path agents = Path.of(System.getProperty("user.home"), "Library", "LaunchAgents");
        Files.createDirectories(agents);
        Path plist = agents.resolve(label + ".plist");
        String xml = """
              <?xml version="1.0" encoding="UTF-8"?>
              <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
              <plist version="1.0"><dict>
                <key>Label</key><string>%s</string>
                <key>ProgramArguments</key><array><string>/bin/sh</string><string>-lc</string><string>%s</string></array>
                <key>RunAtLoad</key><true/>
                <key>StartCalendarInterval</key><dict><key>Minute</key><integer>%d</integer><key>Hour</key><integer>%d</integer></dict>
                <key>StandardOutPath</key><string>%s/%s.out.log</string>
                <key>StandardErrorPath</key><string>%s/%s.err.log</string>
              </dict></plist>""".formatted(label, command, minute, hour, agents, label, agents, label);
        Files.writeString(plist, xml);
        try { run(List.of("launchctl", "unload", plist.toString()), null); } catch (Exception ignore) {}
        run(List.of("launchctl", "load", plist.toString()), null);
        return "launchd job created: " + label;
    }

    @Override
    public String removeJob(String match) throws Exception {
        Path agents = Path.of(System.getProperty("user.home"), "Library", "LaunchAgents");
        if (Files.isDirectory(agents)) try (var ds = Files.newDirectoryStream(agents, "*" + match + "*.plist")) {
            for (Path p : ds) {
                try { run(List.of("launchctl", "unload", p.toString()), null); } catch (Exception ignore) {}
                Files.deleteIfExists(p);
            }
        }
        return "Removed LaunchAgents matching: " + match;
    }
}
