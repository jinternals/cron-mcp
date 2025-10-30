package com.jinternals.cron.mcp.server.services.cron.impl;


import com.jinternals.cron.mcp.server.constants.OS;
import com.jinternals.cron.mcp.server.services.cron.CronStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.jinternals.cron.mcp.server.utils.Util.run;
import static com.jinternals.cron.mcp.server.utils.Util.tailFile;

@Component
@ConditionalOnProperty(name = "cron.mac.backend", havingValue = "crontab")
public class MacCrontabStrategy implements CronStrategy {

    @Override
    public OS os() { return OS.MAC; }

    @Override
    public String userCrontab() throws Exception {
        try {
            String out = run(List.of("crontab", "-l"), null);
            return (out == null || out.isBlank()) ? "# (empty crontab)" : out;
        } catch (Exception e) {
            return "# (crontab not available or failed: " + e.getMessage() + ")";
        }
    }

    @Override
    public String systemCron() throws Exception {
        Path etc = Path.of("/etc/crontab");
        if (Files.exists(etc)) return Files.readString(etc);
        return "# (no /etc/crontab)";
    }

    @Override
    public String cronLog() throws Exception {
        for (String c : List.of("/var/log/cron", "/var/log/cron.log", "/var/log/system.log", "/var/log/syslog")) {
            Path p = Path.of(c);
            if (Files.exists(p)) return tailFile(p, 300);
        }
        return "# (no cron logs found)";
    }

    @Override
    public String listJobs() throws Exception {
        try {
            return run(List.of("crontab", "-l"), null);
        } catch (Exception e) {
            return "# (failed to list crontab: " + e.getMessage() + ")";
        }
    }

    @Override
    public String addJob(String schedule, String command, String nameHint) throws Exception {
        // basic append to user crontab; callers must validate schedule
        String current = "";
        try { current = run(List.of("crontab", "-l"), null); } catch (Exception ignore) {}
        String line = schedule.trim() + " " + command.trim();
        if (!current.isBlank() && current.lines().anyMatch(l -> l.trim().equals(line))) {
            return "Already exists.";
        }
        String updated = (current == null || current.isBlank()) ? line + "\n" : current + "\n" + line + "\n";
        run(List.of("crontab", "-"), updated);
        return "Cron job added to crontab.";
    }

    @Override
    public String removeJob(String match) throws Exception {
        String current = "";
        try { current = run(List.of("crontab", "-l"), null); } catch (Exception ignore) {}
        String filtered = current.lines().filter(l -> !l.contains(match)).collect(java.util.stream.Collectors.joining("\n"));
        run(List.of("crontab", "-"), filtered.isBlank()? "" : filtered + "\n");
        return "Removed lines containing: " + match + " from crontab";
    }
}

