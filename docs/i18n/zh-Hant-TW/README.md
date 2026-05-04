<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../README.md) | [简体中文](../zh-Hans-CN/README.md) | [繁體中文](README.md)

# 智慧求職助手 (Resume Assistant)

**智慧求職助手**是一個由 AI 驅動的平台，旨在為應屆畢業生和轉職者簡化求職流程。它能夠自動解析使用者上傳的履歷，使用語義向量匹配技術評估履歷與就業市場資料的契合度，並提供互動式的 AI 助手來迭代優化履歷內容。透過結合安全的檔案管理、非同步 AI 處理和個人化推薦，該系統為使用者節省了數小時的手動修改時間，同時顯著提高了面試機會。

**部署位址：** [待定 - 將在實作完成後更新]

## 團隊成員

- **Guixing Jia** (@GuixingJia) - 專案經理 (PM)，Python AI 服務 & 前端開發
- **Hansheng Zhang** (@hzhan516) - Java 後端 & 資料庫架構負責人
- **Mu-Hsi Yu** (@muhsiyu) - 前端 & UX 設計負責人，Python AI 服務

## 功能特性

- **履歷管理**：上傳、解析和管理多種格式的履歷
- **AI 智慧解析**：使用 Vertex AI Gemini 從履歷和職位中提取結構化資訊
- **職位匹配**：基於履歷內容和向量相似度的智慧職位推薦
- **申請追蹤**：追蹤求職申請狀態並管理求職流程
- **AI 對話**：互動式聊天助手，提供求職建議和履歷優化指導
- **向量搜尋**：基於 PostgreSQL pgvector 擴充功能的語義搜尋

## 系統架構

本專案採用微服務架構，包含以下元件：

```text
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│     前端    │──────▶│     後端    │◀─────▶│    AI      │
│   (React)   │      │ (Spring    │      │ (FastAPI)  │
│             │      │   Boot)     │      │            │
└─────────────┘      └──────┬──────┘      └──────▲──────┘
                            │                      │
                            ▼                      │
                     ┌─────────────┐               │
                     │  PostgreSQL │               │
                     │  + pgvector │───────────────┘
                     └─────────────┘      (訊息佇列)
                            ▲
                            │
                     ┌─────────────┐
                     │   RabbitMQ  │
                     └─────────────┘
```

| 服務   | 技術棧                       | 連接埠           | 說明            |
|------|---------------------------|--------------|---------------|
| 前端   | React 18 + Vite           | 80           | Nginx 託管的 Web 介面 |
| 後端   | Java 21 + Spring Boot 3.5 | 8080         | REST API 和業務邏輯 |
| AI 服務 | Python 3 + FastAPI        | 8000         | AI 處理與 Vertex AI Gemini 整合 |
| 資料庫  | PostgreSQL 15 + pgvector  | 5432         | 業務資料和向量儲存     |
| 訊息佇列 | RabbitMQ 3                | 5672 / 15672 | 非同步訊息處理        |

## 專案結構

```text
.
├── frontend/              # React 前端應用
│   ├── src/              # 原始碼
│   ├── package.json      # Node.js 依賴
│   └── Dockerfile        # 前端 Docker 映像檔
├── backend/              # Java Spring Boot 後端
│   ├── app/              # 應用程式入口
│   ├── api/              # API 層（DTO、外觀介面）
│   ├── domain/           # 領域層（業務邏輯）
│   ├── infrastructure/   # 基礎設施層（資料庫、快取、訊息佇列）
│   ├── trigger/          # 觸發器層（控制器、定時任務、監聽器）
│   └── types/            # 共享類型和常數
├── ai-service/           # Python AI 服務
│   ├── app/              # FastAPI 應用
│   ├── requirements.txt  # Python 依賴
│   └── Dockerfile        # AI 服務 Docker 映像檔
├── docs/                 # 專案文件
├── eval/                 # AI 評估腳本
├── tests/                # 測試腳本
├── docker-compose.yml    # Docker Compose 設定
└── .env.example          # 環境變數範本
```

## 後端架構

後端採用**六邊形架構 / 領域驅動設計（DDD）**，包含以下分層模組：

| 模組               | 說明               | 依賴               |
|------------------|------------------|------------------|
| `types`          | 基礎類型、列舉、常數       | 無                |
| `domain`         | 領域實體、服務、倉儲介面     | `types`          |
| `api`            | DTO、外觀介面         | `domain`、`types` |
| `infrastructure` | 資料庫、快取、訊息佇列實作    | `domain`、`api`   |
| `trigger`        | 控制器、定時任務、事件監聽器   | `domain`、`api`   |
| `app`            | Spring Boot 啟動和設定 | 所有模組             |

## 快速開始

### 環境要求

- Docker 20.10+ 和 Docker Compose 2.0+
- 或帶有 podman-compose 的 Podman
- 一個用於本地 AI 功能的 LiteLLM 相容模型服務金鑰，例如 Gemini、OpenAI、Anthropic 或 Groq
- Google Cloud / Vertex AI 是選用項，本地開發不需要強制設定


### 1. 複製倉庫

```bash
git clone <倉庫位址>
cd resume-assistant
```

### 2. 設定環境變數

```bash
cp .env.example .env
# 編輯 .env 並填入必要值
```

必要的環境變數：

| 變數                     | 必需 | 說明 |
|--------------------------|------|------|
| `JWT_SECRET`             | 是   | JWT token 產生金鑰（至少 32 個字元） |
| `GEMINI_API_KEY`         | 選用 | 當 `LLM_*_MODEL` 使用 `gemini/` 前綴時使用的 Gemini API 金鑰 |
| `OPENAI_API_KEY`         | 選用 | 當 `LLM_*_MODEL` 使用 `openai/` 前綴時使用的 OpenAI API 金鑰 |
| `ANTHROPIC_API_KEY`      | 選用 | 當 `LLM_*_MODEL` 使用 `anthropic/` 前綴時使用的 Anthropic API 金鑰 |
| `GROQ_API_KEY`           | 選用 | 當 `LLM_*_MODEL` 使用 `groq/` 前綴時使用的 Groq API 金鑰 |
| `LLM_TEXT_MODEL`         | 是   | LiteLLM 文字模型名稱，例如 `gemini/gemini-2.5-flash` |
| `LLM_VISION_MODEL`       | 是   | LiteLLM 視覺模型名稱 |
| `LLM_EMBEDDING_MODEL_DIMENSION` | 是   | 嵌入模型輸出維度（必須與所選模型一致） |
| `LLM_EMBEDDING_MODEL`    | 是   | LiteLLM 嵌入模型名稱 |
| `SPRING_PROFILES_ACTIVE` | 否   | Spring profile：`dev`（預設）或 `prod` |
| `LOG_LEVEL`              | 否   | AI service 日誌等級：`INFO`（預設）或 `DEBUG` |

本地開發時，請將 `.env.example` 複製為 `.env`，並提供一個與所選 LiteLLM 模型前綴匹配的 API key。例如，預設 Gemini 模型使用 `GEMINI_API_KEY`。

只有在您主動將專案設定為使用 Vertex AI 模型時，才需要 Google Cloud ADC。


### 3. 啟動核心服務

使用 Docker Compose：

```bash
docker-compose up -d
```

使用 Podman：

```bash
podman-compose up -d
# 或
podman compose up -d
```

對於本地開發，Docker Compose 和本地 AI 服務程序都會讀取 `.env`。AI 服務使用 LiteLLM，因此請提供一個與所選模型服務匹配的 API key，例如預設 Gemini 模型使用 `GEMINI_API_KEY`。

如果您選擇在本機執行 AI 服務，而不是在 Docker 中執行，請在啟動 Uvicorn 前載入專案根目錄的 `.env`，以確保 RabbitMQ 和 LLM 設定與 Compose 環境一致。


### 4. 驗證服務

| 服務             | URL                                   | 說明                    |
|---------------------|---------------------------------------|--------------------------------|
| 前端            | http://localhost                      | Web 應用                |
| 後端 API         | http://localhost:8080/api             | REST API 端點             |
| 後端健康檢查      | http://localhost:8080/actuator/health | 健康檢查                   |
| AI 服務          | http://localhost:8000                 | FastAPI 文件 / 健康檢查 |
| RabbitMQ 管理 | http://localhost:15672                | 訊息佇列 UI（guest/guest） |

### 5. 停止服務

```bash
docker-compose down

# 移除卷（警告：資料將遺失）
docker-compose down -v
```

## 測試

本專案在所有模組中維護了嚴格的測試套件（程式碼覆蓋率 `> 80%`），以確保系統可靠性。

### 後端測試 (Java)

執行 Spring Boot 後端的單元和整合測試：

```bash
cd backend
mvn test
```

### AI 服務測試 (Python)

執行 FastAPI 和 AI 邏輯的 pytest 測試：

```bash
cd ai-service
pip install -r requirements.txt
pytest
```

## AI 評估套件

為了評估 AI 元件（擷取 F1 分數和推薦 NDCG@5）與基準模型的對比表現：

```bash
cd eval
# 安裝評估依賴
pip install -r requirements.txt
# 執行評估流水線
python run_eval.py
```

*評估結果將匯出至 `eval/results/` 目錄。*

## 開發指南

### 前端開發

```bash
cd frontend
npm install
npm run dev
```

開發伺服器將在 <http://localhost:5173> 啟動。

### 後端開發

環境要求：

- JDK 21
- Maven 3.9+

```bash
cd backend

# 建置所有模組
mvn clean install

# 使用 dev 設定檔執行（預設）
mvn spring-boot:run -pl app

# 使用 prod 設定檔執行
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=prod
```

### AI 服務開發

環境要求：

- Python 3.11+
- pip 或 poetry
- 已在專案根目錄 `.env` 檔案中設定相容 LiteLLM 的模型服務金鑰

```bash
cd ai-service

# 建立虛擬環境
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate

# 安裝依賴
pip install -r requirements.txt

# 載入專案根目錄環境變數
set -a
source ../.env
set +a

# 執行開發伺服器
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

本地開發不需要 Google Cloud ADC，除非您主動將 LiteLLM 設定為使用 Vertex AI 模型。



## 部署上線

詳細的部署說明請參考 [deployment/DOCKER_DEPLOY.md](deployment/DOCKER_DEPLOY.md)，內容包括：

- 生產環境部署檢查清單
- 環境變數設定
- SSL/TLS 設定
- 監控與日誌記錄
- 資料備份策略

## 技術棧

### 前端

- React 18.2
- Vite 5.0
- React Router 6
- Axios

### 後端

- Java 21
- Spring Boot 3.5.7
- PostgreSQL 15 + pgvector
- RabbitMQ 3
- Maven 3.9+

### AI 服務

- Python 3.11
- FastAPI
- Google Vertex AI Gemini
- Google Vertex AI embeddings
- Uvicorn

### 維運

- Docker & Docker Compose
- Nginx
- Flyway（資料庫遷移）

## 貢獻指南

1. Fork 本倉庫
2. 建立功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交變更 (`git commit -m '新增功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 建立 Pull Request

## 授權條款

本專案為亞利桑那州立大學（Arizona State University, SER594 課程）學術目的開發。

## 致謝

- 亞利桑那州立大學
- SER594 課程教學團隊
