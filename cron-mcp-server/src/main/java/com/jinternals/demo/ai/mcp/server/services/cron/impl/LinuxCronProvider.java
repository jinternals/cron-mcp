package com.jinternals.demo.ai.mcp.server.services.cron.impl;

import com.jinternals.demo.ai.mcp.server.services.cron.CronStrategy;
import com.jinternals.demo.ai.mcp.server.constants.OS;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.jinternals.demo.ai.mcp.server.utils.Util.run;
import static com.jinternals.demo.ai.mcp.server.utils.Util.tailFile;

@Component
public class LinuxCronProvider implements CronStrategy {

    @Override
    public OS os() { return OS.LINUX; }

    @Override
    public String userCrontab() {
        try {
            String out = run(java.util.List.of("crontab", "-l"));
            if (out.isBlank()) return "# (empty)";
            return out;
        } catch (Exception e) {
            return "# (crontab not available or empty)";
        }
    }

    @Override
    public String systemCron() throws Exception {
        StringBuilder sb = new StringBuilder();
        Path etc = Path.of("/etc/crontab");
        if (Files.exists(etc)) sb.append("# /etc/crontab\n").append(Files.readString(etc)).append("\n\n");
        Path d = Path.of("/etc/cron.d");
        if (Files.isDirectory(d)) {
            try (var ds = Files.newDirectoryStream(d)) {
                for (Path p : ds) if (Files.isRegularFile(p)) {
                    sb.append("# /etc/cron.d/").append(p.getFileName()).append("\n")
                            .append(Files.readString(p)).append("\n\n");
                }
            }
        }
        return sb.length() == 0 ? "# (none found or inaccessible)" : sb.toString();
    }

    @Override
    public String cronLog() throws Exception {
        for (String c : java.util.List.of("/var/log/cron", "/var/log/cron.log", "/var/log/syslog")) {
            Path p = Path.of(c);
            if (Files.exists(p)) {
                return tailFile(p, 300);
            }
        }
        return "No cron logs found.";
    }

    @Override
    public String listJobs() throws Exception{
        try {
            String out = run(List.of("crontab", "-l"), null);
            return out.isBlank() ? "# (no crontab entries)" : out;
        } catch (IOException e) {
            return "# (crontab not available or empty)";
        }
    }

    @Override
    public String addJob(String schedule, String command, String nameHint) throws Exception {
        var current = "";
        try { current = run(List.of("crontab","-l"), null); } catch (IOException ignore) {}
        var line = schedule.trim() + " " + command.trim();
        if (Arrays.stream(current.split("\\R")).anyMatch(l -> l.trim().equals(line))) {
            return  "Already exists.";
        }
        var updated = (current.isBlank() ? "" : current + "\n") + line + "\n";
        run(List.of("crontab","-"), updated);
        return "Cron job added.";
    }

    @Override
    public String removeJob(String match) throws Exception {
            var current = "";
            try { current = run(List.of("crontab","-l"), null);} catch (IOException ignore) {}
            var filtered = Arrays.stream(current.split("\\R"))
                    .filter(l -> !l.contains(match))
                    .collect(Collectors.joining("\n"));
            run(List.of("crontab","-"), filtered.isBlank()? "" : filtered + "\n");
            return  "Removed lines containing: " + match;

    }


}
