package com.jinternals.demo.ai.mcp.server.services.cron;

import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import lombok.AllArgsConstructor;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CronResources {

    private CronService cronService;

    @McpResource(
            uri = "resource://cron/user",
            description = "User-level schedulers: Linux (crontab), macOS (LaunchAgents), Windows (Task Scheduler listing).",
            mimeType = "text/plain"
    )
    public ReadResourceResult userCrontab(ReadResourceRequest request) {
        String uri = "resource://cron/user";
        try {
           return textResult(uri, "text/plain", cronService.userCrontab());
        } catch (Exception e) {
            return textResult(uri, "text/plain", "# error: " + e.getMessage());
        }
    }

    @McpResource(
            uri = "resource://cron/system",
            description = "System-level schedulers: Linux (/etc/crontab, /etc/cron.d/*), macOS (LaunchDaemons), Windows (Task Scheduler listing).",
            mimeType = "text/plain"
    )
    public ReadResourceResult systemCron(ReadResourceRequest request) {
        String uri = "resource://cron/system";
        try {
            return  textResult(uri, "text/plain", cronService.systemCron());
        } catch (Exception e) {
            return textResult(uri, "text/plain", "# error: " + e.getMessage());
        }
    }

    @McpResource(
            uri = "resource://cron/log",
            description = "Scheduler logs: Linux (cron/syslog), macOS (launchd via unified log), Windows (Task Scheduler event log).",
            mimeType = "text/plain"
    )
    public ReadResourceResult cronLog(ReadResourceRequest request) {
        String uri = "resource://cron/log";
        try {
            return textResult(uri, "text/plain", cronService.cronLog());
        } catch (Exception e) {
            return textResult(uri, "text/plain", "# error: " + e.getMessage());
        }
    }

    private static ReadResourceResult textResult(String uri, String mimeType, String text) {
        return new ReadResourceResult(List.of(new TextResourceContents(uri, mimeType, text)));
    }

}
