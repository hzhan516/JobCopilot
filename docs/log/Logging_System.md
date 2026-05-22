# Logging System / 日志系统

> **English** | [简体中文](../i18n/zh-Hans-CN/log/Logging_System.md) | [繁體中文](../i18n/zh-Hant-TW/log/Logging_System.md)

---

## Overview / 概述

The JobCopilot backend uses **Logback** (the default logging framework shipped with Spring Boot) to handle all application and framework logs. A single `logback-spring.xml` file controls the output format, rolling policy, and retention rules for every log message produced by the backend.

智能求职助手后端使用 **Logback**（Spring Boot 默认附带的日志框架）来处理所有应用与框架日志。一个统一的 `logback-spring.xml` 文件控制后端所有日志消息的输出格式、滚动策略与保留规则。

---

## Configuration File / 配置文件

**Path / 路径:** `backend/app/src/main/resources/logback-spring.xml`

### Log Format / 日志格式

All log lines—whether written to the console or to a file—follow the same pattern:

所有日志行（无论输出到控制台还是文件）均遵循统一格式：

```
2026-04-30 22:05:21.123 [main] INFO  e.a.s.r.Application - Application started
│──────────────────────│ │────│ │───│ │──────────────│ │─────────────────────│
        Time            Thread Level     Logger           Message
        时间            线程名  日志等级   Logger名          消息
```

| Component / 组件 | Description / 说明 |
|---|---|
| `Time` / `时间` | `yyyy-MM-dd HH:mm:ss.SSS` — millisecond precision / 毫秒级精度 |
| `Thread` / `线程名` | Name of the Java thread that emitted the log / 输出日志的 Java 线程名称 |
| `Level` / `日志等级` | `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` — left-padded to 5 chars / 左对齐补至 5 字符 |
| `Logger` / `Logger名` | Abbreviated fully-qualified class name (max 36 chars) / 缩写的全限定类名（最多 36 字符） |
| `Message` / `消息` | The actual log payload / 实际的日志内容 |

### Rolling Policy / 滚动策略

| Rule / 规则 | Value / 取值 | Description / 说明 |
|---|---|---|
| Roll interval / 滚动周期 | 7 days (calendar week) / 7 天（自然周） | Archives are created every Monday at 00:00 / 每周一 00:00 生成归档 |
| Backup count / 备份数量 | 4 | Keep the most recent 4 weekly archives / 保留最近 4 周的归档 |
| Total size cap / 总大小上限 | 100 MB | If the 4 archives exceed 100 MB, older ones are deleted early / 若 4 个归档超过 100 MB，将提前删除旧归档 |
| Clean on start / 启动清理 | `true` | Purge expired archives when the application starts / 应用启动时清理过期归档 |

Archived files are stored under `./logs/archived/app.<year>-<week>.log`.

归档文件存放于 `./logs/archived/app.<year>-<week>.log`。

### Profile Behavior / 各环境行为

| Profile / 环境 | Console / 控制台 | File / 文件 | Purpose / 目的 |
|---|---|---|---|
| `dev` | ✅ | ✅ | Local development: see logs in terminal and persist to disk / 本地开发：终端可见并持久化到磁盘 |
| `prod` | ✅ | ✅ | Production: same dual output, files are the primary audit source / 生产环境：双重输出，文件为主要审计来源 |
| `test` | ✅ | ❌ | CI / unit tests: avoid creating log directories on build agents / CI/单元测试：避免在构建代理上创建日志目录 |

---

## Environment Variables / 环境变量

| Variable / 变量 | Default / 默认值 | Description / 说明 |
|---|---|---|
| `LOG_PATH` | `./logs` | Base directory for the active log file and its archives / 当前日志文件及归档的存放根目录 |

You can override the path at runtime:

可以在运行时覆盖路径：

```bash
# Local run / 本地运行
java -DLOG_PATH=/var/log/JobCopilot -jar app.jar

# Docker / Docker 运行
docker run -e LOG_PATH=/app/logs my-backend-image
```

---

## FAQ / 常见问题

**Q: Why calendar week (`yyyy-ww`) instead of a fixed 7-day interval?**  
**Q: 为什么使用自然周（`yyyy-ww`）而不是固定 7 天间隔？**

A: Logback infers the rolling period from the date pattern in `fileNamePattern`. `yyyy-ww` is the standard way to obtain weekly rotation. It aligns with natural Monday–Sunday boundaries, making it easier to correlate logs with calendar events.

A: Logback 根据 `fileNamePattern` 中的日期模式推断滚动周期。`yyyy-ww` 是标准的一周滚动写法，与周一至周日的自然边界对齐，便于按日历事件关联日志。

**Q: Will I lose logs if the total size drops below 100 MB?**  
**Q: 如果总大小不足 100 MB，日志会丢失吗？**

A: No. `totalSizeCap` is an *upper* bound. As long as the 4 weekly archives are under 100 MB combined, all 4 are kept. Only when the cap is exceeded does Logback delete the oldest archive.

A: 不会。`totalSizeCap` 是*上限*。只要 4 周归档合计不超过 100 MB，就会全部保留；仅当超过上限时，Logback 才会删除最旧的归档。

**Q: Can I change the log level without touching XML?**  
**Q: 可以在不修改 XML 的情况下调整日志级别吗？**

A: Yes. Logger levels are still controlled by `logging.level.*` in `application-dev.yml`, `application-prod.yml`, or externalized config (e.g., `LOG_APP_LEVEL` environment variable).

A: 可以。日志级别仍由 `application-dev.yml`、`application-prod.yml` 或外部配置（如 `LOG_APP_LEVEL` 环境变量）中的 `logging.level.*` 控制。
