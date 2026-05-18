<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](Architecture.md) | [简体中文](../i18n/zh-Hans-CN/architecture/Architecture.md) | [繁體中文](../i18n/zh-Hant-TW/architecture/Architecture.md)

# Intelligent Job Assistant - Architecture Document


---

## 1. Document Information

| Field               | Value                                    |
| ------------------- | ---------------------------------------- |
| **Project Name**    | Intelligent Job Assistant (智能求职助手)       |
| **Document Title**  | System Architecture Document             |
| **Version**         | 1.0.0                                    |
| **Date**            | 2025-01                                  |
| **Authors**         | SER 594 Course Project Team              |
| **Status**          | Draft                                    |
| **Target Audience** | Developers, Architects, Project Managers |

### Revision History

| Version | Date    | Author       | Description                   |
| ------- | ------- | ------------ | ----------------------------- |
| 1.0.0   | 2025-01 | Project Team | Initial architecture document |

---

## 2. Architecture Overview

### 2.1 System Purpose

The Intelligent Job Assistant is a comprehensive job search platform that leverages artificial intelligence to help job
seekers optimize their resumes, discover relevant job opportunities, and track their application progress. The system
combines modern web technologies with advanced AI capabilities to provide an intelligent, conversational experience for
job seekers.

### 2.2 Key Features

| Feature                                | Description                                                                                           | Target Users    |
| -------------------------------------- | ----------------------------------------------------------------------------------------------------- | --------------- |
| **Resume Intelligent Parsing**         | Upload PDF/Word resumes, automatically extract structured information (skills, experience, education) | All job seekers |
| **Job Intelligent Matching**           | AI-powered semantic matching between resumes and job postings                                         | All job seekers |
| **Conversational Resume Optimization** | AI assistant helps optimize resumes through natural dialogue                                          | All job seekers |
| **Job Application Tracking**           | Record application history, interview schedules, and status updates                                   | All job seekers |

### 2.3 Target Users

- **Fresh Graduates**: First-time job seekers needing resume guidance
- **Career Changers**: Professionals transitioning to new industries
- **Employed Professionals**: Active job seekers looking for better opportunities

### 2.4 Architecture Principles

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Core Architecture Principles                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   Separation of  │  │   Domain-Driven  │  │   Event-Driven   │          │
│  │   Concerns       │  │   Design (DDD)   │  │   Architecture   │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │   Unified Data   │  │   Async          │  │   Containerized  │          │
│  │   Storage        │  │   Processing     │  │   Deployment     │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

1. **Separation of Concerns**: Clear boundaries between frontend, backend, AI service, and data layers
2. **Domain-Driven Design (DDD)**: Business logic organized around domain concepts
3. **Event-Driven Architecture**: Asynchronous communication via message queues
4. **Unified Data Storage**: PostgreSQL + pgvector for both business and vector data
5. **Async Processing**: Long-running AI tasks processed asynchronously
6. **Containerized Deployment**: Docker Compose for consistent environments

### 2.5 Technology Stack Summary

| Layer             | Technology                          | Version / Config        | Purpose                         |
| ----------------- | ----------------------------------- | ----------------------- | ------------------------------- |
| **Frontend**      | React + TypeScript + Tailwind CSS   | React 19, Vite 7        | User interface                  |
| **Backend**       | Java + Spring Boot                  | Java 21, Spring Boot 3  | Business logic, API, auth       |
| **AI Service**    | Python FastAPI + LiteLLM            | Python 3.11+            | Parsing, embeddings, ranking, chat |
| **Database**      | PostgreSQL + pgvector               | PostgreSQL 15           | Business data + vector data     |
| **Message Queue** | RabbitMQ                            | RabbitMQ 3              | Async AI task processing        |
| **Cache**         | Redis                               | Redis 7                 | Distributed state, locks, Pub/Sub |
| **Deployment**    | Docker Compose + Nginx              | Compose v2              | Containerized local deployment  |

---

## 3. System Architecture

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                    CLIENT LAYER                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                          Web Browser / Mobile                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                          │
                                          ▼ HTTPS/REST
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                   FRONTEND LAYER                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                     React 19 + TypeScript + Tailwind CSS                      │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │   │
│  │  │ Resume Page  │  │ Job Page     │  │ Chat Page    │  │ Tracking Page│    │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘    │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                          │
                                          ▼ HTTPS/REST
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              JAVA BACKEND SERVICE (DDD)                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                         API Gateway Layer (Controller)                        │   │
│  │  Resume | Job | Conversation | Tracking Controllers                         │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
│                                          │                                          │
│  ┌───────────────────────────────────────▼──────────────────────────────────────┐   │
│  │                        Application Service Layer                               │   │
│  │  Resume | Job | Conversation | Tracking | Captcha Application Services       │   │
│  └───────────────────────────────────────▼──────────────────────────────────────┘   │
│                                          │                                          │
│  ┌───────────────────────────────────────▼──────────────────────────────────────┐   │
│  │                              Domain Layer                                      │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │   │
│  │  │  User    │  │  Resume  │  │   Job    │  │  Conv    │  │  Track   │      │   │
│  │  │  Domain  │  │  Domain  │  │  Domain  │  │  Domain  │  │  Domain  │      │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘      │   │
│  └───────────────────────────────────────▼──────────────────────────────────────┘   │
│                                          │                                          │
│  ┌───────────────────────────────────────▼──────────────────────────────────────┐   │
│  │                          Infrastructure Layer                                  │   │
│  │  Repository | MQ Publisher | MQ Consumer | pgvector Search | Auth | Storage │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                          │
                                          │ RabbitMQ
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              PYTHON AI SERVICE LAYER                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                          Message Consumers                                     │   │
│  │  Resume Parse | Job Parse | Conversation | Job Rank | Model Incremental     │   │
│  └───────────────────────────────────────▼──────────────────────────────────────┘   │
│                                          │                                          │
│  ┌───────────────────────────────────────▼──────────────────────────────────────┐   │
│  │                           AI Processing Engine                                 │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │   │
│  │  │ Resume       │  │ Embedding    │  │ Match        │  │ Chat         │      │   │
│  │  │ Parser       │  │ Generator    │  │ Calculator   │  │ Processor    │      │   │
│  │  │ (PDF/DOCX +  │  │ (LiteLLM     │  │ (pgvector +  │  │ (Context +   │      │   │
│  │  │  LiteLLM)    │  │  Embeddings) │  │  Ranking)    │  │  Memory)     │      │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘      │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                          │
                                          │ JDBC
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                   DATA LAYER                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                              PostgreSQL 15 + pgvector                         │   │
│  │  ┌──────────────────────────────┐  ┌─────────────────────────────────┐     │   │
│  │  │    Business Data Tables      │  │    Vector Tables (pgvector)     │     │   │
│  │  │  - users                     │  │  - resume_vectors               │     │   │
│  │  │  - resume_versions           │  │  - job_vectors                  │     │   │
│  │  │  - jobs                      │  │                                 │     │   │
│  │  │  - conversations             │  │  Unified database management    │     │   │
│  │  │  - messages                  │  │                                 │     │   │
│  │  └──────────────────────────────┘  └─────────────────────────────────┘     │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                              RabbitMQ                                         │   │
│  │  - Message Broker for Async Communication                                     │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                              Redis 7                                          │   │
│  │  - Distributed Caching (CAPTCHA, verification codes)                          │   │
│  │  - Pub/Sub (conversation streaming, model invalidation)                       │   │
│  │  - Distributed Locks (ShedLock, startup sync)                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Service Interaction Flow

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           Typical Request Flow (Resume Upload)                       │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  User          Frontend         Java Backend         RabbitMQ       Python AI        │
│   │                │                  │                  │              │            │
│   │  Upload Resume │                  │                  │              │            │
│   │───────────────>│                  │                  │              │            │
│   │                │  POST /api/v1/resumes              │              │            │
│   │                │─────────────────>│                  │              │            │
│   │                │                  │                  │              │            │
│   │                │                  │  Save Resume     │              │            │
│   │                │                  │  (Pending)       │              │            │
│   │                │                  │  ────────────────│              │            │
│   │                │                  │                  │              │            │
│   │                │                  │  Publish Message │              │            │
│   │                │                  │ ai.req.resume.parse│            │            │
│   │                │                  │─────────────────>│              │            │
│   │                │  Return 202      │                  │              │            │
│   │                │<─────────────────│                  │              │            │
│   │                │  (Accepted)      │                  │              │            │
│   │  Upload ID     │                  │                  │              │            │
│   │<───────────────│                  │                  │              │            │
│   │                │                  │                  │              │            │
│   │                │                  │                  │  Consume     │            │
│   │                │                  │                  │─────────────>│            │
│   │                │                  │                  │              │            │
│   │                │                  │                  │              │  Parse PDF │
│   │                │                  │                  │              │  Extract   │
│   │                │                  │                  │              │  Info      │
│   │                │                  │                  │              │            │
│   │                │                  │                  │              │  Generate  │
│   │                │                  │                  │              │  Embedding │
│   │                │                  │                  │              │            │
│   │                │                  │                  │  Publish     │            │
│   │                │                  │                  │  Result      │            │
│   │                │                  │                  │<─────────────│            │
│   │                │                  │                  │              │            │
│   │                │                  │  Consume Result  │              │            │
│   │                │                  │<─────────────────│              │            │
│   │                │                  │                  │              │            │
│   │                │                  │  Update Resume   │              │            │
│   │                │                  │  (Completed)     │              │            │
│   │                │                  │  ────────────────│              │            │
│   │                │                  │                  │              │            │
│   │  Poll Status   │                  │                  │              │            │
│   │───────────────>│                  │                  │              │            │
│   │                │  GET /api/v1/resumes/versions/{versionId}         │            │
│   │                │─────────────────>│                  │              │            │
│   │                │  Return Result   │                  │              │            │
│   │                │<─────────────────│                  │              │            │
│   │  Parsed Data   │                  │                  │              │            │
│   │<───────────────│                  │                  │              │            │
│   │                │                  │                  │              │            │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 3.3 Incremental Job Training Loop Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                      Incremental Job Training Loop (Job Scoring)                     │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  User          Frontend         Java Backend         RabbitMQ       Python AI        │
│   │                │                  │                  │              │            │
│   │  Submit Job    │                  │                  │              │            │
│   │───────────────>│                  │                  │              │            │
│   │                │  POST /api/v1/jobs                 │              │            │
│   │                │─────────────────>│                  │              │            │
│   │                │                  │  Parse Complete  │              │            │
│   │                │                  │  Write job_dataset              │            │
│   │                │                  │  (Training Corpus)              │            │
│   │                │                  │                  │              │            │
│   │  Score Job     │                  │                  │              │            │
│   │───────────────>│                  │                  │              │            │
│   │                │  POST /api/v1/jobs/{id}/score      │              │            │
│   │                │─────────────────>│                  │              │            │
│   │                │                  │  Call AI /suitability          │              │
│   │                │                  │  Save ScoreRecord│              │            │
│   │                │                  │  Publish ScoreLabel             │            │
│   │                │                  │  ai.req.model.incremental      │              │
│   │                │                  │─────────────────>│              │            │
│   │                │  Return Result   │                  │              │            │
│   │                │<─────────────────│                  │              │            │
│   │                │                  │                  │  Consume     │            │
│   │                │                  │                  │─────────────>│            │
│   │                │                  │                  │              │  Update    │
│   │                │                  │                  │              │  Redis stats
│   │                │                  │                  │              │            │
│   │                │                  │                  │              │  Recompute │
│   │                │                  │                  │              │  Weights   │
│   │                │                  │                  │              │  (if threshold)
│   │                │                  │                  │              │            │
│   │                │                  │                  │              │  Generate  │
│   │                │                  │                  │              │  model artifact
│   │                │                  │                  │              │            │
│   │                │                  │                  │              │  Invalidate│
│   │                │                  │                  │              │  ModelCache│
│   │                │                  │                  │              │            │
│   │  Next Score    │                  │                  │              │  Load New  │
│   │───────────────>│                  │                  │              │  Model     │
│   │                │  POST /api/v1/jobs/{id}/score      │              │  (auto)    │
│   │                │─────────────────>│  Call AI /suitability          │            │
│   │                │                  │  (uses new weights)             │            │
│   │                │<─────────────────│                  │              │            │
│   │                │                  │                  │              │            │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

**Key Design Points:**

1. **Dual Write**: Parsed jobs are written to both the `jobs` table (user-facing, soft-deletable) and the `job_dataset` table (training corpus, persistent).
2. **Fire-and-Forget MQ**: Score labels are sent via Outbox to `ai.queue.model.incremental`. Delivery failures do not block the scoring response.
3. **Soft-Cap Moving Average**: The incremental statistics are stored in Redis Hashes with a soft cap (`FEATURE_COUNT_CAP=5000`) using a Lua script for atomic accumulation, preventing historical data from drowning out new feedback.
4. **Object Storage Model Artifacts**: New model weights are computed from Redis statistics and written to object storage. A Redis version number tracks the latest model artifact.
5. **Redis-Driven Cache Invalidation**: `suitability_service` uses `ModelCache` with a hot-path memory read. A background Pub/Sub listener on `ra:ai:model_invalidate` and a periodic version check mark the cache stale, triggering a reload from object storage without disk I/O on every scoring request.

---

## 4. Component Design

### 4.1 Frontend Layer

#### 4.1.1 Technology Stack

| Component        | Technology                 | Version | Purpose                 |
| ---------------- | -------------------------- | ------- | ----------------------- |
| Framework        | React                      | 19.x    | UI component library    |
| Language         | TypeScript                 | 5.x     | Type-safe development   |
| Styling          | Tailwind CSS               | 3.x     | Utility-first CSS       |
| State Management | Zustand + component state  | 5.x     | Client and page state   |
| HTTP Client      | Axios                      | Latest  | API communication       |
| Build Tool       | Vite                       | 7.x     | Frontend build tool     |

#### 4.1.2 Page Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Frontend Architecture                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                           App Shell                                  │   │
│  │  ┌─────────────┐  ┌─────────────────────────────────────────────┐  │   │
│  │  │  Sidebar    │  │           Main Content Area                  │  │   │
│  │  │  Navigation │  │                                              │  │   │
│  │  │             │  │  ┌────────────────────────────────────────┐  │  │   │
│  │  │ - Resume    │  │  │           Page Router                   │  │  │   │
│  │  │ - Jobs      │  │  │  ┌────────┐ ┌────────┐ ┌────────┐     │  │  │   │
│  │  │ - Chat      │  │  │  │ Resume │ │  Job   │ │  Chat  │ ... │  │  │   │
│  │  │ - Tracking  │  │  │  │ Page   │ │ Page   │ │ Page   │     │  │  │   │
│  │  │             │  │  │  └────────┘ └────────┘ └────────┘     │  │  │   │
│  │  └─────────────┘  │  └────────────────────────────────────────┘  │  │   │
│  │                   └─────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Shared Components                            │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐            │   │
│  │  │ Button   │  │  Input   │  │  Modal   │  │  Card    │            │   │
│  │  │ Loading  │  │  Toast   │  │  Form    │  │  Table   │            │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Service Layer                                │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │   │
│  │  │ Resume API   │  │  Job API     │  │  Chat API    │              │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 4.1.3 Page Components

| Page              | Key Components                              | Features                                   |
| ----------------- | ------------------------------------------- | ------------------------------------------ |
| **Resume Page**   | UploadZone, ResumeViewer, ParsedDataEditor  | PDF/Word upload, preview, edit parsed data |
| **Job Page**      | JobList, JobDetail, MatchScore, FilterPanel | Job search, matching scores, filtering     |
| **Chat Page**     | ChatWindow, MessageList, SuggestionPanel    | AI conversation, resume optimization       |
| **Tracking Page** | ApplicationList, Calendar, StatusBoard      | Application tracking, interview schedule   |

### 4.2 Java Backend Service

#### 4.2.1 DDD Layered Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Java Backend - DDD Layered Architecture                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  Layer 1: API Gateway (Controller Layer)                               │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │  - REST API endpoints                                            │  │ │
│  │  │  - Request/Response DTOs                                         │  │ │
│  │  │  - Input validation                                              │  │ │
│  │  │  - Authentication/Authorization                                  │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                      │                                      │
│                                      ▼                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  Layer 2: Application Service Layer                                    │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │  - Orchestrate use cases                                         │  │ │
│  │  │  - Transaction management                                        │  │ │
│  │  │  - Publish domain events                                         │  │ │
│  │  │  - Coordinate between domains                                    │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                      │                                      │
│                                      ▼                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  Layer 3: Domain Layer (Core Business Logic)                           │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐   │  │ │
│  │  │  │   User     │ │   Resume   │ │    Job     │ │  Tracking  │   │  │ │
│  │  │  │   Domain   │ │   Domain   │ │   Domain   │ │   Domain   │   │  │ │
│  │  │  │            │ │            │ │            │ │            │   │  │ │
│  │  │  │ - Entities │ │ - Entities │ │ - Entities │ │ - Entities │   │  │ │
│  │  │  │ - Value    │ │ - Value    │ │ - Value    │ │ - Value    │   │  │ │
│  │  │  │   Objects  │ │   Objects  │ │   Objects  │ │   Objects  │   │  │ │
│  │  │  │ - Domain   │ │ - Domain   │ │ - Domain   │ │ - Domain   │   │  │ │
│  │  │  │   Services │ │   Services │ │   Services │ │   Services │   │  │ │
│  │  │  │ - Repos    │ │ - Repos    │ │ - Repos    │ │ - Repos    │   │  │ │
│  │  │  └────────────┘ └────────────┘ └────────────┘ └────────────┘   │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                      │                                      │
│                                      ▼                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  Layer 4: Infrastructure Layer                                         │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │  - Repository implementations (JPA)                              │  │ │
│  │  │  - Message Queue publishers/consumers                            │  │ │
│  │  │  - External service clients                                      │  │ │
│  │  │  - Database configuration                                        │  │ │
│  │  │  - Security configuration                                        │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 4.2.2 Domain Modules

| Domain           | Key Entities                                           | Key Services                       | Repositories                                |
| ---------------- | ------------------------------------------------------ | ---------------------------------- | ------------------------------------------- |
| **User**         | User, UserProfile, AuthToken                           | AuthService, UserService           | UserRepository                              |
| **Resume**       | Resume, ParsedResume, Skill, WorkExperience, Education | ResumeService, ResumeParserService | ResumeRepository, ResumeEmbeddingRepository |
| **Job**          | Job, JobRequirement, JobMatch                          | JobService, JobMatchingService     | JobRepository, JobEmbeddingRepository       |
| **Conversation** | Conversation, Message, SuggestedChange                 | ConversationService, ChatService   | ConversationRepository                      |
| **Tracking**     | JobApplication, Interview, ApplicationStatus           | TrackingService                    | TrackingRepository                          |
| **CAPTCHA**      | CaptchaChallenge, CaptchaToken                         | CaptchaService                     | -                                           |

#### 4.2.3 API Controllers

```java
// Controller Structure (Pseudo-code)

@RestController
@RequestMapping("/api/v1")
public class ResumeController {
    @PostMapping("/resumes/upload")
    public ResponseEntity<ResumeUploadResponse> uploadResume(@RequestParam MultipartFile file);

    @GetMapping("/resumes/{id}")
    public ResponseEntity<ResumeDTO> getResume(@PathVariable Long id);

    @PutMapping("/resumes/{id}")
    public ResponseEntity<ResumeDTO> updateResume(@PathVariable Long id, @RequestBody ResumeUpdateRequest request);
}

@RestController
@RequestMapping("/api/v1")
public class JobController {
    @GetMapping("/jobs")
    public ResponseEntity<Page<JobDTO>> searchJobs(JobSearchRequest request);

    @GetMapping("/jobs/{id}")
    public ResponseEntity<JobDTO> getJob(@PathVariable Long id);

    @GetMapping("/jobs/matches")
    public ResponseEntity<List<JobMatchDTO>> getMatchingJobs(@RequestParam Long resumeId);
}

@RestController
@RequestMapping("/api/v1")
public class ConversationController {
    @PostMapping("/conversations")
    public ResponseEntity<ConversationDTO> createConversation();

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageDTO> sendMessage(@PathVariable Long id, @RequestBody MessageRequest request);

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable Long id);
}

@RestController
@RequestMapping("/api/v1")
public class TrackingController {
    @PostMapping("/applications")
    public ResponseEntity<ApplicationDTO> createApplication(@RequestBody ApplicationRequest request);

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationDTO>> getApplications();

    @PutMapping("/applications/{id}/status")
    public ResponseEntity<ApplicationDTO> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request);
}

@RestController
@RequestMapping("/api/v1")
public class CaptchaController {
    @GetMapping("/auth/captcha")
    public ResponseEntity<CaptchaChallengeResponse> getCaptcha();

    @PostMapping("/auth/captcha/verify")
    public ResponseEntity<CaptchaVerifyResponse> verifyCaptcha(@RequestBody CaptchaVerifyRequest request);
}
```

### 4.3 Python AI Service

#### 4.3.1 Service Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Python AI Service Architecture                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         FastAPI Application                             │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │                         API Layer                                 │  │ │
│  │  │  - Health check endpoints                                        │  │ │
│  │  │  - Configuration endpoints                                       │  │ │
│  │  │  - Status endpoints                                              │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                      │                                      │
│                                      ▼                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                      Message Queue Consumer Layer                       │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐ │  │ │
│  │  │  │ Resume Parse     │  │ Job Parse        │  │ Conversation   │ │  │ │
│  │  │  │ Consumer         │  │ Consumer         │  │ Consumer       │ │  │ │
│  │  │  │                  │  │                  │  │                │ │  │ │
│  │  │  │ Queue:           │  │ Queue:           │  │ Queue:         │ │  │ │
│  │  │  │ ai.queue.resume  │  │ ai.queue.job     │  │ ai.queue.      │ │  │ │
│  │  │  │ .parse           │  │ .parse           │  │ conversation   │ │  │ │
│  │  │  └──────────────────┘  └──────────────────┘  └────────────────┘ │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                      │                                      │
│                                      ▼                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         AI Processing Engine                            │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │  │ │
│  │  │  │ Resume       │  │ Embedding    │  │ Match        │          │  │ │
│  │  │  │ Parser       │  │ Generator    │  │ Calculator   │          │  │ │
│  │  │  │              │  │              │  │              │          │  │ │
│  │  │  │ - PDF/Word   │  │ - LiteLLM    │  │ - Cosine     │          │  │ │
│  │  │  │   extraction │  │   embedding  │  │   similarity │          │  │ │
│  │  │  │ - LiteLLM    │  │   model      │  │ - Ranking    │          │  │ │
│  │  │  │   structured │  │ - Batch      │  │ - Filtering  │          │  │ │
│  │  │  │   output     │  │   processing │  │              │          │  │ │
│  │  │  └──────────────┘  └──────────────┘  └──────────────┘          │  │ │
│  │  │                                                                  │  │ │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │  │ │
│  │  │  │ Chat         │  │ Memory       │  │ RAG          │          │  │ │
│  │  │  │ Processor    │  │ Manager      │  │ Retriever    │          │  │ │
│  │  │  │              │  │              │  │              │          │  │ │
│  │  │  │ - LLM API    │  │ - Context    │  │ - Vector     │          │  │ │
│  │  │  │   calls      │  │   window     │  │   search     │          │  │ │
│  │  │  │ - Prompt     │  │ - Message    │  │ - Resume     │          │  │ │
│  │  │  │   templates  │  │   history    │  │   content    │          │  │ │
│  │  │  │ - Response   │  │ - Token      │  │   retrieval  │          │  │ │
│  │  │  │   streaming  │  │   management │  │              │          │  │ │
│  │  │  └──────────────┘  └──────────────┘  └──────────────┘          │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                      │                                      │
│                                      ▼                                      │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         Message Queue Publisher Layer                   │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │  - Result queue publishers                                       │  │ │
│  │  │  - Vector request publishers                                     │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 4.3.2 AI Modules

| Module                  | Technology / Implementation              | Purpose                                                |
| ----------------------- | ---------------------------------------- | ------------------------------------------------------ |
| **Resume Parser**       | PDF/DOCX text extraction + LiteLLM structured output | Extract structured resume data              |
| **Job Parser**          | URL/text parsing + optional vision fallback + LiteLLM | Extract structured job posting data          |
| **Embedding Generator** | Configured LiteLLM embedding model + pgvector | Generate vectors for semantic search              |
| **Match Calculator**    | PostgreSQL pgvector cosine distance + backend matching service | Recall and rank relevant jobs      |
| **Job Ranker**          | LiteLLM text model                       | Explain and rerank recalled jobs                       |
| **Chat Processor**      | Resume/job context + conversation history + LiteLLM | Process AI chat requests                    |
| **Context Builder**     | Resume/job repositories + conversation history | Assemble chat context from selected resume, job, and prior messages |

---

## 5. Data Architecture

### 5.1 Database Schema Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PostgreSQL Database Schema                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         Business Data Tables                            │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │ │
│  │  │    users     │  │   resumes    │  │     jobs     │                 │ │
│  │  │──────────────│  │──────────────│  │──────────────│                 │ │
│  │  │ id (PK)      │  │ id (PK)      │  │ id (PK)      │                 │ │
│  │  │ email        │  │ user_id (FK) │  │ title        │                 │ │
│  │  │ password     │  │ file_url     │  │ company      │                 │ │
│  │  │ name         │  │ status       │  │ description  │                 │ │
│  │  │ created_at   │  │ parsed_data  │  │ requirements │                 │ │
│  │  └──────────────┘  │ embedding_id │  │ location     │                 │ │
│  │                    └──────────────┘  │ salary_range │                 │ │
│  │                                      └──────────────┘                 │ │
│  │                                                                         │ │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐     │ │
│  │  │  conversations   │  │    messages      │  │ app_trackings    │     │ │
│  │  │──────────────────│  │──────────────────│  │──────────────────│     │ │
│  │  │ id (PK)          │  │ id (PK)          │  │ id (PK)          │     │ │
│  │  │ user_id (FK)     │  │ conversation_id  │  │ user_id (FK)     │     │ │
│  │  │ resume_version_id│  │ role             │  │ job_id (FK)      │     │ │
│  │  │ job_id           │  │ content          │  │ resume_id (FK)   │     │ │
│  │  │ ai_opt_version_id│  │ file_url         │  │ status           │     │ │
│  │  └──────────────────┘  └──────────────────┘  │ applied_at       │     │ │
│  │                                              └──────────────────┘     │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         Vector Tables (pgvector)                        │ │
│  │  ┌──────────────────────────┐  ┌──────────────────────────┐            │ │
│  │  │    resume_vectors        │  │      job_vectors         │            │ │
│  │  │──────────────────────────│  │──────────────────────────│            │ │
│  │  │ id (PK)                  │  │ id (PK)                  │            │ │
│  │  │ resume_version_id (FK)   │  │ job_id (FK)              │            │ │
│  │  │ embedding vector(1536)   │  │ embedding vector(1536)   │            │ │
│  │  │ status                   │  │ status                   │            │ │
│  │  │ error_message            │  │ raw_content              │            │ │
│  │  │ created_at               │  │ created_at               │            │ │
│  │  └──────────────────────────┘  └──────────────────────────┘            │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Detailed Table Definitions

#### 5.2.1 Users Table

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

#### 5.2.2 Resumes Table

```sql
CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_file_url VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size_bytes INTEGER,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED

    -- Parsed content (structured JSON)
    parsed_content JSONB,

    -- Summary for quick display (2-5KB)
    summary TEXT,

    -- Processing metadata
    processed_at TIMESTAMP,
    processing_error TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resumes_user_id ON resumes(user_id);
CREATE INDEX idx_resumes_status ON resumes(status);
```

#### 5.2.3 Resume Embeddings Table (pgvector)

```sql
-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS resume_vectors (
    id VARCHAR(64) PRIMARY KEY,
    resume_version_id VARCHAR(64) NOT NULL UNIQUE,
    embedding vector(1536),
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_resume_vectors_version_id ON resume_vectors (resume_version_id);
CREATE INDEX IF NOT EXISTS idx_resume_vectors_status ON resume_vectors (status);
```

#### 5.2.4 Jobs Table

```sql
CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,

    -- Basic info
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    company_logo_url VARCHAR(500),
    location VARCHAR(255),
    job_type VARCHAR(50), -- FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP

    -- Content
    description TEXT NOT NULL,
    requirements TEXT[],
    responsibilities TEXT[],
    skills_required TEXT[],

    -- Compensation
    salary_min INTEGER,
    salary_max INTEGER,
    salary_currency VARCHAR(3) DEFAULT 'USD',

    -- Metadata
    source VARCHAR(100), -- Where the job was scraped from
    external_id VARCHAR(255), -- ID from external source
    posted_at TIMESTAMP,
    expires_at TIMESTAMP,

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_jobs_company ON jobs(company);
CREATE INDEX idx_jobs_location ON jobs(location);
CREATE INDEX idx_jobs_skills ON jobs USING GIN (skills_required);
CREATE INDEX idx_jobs_active ON jobs(is_active) WHERE is_active = TRUE;
```

#### 5.2.5 Job Embeddings Table (pgvector)

```sql
CREATE TABLE IF NOT EXISTS job_vectors (
    id VARCHAR(64) PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL UNIQUE,
    embedding vector(1536),
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    title TEXT,
    description TEXT,
    requirements JSONB,
    raw_content TEXT,
    source_file VARCHAR(255),
    model_version VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_vectors_job_id ON job_vectors (job_id);
CREATE INDEX IF NOT EXISTS idx_job_vectors_status ON job_vectors (status);
```

#### 5.2.6 Conversations Table

```sql
CREATE TABLE conversations (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    title VARCHAR(200),
    status VARCHAR(32),
    resume_version_id VARCHAR(64),
    job_id VARCHAR(64),
    ai_optimized_version_id VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations(user_id);
CREATE INDEX IF NOT EXISTS idx_conversations_status ON conversations(status);
CREATE INDEX IF NOT EXISTS idx_conversations_job_id ON conversations(job_id);
CREATE INDEX IF NOT EXISTS idx_conversations_ai_optimized_version_id
    ON conversations(ai_optimized_version_id);
```

#### 5.2.7 Messages Table

```sql
CREATE TABLE messages (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT,
    sequence INT,
    file_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);
```

#### 5.2.8 Application Trackings Table

```sql
CREATE TABLE IF NOT EXISTS application_trackings (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    company_name VARCHAR(200),
    job_title VARCHAR(200),
    status VARCHAR(32) NOT NULL,
    applied_at DATE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    events JSONB
);

CREATE INDEX IF NOT EXISTS idx_application_trackings_user_id ON application_trackings(user_id);
CREATE INDEX IF NOT EXISTS idx_application_trackings_job_id ON application_trackings(job_id);
CREATE INDEX IF NOT EXISTS idx_application_trackings_status ON application_trackings(status);
```

### 5.3 Vector Storage Design

#### 5.3.1 Embedding Model Selection

| Model                             | Dimension | Provider       | Use Case                  |
| --------------------------------- | --------- | -------------- | ------------------------- |
| **gemini/gemini-embedding-001**   | 1536      | LiteLLM/Gemini | Default local configuration |
| openai/text-embedding-3-small     | 1536      | LiteLLM/OpenAI | Alternative option        |
| openai/text-embedding-3-large     | 3072      | LiteLLM/OpenAI | Higher-dimensional option |

**Selected Model**: configured by `LLM_EMBEDDING_MODEL`

- **Default Dimension**: 1536
- **Configuration**: `.env` controls provider, model, and dimension
- **Storage**: vectors are stored in PostgreSQL `pgvector`
- **Constraint**: `LLM_EMBEDDING_MODEL_DIMENSION` must match the database vector dimension

#### 5.3.2 Vector Search Query Examples

```sql
-- Find top 10 matching jobs for a resume
SELECT 
    job_id,
    title,
    1 - (embedding <=> CAST(:resumeVector AS vector(1536))) AS similarity_score
FROM job_vectors
WHERE status = 'COMPLETED'
ORDER BY embedding <=> CAST(:resumeVector AS vector(1536))
LIMIT 10;

-- Find resumes similar to a job description
SELECT 
    resume_version_id,
    1 - (embedding <=> CAST(:jobVector AS vector(1536))) AS similarity_score
FROM resume_vectors
WHERE status = 'COMPLETED'
ORDER BY embedding <=> CAST(:jobVector AS vector(1536))
LIMIT 5;

-- Hybrid search with structured filtering
SELECT 
    job_id,
    title,
    1 - (embedding <=> CAST(:queryVector AS vector(1536))) AS similarity_score
FROM job_vectors
WHERE status = 'COMPLETED'
  AND requirements ? 'Python'
ORDER BY embedding <=> CAST(:queryVector AS vector(1536))
LIMIT 10;
```

### 5.4 Data Flow Diagrams

#### 5.4.1 Resume Upload Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Resume Upload Data Flow                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  User          Frontend         Java Backend         PostgreSQL    RabbitMQ  │
│   │                │                  │                  │            │      │
│   │  1. Upload     │                  │                  │            │      │
│   │     Resume     │                  │                  │            │      │
│   │───────────────>│                  │                  │            │      │
│   │                │                  │                  │            │      │
│   │                │  2. Store File   │                  │            │      │
│   │                │─────────────────>│                  │            │      │
│   │                │                  │                  │            │      │
│   │                │                  │  3. Save Resume  │            │      │
│   │                │                  │  (status=PENDING)│            │      │
│   │                │                  │─────────────────>│            │      │
│   │                │                  │  4. Return ID    │            │      │
│   │                │                  │<─────────────────│            │      │
│   │                │                  │                  │            │      │
│   │                │                  │  5. Publish      │            │      │
│   │                │                  │     Parse Request│            │      │
│   │                │                  │──────────────────────────────>│      │
│   │                │  6. Return 202   │                  │            │      │
│   │                │<─────────────────│                  │            │      │
│   │  7. Upload ID  │                  │                  │            │      │
│   │<───────────────│                  │                  │            │      │
│   │                │                  │                  │            │      │
│   │  [Async: Python AI processes the message]          │            │      │
│   │                │                  │                  │            │      │
│   │                │                  │  8. Consume      │            │      │
│   │                │                  │     Result       │            │      │
│   │                │                  │<──────────────────────────────│      │
│   │                │                  │                  │            │      │
│   │                │                  │  9. Update       │            │      │
│   │                │                  │     Resume       │            │      │
│   │                │                  │     (status=COMPLETED)        │      │
│   │                │                  │─────────────────>│            │      │
│   │                │                  │                  │            │      │
│   │  10. Poll      │                  │                  │            │      │
│   │      Status    │                  │                  │            │      │
│   │───────────────>│                  │                  │            │      │
│   │                │  11. Get Resume  │                  │            │      │
│   │                │─────────────────>│                  │            │      │
│   │                │                  │  12. Query       │            │      │
│   │                │                  │─────────────────>│            │      │
│   │                │                  │  13. Return Data │            │      │
│   │                │                  │<─────────────────│            │      │
│   │                │  14. Return      │                  │            │      │
│   │                │<─────────────────│                  │            │      │
│   │  15. Parsed    │                  │                  │            │      │
│   │      Resume    │                  │                  │            │      │
│   │<───────────────│                  │                  │            │      │
│   │                │                  │                  │            │      │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 5.4.2 Conversation Message Flow

```
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  User   │───▶│  Frontend   │───▶│ Java Backend│───▶│ Save Message│───▶│ Publish MQ  │
│ Send    │    │ Call API    │    │ Receive     │    │ (USER)      │    │ (Async)     │
│ Message │    │             │    │ Request     │    │             │    │             │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘    └──────┬──────┘
                                                                                │
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
│ Frontend│◀───│ Poll/Push   │◀───│ Save AI     │◀───│ Consume MQ  │◀──────────┘
│ Display │    │ Get Messages│    │ Reply       │    │ Result      │
│         │    │             │    │ (ASSISTANT) │    │ (Python AI) │
└─────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

**File Upload Branch**:

```
Frontend ──▶ POST /api/v1/resumes ──▶ Store in shared-storage volume ──▶ Publish MQ task ──▶ AI service reads file
```

### 5.5 Redis Cache Design (CAPTCHA)

The CAPTCHA subsystem uses Redis for distributed caching with prefix-isolated keys, enabling cross-instance consistency and horizontal scalability:

| Cache Name | Purpose | Redis Key | Type | TTL |
| ---------- | ------- | --------- | ---- | --- |
| **Challenge Store** | Stores slider CAPTCHA challenges (target positions) | `ra:captcha:challenge:{id}` | String | 5 minutes |
| **Token Store** | Stores one-time verification tokens | `ra:captcha:token:{id}` | String | 5 minutes |
| **Rate Limit Window** | Tracks request timestamps per IP | `ra:captcha:ratelimit:{ip}` | Sorted Set | 1 minute |

**IP Rate Limiting**: Each IP address is limited to **20 CAPTCHA requests per minute**. Excess requests receive HTTP 429.

**Security Features**:
- Prefix isolation prevents cache key collisions between challenge, token, and rate-limit entries
- One-time token: Each `captchaToken` can only be redeemed once (consumed on validation)
- Max attempts: 5 verification attempts per challenge before invalidation
- V1 DOM-level verification: Frontend performs challenge solving without exposing the answer
- V2 Graphics2D puzzle evolution: Image-based challenges rendered server-side with Java 2D

---

## 6. Integration Architecture

### 6.1 Message Queue Design

#### 6.1.1 Queue Configuration

| Queue Name               | Type           | Producer     | Consumer     | Message Size | TTL    |
| ------------------------ | -------------- | ------------ | ------------ | ------------ | ------ |
| `ai.queue.resume.parse`        | Work Queue     | Java Backend | Python AI    | < 1KB        | 1 hour |
| `backend.queue.resume.parse`   | Result Queue   | Python AI    | Java Backend | 5-10KB       | 1 hour |
| `ai.queue.job.parse`           | Work Queue     | Java Backend | Python AI    | < 5KB        | 30 min |
| `backend.queue.job.parse`      | Result Queue   | Python AI    | Java Backend | < 5KB        | 30 min |
| `ai.queue.conversation`        | Work Queue     | Java Backend | Python AI    | < 5KB        | 5 min  |
| `backend.queue.conversation`   | Result Queue   | Python AI    | Java Backend | < 5KB        | 5 min  |
| `ai.queue.job.rank`            | Work Queue     | Java Backend | Python AI    | < 5KB        | 30 min |
| `backend.queue.job.rank`       | Result Queue   | Python AI    | Java Backend | < 5KB        | 30 min |
| `ai.dlq.queue`                 | Dead Letter    | RabbitMQ     | Operators    | varies       | -      |

#### 6.1.2 Message Schema Examples

**Resume Parse Request:**

```json
{
  "resumeId": "550e8400-e29b-41d4-a716-446655440002",
  "fileUrl": "http://backend:8080/api/v1/resumes/550e8400-e29b-41d4-a716-446655440002/download",
  "format": "pdf"
}
```

**Resume Parse Result:**

```json
{
  "referenceId": "550e8400-e29b-41d4-a716-446655440002",
  "type": "RESUME_PARSE",
  "status": "COMPLETED",
  "data": {
    "parsedContent": {
      "name": "...",
      "email": "...",
      "skills": ["Python", "Java", "React"],
      "experience": [...]
    },
    "summary": ""
  },
  "errorMessage": null,
  "eventType": "RESUME"
}
```

**Job Parse Request:**

```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440010",
  "url": "https://example.com/jobs/software-engineer",
  "imageCheckEnabled": false,
  "screenshotBase64": null
}
```

**Job Parse Result:**

```json
{
  "referenceId": "550e8400-e29b-41d4-a716-446655440010",
  "type": "JOB_PARSE",
  "status": "COMPLETED",
  "data": {
    "title": "Software Engineer",
    "company": "Example Corp",
    "description": "...",
    "requirements": ["Java", "React"]
  },
  "errorMessage": null,
  "eventType": "JOB"
}
```

**Conversation AI Request (Backend -> Python AI):**

```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "messageHistory": [
    { "role": "USER", "content": "How can I improve my resume?" }
  ],
  "currentMessage": "How can I improve my resume?",
  "fileUrls": [],
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
  "resumeText": "# Resume Markdown...",
  "primaryJobText": "Software Engineer\nExample Corp\nJob description...",
  "relatedJobTexts": ["Backend Engineer\nExample Corp\nRelated job description..."],
  "init": true,
  "locale": "en"
}
```

**Conversation AI Response (Python AI -> Backend):**

```json
{
  "referenceId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "CONVERSATION_REPLY",
  "status": "COMPLETED",
  "data": {
    "content": "Based on your resume, here are my suggestions...",
    "fileUrl": null,
    "resumeModification": {
      "modified": true,
      "markdown": "# Optimized Resume\n\n..."
    }
  },
  "errorMessage": null,
  "eventType": null
}
```

### 6.2 API Design

#### 6.2.1 REST API Endpoints

| Endpoint                                    | Method | Description                                   | Auth Required |
| ------------------------------------------- | ------ | --------------------------------------------- | ------------- |
| `/api/v1/auth/register/email`               | POST   | Email registration                            | No            |
| `/api/v1/auth/login/email`                  | POST   | Email login                                   | No            |
| `/api/v1/auth/login/google`                 | POST   | Google login                                  | No            |
| `/api/v1/auth/refresh`                      | POST   | Refresh access token                          | Yes           |
| `/api/v1/auth/logout`                       | POST   | Logout                                        | Yes           |
| `/api/v1/auth/captcha`                      | GET    | Get CAPTCHA challenge                         | No            |
| `/api/v1/auth/captcha/verify`               | POST   | Verify CAPTCHA and exchange for token         | No            |
| `/api/v1/profile`                           | GET    | Get current user profile                      | Yes           |
| `/api/v1/profile`                           | PUT    | Update user profile                           | Yes           |
| `/api/v1/profile/avatar`                    | PUT    | Update avatar URL                             | Yes           |
| `/api/v1/resumes`                           | POST   | Upload new resume                             | Yes           |
| `/api/v1/resumes/groups`                    | GET    | List resume groups                            | Yes           |
| `/api/v1/resumes/groups/{groupId}`          | GET    | Get resume group                              | Yes           |
| `/api/v1/resumes/groups/{groupId}`          | DELETE | Delete resume group                           | Yes           |
| `/api/v1/resumes/versions/{versionId}`      | GET    | Get resume version                            | Yes           |
| `/api/v1/resumes/versions/{versionId}`      | PUT    | Update resume version                         | Yes           |
| `/api/v1/resumes/versions/{versionId}`      | DELETE | Delete resume version                         | Yes           |
| `/api/v1/jobs`                              | GET    | List visible jobs                             | Yes           |
| `/api/v1/jobs`                              | POST   | Submit job URL and optional screenshot        | Yes           |
| `/api/v1/jobs/{jobId}`                      | GET    | Get job details                               | Yes           |
| `/api/v1/jobs/{jobId}`                      | PUT    | Update job parsed content                     | Yes           |
| `/api/v1/jobs/{jobId}`                      | DELETE | Hide job from user-facing lists               | Yes           |
| `/api/v1/jobs/{jobId}/score`                | POST   | Score one job against a resume                | Yes           |
| `/api/v1/jobs/match`                        | POST   | Start job matching                            | Yes           |
| `/api/v1/jobs/match/history`                | GET    | Get match history                             | Yes           |
| `/api/v1/conversations`                     | GET    | List conversations                            | Yes           |
| `/api/v1/conversations`                     | POST   | Create conversation                           | Yes           |
| `/api/v1/conversations/{conversationId}`    | GET    | Get conversation                              | Yes           |
| `/api/v1/conversations/{conversationId}`    | DELETE | Delete conversation                           | Yes           |
| `/api/v1/conversations/{conversationId}/close` | PUT | Close conversation                            | Yes           |
| `/api/v1/conversations/{conversationId}/messages` | POST | Send message                             | Yes           |
| `/api/v1/conversations/{conversationId}/files` | POST | Upload attachment                           | Yes           |
| `/api/v1/trackings`                         | GET    | List application tracking records             | Yes           |
| `/api/v1/trackings`                         | POST   | Create tracking record                        | Yes           |
| `/api/v1/trackings/{id}`                    | GET    | Get tracking record                           | Yes           |
| `/api/v1/trackings/{id}`                    | PUT    | Update tracking record                        | Yes           |
| `/api/v1/trackings/{id}`                    | DELETE | Delete tracking record                        | Yes           |
| `/api/v1/trackings/stats`                   | GET    | Get tracking statistics                       | Yes           |

#### 6.2.2 API Response Format

```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

Error Response:

```json
{
  "code": 400,
  "message": "Invalid input data",
  "data": null
}
```

### 6.3 Service Communication Patterns

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Service Communication Patterns                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Pattern 1: Synchronous Request-Response (Frontend ↔ Backend)               │
│  ┌──────────┐         HTTPS/REST          ┌──────────┐                     │
│  │ Frontend │  ────────────────────────>  │ Backend  │                     │
│  │          │  <────────────────────────  │          │                     │
│  └──────────┘                             └──────────┘                     │
│                                                                             │
│  Pattern 2: Asynchronous Work Queue (Backend → AI Service)                  │
│  ┌──────────┐    Publish    ┌─────────┐    Consume    ┌──────────┐        │
│  │ Backend  │ ────────────> │ RabbitMQ│ ────────────> │ AI Svc   │        │
│  │          │               │         │               │          │        │
│  │          │ <──────────── │         │ <──────────── │          │        │
│  └──────────┘    Consume    └─────────┘    Publish    └──────────┘        │
│                  Result                      Result                         │
│                                                                             │
│  Pattern 3: Database as Integration Point (All Services)                    │
│  ┌──────────┐         ┌──────────────┐         ┌──────────┐                │
│  │ Backend  │ ──────> │ PostgreSQL   │ <────── │ AI Svc   │                │
│  │          │  JDBC   │ + pgvector   │  JDBC   │ (Vectors)│                │
│  └──────────┘         └──────────────┘         └──────────┘                │
│                                                                             │
│  Pattern 4: Vector Request via MQ (AI Service → Backend)                    │
│  ┌──────────┐    Request    ┌─────────┐    Response   ┌──────────┐        │
│  │ AI Svc   │ ────────────> │ RabbitMQ│ ────────────> │ Backend  │        │
│  │ (needs   │               │         │               │ (queries │        │
│  │  vector) │ <──────────── │         │ <──────────── │  DB)     │        │
│  └──────────┘    Response   └─────────┘    Request    └──────────┘        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. AI Integration

### 7.1 AI Technologies Overview

| #   | Technology                               | Implementation                         | Purpose                                     |
| --- | ---------------------------------------- | -------------------------------------- | ------------------------------------------- |
| 1   | **Structured Outputs**                   | LiteLLM text/vision model + JSON schema prompts | Parse resumes and jobs into structured JSON |
| 2   | **Vector Search / Embeddings**           | LiteLLM embedding model + PostgreSQL pgvector | Semantic matching between resumes and jobs  |
| 3   | **RAG / Context Injection**              | Resume content + job content + conversation history | Ground chat responses in user data |
| 4   | **LLM API Integration**                  | LiteLLM-compatible provider abstraction | Switch between Gemini, OpenAI, Vertex, etc. |
| 5   | **AI Job Ranking / Explanation**         | LiteLLM ranking prompt over recalled jobs | Explain and prioritize job matches          |

### 7.2 AI Component Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           AI Integration Architecture                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         AI Service Layer                                │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │                     1. Resume Parser Module                       │  │ │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │  │ │
│  │  │  │ PDF Extractor│  │ Docx         │  │ LiteLLM      │          │  │ │
│  │  │  │ (pypdf)      │  │ Extractor    │  │ Structured   │          │  │ │
│  │  │  │              │  │ (OpenXML ZIP)│  │ Output       │          │  │ │
│  │  │  └──────────────┘  └──────────────┘  └──────────────┘          │  │ │
│  │  │                              │                                  │  │ │
│  │  │                              ▼                                  │  │ │
│  │  │  ┌──────────────────────────────────────────────────────────┐  │  │ │
│  │  │  │              Structured Resume JSON                       │  │  │ │
│  │  │  │  {personalInfo, skills, workExperience, education}       │  │  │ │
│  │  │  └──────────────────────────────────────────────────────────┘  │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  │                                                                         │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │                  2. Embedding Generator Module                    │  │ │
│  │  │  ┌────────────────────────────────────────────────────────────┐ │  │ │
│  │  │  │  Model: configured by LLM_EMBEDDING_MODEL                  │ │  │ │
│  │  │  │  Default dimension: 1536                                   │ │  │ │
│  │  │  │  Input: Resume text summary                                │ │  │ │
│  │  │  │  Output: Dense vector embedding                            │ │  │ │
│  │  │  └────────────────────────────────────────────────────────────┘ │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  │                                                                         │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │                   3. Match Calculator Module                      │  │ │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │  │ │
│  │  │  │ Vector       │  │ Similarity   │  │ Ranking &    │          │  │ │
│  │  │  │ Retrieval    │  │ Calculation  │  │ Filtering    │          │  │ │
│  │  │  │ (pgvector)   │  │ (Cosine)     │  │              │          │  │ │
│  │  │  └──────────────┘  └──────────────┘  └──────────────┘          │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  │                                                                         │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │                    4. Chat Processor Module                       │  │ │
│  │  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │  │ │
│  │  │  │ RAG          │  │ Memory       │  │ LLM          │          │  │ │
│  │  │  │ Retriever    │  │ Manager      │  │ Processor    │          │  │ │
│  │  │  │              │  │              │  │              │          │  │ │
│  │  │  │ - Context    │  │ - History    │  │ - LiteLLM    │          │  │ │
│  │  │  │   injection  │  │   window     │  │   model      │          │  │ │
│  │  │  │ - Resume     │  │ - Message    │  │ - Prompt     │          │  │ │
│  │  │  │   content    │  │   history    │  │   templates  │          │  │ │
│  │  │  └──────────────┘  └──────────────┘  └──────────────┘          │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.3 Resume Parser Design

#### 7.3.1 Parsing Pipeline

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Resume Parsing Pipeline                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Input: PDF/Word Resume                                                      │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────┐                                                │
│  │  Step 1: Text Extraction │                                               │
│  │  - pypdf for PDF files   │                                               │
│  │  - OpenXML ZIP for DOCX  │                                               │
│  └───────────┬─────────────┘                                               │
│              │ Raw Text                                                     │
│              ▼                                                              │
│  ┌─────────────────────────┐                                                │
│  │  Step 2: Text Cleaning   │                                               │
│  │  - Remove special chars  │                                               │
│  │  - Normalize whitespace  │                                               │
│  │  - Truncate if too long  │                                               │
│  └───────────┬─────────────┘                                               │
│              │ Cleaned Text                                                 │
│              ▼                                                              │
│  ┌─────────────────────────┐                                                │
│  │  Step 3: AI Extraction   │                                               │
│  │  - LiteLLM structured    │                                               │
│  │    output prompt         │                                               │
│  │  - Structured output     │                                               │
│  │    schema                │                                               │
│  └───────────┬─────────────┘                                               │
│              │ Structured JSON                                              │
│              ▼                                                              │
│  ┌─────────────────────────┐                                                │
│  │  Step 4: Validation      │                                               │
│  │  - Schema validation     │                                               │
│  │  - Data type checking    │                                               │
│  │  - Required fields check │                                               │
│  └───────────┬─────────────┘                                               │
│              │ Validated Data                                               │
│              ▼                                                              │
│  ┌─────────────────────────┐                                                │
│  │  Step 5: Summary Gen     │                                               │
│  │  - Generate 2-5KB        │                                               │
│  │    condensed summary     │                                               │
│  │  - For quick display     │                                               │
│  └───────────┬─────────────┘                                               │
│              │                                                              │
│              ▼                                                              │
│  Output: ParsedResume + Summary + Embedding                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 7.3.2 Structured Output Schema

```json
{
  "personalInfo": {
    "name": "string",
    "email": "string",
    "phone": "string",
    "location": "string",
    "linkedin": "string",
    "website": "string"
  },
  "summary": "string",
  "skills": ["string"],
  "workExperience": [
    {
      "company": "string",
      "title": "string",
      "location": "string",
      "startDate": "YYYY-MM",
      "endDate": "YYYY-MM or Present",
      "description": "string",
      "achievements": ["string"]
    }
  ],
  "education": [
    {
      "institution": "string",
      "degree": "string",
      "field": "string",
      "startDate": "YYYY-MM",
      "endDate": "YYYY-MM",
      "gpa": "string"
    }
  ],
  "certifications": [
    {
      "name": "string",
      "issuer": "string",
      "date": "YYYY-MM"
    }
  ],
  "projects": [
    {
      "name": "string",
      "description": "string",
      "technologies": ["string"],
      "url": "string"
    }
  ],
  "languages": [
    {
      "language": "string",
      "proficiency": "string"
    }
  ]
}
```

### 7.4 RAG Implementation

#### 7.4.1 Context Injection Flow for Chat

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Context Injection Flow for Chat Conversations              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  User Query: "How can I improve my resume for software engineering roles?"  │
│                                                                             │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 1: Query Understanding                                         │   │
│  │  - Analyze user intent                                               │   │
│  │  - Extract relevant keywords                                         │   │
│  └────────────────────┬────────────────────────────────────────────────┘   │
│                       │                                                     │
│                       ▼                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 2: Static Context Loading                                      │   │
│  │                                                                       │   │
│  │  2a. Load selected resume text                                        │   │
│  │       ┌──────────────┐                                                │   │
│  │       │ Resume       │ ──> Current or AI-optimized Markdown           │   │
│  │       │ Version      │                                                │   │
│  │       └──────────────┘                                                │   │
│  │                                                                       │   │
│  │  2b. Load primary job text                                            │   │
│  │       ┌──────────────┐                                                │   │
│  │       │ Job          │ ──> Current job description and requirements    │   │
│  │       │ Repository   │                                                │   │
│  │       └──────────────┘                                                │   │
│  │                                                                       │   │
│  │  2c. Load related jobs and conversation history                       │   │
│  │       ┌──────────────┐                                                │   │
│  │       │ Context      │ ──> Up to 5 completed jobs + recent messages   │   │
│  │       │ Builder      │                                                │   │
│  │       └──────────────┘                                                │   │
│  └────────────────────┬────────────────────────────────────────────────┘   │
│                       │                                                     │
│                       ▼                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 3: Context Assembly                                            │   │
│  │                                                                       │   │
│  │  Context = System Prompt + Resume Text + Job Text + History + Query  │   │
│  │                                                                       │   │
│  │  ┌────────────────────────────────────────────────────────────────┐  │   │
│  │  │ System: "You are a helpful resume optimization assistant..."   │  │   │
│  │  │                                                                  │  │   │
│  │  │ Resume Context:                                                  │  │   │
│  │  │ - Skills: Python, Java, React...                                 │  │   │
│  │  │ - Experience: 5 years at XYZ Corp...                             │  │   │
│  │  │                                                                  │  │   │
│  │  │ Conversation History:                                            │  │   │
│  │  │ - User: "I want to apply for senior roles"                       │  │   │
│  │  │ - Assistant: "Great! Let's highlight your leadership..."         │  │   │
│  │  │                                                                  │  │   │
│  │  │ Current Query: "How can I improve my resume..."                  │  │   │
│  │  │ Optional output: resumeModification.markdown                     │  │   │
│  │  └────────────────────────────────────────────────────────────────┘  │   │
│  └────────────────────┬────────────────────────────────────────────────┘   │
│                       │                                                     │
│                       ▼                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 4: LLM Generation                                              │   │
│  │  - Send assembled context to the configured LiteLLM model             │   │
│  │  - Stream response back to user                                      │   │
│  └────────────────────┬────────────────────────────────────────────────┘   │
│                       │                                                     │
│                       ▼                                                     │
│  AI Response: "Based on your experience with Python and 5 years at..."      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.5 LLM API Integration

#### 7.5.1 API Client Design

```python
# Pseudo-code for LLM API Client with retry logic

class LLMClient:
    def __init__(self, api_key: str, model: str = "gpt-4"):
        self.api_key = api_key
        self.model = model
        self.max_retries = 3
        self.retry_delay = 1  # seconds

    async def generate(
        self,
        messages: List[Message],
        temperature: float = 0.7,
        max_tokens: int = 1000,
        functions: Optional[List[Function]] = None
    ) -> LLMResponse:
        """
        Generate response with retry logic and structured logging.
        """
        for attempt in range(self.max_retries):
            try:
                response = await self._call_api(
                    messages=messages,
                    temperature=temperature,
                    max_tokens=max_tokens,
                    functions=functions
                )

                # Log usage when the provider returns usage metadata
                self._log_usage(response.usage)

                return response

            except RateLimitError:
                if attempt < self.max_retries - 1:
                    await asyncio.sleep(self.retry_delay * (2 ** attempt))
                else:
                    raise
            except APIError as e:
                if attempt < self.max_retries - 1:
                    continue
                raise LLMException(f"API call failed: {e}")

    def _log_usage(self, usage: TokenUsage):
        """Log token usage for operational monitoring."""
        metrics.record("llm.tokens.input", usage.prompt_tokens)
        metrics.record("llm.tokens.output", usage.completion_tokens)
```

#### 7.5.2 Provider Cost Awareness

| Model         | Input Cost ($/1M tokens) | Output Cost ($/1M tokens) |
| ------------- | ------------------------ | ------------------------- |
| GPT-4         | $30.00                   | $60.00                    |
| GPT-4 Turbo   | $10.00                   | $30.00                    |
| GPT-3.5 Turbo | $0.50                    | $1.50                     |

The actual cost depends on the configured LiteLLM provider and model. The current code logs LLM failures and supports retries; persistent per-user cost accounting is not stored in the application database.

---

## 8. Security Architecture

### 8.1 Authentication & Authorization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Security Architecture                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                        Authentication Flow                              │ │
│  │                                                                        │ │
│  │  User          Frontend         Backend          JWT           DB      │ │
│  │   │                │                │              │            │      │ │
│  │   │  1. Login      │                │              │            │      │ │
│  │   │  (email/pwd)   │                │              │            │      │ │
│  │   │───────────────>│                │              │            │      │ │
│  │   │                │  2. POST /login│              │            │      │ │
│  │   │                │───────────────>│              │            │      │ │
│  │   │                │                │  3. Validate │            │      │ │
│  │   │                │                │     Password │            │      │ │
│  │   │                │                │─────────────>│            │      │ │
│  │   │                │                │  4. User OK  │            │      │ │
│  │   │                │                │<─────────────│            │      │ │
│  │   │                │                │              │            │      │ │
│  │   │                │                │  5. Generate │            │      │ │
│  │   │                │                │     JWT      │            │      │ │
│  │   │                │                │─────────────>│            │      │ │
│  │   │                │                │  6. Token    │            │      │ │
│  │   │                │                │<─────────────│            │      │ │
│  │   │                │  7. Return     │              │            │      │ │
│  │   │                │     Token      │              │            │      │ │
│  │   │                │<───────────────│              │            │      │ │
│  │   │  8. Store      │                │              │            │      │ │
│  │   │     Token      │                │              │            │      │ │
│  │   │<───────────────│                │              │            │      │ │
│  │   │                │                │              │            │      │ │
│  │   │  [Subsequent requests with JWT in Authorization header]     │      │ │
│  │   │                │                │              │            │      │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                        Authorization (RBAC)                             │ │
│  │                                                                        │ │
│  │  Role: USER                                                            │ │
│  │   - Access own resumes, jobs, conversations, applications              │ │
│  │   - Cannot access other users' data                                    │ │
│  │                                                                        │ │
│  │  Role: ADMIN (future)                                                  │ │
│  │   - All USER permissions                                               │ │
│  │   - Access to admin dashboard                                          │ │
│  │   - User management                                                    │ │
│  │                                                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 8.2 Security Measures

| Category              | Measure               | Implementation                                      |
| --------------------- | --------------------- | --------------------------------------------------- |
| **Authentication**    | JWT Tokens            | HS256 algorithm, 24h access token, 7d refresh token |
| **Password Security** | BCrypt Hashing        | 12 rounds of BCrypt                                 |
| **HTTPS**             | TLS 1.3               | All communications encrypted                        |
| **CORS**              | Whitelist             | Only allow frontend domain                          |
| **Input Validation**  | Bean Validation       | @Valid annotations on DTOs                          |
| **SQL Injection**     | Parameterized Queries | JPA/Hibernate prepared statements                   |
| **XSS Prevention**    | Output Encoding       | React automatic escaping                            |
| **Rate Limiting**     | Bucket Algorithm      | 100 requests/minute per IP                          |
| **Human Verification**| CAPTCHA (Redis)       | Challenge-response with IP rate limit (20/min)      |

### 8.3 Data Protection

| Data Type    | Storage        | Protection                  |
| ------------ | -------------- | --------------------------- |
| Passwords    | PostgreSQL     | BCrypt hashed               |
| JWT Secrets  | Environment    | Docker secrets              |
| API Keys     | Environment    | Docker secrets              |
| Resume Files | Object Storage | Signed URLs, access control |
| PII          | PostgreSQL     | Encryption at rest          |

---

## 9. Deployment Architecture

### 9.1 Docker Compose Configuration

```yaml
# docker-compose.yml (Simplified)
version: '3.8'

services:
  # 1. Frontend Service
  frontend:
    build: ./frontend
    ports:
      - "${FRONTEND_HOST_PORT:-80}:8080"
    depends_on:
      - backend
    networks:
      - public-network

  # 2. Java Backend Service
  backend:
    build: ./backend
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/resume_assistant
      - SPRING_RABBITMQ_HOST=rabbitmq
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - postgres
      - rabbitmq
    networks:
      - public-network
      - internal-network
      - db-network

  # 3. Python AI Service
  ai-service:
    build: ./ai-service
    environment:
      - RABBITMQ_HOST=rabbitmq
      - BACKEND_SERVICE_URL=http://backend:8080
      - LLM_TEXT_MODEL=${LLM_TEXT_MODEL:-gemini/gemini-2.5-flash}
      - MODEL_STORAGE_BASE_PATH=/app/model-artifacts
    depends_on:
      - rabbitmq
      - redis
    networks:
      - internal-network
    volumes:
      - shared-storage:/app/uploads:ro
      - model-artifacts:/app/model-artifacts

  # 4. PostgreSQL Database
  postgres:
    build: ./middleware/postgres
    environment:
      - POSTGRESQL_DATABASE=${POSTGRES_DB:-resume_assistant}
      - POSTGRESQL_USERNAME=${POSTGRES_USER:-resume_user}
      - POSTGRESQL_PASSWORD=${POSTGRES_PASSWORD:-resume_pass}
    volumes:
      - postgres-data:/bitnami/postgresql
    networks:
      - db-network

  # 5. RabbitMQ Message Queue
  rabbitmq:
    image: rabbitmq:3-management
    environment:
      - RABBITMQ_DEFAULT_USER=${RABBITMQ_USERNAME:-guest}
      - RABBITMQ_DEFAULT_PASS=${RABBITMQ_PASSWORD}
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    networks:
      - internal-network

  # 6. Redis Shared State
  redis:
    image: redis:7-alpine
    volumes:
      - redis-data:/data
    networks:
      - internal-network

volumes:
  postgres-data:
  rabbitmq-data:
  redis-data:
  shared-storage:
  model-artifacts:

networks:
  public-network:
    driver: bridge
  internal-network:
    driver: bridge
  db-network:
    driver: bridge
```

### 9.2 Service Topology

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Docker Compose Deployment                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         Host Machine                                    │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │ │
│  │  │                    Docker Network (bridge)                        │  │ │
│  │  │                                                                  │  │ │
│  │  │  ┌─────────────┐                                                │  │ │
│  │  │  │  Frontend   │  Host: ${FRONTEND_HOST_PORT:-80} -> 8080       │  │ │
│  │  │  │  (Nginx +   │                                                │  │ │
│  │  │  │   React)    │                                                │  │ │
│  │  │  └──────┬──────┘                                                │  │ │
│  │  │         │ HTTPS/REST                                             │  │ │
│  │  │         ▼                                                        │  │ │
│  │  │  ┌─────────────┐                                                │  │ │
│  │  │  │  Backend    │  Port: 8080 (internal only)                   │  │ │
│  │  │  │  (Spring    │                                                │  │ │
│  │  │  │   Boot)     │                                                │  │ │
│  │  │  └──────┬──────┘                                                │  │ │
│  │  │         │                                                        │  │ │
│  │  │    ┌────┴────┐                                                   │  │ │
│  │  │    │         │                                                   │  │ │
│  │  │    ▼         ▼ RabbitMQ                                          │  │ │
│  │  │  ┌─────────────┐  ┌─────────────┐                                │  │ │
│  │  │  │  AI Service │  │  RabbitMQ   │  Port: 5672 (internal only)   │  │ │
│  │  │  │  (FastAPI)  │  │             │                                │  │ │
│  │  │  │  Port: 8000 │  │             │                                │  │ │
│  │  │  └──────┬──────┘  └─────────────┘                                │  │ │
│  │  │         │                                                        │  │ │
│  │  │         └──────────────┐                                         │  │ │
│  │  │                        │ JDBC                                     │  │ │
│  │  │                        ▼                                         │  │ │
│  │  │  ┌─────────────────────────────────────────┐                     │  │ │
│  │  │  │  PostgreSQL 15 + pgvector              │  Port: 5432 internal │  │ │
│  │  │  │  - Business data                        │                     │  │ │
│  │  │  │  - Vector embeddings                    │                     │  │ │
│  │  │  └─────────────────────────────────────────┘                     │  │ │
│  │  │                                                                  │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  │                                                                         │ │
│  │  Volumes:                                                               │ │
│  │  - postgres-data:/bitnami/postgresql                                  │ │
│  │  - rabbitmq-data:/var/lib/rabbitmq                                     │ │
│  │  - redis-data:/data                                                   │ │
│  │  - shared-storage:/app/uploads                                        │ │
│  │  - model-artifacts:/app/model-artifacts                               │ │
│  │                                                                         │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 9.3 Port Mapping

| Service             | Container Port | Host Port | Purpose           |
| ------------------- | -------------- | --------- | ----------------- |
| Frontend            | 8080           | `${FRONTEND_HOST_PORT:-80}` | Web application and API reverse proxy |
| Backend             | 8080           | Not exposed by default      | Internal REST API behind Nginx |
| AI Service          | 8000           | Not exposed by default      | Internal AI processing API |
| PostgreSQL          | 5432           | Not exposed by default      | Database access from backend |
| RabbitMQ            | 5672           | Not exposed by default      | Internal message broker |
| RabbitMQ Management | 15672          | Not exposed by default      | Dev-only management UI if port mapping is uncommented |
| Redis               | 6379           | Not exposed by default      | Internal shared state for AI model adaptation |

---

## 10. Scalability and Performance

### 10.1 Performance Targets

| Metric                   | Target  | Measurement                |
| ------------------------ | ------- | -------------------------- |
| API Response Time (p95)  | < 200ms | For non-AI endpoints       |
| Resume Upload Processing | < 30s   | End-to-end with AI parsing |
| Job Match Query          | < 500ms | Vector similarity search   |
| Chat Response            | < 3s    | Full AI reply delivery     |
| Concurrent Users         | 1000    | Supported simultaneously   |
| System Availability      | 99.9%   | Uptime target              |

### 10.2 Scalability Considerations

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Scalability Roadmap                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Current (Single Instance):                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  Frontend   │  │   Backend   │  │  AI Service │  │ PostgreSQL  │        │
│  │   (1)       │  │     (1)     │  │     (1)     │  │     (1)     │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                                             │
│  Phase 1 - Horizontal Scaling (Backend & AI):                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  Frontend   │  │   Backend   │  │  AI Service │  │ PostgreSQL  │        │
│  │   (1)       │  │   (N)       │  │    (N)      │  │     (1)     │        │
│  └─────────────┘  │  + LB       │  │   + LB      │  └─────────────┘        │
│                   └─────────────┘  └─────────────┘                         │
│                                                                             │
│  Phase 2 - Database Scaling:                                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐    │
│  │  Frontend   │  │   Backend   │  │  AI Service │  │ PostgreSQL      │    │
│  │   (N)       │  │   (N)       │  │    (N)      │  │  Primary-Replica│    │
│  │  + CDN      │  │  + LB       │  │   + LB      │  │  + Read Replicas│    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘    │
│                                                                             │
│  Phase 3 - Microservices (Future):                                          │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐           │
│  │  API    │  │  User   │  │  Resume │  │   Job   │  │  Chat   │           │
│  │ Gateway │  │ Service │  │ Service │  │ Service │  │ Service │           │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 10.3 Performance Optimization Strategies

| Area         | Strategy           | Implementation                        |
| ------------ | ------------------ | ------------------------------------- |
| **Database** | Connection Pooling | HikariCP (max 20 connections)         |
| **Database** | Query Optimization | Indexes on frequently queried columns |
| **Database** | Vector Index       | IVFFlat for approximate search        |
| **Caching**  | Redis (future)     | Cache job listings, user sessions     |
| **CDN**      | Static Assets      | CloudFront/Cloudflare for frontend    |
| **AI**       | Batch Processing   | Process multiple resumes in batches   |
| **AI**       | Embedding Caching  | Cache embeddings for similar content  |

### 10.4 Monitoring and Observability

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Monitoring Architecture                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         Metrics Collection                              │ │
│  │                                                                        │ │
│  │  Application Metrics:                                                  │ │
│  │  - Request count, latency, error rate                                  │ │
│  │  - Queue depth, processing time                                        │ │
│  │  - AI API calls, token usage when provider metadata is available       │ │
│  │                                                                        │ │
│  │  System Metrics:                                                       │ │
│  │  - CPU, memory, disk usage                                             │ │
│  │  - Database connections, query time                                    │ │
│  │  - Message queue metrics                                               │ │
│  │                                                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         Logging                                          │ │
│  │                                                                        │ │
│  │  - Structured JSON logs                                                │ │
│  │  - Correlation IDs for request tracing                                 │ │
│  │  - Error logs with stack traces                                        │ │
│  │  - Audit logs for security events                                      │ │
│  │                                                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         Alerting                                         │ │
│  │                                                                        │ │
│  │  - High error rate (> 5%)                                              │ │
│  │  - High latency (p95 > 1s)                                             │ │
│  │  - Queue backlog (> 1000 messages)                                     │ │
│  │  - AI API failures                                                     │ │
│  │  - Database connection issues                                          │ │
│  │                                                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 11. Appendix

### 11.1 Glossary

| Term          | Definition                                                                            |
| ------------- | ------------------------------------------------------------------------------------- |
| **DDD**       | Domain-Driven Design - Software design approach focused on modeling business domains  |
| **RAG**       | Retrieval-Augmented Generation - AI technique combining retrieval with LLM generation |
| **pgvector**  | PostgreSQL extension for vector similarity search                                     |
| **Embedding** | Numerical vector representation of text for semantic search                           |
| **IVFFlat**   | Index type for approximate nearest neighbor search                                    |
| **JWT**       | JSON Web Token - Compact, URL-safe token format                                       |
| **BCrypt**    | Password hashing function with salt                                                   |
| **MQ**        | Message Queue - Asynchronous communication mechanism                                  |

### 11.2 References

1. [Domain-Driven Design: Tackling Complexity in the Heart of Software](https://www.amazon.com/Domain-Driven-Design-Tackling-Complexity-Software/dp/0321125215) -
   Eric Evans
2. [pgvector Documentation](https://github.com/pgvector/pgvector) - GitHub
3. [LiteLLM Documentation](https://docs.litellm.ai/) - LiteLLM
4. [OpenAI API Documentation](https://platform.openai.com/docs) - OpenAI-compatible provider option
5. [Spring Boot Documentation](https://spring.io/projects/spring-boot) - VMware
6. [FastAPI Documentation](https://fastapi.tiangolo.com/) - Sebastián Ramírez
7. [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html) - VMware

### 11.3 Architecture Decision Records (ADRs)

#### ADR-001: Use PostgreSQL + pgvector for Unified Storage

**Status**: Accepted

**Context**: We need to store both business data and vector embeddings. Options:

- Separate databases (PostgreSQL + Pinecone/Milvus)
- Unified storage with pgvector

**Decision**: Use PostgreSQL + pgvector for unified storage.

**Consequences**:

- (+) Simpler deployment and backup
- (+) Single connection pool
- (+) ACID transactions across business and vector data
- (-) Slightly less optimized than dedicated vector DB

#### ADR-002: Use Message Queue for AI Service Communication

**Status**: Accepted

**Context**: AI processing is asynchronous and may take time. Options:

- Synchronous HTTP calls
- Asynchronous message queue

**Decision**: Use RabbitMQ for async communication.

**Consequences**:

- (+) Better decoupling
- (+) Retry and dead letter handling
- (+) Can scale AI workers independently
- (-) Added complexity

#### ADR-003: Use LiteLLM-compatible Embedding Models

**Status**: Accepted

**Context**: The system needs embeddings for semantic job matching. The model provider should be configurable so the project can run with Gemini, OpenAI, Vertex AI, or another LiteLLM-supported provider.

**Decision**: Use LiteLLM-compatible embedding models and store vectors in PostgreSQL `pgvector`.

**Consequences**:

- (+) Provider can be changed through `.env`
- (+) Docker deployment does not depend on a local ML model download
- (+) Embedding model version is tracked with vector data
- (-) The configured embedding dimension must match the database vector dimension

---

## Document End

*This architecture document is a living document and will be updated as the system evolves.*

**Document Version**: 1.0.0  
**Last Updated**: 2025-01  
**Next Review**: 2025-04
