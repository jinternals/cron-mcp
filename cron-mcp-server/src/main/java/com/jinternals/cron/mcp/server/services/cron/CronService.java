package com.jinternals.cron.mcp.server.services.cron;

import com.jinternals.cron.mcp.server.constants.OS;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.jinternals.cron.mcp.server.constants.OS.OTHER;
import static com.jinternals.cron.mcp.server.utils.Util.os;

@Service
public class CronService {

    private final CronStrategy active;

    public CronService(List<CronStrategy> strategies) {
        OS currentOS = os();
        this.active = strategies.stream()
                .filter(s -> s.os() == currentOS)
                .findFirst()
                .orElseGet(() -> strategies.stream()
                        .filter(s -> s.os() == OTHER)
                        .findFirst()
                        .orElse(strategies.get(0)));
        System.out.println("active" + active);
    }

    public String userCrontab() throws Exception {
        return active.userCrontab();
    }

    public String systemCron() throws Exception {
        return active.systemCron();
    }

    public String cronLog() throws Exception {
        return active.cronLog();
    }

    public String listJobs() throws Exception {
        return active.listJobs();
    }

    public String addJob(String schedule, String command, String nameHint) throws Exception {
        return active.addJob(schedule, command, nameHint);
    }

    public String removeJob(String match) throws Exception {
        return active.removeJob(match);
    }

}
