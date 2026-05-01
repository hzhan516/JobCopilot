<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](ai-mq-interfaces.en_US.md) | [简体中文](ai-mq-interfaces.zh-Hans-CN.md) | [繁體中文](ai-mq-interfaces.zh-Hant-TW.md)

# AI Service Interaction API and MQ Event Contracts Summary

> This document consolidates all REST APIs (if any) and RabbitMQ message queue contracts between the Java backend and the Python AI service.
> For detailed module descriptions, please also refer to `job.md`, `conversation.md`, and `resume.md`.

---

## 1. Interaction Principles

- **One-way asynchronous**: The Java backend sends task requests via MQ; the Python AI service returns results via MQ after processing.
- **No direct HTTP coupling**: Except for health checks, the Java backend does not directly call the AI service via HTTP; all time-consuming operations go through the message queue.
- **Unified Exchange**: All MQ messages share `ai.direct.exchange` (DirectExchange).

---

## 2. MQ Topology Overview

| Direction | Task Type | Routing Key | Queue | Producer | Consumer |
|-----------|-----------|-------------|-------|----------|----------|
| Request → AI | Job Parse | `ai.req.job.parse` | `ai.queue.job.parse` | Java Backend | Python AI |
| Response ← AI | Job Parse Result | `backend.res.job.parse` | `backend.queue.job.parse` | Python AI | Java Backend |
| Request → AI | Resume Parse | `ai.req.resume.parse` | `ai.queue.resume.parse` | Java Backend | Python AI |
| Response ← AI | Resume Parse Result | `backend.res.resume.parse` | `backend.queue.resume.parse` | Python AI | Java Backend |
| Request → AI | Vector Gen | `ai.req.vector.gen` | `ai.queue.vector.gen` | Java Backend | Python AI |
| Response ← AI | Vector Gen Result | `backend.res.vector.gen` | `backend.queue.vector.gen` | Python AI | Java Backend |
| Request → AI | Conversation | `ai.req.conversation` | `ai.queue.conversation` | Java Backend | Python AI |
| Response ← AI | Conversation Reply | `backend.res.conversation` | `backend.queue.conversation` | Python AI | Java Backend |
| Request → AI | Job Rank | `ai.req.job.rank` | `ai.queue.job.rank` | Java Backend | Python AI |
| Response ← AI | Job Rank Result | `backend.res.job.rank` | `backend.queue.job.rank` | Python AI | Java Backend |

---

## 3. Request Commands (Backend → AI)

### 3.1 JobParseCommand — Job Parse Request

**Trigger**: After the user submits a job link, `JobApplicationService.submitJob()`

```json
{
  "jobId": "job-uuid-1234",
  "url": "https://www.linkedin.com/jobs/view/12345",
  "imageCheckEnabled": true
}
```

| Field | Type | Description |
|-------|------|-------------|
| `jobId` | String | Job unique identifier |
| `url` | String | Job detail page URL |
| `imageCheckEnabled` | boolean | Whether visual verification is enabled |

---

### 3.2 ResumeParseCommand — Resume Parse Request

**Trigger**: After the user uploads a resume, `ResumeApplicationService.handleUpload()`

```json
{
  "resumeId": "resume-uuid-5678",
  "fileUrl": "https://minio.example.com/resumes/xxx.pdf",
  "fileType": "PDF"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `resumeId` | String | Resume version unique identifier |
| `fileUrl` | String | Resume file URL on MinIO |
| `fileType` | String | File type, e.g. PDF, DOCX |

---

### 3.3 VectorGenCommand — Vector Generation Request

**Trigger**: Triggered asynchronously after job/resume parsing is completed

```json
{
  "referenceId": "entity-uuid",
  "entityType": "JOB",
  "text": "职位或简历的纯文本内容"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `referenceId` | String | Associated entity ID (jobId or resumeVersionId) |
| `entityType` | String | `JOB` / `RESUME` |
| `text` | String | Text to generate vectors from |

---

### 3.4 ConversationRequestCommand — Conversation AI Request

**Trigger**: When the user sends a conversation message, `ConversationApplicationService.sendMessage()`

```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "messageHistory": [
    { "role": "USER", "content": "帮我优化一下项目经验部分", "fileUrl": null }
  ],
  "currentMessage": "帮我优化一下项目经验部分",
  "fileUrls": ["https://minio.example.com/resumes/xxx.pdf"],
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `conversationId` | String | Conversation ID |
| `userId` | String | User ID |
| `messageHistory` | List<Map> | Historical message list (role, content, fileUrl) |
| `currentMessage` | String | Latest message sent by the user |
| `fileUrls` | List<String> | List of external file URLs referenced by the user |
| `resumeVersionId` | String | Associated resume version ID (optional) |

---

## 4. Result Events (AI → Backend)

### Unified Event Structure: AiResultEvent

All AI callbacks use the following unified structure, distinguished by the `type` field:

```json
{
  "referenceId": "关联实体ID",
  "type": "JOB_PARSE | RESUME_PARSE | VECTOR_GEN | CONVERSATION_REPLY",
  "status": "COMPLETED | FAILED",
  "data": { ... },
  "errorMessage": null,
  "eventType": "内部路由标记"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `referenceId` | String | Associated entity ID |
| `type` | String | Event type |
| `status` | String | `COMPLETED` or `FAILED` |
| `data` | Map<String, Object> | Business data payload |
| `errorMessage` | String | Failure reason (required when `status=FAILED`) |
| `eventType` | String | Internal sub-routing tag (e.g. VECTOR_GEN distinguishes JOB/RESUME) |

---

### 4.1 Job Parse Result (type = JOB_PARSE)

**Consumer**: `AiResultMessageListener.onJobParseResult()` → `JobFacade.handleJobProcessResult()`

```json
{
  "referenceId": "job-uuid-1234",
  "type": "JOB_PARSE",
  "status": "COMPLETED",
  "data": {
    "title": "Software Engineer",
    "company": "Tech Corp",
    "description": "Full job description...",
    "requirements": ["Java", "Spring Boot", "AWS"]
  },
  "errorMessage": null,
  "eventType": "JOB"
}
```

---

### 4.2 Resume Parse Result (type = RESUME_PARSE)

**Consumer**: `AiResultMessageListener.onResumeParseResult()` → `ResumeFacade.handleParseResult()`

```json
{
  "referenceId": "resume-uuid-5678",
  "type": "RESUME_PARSE",
  "status": "COMPLETED",
  "data": {
    "parsedContent": { "name": "...", "skills": [...], "experience": [...] },
    "summary": "5年Java开发经验..."
  },
  "errorMessage": null,
  "eventType": "RESUME"
}
```

---

### 4.3 Vector Gen Result (type = VECTOR_GEN)

**Consumer**: `AiResultMessageListener.onVectorGenResult()`

```json
{
  "referenceId": "entity-uuid",
  "type": "VECTOR_GEN",
  "status": "COMPLETED",
  "data": {
    "embedding": [0.1, 0.2, ...],
    "entityType": "JOB"
  },
  "errorMessage": null,
  "eventType": "JOB"
}
```

---

### 4.4 Conversation Reply Result (type = CONVERSATION_REPLY)

**Consumer**: `AiResultMessageListener.onConversationReply()` → `ConversationFacade.saveAiReply()`

```json
{
  "referenceId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "CONVERSATION_REPLY",
  "status": "COMPLETED",
  "data": {
    "content": "根据您的简历，我建议从以下几个方面优化工作经验...",
    "fileUrl": "https://minio.example.com/conversations/xxx/optimized_resume.pdf"
  },
  "errorMessage": null,
  "eventType": null
}
```

| data Sub-field | Type | Description |
|----------------|------|-------------|
| `content` | String | AI reply text content |
| `fileUrl` | String | AI generated file URL (optional) |

---

## 5. File Upload and MinIO

### 5.1 Backend File Upload API

The frontend or AI layer can upload generated file streams to the backend, which then stores them in MinIO:

- **Resume Upload**: `POST /api/v1/resumes` (`multipart/form-data`)
- **Conversation Attachment Upload**: `POST /api/v1/conversations/{conversationId}/files` (`multipart/form-data`)

### 5.2 MinIO Storage Path Conventions

| Business | Object Key Prefix Example |
|----------|---------------------------|
| Resume files | `resumes/{uuid}_{filename}` |
| Conversation attachments | `conversations/{conversationId}/{uuid}_{filename}` |

### 5.3 Presigned URL

`MinioFileStorageService.generatePresignedUrl()` generates a temporary access URL for successfully uploaded files (default 7-day validity).

---

## 6. Status Codes and Error Handling

- `COMPLETED`: AI processing succeeded, `data` contains valid results.
- `FAILED`: AI processing failed, `errorMessage` must contain specific error text, `data` may be empty.
- The consumer wraps exceptions in try-catch to prevent MQ messages from retrying infinitely and blocking the queue.
