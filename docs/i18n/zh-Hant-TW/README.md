<!-- 語言切換器 -->
> [English](../../../README.md) | [簡體中文](../zh-Hans-CN/README.md) | [繁體中文](README.md)

# JobCopilot

**JobCopilot** 是一個面向應屆畢業生和職業轉型者的 AI 驅動平台。它能夠解析上傳的履歷，利用語義向量匹配將其與就業市場資料進行比對評估，並提供互動式 AI 助手，幫助使用者迭代最佳化履歷內容。安全的文件管理、非同步 AI 處理以及個人化推薦，幫助使用者節省時間並提高面試成功率。

**部署：** 系統已通過 Docker Compose 驗證。將 `.env.example` 複製為 `.env`，配置所需值後執行 `docker compose --env-file .env up -d --build`。前端默認可透過 `http://localhost` 存取；如果配置了自訂連接埠，則透過 `http://localhost:${FRONTEND_HOST_PORT}` 存取。

## 功能特性

- **身份認證**：郵箱/密碼註冊（支援可選的郵件驗證）以及 Google OAuth 2.0 登入，受滑塊 CAPTCHA 反爬蟲保護
- **履歷管理**：上傳、解析、版本管理和以多種格式匯出履歷
- **AI 智慧解析**：使用 LiteLLM 相容模型從履歷和職位描述中提取結構化資訊
- **職位匹配**：基於履歷內容與向量相似度的智慧職位推薦
- **增量式職位訓練閉環**：使用者評分行為透過增量學習反饋至 AI 基線模型，無需全量重訓練即可持續提升匹配準確度
- **申請追蹤**：追蹤求職申請狀態並管理求職流程
- **AI 對話**：互動式聊天助手，提供求職建議和履歷最佳化指導
- **國際化**：支援英文、簡體中文和繁體中文介面
- **向量搜尋**：基於 PostgreSQL pgvector 擴充套件的語義搜尋

## 系統架構

JobCopilot 採用容器化服務架構。預設情況下，只有前端容器面向主機暴露連接埠；後端、AI、資料、快取、訊息佇列和模型註冊表服務都透過 Docker 網路通訊。

```text
Browser
  |
  | HTTP :${FRONTEND_HOST_PORT:-80}
  v
Frontend container
  React 靜態應用 + Nginx 反向代理
  |
  | HTTP /api, /health
  v
Backend container
  Spring Boot API、認證、領域工作流、向量持久化
  |-- JDBC --------> PostgreSQL + pgvector
  |-- AMQP --------> RabbitMQ --------> AI worker container
  |-- HTTP --------> AI Service container
  |-- Redis -------> Redis
  `-- Local files -> shared upload volume

AI Service / AI worker
  |-- LiteLLM 相容提供商：解析、嵌入、排序、對話
  |-- 後端內部 API：向量寫入和基線特徵讀取
  |-- Redis：回饋緩衝、分散式鎖、模型重載 Pub/Sub
  `-- MinIO：LightGBM 模型產物註冊表
```

| 元件 | 技術 | 暴露方式 | 職責 |
|------|------|----------|------|
| 前端 / 閘道 | React 19、Vite 7、Nginx | 主機 `${FRONTEND_HOST_PORT:-80}` -> 容器 `8080` | 提供 UI，代理 `/api` 和 `/health` 到後端 |
| 後端 | Java 21、Spring Boot 3.5、DDD 模組 | 內部 `8080`；預設不直接暴露到主機 | REST API、認證、履歷/職位/申請工作流、向量持久化 |
| AI Service | Python 3.11、FastAPI、LiteLLM | 內部 `8000`；預設不直接暴露到主機 | 同步 AI 端點、嵌入、解析、排序、對話支援 |
| AI Worker | Python 3.11、RabbitMQ 消費者、LightGBM | 內部工作程序 | 非同步解析、職位排序任務、回饋採集、增量模型訓練 |
| PostgreSQL | PostgreSQL 15 + pgvector | `db-network` 內部 `5432` | 業務資料和向量儲存 |
| RabbitMQ | RabbitMQ 3 management 映像 | 內部 `5672`；管理連接埠預設關閉 | 後端與 AI 服務之間的持久化非同步任務傳輸 |
| Redis | Redis 7 | 內部 `6379` | CAPTCHA 狀態、分散式鎖、回饋緩衝、模型重載 Pub/Sub |
| MinIO | S3 相容物件儲存 | 內部 `9000` | AI Worker 的 LightGBM 模型產物註冊表 |

## 專案結構

```text
.
|-- backend/                   # Java / Spring Boot 後端
|   |-- api/                   # API DTO、命令、查詢和外觀介面
|   |-- app/                   # 應用服務、排程器和啟動裝配
|   |-- domain/                # 領域實體、值物件、連接埠和業務規則
|   |-- infrastructure/        # 持久化、儲存、訊息、安全和外部整合
|   |-- trigger/               # REST 控制器、WebSocket 端點、MQ 監聽器
|   |-- types/                 # 共享型別和常數
|   |-- scripts/               # 後端維護腳本
|   |-- Dockerfile             # 後端容器映像
|   `-- pom.xml                # Maven 多模組建置
|-- frontend/                  # React / Vite / TypeScript 前端
|   |-- src/                   # UI 原始碼
|   |   |-- components/        # 可重用 UI 元件
|   |   |-- pages/             # 路由頁面
|   |   |-- services/          # API 客戶端和服務封裝
|   |   |-- store/             # 客戶端狀態管理
|   |   |-- hooks/             # 共享 React Hooks
|   |   |-- i18n/              # 執行時國際化設定
|   |   `-- locales/           # 翻譯資源
|   |-- package.json           # Node.js 依賴和腳本
|   `-- Dockerfile             # 前端容器映像
|-- ai-service/                # Python / FastAPI AI 服務和 Worker
|   |-- app/
|   |   |-- api/               # FastAPI 端點
|   |   |-- domain/            # AI 領域邏輯和模型抽象
|   |   |-- infrastructure/    # 外部整合
|   |   |-- mq/                # 訊息整合
|   |   |-- services/          # AI 應用服務
|   |   `-- worker/            # 背景 Worker 入口
|   |-- tests/                 # Pytest 測試套件
|   |-- requirements.txt       # Python 依賴
|   `-- Dockerfile             # AI 服務容器映像
|-- docs/                      # ADR、API 文件、架構、部署和國際化
|-- eval/                      # AI 評估腳本、資料集和結果
|-- middleware/                # 自訂基礎設施映像，例如 PostgreSQL
|-- scripts/                   # 倉庫級自動化輔助腳本
|-- .github/                   # CI、Issue 模板、PR 模板、CODEOWNERS
|-- docker-compose.yml         # 本地 Docker Compose 堆疊
|-- .env.example               # 環境變數模板
`-- empty-vertex.json          # 非 Vertex 本地執行的占位憑證檔案
```

## 後端架構

後端採用**六邊形架構 / 領域驅動設計（DDD）**，包含以下分層模組：

| 模組            | 說明                           | 依賴              |
|-----------------|--------------------------------|-------------------|
| `types`         | 基礎型別、列舉、常數           | 無                |
| `domain`        | 領域實體、服務、倉儲介面       | `types`           |
| `api`           | DTO、外觀介面                   | `domain`、`types` |
| `infrastructure`| 資料庫、快取、訊息佇列實作       | `domain`、`api`   |
| `trigger`       | 控制器、排程器、事件監聽器       | `domain`、`api`   |
| `app`           | Spring Boot 啟動和配置           | 所有模組          |

## 快速開始

### 前置要求

- Docker 20.10+ 和 Docker Compose 2.0+
- 或帶 podman-compose 的 Podman
- 一個 LiteLLM 相容的 AI 服務商金鑰用於本地 AI 功能，例如 Gemini、OpenAI 或 Anthropic
- Google Cloud / Vertex AI 為可選項，本地開發不需要強制配置

### 1. 複製倉庫

```bash
git clone <倉庫地址>
cd JobCopilot
```

### 2. 配置環境變數

**推薦：使用 Web 配置器**

直接在瀏覽器中開啟 `docs/deployment/env-setup.html`（無需伺服器）：
1. 選擇語言並填寫必填欄位（帶 * 標記）
2. 點選金鑰欄位旁的**生成**按鈕以生成安全的隨機值
3. 點選**下載 .env**並儲存到專案根目錄

**替代方案（命令列）：**
```bash
cp .env.example .env
# 編輯 .env 並填寫所需值
```

關鍵環境變數：

| 變數                         | 必需      | 說明                                                              |
|------------------------------|-----------|-------------------------------------------------------------------|
| `JWT_SECRET`                 | 是        | JWT 令牌生成的金鑰（至少 32 個字元）                              |
| `VITE_GOOGLE_CLIENT_ID`      | 是        | 前端登入流程使用的 Google OAuth 2.0 客戶端 ID                      |
| `INTERNAL_API_KEY`           | 推薦      | 後端呼叫 AI 服務的共享金鑰                                        |
| `GEMINI_API_KEY`             | 有條件    | 當 `LLM_*_MODEL` 使用 `gemini/` 字首時使用的 Gemini API 金鑰       |
| `OPENAI_API_KEY`             | 有條件    | 當 `LLM_*_MODEL` 使用 `openai/` 字首時使用的 OpenAI Service 金鑰     |
| `ANTHROPIC_API_KEY`          | 有條件    | 當 `LLM_*_MODEL` 使用 `anthropic/` 字首時使用的 Anthropic API 金鑰 |
| `LLM_TEXT_MODEL`             | 否        | LiteLLM 文字模型名稱；Compose 中預設為 Gemini 模型               |
| `LLM_VISION_MODEL`           | 否        | LiteLLM 視覺模型名稱                                             |
| `LLM_EMBEDDING_MODEL`        | 否        | LiteLLM 嵌入模型名稱                                             |
| `LLM_EMBEDDING_MODEL_DIMENSION` | 否     | 嵌入輸出維度（必須與模型匹配）                                    |
| `SPRING_PROFILES_ACTIVE`     | 否        | Spring 配置檔案：`dev`（預設）或 `prod`                          |
| `LOG_LEVEL`                  | 否        | AI 服務日誌級別：`INFO`（預設）或 `DEBUG`                        |
| `CAPTCHA_ENABLED`            | 否        | 啟用滑塊 CAPTCHA。預設：`true`                                   |
| `CAPTCHA_TOLERANCE`          | 否        | CAPTCHA 拖動容差（畫素）。預設：`8`                              |
| `CAPTCHA_TOKEN_EXPIRY`       | 否        | CAPTCHA 令牌過期時間（秒）。預設：`300`                          |
| `CAPTCHA_TRACK_WIDTH`        | 否        | CAPTCHA 軌道寬度（畫素）。預設：`300`                            |
| `REDIS_HOST`                 | 否        | Redis 主機名。預設：`redis`（Docker）或 `localhost`               |
| `REDIS_PORT`                 | 否        | Redis 埠。預設：`6379`                                         |
| `REDIS_PASSWORD`             | 是        | Compose 使用的 Redis 認證密碼；部署前請更改本地預設值              |
| `MINIO_ENDPOINT`             | 否        | AI 模型註冊表使用的內部 MinIO 端點                                 |
| `MINIO_ACCESS_KEY`           | 是        | AI 模型註冊表使用的 MinIO 存取金鑰                                 |
| `MINIO_SECRET_KEY`           | 是        | AI 模型註冊表使用的 MinIO 金鑰                                     |
| `MINIO_MODEL_BUCKET`         | 否        | 用於儲存訓練模型產物的 MinIO 儲存桶                                |
| `CAPTCHA_MAX_ATTEMPTS`       | 否        | 每 IP 最大 CAPTCHA 嘗試次數。預設：`5`                           |

本地開發時，將 `.env.example` 複製為 `.env`，並提供與您選擇的 LiteLLM 模型字首匹配的 API 金鑰。例如，預設 Gemini 模型使用 `GEMINI_API_KEY`。

僅在您有意將專案配置為使用 Vertex AI 模型時才需要 Google Cloud ADC。
注意：提供的 `docker-compose.yml` 預設在後端以本地檔案模式執行履歷儲存。`.env.example` 中的 MinIO、S3 和 OSS 設定用於自定義部署。
注意：如果在系統執行時更改 `.env` 中的 LLM 提供商、模型、維度或前端構建時變數，請執行 `docker compose --env-file .env up -d --build` 以重建/重新建立受影響的容器。簡單重啟不會套用所有更改。


### 3. 啟動核心服務

使用 Docker Compose：
```bash
docker compose --env-file .env up -d --build
```

如果環境仍使用舊版 Compose CLI，請使用：
```bash
docker-compose --env-file .env up -d --build
```

使用 Podman：
```bash
podman-compose up -d
# 或
podman compose up -d
```

本地開發時，`.env` 由 Docker Compose 和本地 AI 服務程序載入。AI 服務使用 LiteLLM，因此請提供與配置的模型提供商匹配的金鑰，例如預設 Gemini 模型的 `GEMINI_API_KEY`。

如果您在 Docker 外本地執行 AI 服務，請在啟動 Uvicorn 前載入根目錄 `.env`，以便 RabbitMQ 和 LLM 設定與 Compose 環境匹配。


### 4. 驗證服務

| 服務        | 地址                        | 說明                          |
|-------------|-----------------------------|-------------------------------|
| 前端介面    | http://localhost            | 主入口（React 應用）            |
| 後端 API    | http://localhost/api        | 透過 Nginx 的 REST 端點       |
| 系統健康    | http://localhost/health     | 全域健康檢查                   |

*注意：在三層網路架構中，僅將配置的前端連接埠暴露給主機。後端、AI、RabbitMQ、Redis 和資料庫服務透過 Docker 網路安全隔離。*

*注意：如果在 `.env` 中更改了 `FRONTEND_HOST_PORT`，請將 `http://localhost` 替換為 `http://localhost:${FRONTEND_HOST_PORT}`。*

### 5. 停止服務

```bash
docker compose --env-file .env down

# 刪除資料卷（警告：資料將遺失）
docker compose --env-file .env down -v
```

## 測試

本專案包含後端 JUnit 測試、前端 Vitest 測試和 AI 服務 pytest 測試，覆蓋 API、領域、持久化、認證、UI 工具、AI 服務和訊息佇列邏輯。

### 後端測試（Java）

執行 Spring Boot 後端的單元和整合測試：

```bash
cd backend
mvn test
```

部分後端整合測試使用 Testcontainers，需要可用的 Docker 環境。

### 前端測試（TypeScript）

執行 lint、單元測試、覆蓋率檢查和生產構建檢查：

```bash
cd frontend
npm install
npm run lint
npm run test:run
npm run test:coverage
npm run build
```

### AI 服務測試（Python）

執行 FastAPI 和 AI 邏輯的 pytest：

```bash
cd ai-service
pip install -r requirements.txt
pytest
```

## AI 評估套件

針對固定基準用例評估 AI 元件：

```bash
cd eval
# 安裝評估依賴
pip install -r requirements.txt
# 先載入根目錄 .env，以便 LiteLLM 提供商設定可用
# 執行當前評估流水線：
# 履歷解析、職位解析和單職位適合度評分
python run_eval.py

# 可選：同時執行舊版職位排序 NDCG@5 評估
python run_eval.py --include-legacy-ranking
```

*評估結果將匯出至 `eval/results/`。*

## 開發指南

### 前端開發

```bash
cd frontend
npm install
npm run dev
```

開發伺服器將在 <http://localhost:5173> 啟動。

### 後端開發

前置要求：

- JDK 21
- Maven 3.9+

```bash
cd backend

# 建置所有模組
mvn clean install

# 使用 dev 配置檔案執行（預設）
mvn spring-boot:run -pl app

# 使用 prod 配置檔案執行
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=prod
```

### AI 服務開發

前置要求：

- Python 3.11+
- pip 或 poetry
- 在專案根目錄 `.env` 檔案中配置的 LiteLLM 相容提供商金鑰

```bash
cd ai-service

# 建立虛擬環境
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate

# 安裝依賴
pip install -r requirements.txt

# 載入根目錄環境變數
set -a
source ../.env
set +a

# 執行開發伺服器
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

除非您有意將 LiteLLM 配置為使用 Vertex AI 模型，否則本地開發不需要 Google Cloud ADC。


## 部署

詳細部署說明請參見 [../../deployment/DOCKER_DEPLOY.md](../../deployment/DOCKER_DEPLOY.md)，內容包括：

- 生產環境部署檢查清單
- 環境配置
- SSL/TLS 設定
- 監控和日誌
- 備份策略

## 技術棧

### 前端

- React 19
- Vite 7
- TypeScript 5.9
- React Router 7
- Axios
- Zustand

### 後端

- Java 21
- Spring Boot 3.5.7
- PostgreSQL 15 + pgvector
- RabbitMQ 3
- Maven 3.9+

### AI 服務

- Python 3.11
- FastAPI 0.115
- LiteLLM 1.61.11 相容的文字、視覺和嵌入模型
- 預設透過 Google AI Studio 使用 Gemini；Vertex AI 為可選項
- Uvicorn 0.32

### 維運

- Docker & Docker Compose
- Nginx
- Redis 7（分散式狀態、鎖、Pub/Sub）
- Flyway（資料庫遷移）

## 貢獻指南

1. Fork 本倉庫
2. 建立功能分支（`git checkout -b feature/amazing-feature`）
3. 提交更改（`git commit -m '新增功能'`）
4. 推送到分支（`git push origin feature/amazing-feature`）
5. 建立 Pull Request

## 授權條款

本專案基於 MIT 授權條款發布。詳情請參見 [LICENSE](../../../LICENSE)。

## 致謝

感謝支持 JobCopilot 的開源專案、貢獻者和使用者。
