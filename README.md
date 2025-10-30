# cron-mcp

Cross-platform **cron and task scheduler integration layer** exposing a Model Context Protocol (MCP) server.  
`cron-mcp` lets you inspect, explain, and manage scheduled jobs across **Linux**, **macOS** and **Windows** via MCP **tools**, **resources**, and **prompts**.

---

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## Table of Contents

- [Features](#features)
- [Quick start](#quick-start)
- [Usage examples](#usage-examples)
- [Architecture & key classes](#architecture--key-classes)
- [Configuration](#configuration)
- [Development & testing](#development--testing)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- MCP resources exposing system state:
    - `resource://cron/user`, `resource://cron/system`, `resource://cron/log` (see [`CronResources`](cron-mcp-server/src/main/java/com/jinternals/demo/ai/mcp/server/services/cron/CronResources.java))
- MCP tools for management:
    - `listJobs`, `addJob`, `removeJob` (see [`CronTools`](cron-mcp-server/src/main/java/com/jinternals/demo/ai/mcp/server/services/cron/CronTools.java))
- MCP prompts for natural-language assistance:
    - `explain_cron_line`, `new_cron_entry`, `remove_by_match_preview` (see [`CronPrompts`](cron-mcp-server/src/main/java/com/jinternals/demo/ai/mcp/server/services/cron/CronPrompts.java))
- OS-aware strategy pattern for platform specifics:
    - Implementations: [`LinuxCronProvider`](cron-mcp-server/src/main/java/com/jinternals/demo/ai/mcp/server/services/cron/impl/LinuxCronProvider.java), [`MacCrontabStrategy`](cron-mcp-server/src/main/java/com/jinternals/demo/ai/mcp/server/services/cron/impl/MacCrontabStrategy.java), [`MacLaunchdStrategy`](cron-mcp-server/src/main/java/com/jinternals/demo/ai/mcp/server/services/cron/impl/MacLaunchdStrategy.java), [`WindowsCronProvider`](cron-mcp-server/src/main/java/com/jinternals/demo/ai/mcp/server/services/cron/impl/WindowsCronProvider.java), and [`OtherCronProvider`](cron-mcp-server/src/main/java/com/jinternals/demo/ai/mcp/server/services/cron/impl/OtherCronProvider.java)

---

## Quick start

Prerequisites: Java 21+ and Maven.

```bash
# clone repo
git clone https://github.com/jinternals/cron-mcp.git
cd cron-mcp/cron-mcp-server

# build
mvn clean package -DskipTests

# run (JAR path will vary by version)
java -jar target/cron-mcp-server-*.jar
