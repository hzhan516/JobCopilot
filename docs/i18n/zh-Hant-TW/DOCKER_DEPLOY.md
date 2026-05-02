<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../DOCKER_DEPLOY.md) | [简体中文](../zh-Hans-CN/DOCKER_DEPLOY.md) | [繁體中文](DOCKER_DEPLOY.md)

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

預設情況下，專案可以透過 LiteLLM 使用 Gemini 模型，因此本地開發只需要設定 `GEMINI_API_KEY`，除非您選擇其他模型服務。

Google Cloud ADC 是選用項。只有在您主動將 LiteLLM 設定為使用 Vertex AI 模型時才需要。


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

| 服務          | 位址                                   | 說明          |
|-------------|--------------------------------------|-------------|
| 前端介面        | http://localhost                     | 求職者介面       |
| 後端 API      | http://localhost:8080/api            | REST API    |
| AI 服務       | http://localhost:8000                | FastAPI 文件  |
| RabbitMQ 管理 | http://localhost:15672               | guest/guest |
| H2 控制台      | http://localhost:8080/api/h2-console | 開發環境        |

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
   docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
   ```

3. 設定反向代理（Nginx/Traefik）

4. 啟用 HTTPS（Let's Encrypt）

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
