package com.jinternals.cron.mcp.server.services.cron;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CronPrompts {

    @McpPrompt(
            name = "explain_cron_line",
            description = "Explain a crontab line in plain English with timing breakdown + PATH/logging gotchas"
    )
    public GetPromptResult explainCron(
            @McpArg(name = "line", required = true) String line
    ) {
        String body = """
            Explain this crontab line for a developer:
            - Translate timing to plain English
            - Break down each cron field
            - Note PATH, environment, and logging pitfalls

            CRON_LINE:
            %s
            """.formatted(line);

        return new GetPromptResult(
                "Explain Cron Line",
                List.of(new PromptMessage(Role.USER, new TextContent(body)))
        );
    }

    @McpPrompt(
            name = "new_cron_entry",
            description = "Render a single cron entry (5 fields + command); include a '# note:' if risky"
    )
    public GetPromptResult newCron(
            @McpArg(name = "minute",  required = true) String minute,
            @McpArg(name = "hour",    required = true) String hour,
            @McpArg(name = "dom",     required = true) String dom,
            @McpArg(name = "mon",     required = true) String mon,
            @McpArg(name = "dow",     required = true) String dow,
            @McpArg(name = "command", required = true) String command,
            @McpArg(name = "comment", required = false) String comment
    ) {
        String body = """
            # %s
            %s %s %s %s %s %s
            """.formatted(comment == null ? "" : comment, minute, hour, dom, mon, dow, command);

        return new GetPromptResult(
                "New Cron Entry Template",
                List.of(new PromptMessage(Role.USER, new TextContent(body)))
        );
    }

    @McpPrompt(
            name = "remove_by_match_preview",
            description = "Preview lines that would match removal. Warn if broad."
    )
    public GetPromptResult removeByMatch(
            @McpArg(name = "match", required = true) String match
    ) {
        String body = """
            Analyze impact before removal:
            - Show lines matching '%s'
            - Warn if pattern too broad

            MATCH = "%s"
            """.formatted(match, match);

        return new GetPromptResult(
                "Remove Cron Preview",
                List.of(new PromptMessage(Role.USER, new TextContent(body)))
        );
    }
}
