package com.jinternals.cron.mcp.server.services.cron.impl;

import com.jinternals.cron.mcp.server.constants.OS;
import com.jinternals.cron.mcp.server.services.cron.CronStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static com.jinternals.cron.mcp.server.utils.Util.*;

@Component
@ConditionalOnProperty(name = "cron.mac.backend", havingValue = "launchd", matchIfMissing = true)
public class MacLaunchdStrategy implements CronStrategy {

    @Override
    public OS os() {
        return OS.MAC;
    }

    @Override
    public String userCrontab() throws Exception {
        Path agents = Path.of(System.getProperty("user.home"), "Library", "LaunchAgents");
        String body = readAllFilesUnder(agents, "# ~/Library/LaunchAgents/");
        return (body == null || body.isBlank()) ? "# (no LaunchAgents found)" : body;
    }

    @Override
    public String systemCron() {
        StringBuilder sb = new StringBuilder();
        Path ld = Path.of("/Library/LaunchDaemons");
        Path sld = Path.of("/System/Library/LaunchDaemons");

        sb.append(readAllFilesUnder(ld, "# /Library/LaunchDaemons/"));
        sb.append(readAllFilesUnder(sld, "# /System/Library/LaunchDaemons/"));

        Path etc = Path.of("/etc/crontab");
        try {
            if (Files.exists(etc) && Files.isReadable(etc)) {
                sb.append("# /etc/crontab\n")
                        .append(Files.readString(etc, StandardCharsets.UTF_8))
                        .append("\n\n");
            }
        } catch (IOException e) {
            sb.append("# Error reading /etc/crontab: ").append(e.getClass().getSimpleName())
                    .append(" - ").append(e.getMessage()).append("\n\n");
        }

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
        // Generate a sanitized label from the name hint or timestamp
        String label = "com.jinternals.cronmcp." +
                (nameHint != null ? nameHint : System.currentTimeMillis());
        label = label.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Parse minute and hour from cron schedule
        String[] parts = schedule.trim().split("\\s+");
        int minute = (parts.length > 0 && parts[0].matches("\\d+"))
                ? Integer.parseInt(parts[0]) : 0;
        int hour = (parts.length > 1 && parts[1].matches("\\d+"))
                ? Integer.parseInt(parts[1]) : 0;

        // Set up LaunchAgents directory and plist file
        Path agentsDir = Path.of(System.getProperty("user.home"), "Library", "LaunchAgents");
        Files.createDirectories(agentsDir);
        Path plistFile = agentsDir.resolve(label + ".plist");

        // Create plist XML content
        String plistContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" 
                  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                  <dict>
                    <key>Label</key>
                    <string>%s</string>
                    <key>ProgramArguments</key>
                    <array>
                      <string>/bin/sh</string>
                      <string>-lc</string>
                      <string>%s</string>
                    </array>
                    <key>RunAtLoad</key>
                    <true/>
                    <key>StartCalendarInterval</key>
                    <dict>
                      <key>Minute</key>
                      <integer>%d</integer>
                      <key>Hour</key>
                      <integer>%d</integer>
                    </dict>
                    <key>StandardOutPath</key>
                    <string>%s/%s.out.log</string>
                    <key>StandardErrorPath</key>
                    <string>%s/%s.err.log</string>
                  </dict>
                </plist>
                """.formatted(label, command, minute, hour, agentsDir, label, agentsDir, label);

        Files.writeString(plistFile, plistContent);

        // Unload existing job if present (ignore errors)
        try {
            run(List.of("launchctl", "unload", plistFile.toString()), null);
        } catch (Exception ignored) {
           //TODO: Log it
        }

        // Load the new job
        run(List.of("launchctl", "load", plistFile.toString()), null);

        return "launchd job created: " + label;
    }

    @Override
    public String removeJob(String match) throws Exception {
        Path agents = Path.of(System.getProperty("user.home"), "Library", "LaunchAgents");
        if (Files.isDirectory(agents)) try (var ds = Files.newDirectoryStream(agents, "*" + match + "*.plist")) {
            for (Path p : ds) {
                try {
                    run(List.of("launchctl", "unload", p.toString()), null);
                } catch (Exception ignore) {}
                Files.deleteIfExists(p);
            }
        }
        return "Removed LaunchAgents matching: " + match;
    }

    private String readAllFilesUnder(Path dir, String header) {
        StringBuilder out = new StringBuilder();
        if (!Files.exists(dir)) return "";

        out.append(header).append("\n");
        try (Stream<Path> stream = Files.walk(dir, 1)) { // non-recursive; change depth if needed
            stream.filter(p -> !p.equals(dir))
                    .forEach(path -> {
                        try {
                            if (!Files.isRegularFile(path)) {
                                out.append("# Skipping non-regular file: ").append(path).append("\n");
                                return;
                            }
                            if (!Files.isReadable(path)) {
                                out.append("# Unreadable (permission): ").append(path).append("\n");
                                return;
                            }

                            // Try to detect if file is text (basic heuristic)
                            byte[] head = new byte[Math.min(512, (int) Files.size(path))];
                            try (var is = Files.newInputStream(path)) {
                                int read = is.read(head);
                                boolean binary = false;
                                for (int i = 0; i < read; i++) {
                                    byte b = head[i];
                                    if (b == 0) {
                                        binary = true;
                                        break;
                                    }
                                    // crude control-char test
                                    if (b < 0x09 || (b > 0x0A && b < 0x20)) {
                                        binary = true;
                                        break;
                                    }
                                }

                                out.append("# File: ").append(path.getFileName()).append("\n");
                                if (binary) {
                                    // macOS binary plist: give hint to convert or skip
                                    out.append("# (binary file — use `plutil -convert xml1` if you need text)\n\n");
                                } else {
                                    // safe to read as UTF-8 text
                                    String content = Files.readString(path, StandardCharsets.UTF_8);
                                    out.append(content).append("\n\n");
                                }
                            }
                        } catch (AccessDeniedException ade) {
                            out.append("# Access denied: ").append(path).append(" - ").append(ade.getMessage()).append("\n\n");
                        } catch (IOException ioe) {
                            out.append("# IO error reading ").append(path).append(": ").append(ioe.getClass().getSimpleName())
                                    .append(" - ").append(ioe.getMessage()).append("\n\n");
                        } catch (Exception ex) {
                            out.append("# Unexpected error reading ").append(path).append(": ")
                                    .append(ex.getClass().getSimpleName()).append(" - ").append(ex.getMessage()).append("\n\n");
                        }
                    });
        } catch (IOException e) {
            out.append("# Error walking directory ").append(dir).append(": ").append(e.getMessage()).append("\n");
        }
        return out.toString();
    }
}
