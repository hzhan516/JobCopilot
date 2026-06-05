# 日誌系統 / Logging System

> [English](../../../log/Logging_System.md) | [簡體中文](../../zh-Hans-CN/log/Logging_System.md) | **繁體中文**

---

## 概述 / Overview

JobCopilot 後端使用 **Logback**（Spring Boot 預設附帶的日誌框架）來處理所有應用與框架日誌。一個統一的 `logback-spring.xml` 檔案控制後端所有日誌訊息的輸出格式、滾動策略與保留規則。

The JobCopilot backend uses **Logback** (the default logging framework shipped with Spring Boot) to handle all application and framework logs. A single `logback-spring.xml` file controls the output format, rolling policy, and retention rules for every log message produced by the backend.

---

## 設定檔 / Configuration File

**路徑 / Path:** `backend/app/src/main/resources/logback-spring.xml`

### 日誌格式 / Log Format

所有日誌行（無論輸出到主控台還是檔案）均遵循統一格式：

All log lines—whether written to the console or to a file—follow the same pattern:

```
2026-04-30 22:05:21.123 [main] INFO  e.a.s.r.Application - Application started
│──────────────────────│ │────│ │───│ │──────────────│ │─────────────────────│
        時間            執行緒名 日誌等級   Logger名          訊息
        Time            Thread Level     Logger           Message
```

| 元件 / Component | 說明 / Description |
|---|---|
| `時間` / `Time` | `yyyy-MM-dd HH:mm:ss.SSS` — 毫秒級精度 / millisecond precision |
| `執行緒名` / `Thread` | 輸出日誌的 Java 執行緒名稱 / Name of the Java thread that emitted the log |
| `日誌等級` / `Level` | `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` — 左對齊補至 5 字元 / left-padded to 5 chars |
| `Logger名` / `Logger` | 縮寫的全限定類名（最多 36 字元）/ Abbreviated fully-qualified class name (max 36 chars) |
| `訊息` / `Message` | 實際的日誌內容 / The actual log payload |

### 滾動策略 / Rolling Policy

| 規則 / Rule | 取值 / Value | 說明 / Description |
|---|---|---|
| 滾動週期 / Roll interval | 7 天（自然週）/ 7 days (calendar week) | 每週一 00:00 生成歸檔 / Archives are created every Monday at 00:00 |
| 備份數量 / Backup count | 4 | 保留最近 4 週的歸檔 / Keep the most recent 4 weekly archives |
| 總大小上限 / Total size cap | 100 MB | 若 4 個歸檔超過 100 MB，將提前刪除舊歸檔 / If the 4 archives exceed 100 MB, older ones are deleted early |
| 啟動清理 / Clean on start | `true` | 應用啟動時清理過期歸檔 / Purge expired archives when the application starts |

歸檔檔案存放於 `./logs/archived/app.<year>-<week>.log`。

Archived files are stored under `./logs/archived/app.<year>-<week>.log`.

### 各環境行為 / Profile Behavior

| 環境 / Profile | 主控台 / Console | 檔案 / File | 目的 / Purpose |
|---|---|---|---|
| `dev` | ✅ | ✅ | 本地開發：終端可見並持久化到磁碟 / Local development: see logs in terminal and persist to disk |
| `prod` | ✅ | ✅ | 生產環境：雙重輸出，檔案為主要稽核來源 / Production: same dual output, files are the primary audit source |
| `test` | ✅ | ❌ | CI/單元測試：避免在建置代理上建立日誌目錄 / CI / unit tests: avoid creating log directories on build agents |

---

## 環境變數 / Environment Variables

| 變數 / Variable | 預設值 / Default | 說明 / Description |
|---|---|---|
| `LOG_PATH` | `./logs` | 目前日誌檔案及歸檔的存放根目錄 / Base directory for the active log file and its archives |

可以在執行時覆蓋路徑：

You can override the path at runtime:

```bash
# 本地執行 / Local run
java -DLOG_PATH=/var/log/JobCopilot -jar app.jar

# Docker 執行 / Docker run
docker run -e LOG_PATH=/app/logs my-backend-image
```

---

## 常見問題 / FAQ

**Q: 為什麼使用自然週（`yyyy-ww`）而不是固定 7 天間隔？**  
**Q: Why calendar week (`yyyy-ww`) instead of a fixed 7-day interval?**

A: Logback 根據 `fileNamePattern` 中的日期模式推斷滾動週期。`yyyy-ww` 是標準的一週滾動寫法，與週一至週日的自然邊界對齊，便於按日曆事件關聯日誌。

A: Logback infers the rolling period from the date pattern in `fileNamePattern`. `yyyy-ww` is the standard way to obtain weekly rotation. It aligns with natural Monday–Sunday boundaries, making it easier to correlate logs with calendar events.

**Q: 如果總大小不足 100 MB，日誌會遺失嗎？**  
**Q: Will I lose logs if the total size drops below 100 MB?**

A: 不會。`totalSizeCap` 是*上限*。只要 4 週歸檔合計不超過 100 MB，就會全部保留；僅當超過上限時，Logback 才會刪除最舊的歸檔。

A: No. `totalSizeCap` is an *upper* bound. As long as the 4 weekly archives are under 100 MB combined, all 4 are kept. Only when the cap is exceeded does Logback delete the oldest archive.

**Q: 可以在不修改 XML 的情況下調整日誌級別嗎？**  
**Q: Can I change the log level without touching XML?**

A: 可以。日誌級別仍由 `application-dev.yml`、`application-prod.yml` 或外部設定（如 `LOG_APP_LEVEL` 環境變數）中的 `logging.level.*` 控制。

A: Yes. Logger levels are still controlled by `logging.level.*` in `application-dev.yml`, `application-prod.yml`, or externalized config (e.g., `LOG_APP_LEVEL` environment variable).
