<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](ai-mq-interfaces.md) | [简体中文](../../i18n/zh-Hans-CN/api/backend/ai-mq-interfaces.md) | [繁體中文](../../i18n/zh-Hant-TW/api/backend/ai-mq-interfaces.md)

# AI Service Interaction API and MQ Event Contracts Summary

> This document consolidates all REST APIs (if any) and RabbitMQ message queue contracts between the Java backend and the Python AI service.
> For detailed module descriptions, please also refer to `job.md`, `conversation.md`, and `resume.md`.

---

## 1. Interaction Principles

- **One-way asynchronous**: The Java backend sends task requests via MQ; the Python AI service returns results via MQ after processing.
- **Async-first AI processing**: Long-running parsing, conversation, and ranking work goes through MQ. The backend also has synchronous REST calls to the AI service for lightweight embedding, suitability-scoring, and model recompute endpoints.
- **Unified Exchange**: All MQ messages share `ai.direct.exchange` (DirectExchange).
- **Transactional Outbox**: The backend does **not** send MQ messages directly. Instead, it persists them into the `outbox_message` table within the same local database transaction as the business data. An `OutboxRelayScheduler` polls pending records every 2 seconds and delivers them to RabbitMQ asynchronously.
- **Dead Letter Queue (DLQ)**: All workflow queues are configured with `x-dead-letter-exchange: ai.dlx.exchange`. When the Python consumer rejects a message with `nack(requeue=false)`, the message is automatically routed to `ai.dlq.queue` instead of being silently dropped.

---

## 1.5 Outbox Pattern Details

The backend uses the **Transactional Outbox** pattern to guarantee atomicity between database writes and MQ message publishing.

### Flow

1. **Business Transaction**: Within a `@Transactional` method, the backend:
   - Saves business data to PostgreSQL (e.g., `JobMatchResult` with `PROCESSING` status).
   - Serializes the MQ command to JSON and inserts a record into `outbox_message` with `status = PENDING`.

2. **Outbox Relay**: `OutboxRelayScheduler` runs every 2 seconds (`@Scheduled(fixedDelay = 2000)`):
   - Queries all `PENDING` records from `outbox_message`.
   - For each record, calls `rabbitTemplate.convertAndSend(exchange, routingKey, payload)`.
   - On success: updates the record to `status = SENT` and sets `sentAt`.
   - On failure: updates the record to `status = FAILED` and logs the error.

3. **Cleanup**: `OutboxCleanupScheduler` runs daily at 3:00 AM (`@Scheduled(cron = "0 0 3 * * ?")`):
   - Physically deletes records where `status = SENT` and `sentAt < now() - 7 days`.
   - Prevents the `outbox_message` table from growing indefinitely.

### Benefits

- **Atomicity**: If the business transaction rolls back, the Outbox record is also rolled back. No "message sent but DB not committed" inconsistency.
- **Durability**: Even if RabbitMQ is temporarily unavailable, the message remains in the Outbox table and will be retried by the relay scheduler.
- **Observability**: The `outbox_message` table serves as an audit log of all async messages sent to the AI service.

---

## 2. MQ Topology Overview

| Direction | Task Type | Routing Key | Queue | Producer | Consumer |
|-----------|-----------|-------------|-------|----------|----------|
| Request → AI | Job Parse | `ai.req.job.parse` | `ai.queue.job.parse` | Java Backend | Python AI |
| Response ← AI | Job Parse Result | `backend.res.job.parse` | `backend.queue.job.parse` | Python AI | Java Backend |
| Request → AI | Resume Parse | `ai.req.resume.parse` | `ai.queue.resume.parse` | Java Backend | Python AI |
| Response ← AI | Resume Parse Result | `backend.res.resume.parse` | `backend.queue.resume.parse` | Python AI | Java Backend |
| Request → AI | Conversation | `ai.req.conversation` | `ai.queue.conversation` | Java Backend | Python AI |
| Response ← AI | Conversation Reply | `backend.res.conversation` | `backend.queue.conversation` | Python AI | Java Backend |
| Request → AI | Job Rank | `ai.req.job.rank` | `ai.queue.job.rank` | Java Backend | Python AI |
| Response ← AI | Job Rank Result | `backend.res.job.rank` | `backend.queue.job.rank` | Python AI | Java Backend |
| Request → AI | User Feedback | `ai.req.feedback` | `ai.queue.feedback` | Java Backend | Python AI Worker |
| DLX → DLQ | Dead Letter | `dlq.routing.key` | `ai.dlq.queue` | Business queues (auto-forward) | Ops / monitoring |

> **Note on Outbox**: The "Java Backend" producer listed above is technically the `OutboxRelayScheduler`, which reads from the `outbox_message` table and forwards to RabbitMQ. The original business methods (e.g., `JobApplicationService.submitJob`) only write to the Outbox table.

> **Note on DLQ**: The `ai.dlx.exchange` (Dead Letter Exchange) and `ai.dlq.queue` are declared automatically by Spring AMQP at startup. No manual RabbitMQ configuration is required.

---

## 3. Request Commands (Backend → AI)

### 3.1 JobParseCommand — Job Parse Request

**Trigger**: After the user submits a job link, `JobApplicationService.submitJob()` writes the command to the Outbox table.

```json
{
  "jobId": "job-uuid-1234",
  "url": "https://www.linkedin.com/jobs/view/12345",
  "imageCheckEnabled": false,
  "screenshotBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

| Field | Type | Description |
|-------|------|-------------|
| `jobId` | String | Job unique identifier |
| `url` | String | Job detail page URL |
| `imageCheckEnabled` | boolean | Whether visual verification is enabled. The current HTTP submit path sets this to `false` |
| `screenshotBase64` | String | Optional user-uploaded screenshot encoded as Base64 for AI parsing fallback |

---

### 3.2 ResumeParseCommand — Resume Parse Request

**Trigger**: After the user uploads a resume, `ResumeApplicationService.handleUpload()` writes the command to the Outbox table.

```json
{
  "resumeId": "resume-uuid-5678",
  "fileUrl": "https://storage.example.com/resumes/xxx.pdf",
  "format": "application/pdf"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `resumeId` | String | Resume version unique identifier |
| `fileUrl` | String | Temporary file URL generated by the configured storage backend |
| `format` | String | Uploaded file content type, e.g. `application/pdf` or DOCX MIME type |

---

### 3.3 ConversationRequestCommand — Conversation AI Request

**Trigger**: When the user sends a conversation message, `ConversationApplicationService.sendMessage()` writes the command to the Outbox table.

```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "messageHistory": [
    { "role": "USER", "content": "帮我优化一下项目经验部分", "fileUrl": null }
  ],
  "currentMessage": "帮我优化一下项目经验部分",
  "fileUrls": [],
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
  "resumeText": "# Resume Markdown...",
  "primaryJobText": "Software Engineer\nExample Corp\nJob description...",
  "relatedJobTexts": ["Backend Engineer\nExample Corp\nRelated job description..."],
  "init": true,
  "locale": "zh-TW"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `conversationId` | String | Conversation ID |
| `userId` | String | User ID |
| `messageHistory` | List<Map> | Historical message list (role, content, fileUrl) |
| `currentMessage` | String | Latest message sent by the user |
| `fileUrls` | List<String> | File URL list sent to the AI service; current backend context requests send an empty list |
| `resumeVersionId` | String | Associated resume version ID (optional) |
| `resumeText` | String | Resume Markdown/text loaded from the selected resume version or conversation AI working copy |
| `primaryJobText` | String | Current job text loaded from the conversation job ID |
| `relatedJobTexts` | List<String> | Up to five other completed job texts for context |
| `init` | Boolean | Whether this request is the first AI initialization for the conversation |
| `locale` | String | User interface locale, e.g. `en`, `zh-CN`, or `zh-TW` |

---

### 3.4 JobRankCommand — Job Ranking Request

**Trigger**: After the backend vector search recalls visible jobs, `MatchingApplicationService.startMatching()` writes a rank command to the Outbox table.

```json
{
  "matchId": "match-uuid-1234",
  "userId": "user-uuid",
  "resumeVersionId": "resume-uuid",
  "resumeText": "# Resume Markdown...",
  "query": "backend engineer",
  "recalledJobIds": ["job-uuid-1", "job-uuid-2"],
  "jobDetails": {
    "job-uuid-1": {
      "title": "Backend Engineer",
      "company": "Example Corp",
      "description": "Build APIs and distributed services...",
      "semanticMatch": 0.82
    }
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `matchId` | String | Job matching task ID |
| `userId` | String | User ID |
| `resumeVersionId` | String | Resume version ID |
| `resumeText` | String | Parsed resume text or current resume content |
| `query` | String | User query text, may be empty |
| `recalledJobIds` | List<String> | Job IDs returned by backend vector recall after hidden jobs are filtered out |
| `jobDetails` | Map<String, Object> | Details for each recalled job, including title, company, description, and semanticMatch |

---

### 3.5 UserFeedbackCommand — Feedback Label for Incremental Training

**Trigger**: After `JobApplicationService.scoreJob()` completes, the scoring result is serialized and written to the Outbox table with routing key `ai.req.feedback`.

**Note**: This is a **fire-and-forget** message. The AI worker consumes it, buffers labeled feature samples in Redis, and uses them for scheduled LightGBM retraining. No result callback is sent back to the backend.

```json
{
  "matchId": "feedback-uuid-v4",
  "userId": "user-uuid",
  "resumeVersionId": "resume-version-uuid",
  "jobId": "job-uuid",
  "feedbackType": "APPLY",
  "score": 0.85,
  "context": "{\"resume\":{\"skills\":[\"Python\"]},\"job\":{\"title\":\"Software Engineer\"},\"llmOverallScore\":0.82,\"finalScore\":0.85}",
  "timestamp": "2026-05-09T16:00:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `matchId` | String | Unique feedback message ID |
| `userId` | UUID | User who produced the score |
| `resumeVersionId` | String | Resume version unique identifier |
| `jobId` | String | Job unique identifier |
| `feedbackType` | String | Feedback label. Current scoring flow sends `APPLY` for suitable matches and `IGNORE` otherwise |
| `score` | Double | Final fused score (0.0-1.0) |
| `context` | String | JSON string containing resume/job context plus optional `llmOverallScore`, `semanticMatch`, `datasetScore`, and `llmModel` |
| `timestamp` | String | ISO 8601 timestamp |

---

## 4. Result Events (AI → Backend)

### Unified Event Structure: AiResultEvent

All AI callbacks use the following unified structure, distinguished by the `type` field:

```json
{
  "referenceId": "关联实体ID",
  "type": "JOB_PARSE | RESUME_PARSE | CONVERSATION_REPLY | JOB_RANK",
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

### 4.3 Conversation Reply Result (type = CONVERSATION_REPLY)

**Consumer**: `AiResultMessageListener.onConversationReply()` → `ConversationFacade.saveAiReply()`

```json
{
  "referenceId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "CONVERSATION_REPLY",
  "status": "COMPLETED",
  "data": {
    "content": "根据您的简历，我建议从以下几个方面优化工作经验...",
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

| data Sub-field | Type | Description |
|----------------|------|-------------|
| `content` | String | AI reply text content |
| `fileUrl` | String | AI generated file URL (optional) |
| `resumeModification.modified` | Boolean | Whether the AI rewrote or optimized the resume |
| `resumeModification.markdown` | String | Full optimized resume Markdown when `modified=true` |

---

### 4.4 Job Rank Result (type = JOB_RANK)

**Consumer**: `AiResultMessageListener.onJobRankResult()` → `MatchingFacade.saveJobRankResult()`

```json
{
  "referenceId": "match-uuid-1234",
  "type": "JOB_RANK",
  "status": "COMPLETED",
  "data": {
    "rankTimeMs": 125,
    "rankedResults": [
      {
        "jobId": "job-uuid-1",
        "title": "Backend Engineer",
        "company": "Example Corp",
        "matchScore": 0.84,
        "matchFactors": {
          "skillMatch": 0.82,
          "experienceMatch": 0.74,
          "locationMatch": 0.0
        },
        "description": "Build APIs and distributed services...",
        "matchReason": "Your backend and API experience aligns with the role requirements."
      }
    ]
  },
  "errorMessage": null,
  "eventType": null
}
```

| data Sub-field | Type | Description |
|----------------|------|-------------|
| `rankTimeMs` | Integer | AI ranking time in milliseconds |
| `rankedResults` | List<Map> | Ranked job list |
| `rankedResults[].jobId` | String | Job ID |
| `rankedResults[].matchScore` | Float | Final ranking score |
| `rankedResults[].matchFactors` | Object | Skill, experience, and location match breakdown |
| `rankedResults[].matchReason` | String | Optional LLM-generated explanation for top-ranked jobs |

---

## 5. File Upload and Storage

### 5.1 Backend File Upload API

The frontend or AI layer can upload generated file streams to the backend, which then stores them through the configured storage backend:

- **Resume Upload**: `POST /api/v1/resumes` (`multipart/form-data`)
- **Conversation Attachment Upload**: `POST /api/v1/conversations/{conversationId}/files` (`multipart/form-data`)

### 5.2 Storage Path Conventions

| Business | Object Key Prefix Example |
|----------|---------------------------|
| Resume files | `resumes/{uuid}_{filename}` |
| Conversation attachments | `conversations/{conversationId}/{uuid}_{filename}` |

### 5.3 Presigned URL

`FileStorageService.generatePresignedUrl()` generates a temporary access URL for successfully uploaded files (default 7-day validity).

---

## 6. Status Codes and Error Handling

- `COMPLETED`: AI processing succeeded, `data` contains valid results.
- `FAILED`: AI processing failed, `errorMessage` must contain specific error text, `data` may be empty.
- The consumer wraps exceptions in try-catch to prevent MQ messages from retrying infinitely and blocking the queue.
