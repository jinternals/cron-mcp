package com.jinternals.cron.mcp.server.services.cron;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.AllArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CronTools {

    private CronService cronService;

    @McpTool(
            name = "listJobs",
            description = "List scheduled jobs for the current user (crontab/launchd/schtasks)"
    )
    public CallToolResult listJobs() throws Exception {
        return textResult(cronService.listJobs());
    }

    @McpTool(
            name = "addJob",
            description = """
                    Add a scheduled job. For Linux, 'schedule' is 5-field cron. macOS maps to launchd (fixed hour/min),
                    Windows maps to Task Scheduler (DAILY or MINUTE if not fixed).
                    """
    )
    public CallToolResult addJob(
            @McpToolParam(description = "5-field cron like '0 2 * * *' (Linux); best-effort mapping on macOS/Windows", required = true) String schedule,
            @McpToolParam(description = "Command to run", required = true) String command,
            @McpToolParam(description = "Optional identifier/name", required = false) String nameHint
    ) throws Exception {
        return textResult(cronService.addJob(schedule, command, nameHint));
    }

    @McpTool(
            name = "removeJob",
            description = "Remove a scheduled job by substring (crontab line, LaunchAgent label, or Task name)"
    )
    public CallToolResult removeJob(
            @McpToolParam(description = "substring or exact name", required = true) String match
    ) throws Exception {
        return textResult(cronService.removeJob(match));
    }


    private static CallToolResult textResult(String body) {
        return new CallToolResult(body, false);
    }
}
