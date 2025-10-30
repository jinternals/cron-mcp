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
            description = "Preview cron jobs/scheduled tasks matching a pattern before removal. Shows impact analysis and warns if pattern is too broad."
    )
    public GetPromptResult removeByMatch(
            @McpArg(name = "match", required = true, description = "Substring or exact name to search for in scheduled jobs (crontab lines, LaunchAgent labels, or Windows Task names)") String match
    ) {
        String bodyUser = """
        You are analyzing the impact of removing scheduled jobs that match a pattern.
        
        CRITICAL INSTRUCTIONS:
        1. First, call the 'listJobs' tool to retrieve all current scheduled jobs
        2. Search for jobs containing the pattern: '%s'
        3. Display ALL matching jobs with their full details (PID, Status, Label/Name)
        4. Provide an impact assessment:
           - How many jobs match?
           - Are any currently running?
           - Does the pattern seem too broad? (e.g., matches system jobs, multiple unrelated jobs)
        5. If pattern matches system-critical jobs (e.g., com.apple.*, com.microsoft.*), issue a STRONG WARNING
        6. If no matches found, clearly state that
        7. If exactly 1 match found and it's safe to remove, ask user for confirmation
        
        SEARCH PATTERN: "%s"
        
        After showing the analysis, if the user wants to proceed with removal, 
        they can confirm and you should call: removeJob with argument "%s"
        
        IMPORTANT: Never remove jobs without showing the user what will be deleted first.
        """.formatted(match, match, match);

        return new GetPromptResult(
                "Remove Cron Job Preview & Impact Analysis",
                List.of(new PromptMessage(Role.USER, new TextContent(bodyUser)))
        );
    }

    @McpPrompt(
            name = "cron_safety_check",
            description = "Analyze a cron schedule or command for common mistakes and security issues"
    )
    public GetPromptResult cronSafetyCheck(
            @McpArg(name = "schedule", required = false) String schedule,
            @McpArg(name = "command", required = false) String command
    ) {
        String body = """
        Perform a safety analysis on this cron configuration:
        
        Schedule: %s
        Command: %s
        
        Check for:
        1. **Timing Issues**:
           - Overlapping executions if task runs longer than interval
           - Too frequent execution causing system load
           - Timezone considerations
        
        2. **Command Issues**:
           - Missing absolute paths
           - No output redirection (will cause mail spam)
           - Missing error handling
           - Dangerous commands (rm -rf, etc.)
           - Commands that require user interaction
        
        3. **Environment Issues**:
           - PATH not set
           - Missing environment variables
           - Permission issues
        
        4. **Best Practice Recommendations**:
           - Add logging with timestamps
           - Use file locking if command shouldn't overlap
           - Add timeout mechanisms
           - Consider using systemd timers (on Linux) for better logging
        
        Provide specific, actionable recommendations for improvement.
        """.formatted(
                schedule != null ? schedule : "(not provided)",
                command != null ? command : "(not provided)"
        );

        return new GetPromptResult(
                "Cron Safety & Best Practices Analysis",
                List.of(new PromptMessage(Role.USER, new TextContent(body)))
        );
    }

    @McpPrompt(
            name = "list_and_explain_jobs",
            description = "List all scheduled jobs with human-readable explanations of what they do and when they run"
    )
    public GetPromptResult listAndExplain() {
        String body = """
        List all scheduled cron jobs/tasks on this system and provide:
        
        1. Call the 'listJobs' tool to get all scheduled jobs
        2. For each job, provide:
           - Job name/label
           - Current status (running/stopped)
           - What it likely does (based on the name)
           - Whether it's a system job or user-created
        3. Highlight any:
           - User-created jobs (these are typically safe to modify)
           - Jobs that aren't running but should be
           - Duplicate or redundant jobs
        4. Group jobs by category (system, user apps, custom)
        
        Present the information in a clear, organized format.
        """;

        return new GetPromptResult(
                "List & Explain All Scheduled Jobs",
                List.of(new PromptMessage(Role.USER, new TextContent(body)))
        );
    }
}