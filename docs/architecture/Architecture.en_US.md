<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](Architecture.en_US.md) | [简体中文](Architecture.zh-Hans-CN.md) | [繁體中文](Architecture.zh-Hant-TW.md)

# Intelligent Job Assistant - Architecture Document

**[中文](Architecture.zh-Hans-CN.md) | English**

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

| Layer             | Technology                        | Version | Purpose                 |
| ----------------- | --------------------------------- | ------- | ----------------------- |
| **Frontend**      | React + TypeScript + Tailwind CSS | 18.x    | User interface          |
| **Backend**       | Java Spring Boot                  | 3.x     | Business logic, API     |
| **AI Service**    | Python FastAPI                    | Latest  | AI/ML processing        |
| **Database**      | PostgreSQL + pgvector             | 15      | Business + vector data  |
| **Message Queue** | RabbitMQ                          | 3.x     | Async communication     |
| **Deployment**    | Docker Compose                    | -       | Container orchestration |

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
│  │                     React 18 + TypeScript + Tailwind CSS                      │   │
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
│  │  ResumeController | JobController | ChatController | TrackController        │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
│                                          │                                          │
│  ┌───────────────────────────────────────▼──────────────────────────────────────┐   │
│  │                        Application Service Layer                               │   │
│  │  ResumeAppService | JobAppService | ChatAppService | TrackAppService         │   │
│  └───────────────────────────────────────▼──────────────────────────────────────┘   │
│                                          │                                          │
│  ┌───────────────────────────────────────▼──────────────────────────────────────┐   │
│  │                              Domain Layer                                      │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │   │
│  │  │  User    │  │  Resume  │  │   Job    │  │  Chat    │  │  Track   │      │   │
│  │  │  Domain  │  │  Domain  │  │  Domain  │  │  Domain  │  │  Domain  │      │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘      │   │
│  └───────────────────────────────────────▼──────────────────────────────────────┘   │
│                                          │                                          │
│  ┌───────────────────────────────────────▼──────────────────────────────────────┐   │
│  │                          Infrastructure Layer                                  │   │
│  │  Repository | MQ Publisher | MQ Consumer | PGVector Client | Auth Service | MinIO │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                          │
                                          │ RabbitMQ
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              PYTHON AI SERVICE LAYER                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                          Message Consumers                                     │   │
│  │  ResumeParseConsumer | JobMatchConsumer | ChatConsumer                       │   │
│  └───────────────────────────────────────▼──────────────────────────────────────┘   │
│                                          │                                          │
│  ┌───────────────────────────────────────▼──────────────────────────────────────┐   │
│  │                           AI Processing Engine                                 │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │   │
│  │  │ Resume       │  │ Embedding    │  │ Match        │  │ Chat         │      │   │
│  │  │ Parser       │  │ Generator    │  │ Calculator   │  │ Processor    │      │   │
│  │  │ (PyPDF2 +    │  │ (Sentence-   │  │ (Similarity  │  │ (RAG +       │      │   │
│  │  │  OpenAI)     │  │  Transformers│  │  Ranking)    │  │  Memory)     │      │   │
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
│  │  │  - users                     │  │  - resume_embeddings            │     │   │
│  │  │  - resumes                   │  │  - job_embeddings               │     │   │
│  │  │  - jobs                      │  │                                 │     │   │
│  │  │  - conversations             │  │  Unified database management    │     │   │
│  │  │  - messages                  │  │                                 │     │   │
│  │  └──────────────────────────────┘  └─────────────────────────────────┘     │   │
│  └─────────────────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────────────────┐   │
│  │                              RabbitMQ                                         │   │
│  │  - Message Broker for Async Communication                                     │   │
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
│   │                │  POST /api/resumes/upload          │              │            │
│   │                │─────────────────>│                  │              │            │
│   │                │                  │                  │              │            │
│   │                │                  │  Save Resume     │              │            │
│   │                │                  │  (Pending)       │              │            │
│   │                │                  │  ────────────────│              │            │
│   │                │                  │                  │              │            │
│   │                │                  │  Publish Message │              │            │
│   │                │                  │  ai.resume.parse │              │            │
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
│   │                │  GET /api/resumes/{id}              │              │            │
│   │                │─────────────────>│                  │              │            │
│   │                │  Return Result   │                  │              │            │
│   │                │<─────────────────│                  │              │            │
│   │  Parsed Data   │                  │                  │              │            │
│   │<───────────────│                  │                  │              │            │
│   │                │                  │                  │              │            │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Component Design

### 4.1 Frontend Layer

#### 4.1.1 Technology Stack

| Component        | Technology   | Version | Purpose                 |
| ---------------- | ------------ | ------- | ----------------------- |
| Framework        | React        | 18.x    | UI component library    |
| Language         | TypeScript   | 5.x     | Type-safe development   |
| Styling          | Tailwind CSS | 3.x     | Utility-first CSS       |
| State Management | React Query  | Latest  | Server state management |
| HTTP Client      | Axios        | Latest  | API communication       |
| Build Tool       | Vite         | Latest  | Fast development build  |

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
public class ChatController {
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
│  │  │  │ Resume Parse     │  │ Job Match        │  │ Chat Message   │ │  │ │
│  │  │  │ Consumer         │  │ Consumer         │  │ Consumer       │ │  │ │
│  │  │  │                  │  │                  │  │                │ │  │ │
│  │  │  │ Queue:           │  │ Queue:           │  │ Queue:         │ │  │ │
│  │  │  │ ai.resume.parse  │  │ ai.job.match     │  │ ai.chat.message│ │  │ │
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
│  │  │  │ - PDF/Word   │  │ - Sentence   │  │ - Cosine     │          │  │ │
│  │  │  │   extraction │  │   Transformer│  │   similarity │          │  │ │
│  │  │  │ - OpenAI     │  │   (384-dim)  │  │ - Ranking    │          │  │ │
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

| Module                  | Technology                               | Purpose                                                |
| ----------------------- | ---------------------------------------- | ------------------------------------------------------ |
| **Resume Parser**       | PyPDF2, python-docx, OpenAI GPT-4        | Extract structured data from PDF/Word resumes          |
| **Embedding Generator** | sentence-transformers (all-MiniLM-L6-v2) | Generate 384-dimensional embeddings                    |
| **Match Calculator**    | pgvector, cosine similarity              | Calculate semantic similarity between resumes and jobs |
| **Chat Processor**      | OpenAI GPT-4, LangChain                  | Process conversational queries with RAG                |
| **Memory Manager**      | Custom implementation                    | Manage conversation history and context                |
| **RAG Retriever**       | pgvector, embedding search               | Retrieve relevant resume content for context           |

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
│  │  │  conversations   │  │    messages      │  │ job_applications │     │ │
│  │  │──────────────────│  │──────────────────│  │──────────────────│     │ │
│  │  │ id (PK)          │  │ id (PK)          │  │ id (PK)          │     │ │
│  │  │ user_id (FK)     │  │ conversation_id  │  │ user_id (FK)     │     │ │
│  │  │ resume_id (FK)   │  │ role             │  │ job_id (FK)      │     │ │
│  │  │ title            │  │ content          │  │ resume_id (FK)   │     │ │
│  │  │ created_at       │  │ created_at       │  │ status           │     │ │
│  │  └──────────────────┘  └──────────────────┘  │ applied_at       │     │ │
│  │                                              └──────────────────┘     │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         Vector Tables (pgvector)                        │ │
│  │  ┌──────────────────────────┐  ┌──────────────────────────┐            │ │
│  │  │   resume_embeddings      │  │    job_embeddings        │            │ │
│  │  │──────────────────────────│  │──────────────────────────│            │ │
│  │  │ id (PK)                  │  │ id (PK)                  │            │ │
│  │  │ resume_id (FK)           │  │ job_id (FK)              │            │ │
│  │  │ embedding VECTOR(384)    │  │ embedding VECTOR(384)    │            │ │
│  │  │ metadata JSONB           │  │ metadata JSONB           │            │ │
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

CREATE TABLE resume_embeddings (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,

    -- 384-dimensional embedding (all-MiniLM-L6-v2)
    embedding VECTOR(384) NOT NULL,

    -- Metadata for filtering and context
    metadata JSONB DEFAULT '{}',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(resume_id)
);

-- Vector similarity search index (IVFFlat for approximate search)
CREATE INDEX idx_resume_embeddings_vector ON resume_embeddings 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- GIN index for metadata filtering
CREATE INDEX idx_resume_embeddings_metadata ON resume_embeddings USING GIN (metadata);
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
CREATE TABLE job_embeddings (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,

    -- 384-dimensional embedding
    embedding VECTOR(384) NOT NULL,

    -- Metadata for filtering and context
    metadata JSONB DEFAULT '{}',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(job_id)
);

-- Vector similarity search index
CREATE INDEX idx_job_embeddings_vector ON job_embeddings 
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE INDEX idx_job_embeddings_metadata ON job_embeddings USING GIN (metadata);
```

#### 5.2.6 Conversations Table

```sql
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE SET NULL,

    -- Conversation metadata
    title VARCHAR(255),
    context TEXT, -- Initial context for the conversation

    -- Status
    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_resume_id ON conversations(resume_id);
```

#### 5.2.7 Messages Table

```sql
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,

    -- Message content
    role VARCHAR(20) NOT NULL, -- 'user', 'assistant', 'system'
    content TEXT NOT NULL,

    -- AI-specific fields
    model_used VARCHAR(50),
    tokens_used INTEGER,

    -- For resume optimization suggestions
    suggested_changes JSONB, -- Structured change suggestions

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);
```

#### 5.2.8 Job Applications Table

```sql
CREATE TABLE job_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE SET NULL,

    -- Application details
    status VARCHAR(50) DEFAULT 'APPLIED', -- APPLIED, SCREENING, INTERVIEW, OFFER, REJECTED, WITHDRAWN
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Source tracking
    source VARCHAR(100), -- Where the application was submitted
    application_url VARCHAR(500),

    -- Notes
    notes TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_applications_user_id ON job_applications(user_id);
CREATE INDEX idx_applications_status ON job_applications(status);
CREATE INDEX idx_applications_applied_at ON job_applications(applied_at);
```

### 5.3 Vector Storage Design

#### 5.3.1 Embedding Model Selection

| Model                  | Dimension | Performance             | Use Case                  |
| ---------------------- | --------- | ----------------------- | ------------------------- |
| **all-MiniLM-L6-v2**   | 384       | Fast, good quality      | Selected for this project |
| all-mpnet-base-v2      | 768       | Slower, better quality  | Alternative option        |
| text-embedding-ada-002 | 1536      | API-based, high quality | Cloud option              |

**Selected Model**: `sentence-transformers/all-MiniLM-L6-v2`

- **Dimension**: 384
- **Max Sequence Length**: 256 tokens
- **Average Performance**: Strong semantic similarity
- **Speed**: Fast inference on CPU

#### 5.3.2 Vector Search Query Examples

```sql
-- Find top 10 matching jobs for a resume
SELECT 
    j.id,
    j.title,
    j.company,
    1 - (je.embedding <=> resume_embedding) AS similarity_score
FROM jobs j
JOIN job_embeddings je ON j.id = je.job_id
WHERE j.is_active = TRUE
ORDER BY je.embedding <=> resume_embedding
LIMIT 10;

-- Find resumes similar to a job description
SELECT 
    r.id,
    r.summary,
    1 - (re.embedding <=> job_embedding) AS similarity_score
FROM resumes r
JOIN resume_embeddings re ON r.id = re.resume_id
WHERE r.user_id = :user_id
ORDER BY re.embedding <=> job_embedding
LIMIT 5;

-- Hybrid search with metadata filtering
SELECT 
    j.id,
    j.title,
    j.company,
    1 - (je.embedding <=> query_embedding) AS similarity_score
FROM jobs j
JOIN job_embeddings je ON j.id = je.job_id
WHERE j.is_active = TRUE
  AND j.location = 'Remote'
  AND je.metadata @> '{"skills": ["Python"]}'
ORDER BY je.embedding <=> query_embedding
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
AI Service / Frontend ──▶ Call Backend Upload API ──▶ Store to MinIO ──▶ Return Presigned URL ──▶ Update Message(fileUrl)
```

---

## 6. Integration Architecture

### 6.1 Message Queue Design

#### 6.1.1 Queue Configuration

| Queue Name               | Type           | Producer     | Consumer     | Message Size | TTL    |
| ------------------------ | -------------- | ------------ | ------------ | ------------ | ------ |
| `ai.resume.parse`        | Work Queue     | Java Backend | Python AI    | < 1KB        | 1 hour |
| `ai.resume.parse.result` | Result Queue   | Python AI    | Java Backend | 5-10KB       | 1 hour |
| `ai.job.match`           | Work Queue     | Java Backend | Python AI    | < 5KB        | 30 min |
| `ai.job.match.result`    | Result Queue   | Python AI    | Java Backend | < 5KB        | 30 min |
| `ai.chat.message`        | Work Queue     | Java Backend | Python AI    | < 5KB        | 5 min  |
| `ai.chat.message.result` | Result Queue   | Python AI    | Java Backend | < 5KB        | 5 min  |
| `ai.conversation`        | Work Queue     | Java Backend | Python AI    | < 5KB        | 5 min  |
| `ai.conversation.result` | Result Queue   | Python AI    | Java Backend | < 5KB        | 5 min  |
| `ai.vector.request`      | Request Queue  | Python AI    | Java Backend | < 1KB        | 1 min  |
| `ai.vector.response`     | Response Queue | Java Backend | Python AI    | 2-5KB        | 1 min  |

#### 6.1.2 Message Schema Examples

**Resume Parse Request:**

```json
{
  "messageId": "uuid-v4",
  "timestamp": "2025-01-15T10:30:00Z",
  "type": "RESUME_PARSE_REQUEST",
  "payload": {
    "resumeId": 12345,
    "fileUrl": "https://storage.example.com/resumes/12345.pdf",
    "fileType": "PDF"
  }
}
```

**Resume Parse Result:**

```json
{
  "messageId": "uuid-v4",
  "correlationId": "original-message-id",
  "timestamp": "2025-01-15T10:31:30Z",
  "type": "RESUME_PARSE_RESULT",
  "payload": {
    "resumeId": 12345,
    "status": "SUCCESS",
    "parsedContent": {
      "personalInfo": { "name": "...", "email": "..." },
      "skills": ["Python", "Java", "React"],
      "workExperience": [...],
      "education": [...]
    },
    "summary": "Software engineer with 5 years...",
    "embedding": [0.023, -0.156, ...] // 384 dimensions
  }
}
```

**Job Match Request:**

```json
{
  "messageId": "uuid-v4",
  "timestamp": "2025-01-15T11:00:00Z",
  "type": "JOB_MATCH_REQUEST",
  "payload": {
    "resumeId": 12345,
    "userId": 67890,
    "filters": {
      "location": "Remote",
      "jobType": "FULL_TIME"
    },
    "limit": 10
  }
}
```

**Chat Message Request:**

```json
{
  "messageId": "uuid-v4",
  "timestamp": "2025-01-15T12:00:00Z",
  "type": "CHAT_MESSAGE_REQUEST",
  "payload": {
    "conversationId": 98765,
    "userId": 67890,
    "message": "How can I improve my resume for software engineering roles?",
    "context": {
      "resumeId": 12345,
      "previousMessages": [...]
    }
  }
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
  "fileUrls": ["https://minio.example.com/resumes/xxx.pdf"],
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002"
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
    "fileUrl": "https://minio.example.com/conversations/xxx/optimized.pdf"
  },
  "errorMessage": null,
  "eventType": null
}
```

### 6.2 API Design

#### 6.2.1 REST API Endpoints

| Endpoint                                    | Method | Description                                   | Auth Required |
| ------------------------------------------- | ------ | --------------------------------------------- | ------------- |
| `/api/v1/auth/register`                     | POST   | User registration                             | No            |
| `/api/v1/auth/login`                        | POST   | User login                                    | No            |
| `/api/v1/auth/refresh`                      | POST   | Refresh access token                          | Yes           |
| `/api/v1/users/me`                          | GET    | Get current user profile                      | Yes           |
| `/api/v1/users/me`                          | PUT    | Update user profile                           | Yes           |
| `/api/v1/resumes`                           | GET    | List user resumes                             | Yes           |
| `/api/v1/resumes`                           | POST   | Upload new resume                             | Yes           |
| `/api/v1/resumes/{id}`                      | GET    | Get resume details                            | Yes           |
| `/api/v1/resumes/{id}`                      | PUT    | Update resume                                 | Yes           |
| `/api/v1/resumes/{id}`                      | DELETE | Delete resume                                 | Yes           |
| `/api/v1/jobs`                              | GET    | Search jobs                                   | Yes           |
| `/api/v1/jobs/{id}`                         | GET    | Get job details                               | Yes           |
| `/api/v1/jobs/matches`                      | GET    | Get matching jobs                             | Yes           |
| `/api/v1/conversations`                     | GET    | List conversations                            | Yes           |
| `/api/v1/conversations`                     | POST   | Create conversation                           | Yes           |
| `/api/v1/conversations/{id}`                | GET    | Get conversation                              | Yes           |
| `/api/v1/conversations/{id}/messages`       | GET    | Get messages                                  | Yes           |
| `/api/v1/conversations/{id}/messages`       | POST   | Send message                                  | Yes           |
| `/api/v1/conversations/{id}/files`          | POST   | Upload attachment                             | Yes           |
| `/api/v1/conversations/{id}?page=0&size=20` | GET    | Get conversation details (paginated messages) | Yes           |
| `/api/v1/applications`                      | GET    | List applications                             | Yes           |
| `/api/v1/applications`                      | POST   | Create application                            | Yes           |
| `/api/v1/applications/{id}`                 | PUT    | Update application                            | Yes           |

#### 6.2.2 API Response Format

```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "timestamp": "2025-01-15T10:30:00Z",
    "requestId": "uuid-v4"
  }
}
```

Error Response:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": [
      { "field": "email", "message": "Email is required" }
    ]
  },
  "meta": {
    "timestamp": "2025-01-15T10:30:00Z",
    "requestId": "uuid-v4"
  }
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

| #   | Technology                               | Implementation                   | Purpose                                     |
| --- | ---------------------------------------- | -------------------------------- | ------------------------------------------- |
| 1   | **Vector Search/Embeddings**             | sentence-transformers + pgvector | Semantic matching between resumes and jobs  |
| 2   | **Structured Outputs**                   | OpenAI Function Calling          | Parse resumes into structured JSON          |
| 3   | **LLM API Integration**                  | OpenAI API with retry logic      | AI assistant for chat and optimization      |
| 4   | **Memory/Conversation Management**       | Custom context window            | Dialogue history management                 |
| 5   | **RAG (Retrieval-Augmented Generation)** | pgvector + prompt engineering    | Retrieve resume content as dialogue context |

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
│  │  │  │ PDF Extractor│  │ Docx         │  │ OpenAI       │          │  │ │
│  │  │  │ (PyPDF2)     │  │ Extractor    │  │ Structured   │          │  │ │
│  │  │  │              │  │ (python-docx)│  │ Output       │          │  │ │
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
│  │  │  │  Model: sentence-transformers/all-MiniLM-L6-v2             │ │  │ │
│  │  │  │  Dimension: 384                                            │ │  │ │
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
│  │  │  │ - Vector     │  │ - Context    │  │ - OpenAI     │          │  │ │
│  │  │  │   search     │  │   window     │  │   GPT-4      │          │  │ │
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
│  │  - PyPDF2 for PDF files  │                                               │
│  │  - python-docx for DOCX  │                                               │
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
│  │  - OpenAI GPT-4 with     │                                               │
│  │    function calling      │                                               │
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

#### 7.4.1 RAG Flow for Chat

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        RAG Flow for Chat Conversations                        │
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
│  │  Step 2: Context Retrieval (RAG)                                     │   │
│  │                                                                       │   │
│  │  2a. Generate query embedding                                         │   │
│  │       ┌──────────────┐                                                │   │
│  │       │  Embedding   │ ──> Query Vector (384-dim)                     │   │
│  │       │  Generator   │                                                │   │
│  │       └──────────────┘                                                │   │
│  │                                                                       │   │
│  │  2b. Search resume content                                            │   │
│  │       ┌──────────────┐                                                │   │
│  │       │  pgvector    │ ──> Top-K relevant resume sections             │   │
│  │       │  Search      │                                                │   │
│  │       └──────────────┘                                                │   │
│  │                                                                       │   │
│  │  2c. Retrieve conversation history                                    │   │
│  │       ┌──────────────┐                                                │   │
│  │       │  Memory      │ ──> Recent messages (last N)                   │   │
│  │       │  Manager     │                                                │   │
│  │       └──────────────┘                                                │   │
│  └────────────────────┬────────────────────────────────────────────────┘   │
│                       │                                                     │
│                       ▼                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 3: Context Assembly                                            │   │
│  │                                                                       │   │
│  │  Context = System Prompt + Resume Content + History + User Query     │   │
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
│  │  └────────────────────────────────────────────────────────────────┘  │   │
│  └────────────────────┬────────────────────────────────────────────────┘   │
│                       │                                                     │
│                       ▼                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 4: LLM Generation                                              │   │
│  │  - Send assembled context to OpenAI GPT-4                            │   │
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
        Generate response with retry logic and cost tracking.
        """
        for attempt in range(self.max_retries):
            try:
                response = await self._call_api(
                    messages=messages,
                    temperature=temperature,
                    max_tokens=max_tokens,
                    functions=functions
                )

                # Track usage
                self._track_usage(response.usage)

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

    def _track_usage(self, usage: TokenUsage):
        """Track token usage for cost monitoring."""
        # Log to monitoring system
        metrics.record("llm.tokens.input", usage.prompt_tokens)
        metrics.record("llm.tokens.output", usage.completion_tokens)
        metrics.record("llm.cost", self._calculate_cost(usage))
```

#### 7.5.2 Cost Tracking

| Model         | Input Cost ($/1M tokens) | Output Cost ($/1M tokens) |
| ------------- | ------------------------ | ------------------------- |
| GPT-4         | $30.00                   | $60.00                    |
| GPT-4 Turbo   | $10.00                   | $30.00                    |
| GPT-3.5 Turbo | $0.50                    | $1.50                     |

**Estimated Monthly Cost** (1000 active users, 50 messages/user):

- Input tokens: ~25M = $250 (GPT-4 Turbo)
- Output tokens: ~10M = $300 (GPT-4 Turbo)
- **Total: ~$550/month**

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
# docker-compose.en_US.yml (Simplified)
version: '3.8'

services:
  # 1. Frontend Service
  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - job-assistant-network

  # 2. Java Backend Service
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/jobassistant
      - SPRING_RABBITMQ_HOST=rabbitmq
      - JWT_SECRET=${JWT_SECRET}
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    depends_on:
      - postgres
      - rabbitmq
    networks:
      - job-assistant-network

  # 3. Python AI Service
  ai-service:
    build: ./ai-service
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=postgresql://postgres:5432/jobassistant
      - RABBITMQ_URL=amqp://rabbitmq:5672
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    depends_on:
      - postgres
      - rabbitmq
    networks:
      - job-assistant-network

  # 4. PostgreSQL Database
  postgres:
    image: ankane/pgvector:latest
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=jobassistant
      - POSTGRES_USER=jobassistant
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - job-assistant-network

  # 5. RabbitMQ Message Queue
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      - RABBITMQ_DEFAULT_USER=jobassistant
      - RABBITMQ_DEFAULT_PASS=${RABBITMQ_PASSWORD}
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
│  │  │  │  Frontend   │  Port: 80 (host) -> 80 (container)             │  │ │
│  │  │  │  (Nginx +   │                                                │  │ │
│  │  │  │   React)    │                                                │  │ │
│  │  │  └──────┬──────┘                                                │  │ │
│  │  │         │ HTTPS/REST                                             │  │ │
│  │  │         ▼                                                        │  │ │
│  │  │  ┌─────────────┐                                                │  │ │
│  │  │  │  Backend    │  Port: 8080 (host) -> 8080 (container)         │  │ │
│  │  │  │  (Spring    │                                                │  │ │
│  │  │  │   Boot)     │                                                │  │ │
│  │  │  └──────┬──────┘                                                │  │ │
│  │  │         │                                                        │  │ │
│  │  │    ┌────┴────┐                                                   │  │ │
│  │  │    │         │                                                   │  │ │
│  │  │    ▼         ▼ RabbitMQ                                          │  │ │
│  │  │  ┌─────────────┐  ┌─────────────┐                                │  │ │
│  │  │  │  AI Service │  │  RabbitMQ   │  Port: 5672, 15672            │  │ │
│  │  │  │  (FastAPI)  │  │             │                                │  │ │
│  │  │  │  Port: 8000 │  │             │                                │  │ │
│  │  │  └──────┬──────┘  └─────────────┘                                │  │ │
│  │  │         │                                                        │  │ │
│  │  │         └──────────────┐                                         │  │ │
│  │  │                        │ JDBC                                     │  │ │
│  │  │                        ▼                                         │  │ │
│  │  │  ┌─────────────────────────────────────────┐                     │  │ │
│  │  │  │  PostgreSQL 15 + pgvector              │  Port: 5432          │  │ │
│  │  │  │  - Business data                        │                     │  │ │
│  │  │  │  - Vector embeddings                    │                     │  │ │
│  │  │  └─────────────────────────────────────────┘                     │  │ │
│  │  │                                                                  │  │ │
│  │  └──────────────────────────────────────────────────────────────────┘  │ │
│  │                                                                         │ │
│  │  Volumes:                                                               │ │
│  │  - postgres_data:/var/lib/postgresql/data                              │ │
│  │  - rabbitmq_data:/var/lib/rabbitmq                                     │ │
│  │                                                                         │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 9.3 Port Mapping

| Service             | Container Port | Host Port | Purpose           |
| ------------------- | -------------- | --------- | ----------------- |
| Frontend            | 80             | 80        | Web application   |
| Backend             | 8080           | 8080      | REST API          |
| AI Service          | 8000           | 8000      | AI processing API |
| PostgreSQL          | 5432           | 5432      | Database access   |
| RabbitMQ            | 5672           | 5672      | Message broker    |
| RabbitMQ Management | 15672          | 15672     | Web management UI |

---

## 10. Scalability and Performance

### 10.1 Performance Targets

| Metric                   | Target  | Measurement                |
| ------------------------ | ------- | -------------------------- |
| API Response Time (p95)  | < 200ms | For non-AI endpoints       |
| Resume Upload Processing | < 30s   | End-to-end with AI parsing |
| Job Match Query          | < 500ms | Vector similarity search   |
| Chat Response            | < 3s    | First token from LLM       |
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
│  │  - AI API calls, token usage, cost                                     │ │
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
3. [Sentence Transformers Documentation](https://www.sbert.net/) - UKPLab
4. [OpenAI API Documentation](https://platform.openai.com/docs) - OpenAI
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

#### ADR-003: Use sentence-transformers for Embeddings

**Status**: Accepted

**Context**: Need to generate embeddings for semantic search. Options:

- OpenAI text-embedding-ada-002 (API-based)
- sentence-transformers (local)

**Decision**: Use sentence-transformers/all-MiniLM-L6-v2.

**Consequences**:

- (+) No API costs for embeddings
- (+) Fast local inference
- (+) 384-dim vectors (efficient storage)
- (-) Slightly lower quality than OpenAI

---

## Document End

*This architecture document is a living document and will be updated as the system evolves.*

**Document Version**: 1.0.0  
**Last Updated**: 2025-01  
**Next Review**: 2025-04
