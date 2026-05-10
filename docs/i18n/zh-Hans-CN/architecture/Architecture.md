<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../architecture/Architecture.md) | [简体中文](Architecture.md) | [繁體中文](../../zh-Hant-TW/architecture/Architecture.md)

# 智能求职助手 - 架构设计文档


---

## 1. 文档信息

| 项目       | 内容                                 |
|----------|------------------------------------|
| **文档标题** | 智能求职助手 - 架构设计文档                    |
| **版本**   | 1.0                                |
| **日期**   | 2025年1月                            |
| **作者**   | SER 594 课程项目组                      |
| **项目**   | 智能求职助手 (Intelligent Job Assistant) |
| **状态**   | 草案                                 |

### 版本历史

| 版本  | 日期      | 作者          | 变更说明 |
|-----|---------|-------------|------|
| 1.0 | 2025-01 | SER 594 项目组 | 初始版本 |

---

## 2. 架构概述

### 2.1 系统简介

智能求职助手是一个基于人工智能的求职辅助平台，旨在通过先进的AI技术帮助求职者优化求职流程。系统整合了简历智能解析、职位智能匹配、对话式简历优化和求职进度追踪四大核心功能。

### 2.2 设计目标

| 目标       | 描述               | 优先级 |
|----------|------------------|-----|
| **可扩展性** | 支持未来功能扩展和用户增长    | 高   |
| **可维护性** | 清晰的模块划分，便于维护和迭代  | 高   |
| **性能**   | 响应时间 < 2秒，支持并发用户 | 高   |
| **可靠性**  | 服务可用性 > 99.5%    | 中   |
| **安全性**  | 数据加密，访问控制        | 高   |

### 2.3 架构原则

1. **领域驱动设计 (DDD)** - 清晰的领域边界，业务逻辑内聚
2. **统一存储** - PostgreSQL + pgvector 同时存储业务数据和向量数据
3. **数据隔离** - AI服务不直接访问数据库，通过消息队列通信
4. **预处理架构** - 简历上传时即生成摘要和向量
5. **按需检索** - AI服务需要向量时通过消息队列向Java后端请求

### 2.4 技术栈概览

| 层级       | 技术                                   | 说明                |
|----------|--------------------------------------|-------------------|
| **前端**   | React 18 + TypeScript + Tailwind CSS | 求职者交互界面           |
| **后端**   | Java Spring Boot 3.x + DDD           | 业务逻辑、数据管理、消息队列    |
| **AI服务** | Python FastAPI                       | 简历解析、匹配计算、对话处理    |
| **数据库**  | PostgreSQL 15 + pgvector             | 业务数据 + 向量数据（统一存储） |
| **消息队列** | RabbitMQ                             | 异步服务通信            |
| **对象存储** | MinIO                                | 文件存储（简历、对话附件） |
| **部署**   | Docker Compose                       | 5服务架构             |

---

## 3. 系统架构

### 3.1 高层架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端层 (React)                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ 简历页面     │  │ 职位页面     │  │ 对话页面     │  │ 追踪页面     │     │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘     │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │ HTTPS / REST
┌───────────────────────────────────▼─────────────────────────────────────────┐
│                          Java后端服务 (DDD)                                   │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                        API网关层 (Controller)                           │ │
│  │  ResumeController | JobController | ChatController | TrackController   │ │
│  └──────────────────────────────────┬─────────────────────────────────────┘ │
│                                     │                                       │
│  ┌──────────────────────────────────▼─────────────────────────────────────┐ │
│  │                        应用服务层                                        │ │
│  │  ResumeAppService | JobAppService | ChatAppService | TrackAppService | CaptchaAppService │ │
│  └──────────────────────────────────┬─────────────────────────────────────┘ │
│                                     │                                       │
│  ┌──────────────────────────────────▼─────────────────────────────────────┐ │
│  │                        领域层                                           │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐│ │
│  │  │   用户       │  │   简历       │  │   职位       │  │  对话        ││ │
│  │  │   领域       │  │   领域       │  │   领域       │  │   领域       ││ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘│ │
│  └──────────────────────────────────┬─────────────────────────────────────┘ │
│                                     │                                       │
│  ┌──────────────────────────────────▼─────────────────────────────────────┐ │
│  │                        基础设施层                                        │ │
│  │  Repository | MQ Publisher | MQ Consumer | PGVector Client | MinIO     │ │
│  └──────────────────────────────────┬─────────────────────────────────────┘ │
└───────────────────────────────────┬─┴───────────────────────────────────────┘
                                    │ 消息队列 (RabbitMQ)
┌───────────────────────────────────▼─────────────────────────────────────────┐
│                          Python AI服务层                                      │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                        消息消费者                                        │ │
│  │  简历解析消费者 | 职位匹配消费者 | 对话消费者 | 模型增量消费者 │ │
│  └──────────────────────────────────┬─────────────────────────────────────┘ │
│                                     │                                       │
│  ┌──────────────────────────────────▼─────────────────────────────────────┐ │
│  │                        AI处理引擎                                        │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐│ │
│  │  │ 简历解析器   │  │ 嵌入生成器   │  │ 匹配计算器   │  │ 对话处理器   ││ │
│  │  │ PyPDF2       │  │ Sentence-    │  │ 相似度       │  │ RAG +        ││ │
│  │  │ OpenAI API   │  │ Transformers │  │ 排序         │  │ 记忆         ││ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘│ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              数据层 (PostgreSQL + pgvector)                   │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         PostgreSQL 15                                   │ │
│  │  ┌──────────────────────────────┐  ┌─────────────────────────────────┐ │ │
│  │  │    业务数据表                 │  │    向量表 (pgvector)            │ │ │
│  │  │  - users                     │  │  - resume_embeddings            │ │ │
│  │  │  - resumes                   │  │  - job_embeddings               │ │ │
│  │  │  - jobs                      │  │                                 │ │ │
│  │  │  - conversations             │  │  统一数据库管理                  │ │ │
│  │  │  - messages                  │  │                                 │ │ │
│  │  └──────────────────────────────┘  └─────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 服务交互流程

```
┌─────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  用户   │────▶│  React前端  │────▶│ Java后端    │────▶│ PostgreSQL  │
│         │◀────│             │◀────│ (DDD架构)   │◀────│ (pgvector)  │
└─────────┘     └─────────────┘     └──────┬──────┘     └─────────────┘
                                           │
                                           │ RabbitMQ
                                           ▼
                                    ┌─────────────┐
                                    │ Python AI   │
                                    │ 服务        │
                                    │ (FastAPI)   │
                                    └─────────────┘
```

---

### 3.3 增量式职位训练闭环数据流

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                      增量式职位训练闭环（职位评分）                                     │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  用户          前端              Java 后端           RabbitMQ         Python AI      │
│   │              │                    │                  │                │          │
│   │  提交职位    │                    │                  │                │          │
│   │─────────────>│                    │                  │                │          │
│   │              │  POST /api/v1/jobs │                  │                │          │
│   │              │───────────────────>│                  │                │          │
│   │              │                    │  解析完成        │                │          │
│   │              │                    │  写入 job_dataset│                │          │
│   │              │                    │  （训练语料）    │                │          │
│   │              │                    │                  │                │          │
│   │  评分职位    │                    │                  │                │          │
│   │─────────────>│                    │                  │                │          │
│   │              │  POST /api/v1/jobs/{id}/score       │                │          │
│   │              │───────────────────>│                  │                │          │
│   │              │                    │  调用 AI /suitability            │          │
│   │              │                    │  保存 ScoreRecord│                │          │
│   │              │                    │  发布 ScoreLabel │                │          │
│   │              │                    │  ai.req.model.incremental       │          │
│   │              │                    │──────────────────>│               │          │
│   │              │  返回结果          │                  │                │          │
│   │              │<───────────────────│                  │                │          │
│   │              │                    │                  │  消费          │          │
│   │              │                    │                  │───────────────>│          │
│   │              │                    │                  │                │  更新    │
│   │              │                    │                  │                │  incremental_stats.json
│   │              │                    │                  │                │          │
│   │              │                    │                  │                │  重新计算│
│   │              │                    │                  │                │  权重    │
│   │              │                    │                  │                │  （若达阈值）
│   │              │                    │                  │                │          │
│   │              │                    │                  │                │  生成    │
│   │              │                    │                  │                │  baseline_model_v{N}.json
│   │              │                    │                  │                │          │
│   │              │                    │                  │                │  失效    │
│   │              │                    │                  │                │  ModelCache│
│   │              │                    │                  │                │          │
│   │  下次评分    │                    │                  │                │  加载新  │
│   │─────────────>│                    │                  │                │  模型    │
│   │              │  POST /api/v1/jobs/{id}/score       │                │  （自动）│
│   │              │───────────────────>│  调用 AI /suitability            │          │
│   │              │                    │  （使用新权重）  │                │          │
│   │              │<───────────────────│                  │                │          │
│   │              │                    │                  │                │          │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

**关键设计点：**

1. **双写**：解析后的职位同时写入 `jobs` 表（面向用户，支持软删除）和 `job_dataset` 表（训练语料，持久保存）。
2. **发后即忘 MQ**：评分标签通过 Outbox 发送到 `ai.queue.model.incremental`。投递失败不会阻塞评分响应。
3. **软上限移动平均**：增量统计使用软上限（`FEATURE_COUNT_CAP=5000`）配合衰减机制，防止历史数据淹没新反馈。
4. **原子模型切换**：新模型版本先写入临时文件再原子移动。符号链接（`baseline_model_latest.json`）始终指向最新版本。
5. **内存缓存**：`suitability_service` 使用基于 mtime 的懒刷新内存缓存 `ModelCache`，避免每次评分请求都进行磁盘 I/O。

---

## 4. 组件设计

### 4.1 前端组件 (React)

#### 4.1.1 页面结构

| 页面       | 功能         | 核心组件                                        |
|----------|------------|---------------------------------------------|
| **简历页面** | 上传、查看、管理简历 | ResumeUpload, ResumeViewer, ResumeList      |
| **职位页面** | 浏览职位、查看匹配度 | JobList, JobDetail, MatchScore              |
| **对话页面** | AI对话优化简历   | ChatInterface, MessageList, SuggestionPanel |
| **追踪页面** | 申请记录、面试日程  | ApplicationTracker, InterviewCalendar       |

#### 4.1.2 组件层次

```
App
├── Layout (Header, Sidebar, Footer)
│   ├── ResumePage
│   │   ├── ResumeUpload (文件上传)
│   │   ├── ResumeViewer (简历预览)
│   │   └── ResumeEditor (简历编辑)
│   ├── JobPage
│   │   ├── JobSearch (职位搜索)
│   │   ├── JobList (职位列表)
│   │   └── JobDetail (职位详情)
│   ├── ChatPage
│   │   ├── ChatWindow (对话窗口)
│   │   ├── MessageInput (消息输入)
│   │   └── SuggestionList (优化建议)
│   └── TrackingPage
│       ├── ApplicationList (申请列表)
│       └── CalendarView (日历视图)
└── SharedComponents
    ├── Button, Input, Modal
    ├── LoadingSpinner
    └── ErrorBoundary
```

### 4.2 Java后端组件 (DDD架构)

#### 4.2.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                      接口适配层 (Adapter)                     │
│  Controller | DTO | Mapper | ExceptionHandler               │
├─────────────────────────────────────────────────────────────┤
│                      应用服务层 (Application)                 │
│  AppService | UseCase | Transaction | EventPublisher        │
├─────────────────────────────────────────────────────────────┤
│                      领域层 (Domain)                         │
│  Entity | ValueObject | DomainService | Repository Interface│
│  DomainEvent | Aggregate Root                               │
├─────────────────────────────────────────────────────────────┤
│                      基础设施层 (Infrastructure)              │
│  RepositoryImpl | MQ Client | Cache | External API Client   │
└─────────────────────────────────────────────────────────────┘
```

#### 4.2.2 领域设计

| 领域       | 聚合根            | 实体                                             | 值对象                    | 领域服务                |
|----------|----------------|------------------------------------------------|------------------------|---------------------|
| **用户领域** | User           | UserProfile                                    | Email, Phone           | AuthService         |
| **简历领域** | Resume         | ParsedResume, Skill, WorkExperience, Education | ResumeSummary          | ResumeParserService |
| **职位领域** | Job            | JobRequirement, JobMatch                       | JobSummary, MatchScore | JobMatchingService  |
| **对话领域** | Conversation   | Message, SuggestedChange                       | MessageContent         | ConversationService |
| **追踪领域** | JobApplication | Interview                                      | ApplicationStatus      | TrackingService     |
| **CAPTCHA** | CaptchaChallenge | CaptchaToken                                 | -                      | CaptchaService      |

### 4.3 Python AI服务组件 (FastAPI)

#### 4.3.1 模块结构

```
ai_service/
├── main.py                 # FastAPI应用入口
├── config/                 # 配置管理
│   ├── settings.py
│   └── model_config.yaml
├── api/                    # API路由
│   ├── resume_parser.py
│   ├── job_matcher.py
│   └── chat_processor.py
├── services/               # 业务服务
│   ├── resume_parser_service.py
│   ├── embedding_service.py
│   ├── matching_service.py
│   └── chat_service.py
├── models/                 # AI模型封装
│   ├── llm_client.py
│   ├── embedding_model.py
│   └── vector_store.py
├── consumers/              # 消息队列消费者
│   ├── resume_consumer.py
│   ├── match_consumer.py
│   └── chat_consumer.py
└── utils/                  # 工具函数
    ├── pdf_extractor.py
    ├── text_processor.py
    └── retry_decorator.py
```

#### 4.3.2 AI处理引擎

| 模块        | 功能                     | 依赖                                       |
|-----------|------------------------|------------------------------------------|
| **简历解析器** | 提取PDF/Word内容，生成结构化JSON | PyPDF2, python-docx, OpenAI API          |
| **嵌入生成器** | 生成文本向量表示               | sentence-transformers (all-MiniLM-L6-v2) |
| **匹配计算器** | 计算简历-职位相似度             | cosine similarity, ranking algorithm     |
| **对话处理器** | RAG对话，记忆管理             | LangChain, OpenAI API, pgvector          |

---

## 5. 数据架构

### 5.1 数据库设计

#### 5.1.1 业务数据表

```sql
-- 用户表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 简历表
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

-- 职位表
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

-- 对话表
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE SET NULL,
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 消息表
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, -- 'user', 'assistant', 'system'
    content TEXT NOT NULL,
    suggested_changes JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 求职申请表
CREATE TABLE job_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE SET NULL,
    job_id BIGINT REFERENCES jobs(id) ON DELETE SET NULL,
    status VARCHAR(50) DEFAULT 'applied', -- applied, screening, interview, offer, rejected
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

-- 面试表
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

#### 5.1.2 向量数据表 (pgvector)

```sql
-- 简历嵌入向量表
CREATE TABLE resume_embeddings (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE CASCADE,
    embedding VECTOR(384),  -- all-MiniLM-L6-v2 维度
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(resume_id)
);

-- 职位嵌入向量表
CREATE TABLE job_embeddings (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT REFERENCES jobs(id) ON DELETE CASCADE,
    embedding VECTOR(384),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(job_id)
);

-- 向量相似度搜索索引 (IVFFlat - 快速近似搜索)
CREATE INDEX idx_resume_embeddings_vector ON resume_embeddings 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE INDEX idx_job_embeddings_vector ON job_embeddings 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

### 5.2 向量存储设计

#### 5.2.1 嵌入模型选择

| 模型                   | 维度  | 用途        | 说明           |
|----------------------|-----|-----------|--------------|
| **all-MiniLM-L6-v2** | 384 | 简历/职位语义匹配 | 轻量级，速度快，效果良好 |

#### 5.2.2 相似度搜索示例

```sql
-- 查找与给定简历最匹配的职位
SELECT 
    j.id,
    j.title,
    j.company,
    1 - (je.embedding <=> $1) AS similarity_score
FROM job_embeddings je
JOIN jobs j ON je.job_id = j.id
ORDER BY je.embedding <=> $1
LIMIT 10;

-- 查找与给定职位最匹配的简历
SELECT 
    r.id,
    r.summary,
    1 - (re.embedding <=> $1) AS similarity_score
FROM resume_embeddings re
JOIN resumes r ON re.resume_id = r.id
WHERE r.user_id = $2
ORDER BY re.embedding <=> $1
LIMIT 5;
```

### 5.3 数据流

#### 5.3.1 简历上传流程

```
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 用户上传 │───▶│ 前端上传    │───▶│ Java后端    │───▶│ 保存文件    │───▶│ 发送MQ消息  │
│ 简历文件 │    │ 简历文件    │    │ 接收文件    │    │ 创建记录    │    │ (异步处理)  │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘    └──────┬──────┘
                                                                                │
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
│ 更新    │◀───│ 接收结果    │◀───│ 发送结果    │◀───│ AI解析      │◀──────────┘
│ 状态    │    │ 保存数据    │    │ 消息        │    │ 处理        │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

#### 5.3.2 职位匹配流程

```
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 用户请求 │───▶│ Java后端    │───▶│ 查询向量    │───▶│ pgvector    │
│ 匹配职位 │    │ 接收请求    │    │ 数据库      │    │ 相似度搜索  │
└─────────┘    └─────────────┘    └─────────────┘    └──────┬──────┘
                                                            │
┌─────────┐    ┌─────────────┐    ┌─────────────┐          │
│ 返回    │◀───│ 组装结果    │◀───│ 获取职位    │◀─────────┘
│ 结果    │    │ 返回前端    │    │ 详情        │
└─────────┘    └─────────────┘    └─────────────┘
```

#### 5.3.3 对话消息流程

```
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 用户    │───▶│ 前端        │───▶│ Java后端    │───▶│ 保存消息    │───▶│ 发送MQ      │
│ 发消息  │    │ 调用API     │    │ 接收请求    │    │ (USER)      │    │ (异步)      │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘    └──────┬──────┘
                                                                                │
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
│ 前端    │◀───│ 轮询/推送   │◀───│ 保存AI回复  │◀───│ 接收MQ结果  │◀──────────┘
│ 展示    │    │ 获取消息    │    │ (ASSISTANT) │    │ (Python AI) │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

**文件上传支线**：
```
AI服务 / 前端 ──▶ 调用后端上传API ──▶ 转存MinIO ──▶ 返回预签名URL ──▶ 更新Message记录(fileUrl)
```

### 5.4 Caffeine 双缓存设计（CAPTCHA）

CAPTCHA 子系统使用 Caffeine 实现高性能内存缓存，并通过前缀隔离不同存储：

| 缓存名称 | 用途 | 键前缀 | 驱逐策略 |
|----------|------|--------|----------|
| **Challenge Cache** | 存储滑动验证码挑战（目标位置） | `CAPTCHA_CHALLENGE:` | 写入后 5 分钟 |
| **Token Cache** | 存储一次性验证 token | `CAPTCHA_TOKEN:` | 写入后 5 分钟 |
| **Rate Limit Cache** | 按 IP 记录请求时间戳 | `RATE_LIMIT:` | 写入后 1 分钟 |

**IP 速率限制**：每个 IP 地址每分钟最多 **20 次** CAPTCHA 请求，防止滥用。超限请求返回 HTTP 429。

**安全特性**：
- 前缀隔离：`CAPTCHA_CHALLENGE:`、`CAPTCHA_TOKEN:` 与 `RATE_LIMIT:` 命名空间防止缓存键冲突
- 一次性 token：每个 `captchaToken` 仅可兑换一次，验证后立即消耗
- 最大尝试次数：每个挑战最多 5 次验证尝试，超限后失效
- V1 DOM 级验证：前端完成挑战求解，不暴露答案
- V2 Graphics2D 谜题演进：服务端使用 Java 2D 渲染基于图像的挑战

---

## 6. 集成架构

### 6.1 消息队列设计 (RabbitMQ)

#### 6.1.1 队列定义

| 队列名称                        | 类型   | 生产者       | 消费者       | 消息大小   |
|-----------------------------|------|-----------|-----------|--------|
| ai.resume.preprocess        | 工作队列 | Java后端    | Python AI | < 1KB  |
| ai.resume.preprocess.result | 结果队列 | Python AI | Java后端    | 5-10KB |
| ai.job.match                | 工作队列 | Java后端    | Python AI | < 5KB  |
| ai.job.match.result         | 结果队列 | Python AI | Java后端    | < 5KB  |
| ai.chat.message             | 工作队列 | Java后端    | Python AI | < 5KB  |
| ai.chat.message.result      | 结果队列 | Python AI | Java后端    | < 5KB  |
| ai.conversation             | 工作队列 | Java后端    | Python AI | < 5KB  |
| ai.conversation.result      | 结果队列 | Python AI | Java后端    | < 5KB  |
| ai.vector.request           | 请求队列 | Python AI | Java后端    | < 1KB  |
| ai.vector.response          | 响应队列 | Java后端    | Python AI | 2-5KB  |

#### 6.1.2 消息格式

```json
// 简历预处理请求消息
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

// 简历预处理结果消息
{
  "messageId": "uuid",
  "type": "RESUME_PARSE_RESULT",
  "payload": {
    "resumeId": 123,
    "status": "success",
    "parsedData": {
      "name": "张三",
      "email": "zhangsan@example.com",
      "skills": ["Java", "Python", "React"],
      "experience": [...],
      "education": [...]
    },
    "summary": "5年Java开发经验...",
    "embedding": [0.1, 0.2, ...]  // 384维向量
  },
  "timestamp": "2025-01-15T10:31:00Z"
}

// 职位匹配请求消息
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

// 对话消息请求
{
  "messageId": "uuid",
  "type": "CHAT_MESSAGE_REQUEST",
  "payload": {
    "conversationId": 456,
    "message": "帮我优化工作经验部分",
    "resumeContext": { "summary": "...", "skills": [...] },
    "chatHistory": [...]
  },
  "timestamp": "2025-01-15T10:40:00Z"
}

// 对话 AI 请求 (Backend -> Python AI)
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "messageHistory": [
    { "role": "USER", "content": "帮我优化工作经验部分" }
  ],
  "currentMessage": "帮我优化工作经验部分",
  "fileUrls": ["https://minio.example.com/resumes/xxx.pdf"],
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002"
}

// 对话 AI 响应 (Python AI -> Backend)
{
  "referenceId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "CONVERSATION_REPLY",
  "status": "COMPLETED",
  "data": {
    "content": "根据您的简历，我建议从以下几个方面优化工作经验...",
    "fileUrl": "https://minio.example.com/conversations/xxx/optimized.pdf"
  },
  "errorMessage": null,
  "eventType": null
}
```

### 6.2 REST API设计

#### 6.2.1 API端点概览

| 端点                                    | 方法     | 描述       | 认证 |
|---------------------------------------|--------|----------|----|
| `/api/v1/auth/register`               | POST   | 用户注册     | 否  |
| `/api/v1/auth/login`                  | POST   | 用户登录     | 否  |
| `/api/v1/auth/captcha`                | GET    | 获取CAPTCHA挑战 | 否  |
| `/api/v1/auth/captcha/verify`         | POST   | 验证CAPTCHA并换取token | 否  |
| `/api/v1/resumes`                     | GET    | 获取用户简历列表 | 是  |
| `/api/v1/resumes`                     | POST   | 上传新简历    | 是  |
| `/api/v1/resumes/{id}`                | GET    | 获取简历详情   | 是  |
| `/api/v1/resumes/{id}`                | DELETE | 删除简历     | 是  |
| `/api/v1/jobs`                        | GET    | 获取职位列表   | 是  |
| `/api/v1/jobs/{id}`                   | GET    | 获取职位详情   | 是  |
| `/api/v1/jobs/match`                  | POST   | 获取匹配职位   | 是  |
| `/api/v1/conversations`               | GET    | 获取对话列表   | 是  |
| `/api/v1/conversations`               | POST   | 创建新对话    | 是  |
| `/api/v1/conversations/{id}/messages` | GET    | 获取对话消息   | 是  |
| `/api/v1/conversations/{id}/messages` | POST   | 发送消息     | 是  |
| `/api/v1/conversations/{id}/files`    | POST   | 上传对话附件   | 是  |
| `/api/v1/conversations/{id}?page=0&size=20` | GET | 获取对话详情（支持消息分页） | 是 |
| `/api/v1/applications`                | GET    | 获取申请记录   | 是  |
| `/api/v1/applications`                | POST   | 创建申请记录   | 是  |
| `/api/v1/applications/{id}`           | PUT    | 更新申请状态   | 是  |

#### 6.2.2 请求/响应示例

```http
// 上传简历
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
  "message": "简历正在处理中，请稍后查看"
}

// 获取匹配职位
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
      "title": "高级Java工程师",
      "company": "ABC科技",
      "location": "北京",
      "matchScore": 0.92,
      "matchReason": "技能匹配度高，经验符合要求"
    },
    ...
  ]
}

// 发送对话消息
POST /api/v1/conversations/789/messages
Content-Type: application/json
Authorization: Bearer {token}

Request:
{
  "content": "帮我优化工作经验部分"
}

Response (200 OK):
{
  "messageId": 1001,
  "role": "assistant",
  "content": "根据您的简历，我建议从以下几个方面优化工作经验...",
  "suggestedChanges": [
    {
      "section": "experience",
      "original": "负责后端开发",
      "suggested": "主导后端系统架构设计与开发，优化系统性能提升30%"
    }
  ],
  "timestamp": "2025-01-15T10:40:30Z"
}
```

### 6.3 服务通信模式

```
┌─────────────────────────────────────────────────────────────────┐
│                      同步通信 (REST API)                         │
│  - 前端 ↔ Java后端                                               │
│  - 用户操作、数据查询                                             │
│  - 需要即时响应                                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      异步通信 (消息队列)                          │
│  - Java后端 ↔ Python AI服务                                      │
│  - AI处理任务（解析、匹配、对话）                                  │
│  - 耗时操作，可容忍延迟                                           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      数据访问模式                                 │
│  - Java后端直接访问PostgreSQL                                     │
│  - Python AI服务通过消息队列请求数据                              │
│  - 数据隔离，保证安全性                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. AI集成

### 7.1 AI技术栈

| 技术            | 用途             | 实现                               |
|---------------|----------------|----------------------------------|
| **向量搜索/嵌入**   | 简历与职位语义匹配      | sentence-transformers + pgvector |
| **结构化输出**     | 解析简历为结构化JSON   | OpenAI GPT-4 + JSON Schema       |
| **LLM API集成** | API调用层，重试和成本追踪 | 自定义客户端 + 装饰器                     |
| **记忆/对话管理**   | 对话历史管理         | 数据库存储 + 上下文窗口管理                  |
| **RAG**       | 检索简历内容作为对话上下文  | LangChain + pgvector             |

### 7.2 简历解析模块

#### 7.2.1 处理流程

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ PDF/Word    │───▶│ 文本提取    │───▶│ LLM解析     │───▶│ 结构化JSON  │
│ 文件        │    │ (PyPDF2)    │    │ (GPT-4)     │    │ 输出        │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

#### 7.2.2 输出Schema

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

### 7.3 职位匹配模块

#### 7.3.1 匹配算法

```python
# 相似度计算流程
def calculate_match_score(resume_embedding, job_embedding):
    # 1. 计算余弦相似度
    cosine_sim = cosine_similarity(resume_embedding, job_embedding)
    
    # 2. 归一化到0-1范围
    normalized_score = (cosine_sim + 1) / 2
    
    return normalized_score

# 职位推荐流程
def recommend_jobs(resume_id, top_k=10):
    # 1. 获取简历向量
    resume_embedding = get_resume_embedding(resume_id)
    
    # 2. 向量相似度搜索
    similar_jobs = pgvector_search(resume_embedding, top_k)
    
    # 3. 排序并返回
    return sorted(similar_jobs, key=lambda x: x.score, reverse=True)
```

### 7.4 对话处理模块 (RAG)

#### 7.4.1 RAG架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         RAG流程                                  │
│                                                                  │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐      │
│  │ 用户    │───▶│ 检索    │───▶│ 组装    │───▶│ LLM     │      │
│  │ 输入    │    │ 简历    │    │ 上下文  │    │ 生成    │      │
│  └─────────┘    │ 内容    │    │         │    │ 回复    │      │
│                 └─────────┘    └─────────┘    └─────────┘      │
│                      │                                          │
│                      ▼                                          │
│                 ┌─────────┐                                     │
│                 │ pgvector│                                     │
│                 │ 向量库  │                                     │
│                 └─────────┘                                     │
└─────────────────────────────────────────────────────────────────┘
```

#### 7.4.2 提示词模板

```python
RESUME_OPTIMIZATION_PROMPT = """
你是一位专业的简历优化顾问。请根据用户的简历内容和问题，提供专业的优化建议。

用户简历摘要：
{resume_summary}

用户技能：
{skills}

对话历史：
{chat_history}

用户问题：
{user_message}

请提供：
1. 针对性的优化建议
2. 具体的修改示例（如果有）
3. 修改原因说明

以JSON格式返回：
{
  "response": "回复内容",
  "suggested_changes": [
    {
      "section": "section_name",
      "original": "原文",
      "suggested": "建议修改",
      "reason": "修改原因"
    }
  ]
}
"""
```

### 7.5 记忆管理

#### 7.5.1 对话历史存储

```sql
-- 消息表结构
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT REFERENCES conversations(id),
    role VARCHAR(20), -- 'user', 'assistant', 'system'
    content TEXT,
    token_count INTEGER, -- 用于上下文窗口管理
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 获取对话历史（限制token数）
SELECT role, content 
FROM messages 
WHERE conversation_id = $1 
ORDER BY created_at DESC 
LIMIT $2;
```

#### 7.5.2 上下文窗口管理

| 模型      | 最大上下文     | 保留策略            |
|---------|-----------|-----------------|
| GPT-4   | 8K tokens | 保留最近N条消息，摘要早期对话 |
| GPT-3.5 | 4K tokens | 同上              |

---

## 8. 安全架构

### 8.1 认证与授权

#### 8.1.1 JWT认证流程

```
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ 用户    │───▶│ 登录请求    │───▶│ 验证凭证    │───▶│ 生成JWT     │
│         │    │             │    │             │    │             │
│         │◀───│ 返回Token   │◀───│             │◀───│             │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘

┌─────────┐    ┌─────────────┐    ┌─────────────┐
│ 用户    │───▶│ 携带JWT     │───▶│ 验证Token   │───▶ 访问资源
│ 请求    │    │ 请求API     │    │ 提取用户ID  │
└─────────┘    └─────────────┘    └─────────────┘
```

#### 8.1.2 Token结构

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

### 8.2 数据保护

#### 8.2.1 敏感数据处理

| 数据类型   | 保护措施                    |
|--------|-------------------------|
| 密码     | bcrypt哈希，salt rounds=12 |
| 简历内容   | 传输TLS加密，存储加密            |
| 个人身份信息 | 字段级加密                   |
| API密钥  | 环境变量存储，不提交到代码库          |

#### 8.2.2 传输安全

- 所有API通信使用HTTPS (TLS 1.3)
- HSTS头部配置
- CORS策略限制

### 8.3 访问控制

#### 8.3.1 权限矩阵

| 资源   | 所有者  | 其他用户 | 管理员  |
|------|------|------|------|
| 简历   | CRUD | -    | R    |
| 职位   | R    | R    | CRUD |
| 对话   | CRUD | -    | R    |
| 申请记录 | CRUD | -    | R    |

### 8.4 人机验证（CAPTCHA）

后端 CAPTCHA 模块作为独立的安全层，用于防止自动化攻击：

- **Challenge-Response 机制**：用户必须正确完成滑动验证码挑战才能获得 `captchaToken`
- **IP 速率限制**：同一 IP 每分钟最多 20 次请求
- **一次性 Token**：`captchaToken` 验证后立即消耗，防止重放攻击
- **最大尝试次数**：每个挑战最多 5 次尝试
- **版本演进**：V1 为 DOM 级验证；V2 使用 Graphics2D 生成图像谜题

---

## 9. 部署架构

### 9.1 Docker Compose配置

#### 9.1.1 服务定义

```yaml
version: '3.8'

services:
  # 1. 前端服务 (Nginx + React)
  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - job-assistant-network

  # 2. Java后端服务
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/jobassistant
      - SPRING_RABBITMQ_HOST=rabbitmq
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - postgres
      - rabbitmq
    networks:
      - job-assistant-network

  # 3. Python AI服务
  ai-service:
    build: ./ai-service
    ports:
      - "8000:8000"
    environment:
      - RABBITMQ_URL=amqp://rabbitmq:5672
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - DATABASE_URL=postgresql://postgres:5432/jobassistant
    depends_on:
      - rabbitmq
      - postgres
    networks:
      - job-assistant-network

  # 4. PostgreSQL + pgvector
  postgres:
    image: ankane/pgvector:latest
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=jobassistant
      - POSTGRES_USER=${DB_USER}
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    networks:
      - job-assistant-network

  # 5. RabbitMQ
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      - RABBITMQ_DEFAULT_USER=${MQ_USER}
      - RABBITMQ_DEFAULT_PASS=${MQ_PASSWORD}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - job-assistant-network

volumes:
  postgres_data:
  rabbitmq_data:

networks:
  job-assistant-network:
    driver: bridge
```

### 9.2 服务端口映射

| 服务         | 内部端口       | 外部端口       | 用途        |
|------------|------------|------------|-----------|
| Frontend   | 80         | 80         | Web界面     |
| Backend    | 8080       | 8080       | REST API  |
| AI Service | 8000       | 8000       | AI处理API   |
| PostgreSQL | 5432       | 5432       | 数据库       |
| RabbitMQ   | 5672/15672 | 5672/15672 | 消息队列/管理界面 |

### 9.3 基础设施图

```
┌─────────────────────────────────────────────────────────────────┐
│                         Docker Host                              │
│                                                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │  Nginx      │  │  Spring Boot│  │  FastAPI    │             │
│  │  (Frontend) │  │  (Backend)  │  │  (AI)       │             │
│  │  Port 80    │  │  Port 8080  │  │  Port 8000  │             │
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
│  │ + pgvector  │  │             │  │  (持久化)    │             │
│  │ Port 5432   │  │ Port 5672   │  │             │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 10. 可扩展性和性能

### 10.1 性能指标

| 指标      | 目标值           | 说明   |
|---------|---------------|------|
| API响应时间 | < 500ms (P95) | 普通查询 |
| 简历解析时间  | < 10秒         | 异步处理 |
| 职位匹配时间  | < 2秒          | 向量搜索 |
| 对话响应时间  | < 3秒          | AI生成 |
| 并发用户数   | 100+          | 同时在线 |
| 系统可用性   | 99.5%         | 年度目标 |

### 10.2 扩展策略

#### 10.2.1 水平扩展

```
                    ┌─────────────┐
                    │  负载均衡器  │
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

#### 10.2.2 性能优化措施

| 层面       | 优化措施          |
|----------|---------------|
| **数据库**  | 索引优化、查询优化、连接池 |
| **缓存**   | Redis缓存热点数据   |
| **CDN**  | 静态资源CDN加速     |
| **AI服务** | 模型量化、批处理、异步处理 |
| **消息队列** | 消费者水平扩展       |

### 10.3 监控和日志

#### 10.3.1 监控指标

| 类别     | 指标             |
|--------|----------------|
| **应用** | QPS、响应时间、错误率   |
| **系统** | CPU、内存、磁盘、网络   |
| **业务** | 活跃用户、简历处理量、对话数 |
| **AI** | API调用次数、成本、延迟  |

#### 10.3.2 日志结构

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

## 11. 附录

### 11.1 术语表

| 术语     | 英文             | 说明                                       |
|--------|----------------|------------------------------------------|
| 领域驱动设计 | DDD            | Domain-Driven Design，业务逻辑内聚的架构方法         |
| 向量嵌入   | Embedding      | 将文本转换为数值向量的技术                            |
| 检索增强生成 | RAG            | Retrieval-Augmented Generation，结合检索的生成模型 |
| 消息队列   | MQ             | Message Queue，异步通信机制                     |
| 聚合根    | Aggregate Root | DDD中的核心实体                                |
| 值对象    | Value Object   | DDD中无身份标识的对象                             |

### 11.2 参考资料

1. [Domain-Driven Design: Tackling Complexity in the Heart of Software](https://domainlanguage.com/ddd/) - Eric Evans
2. [pgvector Documentation](https://github.com/pgvector/pgvector)
3. [Spring Boot Documentation](https://spring.io/projects/spring-boot)
4. [FastAPI Documentation](https://fastapi.tiangolo.com/)
5. [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
6. [Sentence Transformers](https://www.sbert.net/)

### 11.3 架构决策记录 (ADR)

#### ADR-001: 选择PostgreSQL + pgvector作为统一存储

**状态**: 已接受

**背景**: 需要同时存储业务数据和向量数据

**决策**: 使用PostgreSQL + pgvector扩展

**理由**:

- 统一数据库管理，简化运维
- pgvector支持高效的向量相似度搜索
- 事务一致性保证

#### ADR-002: 使用消息队列进行服务间通信

**状态**: 已接受

**背景**: Java后端和Python AI服务需要通信

**决策**: 使用RabbitMQ作为消息队列

**理由**:

- 异步处理，解耦服务
- 支持可靠消息传递
- 数据隔离，AI服务不直接访问数据库

#### ADR-003: 使用sentence-transformers生成嵌入

**状态**: 已接受

**背景**: 需要生成文本向量用于语义匹配

**决策**: 使用all-MiniLM-L6-v2模型

**理由**:

- 轻量级，384维向量
- 速度快，适合实时应用
- 多语言支持

---

## 文档结束

*本文档由SER 594课程项目组编写，用于智能求职助手项目的架构设计参考。*
