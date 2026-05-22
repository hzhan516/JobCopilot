<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../architecture/Architecture.md) | [简体中文](../../zh-Hans-CN/architecture/Architecture.md) | [繁體中文](Architecture.md)

# 智慧求職助手 - 架構設計文件


---

## 1. 文件資訊

| 專案       | 內容                                 |
|----------|------------------------------------|
| **文件標題** | 智慧求職助手 - 架構設計文件                    |
| **版本**   | 1.0                                |
| **日期**   | 2025年1月                            |
| **作者**   | SER 594 課程專案組                      |
| **專案**   | 智慧求職助手 (Intelligent Job Assistant) |
| **狀態**   | 草案                                 |

### 版本歷史

| 版本  | 日期      | 作者          | 變更說明 |
|-----|---------|-------------|------|
| 1.0 | 2025-01 | SER 594 專案組 | 初始版本 |

---

## 2. 架構概述

### 2.1 系統簡介

智慧求職助手是一個基於人工智慧的求職輔助平臺，旨在透過先進的AI技術幫助求職者最佳化求職流程。系統整合了履歷智慧剖析、職位智慧匹配、對話式履歷最佳化和求職進度追蹤四大核心功能。

### 2.2 設計目標

| 目標       | 描述               | 優先順序 |
|----------|------------------|-----|
| **可擴充性** | 支援未來功能擴充和使用者增長    | 高   |
| **可維護性** | 清晰的模組劃分，便於維護和迭代  | 高   |
| **效能**   | 回應時間 < 2秒，支援併發使用者 | 高   |
| **可靠性**  | 服務可用性 > 99.5%    | 中   |
| **安全性**  | 資料加密，存取控制        | 高   |

### 2.3 架構原則

1. **領域驅動設計 (DDD)** - 清晰的領域邊界，業務邏輯內聚
2. **統一儲存** - PostgreSQL + pgvector 同時儲存業務資料和向量資料
3. **資料隔離** - AI服務不直接存取資料庫，透過訊息佇列通訊
4. **預處理架構** - 履歷上傳時即生成摘要和向量
5. **按需檢索** - AI服務需要向量時透過訊息佇列向Java後端請求

### 2.4 技術棧概覽

| 層級       | 技術                                   | 說明                |
|----------|--------------------------------------|-------------------|
| **前端**   | React 19 + TypeScript + Tailwind CSS | 求職者互動介面           |
| **後端**   | Java Spring Boot 3.x + DDD           | 業務邏輯、資料管理、訊息佇列    |
| **AI API** | Python FastAPI + LiteLLM             | 履歷剖析、匹配計算、對話處理    |
| **AI Worker** | Python LightGBM                   | 用於增量模型訓練的背景工作程式 |
| **資料庫**  | PostgreSQL 15 + pgvector             | 業務資料 + 向量資料（統一儲存） |
| **訊息佇列** | RabbitMQ                             | 非同步服務通訊            |
| **快取**   | Redis 7                              | 分散式狀態、鎖、Pub/Sub |
| **模型註冊表** | MinIO                              | 儲存已訓練的 LightGBM 模型 |
| **部署**   | Docker Compose                       | 容器化本地部署           |

---

## 3. 系統架構

### 3.1 高層架構圖

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端層 (React)                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ 履歷頁面     │  │ 職位頁面     │  │ 對話頁面     │  │ 追蹤頁面     │     │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘     │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │ HTTPS / REST
┌───────────────────────────────────▼─────────────────────────────────────────┐
│                          Java後端服務 (DDD)                                   │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                        API閘道器層 (Controller)                           │ │
│  │  ResumeController | JobController | ChatController | TrackController   │ │
│  └──────────────────────────────────┬─────────────────────────────────────┘ │
│                                     │                                       │
│  ┌──────────────────────────────────▼─────────────────────────────────────┐ │
│  │                        應用服務層                                        │ │
│  │  ResumeAppService | JobAppService | ChatAppService | TrackAppService | CaptchaAppService │ │
│  └──────────────────────────────────┬─────────────────────────────────────┘ │
│                                     │                                       │
│  ┌──────────────────────────────────▼─────────────────────────────────────┐ │
│  │                        領域層                                           │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐│ │
│  │  │   使用者       │  │   履歷       │  │   職位       │  │  對話        ││ │
│  │  │   領域       │  │   領域       │  │   領域       │  │   領域       ││ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘│ │
│  └──────────────────────────────────┬─────────────────────────────────────┘ │
│                                     │                                       │
│  ┌──────────────────────────────────▼─────────────────────────────────────┐ │
│  │                        基礎設施層                                        │ │
│  │  Repository | MQ Publisher | MQ Consumer | PGVector Client | Storage Service │ │
│  └──────────────────────────────────┬─────────────────────────────────────┘ │
└───────────────────────────────────┬─┴───────────────────────────────────────┘
                                    │ 訊息佇列 (RabbitMQ)
┌───────────────────────────────────▼─────────────────────────────────────────┐
│                          Python AI服務層                                      │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                        訊息消費者                                        │ │
│  │  ResumeParseConsumer | JobMatchConsumer | ChatConsumer | 模型增量消費者 │ │
│  └──────────────────────────────────┬─────────────────────────────────────┘ │
│                                     │                                       │
│  ┌──────────────────────────────────▼─────────────────────────────────────┐ │
│  │                        AI處理引擎                                        │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐│ │
│  │  │ 履歷剖析器   │  │ 嵌入生成器   │  │ 匹配計算器   │  │ 對話處理器   ││ │
│  │  │ pypdf /      │  │ LiteLLM      │  │ 相似度       │  │ RAG +        ││ │
│  │  │ OpenXML ZIP  │  │ Embeddings   │  │ 排序         │  │ 記憶         ││ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘│ │
│  └──────────────────────────────────┬─────────────────────────────────────┘ │
│                                     │                                       │
│  ┌──────────────────────────────────▼─────────────────────────────────────┐ │
│  │                        AI Worker (LightGBM)                            │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                  │ │
│  │  │ 增量訓練     │  │ 模型評估     │  │ MinIO        │                  │ │
│  │  │              │  │              │  │ 註冊表       │                  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              資料層 (PostgreSQL + pgvector)                   │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         PostgreSQL 15                                   │ │
│  │  ┌──────────────────────────────┐  ┌─────────────────────────────────┐ │ │
│  │  │    業務資料表                 │  │    向量表 (pgvector)            │ │ │
│  │  │  - users                     │  │  - resume_vectors               │ │ │
│  │  │  - resumes                   │  │  - job_vectors                  │ │ │
│  │  │  - jobs                      │  │                                 │ │ │
│  │  │  - conversations             │  │  統一資料庫管理                  │ │ │
│  │  │  - messages                  │  │                                 │ │ │
│  │  └──────────────────────────────┘  └─────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 服務互動流程

```
┌─────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  使用者   │────▶│  React前端  │────▶│ Java後端    │────▶│ PostgreSQL  │
│         │◀────│             │◀────│ (DDD架構)   │◀────│ (pgvector)  │
└─────────┘     └─────────────┘     └──────┬──────┘     └─────────────┘
                                           │
                                           │ RabbitMQ
                                           │
                                           ▼
                                    ┌─────────────┐
                                    │ Python AI   │
                                    │ 服務        │
                                    │ (FastAPI)   │
                                    └──────┬──────┘
                                           │
                                           │ Redis
                                           ▼
                                    ┌─────────────┐
                                    │   Redis 7   │
                                    │ (快取與鎖)  │
                                    └─────────────┘
```

---

### 3.3 增量式職位訓練閉環數據流

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                      增量式職位訓練閉環 (職位評分)                                     │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  用戶          前端             Java 後端           RabbitMQ       Python AI         │
│   │              │                  │                  │              │              │
│   │  提交職位    │                  │                  │              │              │
│   │─────────────>│                  │                  │              │              │
│   │              │  POST /api/v1/jobs                 │              │              │
│   │              │─────────────────>│                  │              │              │
│   │              │                  │  剖析完成        │              │              │
│   │              │                  │  寫入 job_dataset              │              │
│   │              │                  │  (訓練語料庫)                   │              │
│   │              │                  │                  │              │              │
│   │  評分職位    │                  │                  │              │              │
│   │─────────────>│                  │                  │              │              │
│   │              │  POST /api/v1/jobs/{id}/score      │              │              │
│   │              │─────────────────>│                  │              │              │
│   │              │                  │  呼叫 AI /suitability          │              │
│   │              │                  │  儲存 ScoreRecord│              │              │
│   │              │                  │  發布反饋        │              │              │
│   │              │                  │  ai.req.feedback │              │              │
│   │              │                  │─────────────────>│              │              │
│   │              │  返回結果        │                  │              │              │
│   │              │<─────────────────│                  │              │              │
│   │              │                  │                  │  消費        │              │
│   │              │                  │                  │─────────────>│              │
│   │              │                  │                  │              │  緩衝        │
│   │              │                  │                  │              │  反饋
│   │              │                  │                  │              │              │
│   │              │                  │                  │              │  訓練        │
│   │              │                  │                  │              │  LightGBM
│   │              │                  │                  │              │  (若達閾值)  │
│   │              │                  │                  │              │              │
│   │              │                  │                  │              │  生成        │
│   │              │                  │                  │              │  模型 artifact
│   │              │                  │                  │              │              │
│   │              │                  │                  │              │  重新載入    │
│   │              │                  │                  │              │  模型        │
│   │              │                  │                  │              │              │
│   │  下次評分    │                  │                  │              │  載入新模型  │
│   │─────────────>│                  │                  │              │  (自動)      │
│   │              │  POST /api/v1/jobs/{id}/score      │              │              │
│   │              │─────────────────>│  呼叫 AI /suitability          │              │
│   │              │                  │  (使用新權重)                   │              │
│   │              │<─────────────────│                  │              │              │
│   │              │                  │                  │              │              │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

**關鍵設計要點：**

1. **雙寫**：剖析完成的職位同時寫入 `jobs` 資料表（面向用戶、可軟刪除）與 `job_dataset` 資料表（訓練語料庫、永久保存）。
2. **發送後即忘 MQ**：評分標籤透過 Outbox 發送至 `ai.queue.feedback`。投遞失敗不會阻塞評分回應。
3. **Redis 反饋緩衝**：AI worker 將評分反饋轉換為帶標籤的特徵樣本，並寫入 Redis 緩衝區。
4. **MinIO 模型產物**：AI worker 使用 baseline features 與緩衝反饋訓練 LightGBM ranker，並將 `ranker_model_<version>.txt` 和 `latest_meta.json` 寫入 MinIO。
5. **Redis 驅動模型重載**：model manager 從 MinIO 載入最新模型，並在 worker 發布 `ai.model.reload` 通知時重新載入。

---

## 4. 元件設計

### 4.1 前端元件 (React)

#### 4.1.1 頁面結構

| 頁面       | 功能         | 核心元件                                        |
|----------|------------|---------------------------------------------|
| **履歷頁面** | 上傳、檢視、管理履歷 | ResumeUpload, ResumeViewer, ResumeList      |
| **職位頁面** | 瀏覽職位、檢視匹配度 | JobList, JobDetail, MatchScore              |
| **對話頁面** | AI對話最佳化履歷   | ChatInterface, MessageList, SuggestionPanel |
| **追蹤頁面** | 申請記錄、面試日程  | ApplicationTracker, InterviewCalendar       |

#### 4.1.2 元件層次

```
App
├── Layout (Header, Sidebar, Footer)
│   ├── ResumePage
│   │   ├── ResumeUpload (檔案上傳)
│   │   ├── ResumeViewer (履歷預覽)
│   │   └── ResumeEditor (履歷編輯)
│   ├── JobPage
│   │   ├── JobSearch (職位搜尋)
│   │   ├── JobList (職位列表)
│   │   └── JobDetail (職位詳情)
│   ├── ChatPage
│   │   ├── ChatWindow (對話視窗)
│   │   ├── MessageInput (訊息輸入)
│   │   └── SuggestionList (最佳化建議)
│   └── TrackingPage
│       ├── ApplicationList (申請列表)
│       └── CalendarView (日曆檢視)
└── SharedComponents
    ├── Button, Input, Modal
    ├── LoadingSpinner
    └── ErrorBoundary
```

### 4.2 Java後端元件 (DDD架構)

#### 4.2.1 分層架構

```
┌─────────────────────────────────────────────────────────────┐
│                      介面適配層 (Adapter)                     │
│  Controller | DTO | Mapper | ExceptionHandler               │
├─────────────────────────────────────────────────────────────┤
│                      應用服務層 (Application)                 │
│  AppService | UseCase | Transaction | EventPublisher        │
├─────────────────────────────────────────────────────────────┤
│                      領域層 (Domain)                         │
│  Entity | ValueObject | DomainService | Repository Interface│
│  DomainEvent | Aggregate Root                               │
├─────────────────────────────────────────────────────────────┤
│                      基礎設施層 (Infrastructure)              │
│  RepositoryImpl | MQ Client | Cache | External API Client   │
└─────────────────────────────────────────────────────────────┘
```

#### 4.2.2 領域設計

| 領域       | 聚合根            | 實體                                             | 值物件                    | 領域服務                |
|----------|----------------|------------------------------------------------|------------------------|---------------------|
| **使用者領域** | User           | UserProfile                                    | Email, Phone           | AuthService         |
| **履歷領域** | Resume         | ParsedResume, Skill, WorkExperience, Education | ResumeSummary          | ResumeParserService |
| **職位領域** | Job            | JobRequirement, JobMatch                       | JobSummary, MatchScore | JobMatchingService  |
| **對話領域** | Conversation   | Message, SuggestedChange                       | MessageContent         | ConversationService |
| **追蹤領域** | JobApplication | Interview                                      | ApplicationStatus      | TrackingService     |
| **CAPTCHA** | CaptchaChallenge | CaptchaToken                                 | -                      | CaptchaService      |

### 4.3 Python AI服務元件 (FastAPI)

#### 4.3.1 模組結構

```
ai_service/
├── main.py                 # FastAPI應用入口
├── config/                 # 設定管理
│   ├── settings.py
│   └── model_config.yaml
├── api/                    # FastAPI 無狀態端點
│   ├── resume_parser.py
│   ├── job_matcher.py
│   └── chat_processor.py
├── worker/                 # 有狀態背景工作程式 (LightGBM)
│   ├── incremental_trainer.py
│   └── model_evaluator.py
├── domain/                 # 核心 AI 邏輯與模型
│   ├── resume_parser_service.py
│   ├── embedding_service.py
│   ├── matching_service.py
│   └── chat_service.py
├── infrastructure/         # 外部整合 (MinIO, MQ, DB)
│   ├── llm_client.py
│   ├── embedding_model.py
│   ├── vector_store.py
│   ├── minio_client.py
│   └── consumers/          # 訊息佇列消費者
│       ├── resume_consumer.py
│       ├── match_consumer.py
│       └── chat_consumer.py
└── utils/                  # 工具函式
    ├── pdf_extractor.py
    ├── text_processor.py
    └── retry_decorator.py
```

#### 4.3.2 AI處理引擎

| 模組        | 功能                     | 依賴                                       |
|-----------|------------------------|------------------------------------------|
| **履歷剖析器** | 提取PDF/Word內容，生成結構化JSON | pypdf, OpenXML ZIP, LiteLLM              |
| **嵌入生成器** | 生成文字向量表示               | LiteLLM embedding provider               |
| **匹配計算器** | 計算履歷-職位相似度             | cosine similarity, ranking algorithm     |
| **對話處理器** | RAG對話，記憶管理             | LiteLLM, 資料庫歷史, pgvector            |

---

## 5. 資料架構

### 5.1 資料庫設計

#### 5.1.1 業務資料表

```sql
-- 使用者表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 履歷表
CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    original_file_path VARCHAR(500),
    file_type VARCHAR(10), -- 'pdf', 'docx'
    parsed_content JSONB,
    summary TEXT,
    status VARCHAR(20) DEFAULT 'pending', -- pending, processing, completed, failed
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 職位表
CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    description TEXT,
    requirements JSONB,
    salary_range VARCHAR(100),
    job_type VARCHAR(50),
    posted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 對話表
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE SET NULL,
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 訊息表
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, -- 'user', 'assistant', 'system'
    content TEXT NOT NULL,
    suggested_changes JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 求職申請表
CREATE TABLE job_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE SET NULL,
    job_id BIGINT REFERENCES jobs(id) ON DELETE SET NULL,
    status VARCHAR(50) DEFAULT 'applied', -- applied, screening, interview, offer, rejected
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

-- 面試表
CREATE TABLE interviews (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT REFERENCES job_applications(id) ON DELETE CASCADE,
    interview_type VARCHAR(50), -- phone, video, onsite
    scheduled_at TIMESTAMP,
    duration_minutes INTEGER,
    location VARCHAR(255),
    notes TEXT,
    status VARCHAR(20) DEFAULT 'scheduled'
);
```

#### 5.1.2 向量資料表 (pgvector)

```sql
-- 履歷嵌入向量表
CREATE TABLE resume_vectors (
    id VARCHAR(64) PRIMARY KEY,
    resume_version_id VARCHAR(64) NOT NULL UNIQUE,
    embedding VECTOR(1536),  -- 預設維度，可由初始化腳本依環境變數替換
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 職位嵌入向量表
CREATE TABLE job_vectors (
    id VARCHAR(64) PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL UNIQUE,
    embedding VECTOR(1536),
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    title TEXT,
    description TEXT,
    requirements JSONB,
    raw_content TEXT,
    source_file VARCHAR(255),
    model_version VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resume_vectors_version_id ON resume_vectors (resume_version_id);
CREATE INDEX idx_resume_vectors_status ON resume_vectors (status);

CREATE INDEX idx_job_vectors_job_id ON job_vectors (job_id);
CREATE INDEX idx_job_vectors_status ON job_vectors (status);
```

### 5.2 向量儲存設計

#### 5.2.1 嵌入模型選擇

| 模型                   | 維度  | 用途        | 說明           |
|----------------------|-----|-----------|--------------|
| **LLM_EMBEDDING_MODEL** | 1536（預設） | 履歷/職位語義匹配 | 透過 LiteLLM 配置，可依環境變數調整 |

#### 5.2.2 相似度搜尋示例

```sql
-- 查詢與給定履歷最匹配的職位
SELECT 
    j.id,
    j.title,
    j.company,
    1 - (je.embedding <=> $1) AS similarity_score
FROM job_vectors je
JOIN jobs j ON je.job_id = j.id
ORDER BY je.embedding <=> $1
LIMIT 10;

-- 查詢與給定職位最匹配的履歷
SELECT 
    r.id,
    r.summary,
    1 - (re.embedding <=> $1) AS similarity_score
FROM resume_vectors re
JOIN resumes r ON re.resume_id = r.id
WHERE r.user_id = $2
ORDER BY re.embedding <=> $1
LIMIT 5;
```

### 5.3 資料流

#### 5.3.1 履歷上傳流程

```
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 使用者上傳 │───▶│ 前端上傳    │───▶│ Java後端    │───▶│ 儲存檔案    │───▶│ 傳送MQ訊息  │
│ 履歷檔案 │    │ 履歷檔案    │    │ 接收檔案    │    │ 建立記錄    │    │ (非同步處理)  │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘    └──────┬──────┘
                                                                                │
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
│ 更新    │◀───│ 接收結果    │◀───│ 傳送結果    │◀───│ AI剖析      │◀──────────┘
│ 狀態    │    │ 儲存資料    │    │ 訊息        │    │ 處理        │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

#### 5.3.2 職位匹配流程

```
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 使用者請求 │───▶│ Java後端    │───▶│ 查詢向量    │───▶│ pgvector    │
│ 匹配職位 │    │ 接收請求    │    │ 資料庫      │    │ 相似度搜尋  │
└─────────┘    └─────────────┘    └─────────────┘    └──────┬──────┘
                                                            │
┌─────────┐    ┌─────────────┐    ┌─────────────┐          │
│ 返回    │◀───│ 組裝結果    │◀───│ 取得職位    │◀─────────┘
│ 結果    │    │ 返回前端    │    │ 詳情        │
└─────────┘    └─────────────┘    └─────────────┘
```

#### 5.3.3 對話訊息流程

```
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 使用者    │───▶│ 前端        │───▶│ Java後端    │───▶│ 儲存訊息    │───▶│ 傳送MQ      │
│ 發訊息  │    │ 呼叫API     │    │ 接收請求    │    │ (USER)      │    │ (非同步)      │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘    └──────┬──────┘
                                                                                │
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
│ 前端    │◀───│ 輪詢/推送   │◀───│ 儲存AI回覆  │◀───│ 接收MQ結果  │◀──────────┘
│ 展示    │    │ 取得訊息    │    │ (ASSISTANT) │    │ (Python AI) │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

**檔案上傳支線**：
```
AI服務 / 前端 ──▶ 呼叫後端上傳API ──▶ 儲存後端/共享卷 ──▶ 返回檔案URL ──▶ 更新Message記錄(fileUrl)
```

### 5.4 Redis 快取設計（CAPTCHA）

CAPTCHA 子系統使用 Redis 實現分散式快取，透過前綴隔離不同鍵，支援跨實例一致性和水平擴展：

| 快取名稱 | 用途 | Redis 鍵 | 類型 | TTL |
|----------|------|---------|------|-----|
| **Challenge Store** | 儲存滑動驗證碼挑戰（目標位置） | `ra:captcha:challenge:{id}` | String | 5 分鐘 |
| **Token Store** | 儲存一次性驗證 token | `ra:captcha:token:{id}` | String | 5 分鐘 |
| **Rate Limit Window** | 按 IP 記錄請求時間戳 | `ra:captcha:ratelimit:{ip}` | Sorted Set | 1 分鐘 |

**IP 速率限制**：每個 IP 位址每分鐘最多 **20 次** CAPTCHA 請求，防止濫用。超限行求回傳 HTTP 429。

**安全特性**：
- 前綴隔離：`CAPTCHA_CHALLENGE:`、`CAPTCHA_TOKEN:` 與 `RATE_LIMIT:` 命名空間防止快取鍵衝突
- 一次性 token：每個 `captchaToken` 僅可兌換一次，驗證後立即消耗
- 最大嘗試次數：每個挑戰最多 5 次驗證嘗試，超限後失效
- V1 DOM 級驗證：前端完成挑戰求解，不暴露答案
- V2 Graphics2D 謎題演進：服務端使用 Java 2D 渲染基於影像的挑戰

---

## 6. 整合架構

### 6.1 訊息佇列設計 (RabbitMQ)

#### 6.1.1 佇列定義

| 佇列名稱                        | 型別   | 生產者       | 消費者       | 訊息大小   |
|-----------------------------|------|-----------|-----------|--------|
| ai.resume.preprocess        | 工作佇列 | Java後端    | Python AI | < 1KB  |
| ai.resume.preprocess.result | 結果佇列 | Python AI | Java後端    | 5-10KB |
| ai.job.match                | 工作佇列 | Java後端    | Python AI | < 5KB  |
| ai.job.match.result         | 結果佇列 | Python AI | Java後端    | < 5KB  |
| ai.chat.message             | 工作佇列 | Java後端    | Python AI | < 5KB  |
| ai.chat.message.result      | 結果佇列 | Python AI | Java後端    | < 5KB  |
| ai.conversation             | 工作佇列 | Java後端    | Python AI | < 5KB  |
| ai.conversation.result      | 結果佇列 | Python AI | Java後端    | < 5KB  |
| ai.vector.request           | 請求佇列 | Python AI | Java後端    | < 1KB  |
| ai.vector.response          | 回應佇列 | Java後端    | Python AI | 2-5KB  |

#### 6.1.2 訊息格式

```json
// 履歷預處理請求訊息
{
  "messageId": "uuid",
  "type": "RESUME_PARSE_REQUEST",
  "payload": {
    "resumeId": 123,
    "filePath": "/uploads/resume_123.pdf",
    "fileType": "pdf"
  },
  "timestamp": "2025-01-15T10:30:00Z"
}

// 履歷預處理結果訊息
{
  "messageId": "uuid",
  "type": "RESUME_PARSE_RESULT",
  "payload": {
    "resumeId": 123,
    "status": "success",
    "parsedData": {
      "name": "張三",
      "email": "zhangsan@example.com",
      "skills": ["Java", "Python", "React"],
      "experience": [...],
      "education": [...]
    },
    "summary": "5年Java開發經驗...",
    "embedding": [0.1, 0.2, ...]  // 384維向量
  },
  "timestamp": "2025-01-15T10:31:00Z"
}

// 職位匹配請求訊息
{
  "messageId": "uuid",
  "type": "JOB_MATCH_REQUEST",
  "payload": {
    "resumeId": 123,
    "resumeEmbedding": [0.1, 0.2, ...],
    "topK": 10
  },
  "timestamp": "2025-01-15T10:35:00Z"
}

// 對話訊息請求
{
  "messageId": "uuid",
  "type": "CHAT_MESSAGE_REQUEST",
  "payload": {
    "conversationId": 456,
    "message": "幫我最佳化工作經驗部分",
    "resumeContext": { "summary": "...", "skills": [...] },
    "chatHistory": [...]
  },
  "timestamp": "2025-01-15T10:40:00Z"
}

// 對話 AI 請求 (Backend -> Python AI)
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "messageHistory": [
    { "role": "USER", "content": "幫我最佳化工作經驗部分" }
  ],
  "currentMessage": "幫我最佳化工作經驗部分",
  "fileUrls": ["https://minio.example.com/resumes/xxx.pdf"],
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002"
}

// 對話 AI 回應 (Python AI -> Backend)
{
  "referenceId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "CONVERSATION_REPLY",
  "status": "COMPLETED",
  "data": {
    "content": "根據您的履歷，我建議從以下幾個方面最佳化工作經驗...",
    "fileUrl": "https://minio.example.com/conversations/xxx/optimized.pdf"
  },
  "errorMessage": null,
  "eventType": null
}
```

### 6.2 REST API設計

#### 6.2.1 API端點概覽

| 端點                                    | 方法     | 描述       | 認證 |
|---------------------------------------|--------|----------|----|
| `/api/v1/auth/register`               | POST   | 使用者註冊     | 否  |
| `/api/v1/auth/login`                  | POST   | 使用者登入     | 否  |
| `/api/v1/auth/captcha`                | GET    | 取得CAPTCHA挑戰 | 否  |
| `/api/v1/auth/captcha/verify`         | POST   | 驗證CAPTCHA並換取token | 否  |
| `/api/v1/resumes`                     | GET    | 取得使用者履歷列表 | 是  |
| `/api/v1/resumes`                     | POST   | 上傳新履歷    | 是  |
| `/api/v1/resumes/{id}`                | GET    | 取得履歷詳情   | 是  |
| `/api/v1/resumes/{id}`                | DELETE | 刪除履歷     | 是  |
| `/api/v1/jobs`                        | GET    | 取得職位列表   | 是  |
| `/api/v1/jobs/{id}`                   | GET    | 取得職位詳情   | 是  |
| `/api/v1/jobs/match`                  | POST   | 取得匹配職位   | 是  |
| `/api/v1/conversations`               | GET    | 取得對話列表   | 是  |
| `/api/v1/conversations`               | POST   | 建立新對話    | 是  |
| `/api/v1/conversations/{id}/messages` | GET    | 取得對話訊息   | 是  |
| `/api/v1/conversations/{id}/messages` | POST   | 傳送訊息     | 是  |
| `/api/v1/conversations/{id}/files`    | POST   | 上傳對話附件   | 是  |
| `/api/v1/conversations/{id}?page=0&size=20` | GET | 取得對話詳情（支援訊息分頁） | 是 |
| `/api/v1/applications`                | GET    | 取得申請記錄   | 是  |
| `/api/v1/applications`                | POST   | 建立申請記錄   | 是  |
| `/api/v1/applications/{id}`           | PUT    | 更新申請狀態   | 是  |

#### 6.2.2 請求/回應示例

```http
// 上傳履歷
POST /api/v1/resumes
Content-Type: multipart/form-data
Authorization: Bearer {token}

Request:
------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="resume.pdf"
Content-Type: application/pdf

<binary data>
------WebKitFormBoundary--

Response (202 Accepted):
{
  "id": 123,
  "status": "processing",
  "message": "履歷正在處理中，請稍後檢視"
}

// 取得匹配職位
POST /api/v1/jobs/match
Content-Type: application/json
Authorization: Bearer {token}

Request:
{
  "resumeId": 123,
  "topK": 10
}

Response (200 OK):
{
  "matches": [
    {
      "jobId": 456,
      "title": "高階Java工程師",
      "company": "ABC科技",
      "location": "北京",
      "matchScore": 0.92,
      "matchReason": "技能匹配度高，經驗符合要求"
    },
    ...
  ]
}

// 傳送對話訊息
POST /api/v1/conversations/789/messages
Content-Type: application/json
Authorization: Bearer {token}

Request:
{
  "content": "幫我最佳化工作經驗部分"
}

Response (200 OK):
{
  "messageId": 1001,
  "role": "assistant",
  "content": "根據您的履歷，我建議從以下幾個方面最佳化工作經驗...",
  "suggestedChanges": [
    {
      "section": "experience",
      "original": "負責後端開發",
      "suggested": "主導後端系統架構設計與開發，最佳化系統效能提升30%"
    }
  ],
  "timestamp": "2025-01-15T10:40:30Z"
}
```

### 6.3 服務通訊模式

```
┌─────────────────────────────────────────────────────────────────┐
│                      同步通訊 (REST API)                         │
│  - 前端 ↔ Java後端                                               │
│  - 使用者操作、資料查詢                                             │
│  - 需要即時回應                                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      非同步通訊 (訊息佇列)                          │
│  - Java後端 ↔ Python AI服務                                      │
│  - AI處理任務（剖析、匹配、對話）                                  │
│  - 耗時操作，可容忍延遲                                           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      資料存取模式                                 │
│  - Java後端直接存取PostgreSQL                                     │
│  - Python AI服務透過訊息佇列請求資料                              │
│  - 資料隔離，保證安全性                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. AI整合

### 7.1 AI技術棧

| 技術            | 用途             | 實現                               |
|---------------|----------------|----------------------------------|
| **向量搜尋/嵌入**   | 履歷與職位語義匹配      | LiteLLM embeddings + pgvector |
| **結構化輸出**     | 剖析履歷為結構化JSON   | LiteLLM provider + JSON Schema |
| **LLM API整合** | API呼叫層，重試和降級 | LiteLLM 客戶端 + 重試邏輯                     |
| **記憶/對話管理**   | 對話歷史管理         | 資料庫儲存 + 上下文視窗管理                  |
| **RAG**       | 檢索履歷內容作為對話上下文  | 資料庫上下文 + LiteLLM             |

### 7.2 履歷剖析模組

#### 7.2.1 處理流程

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ PDF/Word    │───▶│ 文字提取    │───▶│ LLM剖析     │───▶│ 結構化JSON  │
│ 檔案        │    │ (pypdf/ZIP) │    │ (LiteLLM)   │    │ 輸出        │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

#### 7.2.2 輸出Schema

```json
{
  "name": "string",
  "email": "string",
  "phone": "string",
  "summary": "string",
  "skills": ["string"],
  "experience": [
    {
      "company": "string",
      "title": "string",
      "startDate": "YYYY-MM",
      "endDate": "YYYY-MM",
      "description": "string",
      "achievements": ["string"]
    }
  ],
  "education": [
    {
      "institution": "string",
      "degree": "string",
      "field": "string",
      "graduationYear": "number"
    }
  ],
  "certifications": ["string"],
  "languages": ["string"]
}
```

### 7.3 職位匹配模組

#### 7.3.1 匹配演算法

```python
# 相似度計算流程
def calculate_match_score(resume_embedding, job_embedding):
    # 1. 計算餘弦相似度
    cosine_sim = cosine_similarity(resume_embedding, job_embedding)
    
    # 2. 歸一化到0-1範圍
    normalized_score = (cosine_sim + 1) / 2
    
    return normalized_score

# 職位推薦流程
def recommend_jobs(resume_id, top_k=10):
    # 1. 取得履歷向量
    resume_embedding = get_resume_embedding(resume_id)
    
    # 2. 向量相似度搜尋
    similar_jobs = pgvector_search(resume_embedding, top_k)
    
    # 3. 排序並返回
    return sorted(similar_jobs, key=lambda x: x.score, reverse=True)
```

### 7.4 對話處理模組 (RAG)

#### 7.4.1 RAG架構

```
┌─────────────────────────────────────────────────────────────────┐
│                         RAG流程                                  │
│                                                                  │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐      │
│  │ 使用者    │───▶│ 檢索    │───▶│ 組裝    │───▶│ LLM     │      │
│  │ 輸入    │    │ 履歷    │    │ 上下文  │    │ 生成    │      │
│  └─────────┘    │ 內容    │    │         │    │ 回覆    │      │
│                 └─────────┘    └─────────┘    └─────────┘      │
│                      │                                          │
│                      ▼                                          │
│                 ┌─────────┐                                     │
│                 │ pgvector│                                     │
│                 │ 向量庫  │                                     │
│                 └─────────┘                                     │
└─────────────────────────────────────────────────────────────────┘
```

#### 7.4.2 提示詞模板

```python
RESUME_OPTIMIZATION_PROMPT = """
你是一位專業的履歷最佳化顧問。請根據使用者的履歷內容和問題，提供專業的最佳化建議。

使用者履歷摘要：
{resume_summary}

使用者技能：
{skills}

對話歷史：
{chat_history}

使用者問題：
{user_message}

請提供：
1. 針對性的最佳化建議
2. 具體的修改示例（如果有）
3. 修改原因說明

以JSON格式返回：
{
  "response": "回覆內容",
  "suggested_changes": [
    {
      "section": "section_name",
      "original": "原文",
      "suggested": "建議修改",
      "reason": "修改原因"
    }
  ]
}
"""
```

### 7.5 記憶管理

#### 7.5.1 對話歷史儲存

```sql
-- 訊息表結構
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT REFERENCES conversations(id),
    role VARCHAR(20), -- 'user', 'assistant', 'system'
    content TEXT,
    token_count INTEGER, -- 用於上下文視窗管理
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 取得對話歷史（限制token數）
SELECT role, content 
FROM messages 
WHERE conversation_id = $1 
ORDER BY created_at DESC 
LIMIT $2;
```

#### 7.5.2 上下文視窗管理

| 模型      | 最大上下文     | 保留策略            |
|---------|-----------|-----------------|
| GPT-4   | 8K tokens | 保留最近N條訊息，摘要早期對話 |
| GPT-3.5 | 4K tokens | 同上              |

---

## 8. 安全架構

### 8.1 認證與授權

#### 8.1.1 JWT認證流程

```
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 使用者    │───▶│ 登入請求    │───▶│ 驗證憑證    │───▶│ 生成JWT     │
│         │    │             │    │             │    │             │
│         │◀───│ 返回Token   │◀───│             │◀───│             │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘

┌─────────┐    ┌─────────────┐    ┌─────────────┐
│ 使用者    │───▶│ 攜帶JWT     │───▶│ 驗證Token   │───▶ 存取資源
│ 請求    │    │ 請求API     │    │ 提取使用者ID  │
└─────────┘    └─────────────┘    └─────────────┘
```

#### 8.1.2 Token結構

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user_id",
    "email": "user@example.com",
    "iat": 1705312800,
    "exp": 1705399200,
    "roles": ["user"]
  },
  "signature": "..."
}
```

### 8.2 資料保護

#### 8.2.1 敏感資料處理

| 資料型別   | 保護措施                    |
|--------|-------------------------|
| 密碼     | bcrypt雜湊，salt rounds=12 |
| 履歷內容   | 傳輸TLS加密，儲存加密            |
| 個人身份資訊 | 欄位級加密                   |
| API金鑰  | 環境變數儲存，不提交到程式碼庫          |

#### 8.2.2 傳輸安全

- 所有API通訊使用HTTPS (TLS 1.3)
- HSTS頭部設定
- CORS策略限制

### 8.3 存取控制

#### 8.3.1 權限矩陣

| 資源   | 所有者  | 其他使用者 | 管理員  |
|------|------|------|------|
| 履歷   | CRUD | -    | R    |
| 職位   | R    | R    | CRUD |
| 對話   | CRUD | -    | R    |
| 申請記錄 | CRUD | -    | R    |

### 8.4 人機驗證（CAPTCHA）

後端 CAPTCHA 模組作為獨立的安全層，用於防止自動化攻擊：

- **Challenge-Response 機制**：使用者必須正確完成滑動驗證碼挑戰才能獲得 `captchaToken`
- **IP 速率限制**：同一 IP 每分鐘最多 20 次請求
- **一次性 Token**：`captchaToken` 驗證後立即消耗，防止重放攻擊
- **最大嘗試次數**：每個挑戰最多 5 次嘗試
- **版本演進**：V1 為 DOM 級驗證；V2 使用 Graphics2D 產生影像謎題

---

## 9. 部署架構

### 9.1 Docker Compose設定

#### 9.1.1 服務定義

```yaml
version: '3.8'

services:
  # 1. 前端服務 (Nginx + React)
  frontend:
    build: ./frontend
    ports:
      - "${FRONTEND_HOST_PORT:-80}:8080"
    depends_on:
      - backend
    networks:
      - public-network

  # 2. Java後端服務
  backend:
    build: ./backend
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/JobCopilot
      - SPRING_RABBITMQ_HOST=rabbitmq
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - postgres
      - rabbitmq
    networks:
      - public-network
      - internal-network
      - db-network

  # 3. Python AI服務
  ai-service:
    build: ./ai-service
    environment:
      - RABBITMQ_HOST=rabbitmq
      - BACKEND_SERVICE_URL=http://backend:8080
      - LLM_TEXT_MODEL=${LLM_TEXT_MODEL:-gemini/gemini-2.5-flash}
      - MINIO_ENDPOINT=http://minio:9000
      - MINIO_MODEL_BUCKET=${MINIO_MODEL_BUCKET:-ai-models}
    depends_on:
      - rabbitmq
      - redis
      - minio
    networks:
      - internal-network
    volumes:
      - shared-storage:/app/uploads:ro

  # 4. PostgreSQL + pgvector
  postgres:
    image: docker.io/ankane/pgvector:latest
    environment:
      - POSTGRES_DB=${POSTGRES_DB:-JobCopilot}
      - POSTGRES_USER=${POSTGRES_USER:-resume_user}
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-resume_pass}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - db-network

  # 5. RabbitMQ
  rabbitmq:
    image: rabbitmq:3-management
    environment:
      - RABBITMQ_DEFAULT_USER=${RABBITMQ_USERNAME:-guest}
      - RABBITMQ_DEFAULT_PASS=${RABBITMQ_PASSWORD}
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    networks:
      - internal-network

  # 6. Redis共享狀態
  redis:
    image: redis:7-alpine
    volumes:
      - redis-data:/data
    networks:
      - internal-network

  # 7. MinIO模型註冊表
  minio:
    image: quay.io/minio/minio:latest
    command: ["server", "/data", "--console-address", ":9001"]
    volumes:
      - minio-data:/data
    networks:
      - internal-network

volumes:
  postgres-data:
  rabbitmq-data:
  redis-data:
  shared-storage:
  minio-data:

networks:
  public-network:
    driver: bridge
  internal-network:
    driver: bridge
  db-network:
    driver: bridge
```

### 9.2 服務埠對映

| 服務         | 內部埠       | 外部埠       | 用途        |
|------------|------------|------------|-----------|
| Frontend   | 8080       | `${FRONTEND_HOST_PORT:-80}` | Web介面與API反向代理 |
| Backend    | 8080       | 預設不暴露       | Nginx 後方的內部 REST API |
| AI Service | 8000       | 預設不暴露       | 內部 AI 處理 API |
| PostgreSQL | 5432       | 預設不暴露       | 後端存取資料庫 |
| RabbitMQ   | 5672/15672 | 預設不暴露       | 內部訊息佇列/開發除錯管理介面 |
| Redis      | 6379       | 預設不暴露       | AI 增量模型共享狀態 |

### 9.3 基礎設施圖

```
┌─────────────────────────────────────────────────────────────────┐
│                         Docker Host                              │
│                                                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │  Nginx      │  │  Spring Boot│  │  FastAPI    │             │
│  │  (Frontend) │  │  (Backend)  │  │  (AI)       │             │
│  │  Host->8080 │  │ Internal    │  │ Internal    │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│         │                │                │                     │
│         └────────────────┴────────────────┘                     │
│                          │                                       │
│  ┌───────────────────────┴───────────────────────┐              │
│  │           Docker Network (Bridge)              │              │
│  └───────────────────────┬───────────────────────┘              │
│                          │                                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ PostgreSQL  │  │  RabbitMQ   │  │  Volumes    │             │
│  │ + pgvector  │  │             │  │  (永續儲存)    │             │
│  │ Internal    │  │ Internal    │  │             │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 10. 可擴充性和效能

### 10.1 效能指標

| 指標      | 目標值           | 說明   |
|---------|---------------|------|
| API回應時間 | < 500ms (P95) | 普通查詢 |
| 履歷剖析時間  | < 10秒         | 非同步處理 |
| 職位匹配時間  | < 2秒          | 向量搜尋 |
| 對話回應時間  | < 3秒          | AI生成 |
| 併發使用者數   | 100+          | 同時線上 |
| 系統可用性   | 99.5%         | 年度目標 |

### 10.2 擴充策略

#### 10.2.1 水平擴充

```
                    ┌─────────────┐
                    │  負載均衡器  │
                    │   (Nginx)   │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │  Backend    │ │  Backend    │ │  Backend    │
    │  Instance 1 │ │  Instance 2 │ │  Instance N │
    └─────────────┘ └─────────────┘ └─────────────┘
           │               │               │
           └───────────────┼───────────────┘
                           ▼
                    ┌─────────────┐
                    │  PostgreSQL │
                    │  (Primary)  │
                    └─────────────┘
```

#### 10.2.2 效能最佳化措施

| 層面       | 最佳化措施          |
|----------|---------------|
| **資料庫**  | 索引最佳化、查詢最佳化、連線池 |
| **快取**   | Redis快取熱點資料   |
| **CDN**  | 靜態資源CDN加速     |
| **AI服務** | 模型量化、批次處理、非同步處理 |
| **訊息佇列** | 消費者水平擴充       |

### 10.3 監控和紀錄

#### 10.3.1 監控指標

| 類別     | 指標             |
|--------|----------------|
| **應用** | QPS、回應時間、錯誤率   |
| **系統** | CPU、記憶體、磁碟、網路   |
| **業務** | 活躍使用者、履歷處理量、對話數 |
| **AI** | API呼叫次數、成本、延遲  |

#### 10.3.2 紀錄結構

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "level": "INFO",
  "service": "backend",
  "traceId": "uuid",
  "userId": "123",
  "action": "resume_upload",
  "duration": 150,
  "status": "success",
  "message": "Resume uploaded successfully"
}
```

---

## 11. 附錄

### 11.1 術語表

| 術語     | 英文             | 說明                                       |
|--------|----------------|------------------------------------------|
| 領域驅動設計 | DDD            | Domain-Driven Design，業務邏輯內聚的架構方法         |
| 向量嵌入   | Embedding      | 將文字轉換為數值向量的技術                            |
| 檢索增強生成 | RAG            | Retrieval-Augmented Generation，結合檢索的生成模型 |
| 訊息佇列   | MQ             | Message Queue，非同步通訊機制                     |
| 聚合根    | Aggregate Root | DDD中的核心實體                                |
| 值物件    | Value Object   | DDD中無身份標識的物件                             |

### 11.2 參考資料

1. [Domain-Driven Design: Tackling Complexity in the Heart of Software](https://domainlanguage.com/ddd/) - Eric Evans
2. [pgvector Documentation](https://github.com/pgvector/pgvector)
3. [Spring Boot Documentation](https://spring.io/projects/spring-boot)
4. [FastAPI Documentation](https://fastapi.tiangolo.com/)
5. [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
6. [LiteLLM Documentation](https://docs.litellm.ai/)

### 11.3 架構決策記錄 (ADR)

#### ADR-001: 選擇PostgreSQL + pgvector作為統一儲存

**狀態**: 已接受

**背景**: 需要同時儲存業務資料和向量資料

**決策**: 使用PostgreSQL + pgvector擴充

**理由**:

- 統一資料庫管理，簡化維運
- pgvector支援高效的向量相似度搜尋
- 事務一致性保證

#### ADR-002: 使用訊息佇列進行服務間通訊

**狀態**: 已接受

**背景**: Java後端和Python AI服務需要通訊

**決策**: 使用RabbitMQ作為訊息佇列

**理由**:

- 非同步處理，解耦服務
- 支援可靠訊息傳遞
- 資料隔離，AI服務不直接存取資料庫

#### ADR-003: 使用 LiteLLM provider 生成嵌入

**狀態**: 已接受

**背景**: 需要生成文字向量用於語義匹配

**決策**: 使用由 `LLM_EMBEDDING_MODEL` 配置的 LiteLLM 相容嵌入模型，預設 `gemini/gemini-embedding-001`

**理由**:

- 與文字/視覺 LLM provider 配置方式一致
- 預設 1536 維，和資料庫初始化腳本的預設向量維度一致
- 可透過環境變數切換到 OpenAI 或 Vertex AI 等相容模型

---

## 文件結束

*本文件由SER 594課程專案組編寫，用於智慧求職助手專案的架構設計參考。*
