<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../deployment/DOCKER_DEPLOY.md) | [简体中文](../zh-Hans-CN/deployment/DOCKER_DEPLOY.md) | [繁體中文](DOCKER_DEPLOY.md)

# Docker/Podman 部署指南

## 環境要求

- **Docker**: 20.10+ 或 **Podman**: 4.0+
- **Docker Compose**: 2.0+ 或 **podman-compose**: 1.0+
- 記憶體：至少 4GB 可用記憶體
- 磁碟：至少 10GB 可用空間

## 快速開始

### 1. 設定環境變數

```bash
# 複製環境變數範本
cp .env.example .env

# 編輯 .env 檔案，填入必要的設定
vim .env
```

必要設定項目：

- `JWT_SECRET`: JWT 簽名金鑰（生產環境必須修改）
- 一個相容 LiteLLM 的模型服務金鑰，例如 `GEMINI_API_KEY`、`OPENAI_API_KEY`、`ANTHROPIC_API_KEY` 或 `GROQ_API_KEY`
- `LLM_TEXT_MODEL`、`LLM_VISION_MODEL` 和 `LLM_EMBEDDING_MODEL`: 與所選模型服務前綴匹配的模型名稱
- `LLM_EMBEDDING_MODEL_DIMENSION`: 嵌入模型輸出維度（必須與所選模型一致，預設 1536）
- `CAPTCHA_ENABLED`: 是否啟用 CAPTCHA 驗證（`true`/`false`，預設 `true`）
- `CAPTCHA_TOLERANCE`: 滑動容差像素（預設 `8`）
- `CAPTCHA_MAX_ATTEMPTS`: 每個挑戰最大驗證次數（預設 `5`）
- `CAPTCHA_TOKEN_EXPIRY`: Token 快取 TTL，單位秒（預設 `300`）

預設情況下，專案可以透過 LiteLLM 使用 Gemini 模型，因此本地開發只需要設定 `GEMINI_API_KEY`，除非您選擇其他模型服務。

Google Cloud ADC 是選用項。只有在您主動將 LiteLLM 設定為使用 Vertex AI 模型時才需要。
註：如果您在系統運行時修改了 .env 中的 LLM 提供商、模型或維度，您必須執行 "docker compose up -d" 以應用新的環境變數。單純的重啟服務將不會生效。


### 2. 啟動服務

#### 使用 Docker

```bash
# 建置並啟動所有服務
docker-compose up -d

# 檢視日誌
docker-compose logs -f

# 停止服務
docker-compose down

# 停止並刪除資料卷（謹慎使用）
docker-compose down -v
```

#### 使用 Podman

```bash
# 方法 1: 使用 podman-compose
podman-compose up -d

# 方法 2: 使用 podman 原生 compose（Podman 3.0+）
podman compose up -d

# 檢視日誌
podman-compose logs -f

# 停止服務
podman-compose down
```

### 3. 驗證服務狀態

```bash
# 檢視所有容器狀態
docker-compose ps
# 或
podman-compose ps

# 健康檢查
curl http://localhost:8080/api/actuator/health
curl http://localhost:8000/health
curl http://localhost/health
```

## 服務存取位址

| 服務     | 位址               | 說明                      |
|--------|------------------|-------------------------|
| 前端介面   | http://localhost | 統一入口 (包含 Web 與 API 代理) |
| 系統健康   | http://localhost/health | 整體服務狀態                |

## 常用指令

### 檢視日誌

```bash
# 所有服務日誌
docker-compose logs -f

# 特定服務日誌
docker-compose logs -f backend
docker-compose logs -f ai-service
docker-compose logs -f postgres
```

### 重新啟動服務

```bash
# 重新啟動所有服務
docker-compose restart

# 重新啟動特定服務
docker-compose restart backend
```

### 重建服務

```bash
# 重新建置並啟動（程式碼更新後）
docker-compose up -d --build

# 僅重建特定服務
docker-compose up -d --build backend
```

### 進入容器

```bash
# 進入後端容器
docker-compose exec backend sh

# 進入資料庫容器
docker-compose exec postgres psql -U resume_user -d resume_assistant
```

## 資料持久化

資料透過 Docker 卷持久化儲存：

- `postgres-data`: PostgreSQL 資料庫資料
- `rabbitmq-data`: RabbitMQ 訊息佇列資料
- `shared-storage`: 上傳的履歷檔案（後端和 AI 服務共享）

```bash
# 檢視卷
docker volume ls

# 備份資料（PostgreSQL）
docker-compose exec postgres pg_dump -U resume_user resume_assistant > backup.sql

# 還原資料
docker-compose exec -T postgres psql -U resume_user resume_assistant < backup.sql
```

## 故障排查

### 連接埠衝突

如果啟動時報連接埠衝突錯誤，修改 `docker-compose.yml` 中的連接埠對應：

```yaml
ports:
  - "8081:8080"  # 將主機連接埠改為 8081
```

### 記憶體不足

如果容器頻繁重新啟動，可能是記憶體不足：

```bash
# 檢視容器資源使用
docker stats

# 增加 Docker 記憶體限制（Docker Desktop）
```

### 清理建置快取

```bash
# 清理未使用的映像檔、容器、卷
docker system prune -a --volumes

# 重新建置
docker-compose up -d --build --force-recreate
```

### CAPTCHA 速率限制觸發

**現象**：請求 CAPTCHA 挑戰時回傳 `429 Too Many Requests`。

**原因**：同一 IP 在 1 分鐘內超過 20 次 CAPTCHA 請求。

**解決**：等待 1 分鐘讓速率限制快取過期，或在 `.env` 中設定 `CAPTCHA_ENABLED=false` 以在本機測試時停用 CAPTCHA。

### 406 PRECONDITION_FAILED（RabbitMQ）

**現象**：後端日誌出現 `channel.close(reply-code=406, reply-text=PRECONDITION_FAILED - inequivalent arg 'x-dead-letter-exchange'...)`。

**原因**：RabbitMQ 中已存在同名佇列，但參數不同。佇列參數不可變。

**解決**：執行佇列重置腳本並重新啟動後端：
```bash
cd backend && ./scripts/reset-rabbitmq-queues.sh
cd .. && docker-compose up -d --build backend
```

或徹底重建 RabbitMQ（刪除卷）：
```bash
docker-compose down -v
docker-compose up -d
```

### "Relation does not exist"（PostgreSQL）

**現象**：後端日誌出現 `ERROR: relation "outbox_message" does not exist`。

**原因**：`outbox_message` 表在資料庫初始化完成後才新增。`docker-entrypoint-initdb.d` 僅在首次初始化時執行。

**解決**：手動執行初始化 SQL：
```bash
docker exec -i resume-assistant-postgres \
  psql -U resume_user -d resume_assistant \
  < backend/app/src/main/resources/db/migration/init_outbox_message.sql
```

或重建資料庫卷：
```bash
docker-compose down -v
docker-compose up -d
```

### `FATAL: database "resume_assistant" does not exist`（PostgreSQL）

**現象**：後端或 PostgreSQL 健康檢查報錯 `database "resume_assistant" does not exist` 或 `role "resume_user" does not exist`。

**原因**：`postgres-data` 資料卷已被初始化過（例如使用預設的 `postgres` 憑證或來自之前的專案）。PostgreSQL 的 `docker-entrypoint-initdb.d` 以及 `POSTGRES_USER`/`POSTGRES_DB` 環境變數僅在資料目錄為空時的**首次**初始化生效。

**解決**：刪除資料卷並重新初始化：
```bash
docker-compose down
docker volume rm <project_name>_postgres-data
docker-compose up -d
```

## 生產環境部署

1. 修改 `.env` 檔案：
   ```
   SPRING_PROFILES_ACTIVE=prod
   JWT_SECRET=<強金鑰>
   POSTGRES_PASSWORD=<強密碼>
   RABBITMQ_PASS=<強密碼>
   ```

2. 使用生產環境設定：
   ```bash
   docker-compose -f docker-compose.yml.example -f docker-compose.prod.yml up -d
   ```

3. 設定反向代理（Nginx/Traefik）

4. 啟用 HTTPS（Let's Encrypt）

## 佇列參數變更與重置

RabbitMQ 佇列一旦建立，其參數不可變。如果應用程式程式碼變更了佇列宣告（例如加入 `x-dead-letter-exchange` 以支援 DLX/DLQ），Spring AMQP 在嘗試重新宣告已有佇列時會回報 `406 PRECONDITION_FAILED` 錯誤。

### 開發環境

刪除舊佇列，讓 Spring AMQP 在下次啟動時自動重新宣告：

```bash
cd backend
./scripts/reset-rabbitmq-queues.sh
```

然後重新啟動後端容器：

```bash
docker-compose up -d --build backend
```

### 生產環境

建議安排在維護時段執行：
1. 停止生產者（backend），防止新訊息進入。
2. 排空或備份受影響佇列中的訊息。
3. 透過 RabbitMQ Management UI 或 CLI 刪除舊佇列。
4. 重新啟動後端服務，Spring AMQP 將使用新參數宣告佇列。

如果訊息遺失可接受，也可以直接使用 `reset-rabbitmq-queues.sh` 腳本。

## 資料庫初始化與遷移

開發環境停用了 Flyway（`application-dev.yml` 中 `spring.flyway.enabled=false`）。資料庫初始化依賴於 `docker-entrypoint-initdb.d`，該機制**僅在資料庫首次初始化**（資料目錄為空）時執行一次。

### 全新環境

使用全新的 `postgres-data` 卷啟動時，`docker-entrypoint-initdb.d` 會按字母順序執行 `backend/app/src/main/resources/db/migration/` 目錄下的所有 `.sql` 檔案。新增的資料表（如 `outbox_message`）會自動建立。

### 已有環境

如果資料庫已經初始化過，新增 `.sql` 檔案**不會**自動執行。有兩種處理方式：

1. **手動執行 SQL**（推薦用於有資料的開發環境）：
   ```bash
   docker exec -i resume-assistant-postgres \
     psql -U resume_user -d resume_assistant \
     < backend/app/src/main/resources/db/migration/init_outbox_message.sql
   ```

2. **重建資料卷**（會清空所有資料）：
   ```bash
   docker-compose down -v
   docker-compose up -d
   ```

> **生產環境說明**：生產環境使用 `spring.flyway.enabled=true`（見 `application-prod.yml`）。Flyway 會在啟動時自動套用待執行的遷移腳本，因此新增表會自動處理。

## 注意事項

1. **Podman 使用者**：
    - 確保安裝 `podman-compose`
    - 或使用 `podman compose`（Podman 3.0+ 原生支援）
    - 如果遇到權限問題，檢查 SELinux 設定

2. **Windows 使用者**：
    - 使用 WSL2 執行 Docker
    - 檔案共享效能可能較慢

3. **Mac 使用者**：
    - Docker Desktop 記憶體預設 2GB，建議增加到 4GB+
