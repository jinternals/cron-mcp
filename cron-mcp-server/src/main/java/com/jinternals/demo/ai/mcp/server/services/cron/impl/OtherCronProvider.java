package com.jinternals.demo.ai.mcp.server.services.cron.impl;

import com.jinternals.demo.ai.mcp.server.services.cron.CronStrategy;
import com.jinternals.demo.ai.mcp.server.constants.OS;
import org.springframework.stereotype.Component;

@Component
public class OtherCronProvider implements CronStrategy {
    @Override
    public OS os() {
        return OS.OTHER;
    }

    @Override
    public String userCrontab() throws Exception {
        return "Unsupported OS";
    }

    @Override
    public String systemCron() throws Exception {
        return "Unsupported OS";
    }

    @Override
    public String cronLog() throws Exception {
        return "Unsupported OS";
    }

    @Override
    public String listJobs() throws Exception {
        return "Unsupported OS";
    }

    @Override
    public String addJob(String schedule, String command, String nameHint) throws Exception {
        return "Unsupported OS";
    }

    @Override
    public String removeJob(String match) throws Exception {
        return "Unsupported OS";
    }
}
