package com.jinternals.demo.ai.mcp.server.services.cron.impl;

import com.jinternals.demo.ai.mcp.server.services.cron.CronStrategy;
import com.jinternals.demo.ai.mcp.server.constants.OS;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.jinternals.demo.ai.mcp.server.utils.Util.run;

@Component
public class WindowsCronProvider implements CronStrategy {
    @Override
    public OS os() {
        return OS.WINDOWS;
    }

    @Override
    public String userCrontab() throws Exception {
        String out = run(java.util.List.of("schtasks.exe", "/Query", "/FO", "LIST", "/V"));
        if (out.isBlank()) return "# (no tasks found or insufficient permissions)";
        return out;
    }

    @Override
    public String systemCron() throws Exception {
        String out = run(java.util.List.of("schtasks.exe", "/Query", "/FO", "LIST", "/V"));
        if (out.isBlank()) return "# (no tasks found or insufficient permissions)";
        return out;
    }

    @Override
    public String cronLog() throws Exception {
        String out;
        try {
            out = run(java.util.List.of("wevtutil", "qe", "Microsoft-Windows-TaskScheduler/Operational", "/f:text", "/c:300"));
        } catch (Exception e) {
            out = run(java.util.List.of("wevtutil", "qe", "System",
                    "/q:*[System[(Provider[@Name=\"Microsoft-Windows-TaskScheduler\"])]]",
                    "/f:text", "/c:300"));
        }
        if (out.isBlank()) return "(no Task Scheduler events found)";
        return out;
    }

    @Override
    public String listJobs() throws Exception {
        return run(List.of("schtasks.exe", "/Query", "/FO", "LIST", "/V"), null);
    }

    @Override
    public String addJob(String schedule, String command, String nameHint) throws Exception {
        String task = (nameHint == null ? "CronMcp_" + System.currentTimeMillis() : nameHint)
                .replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String[] p = schedule.trim().split("\\s+");
        String m = p.length > 0 ? p[0] : "0";
        String h = p.length > 1 ? p[1] : "0";
        boolean fixed = m.matches("\\d+") && h.matches("\\d+");
        List<String> cmd = new ArrayList<>(List.of("schtasks.exe", "/Create", "/TN", task, "/TR", command));
        if (fixed) {
            cmd.addAll(List.of("/SC", "DAILY", "/ST", String.format("%02d:%02d", Integer.parseInt(h), Integer.parseInt(m))));
        } else {
            cmd.addAll(List.of("/SC", "MINUTE", "/MO", "15"));
        }
        run(cmd, null);
        return "Windows task created: " + task;
    }

    @Override
    public String removeJob(String match) throws Exception {
        try {
            run(List.of("schtasks.exe", "/Delete", "/TN", match, "/F"), null);
        } catch (Exception e) {
            var listing = run(List.of("schtasks.exe", "/Query", "/FO", "LIST", "/V"), null);
            for (String line : listing.split("\\R")) {
                if (line.toLowerCase().contains("taskname") && line.toLowerCase().contains(match.toLowerCase())) {
                    String name = line.split(":", 2)[1].trim();
                    try {
                        run(List.of("schtasks.exe", "/Delete", "/TN", name, "/F"), null);
                    } catch (Exception ignore) {
                    }
                }
            }
        }
        return "Deleted tasks containing: " + match;
    }


}
