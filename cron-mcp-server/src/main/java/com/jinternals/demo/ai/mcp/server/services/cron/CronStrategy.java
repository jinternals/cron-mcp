package com.jinternals.demo.ai.mcp.server.services.cron;

import com.jinternals.demo.ai.mcp.server.constants.OS;

public interface CronStrategy {

    /** Identifier for OS (LINUX, MAC, WINDOWS, OTHER) */
    OS os();

    /** User-level scheduling info (crontab, LaunchAgents, Task Scheduler user tasks) */
    String userCrontab() throws Exception;

    /** System-level scheduling info (/etc/cron.d, LaunchDaemons, system tasks) */
    String systemCron() throws Exception;

    /** Logs related to scheduling (cron.log, launchd logs, Windows event logs) */
    String cronLog() throws Exception;

    String listJobs() throws Exception;

    String addJob(String schedule, String command, String nameHint) throws Exception;

    String removeJob(String match) throws Exception;

}

