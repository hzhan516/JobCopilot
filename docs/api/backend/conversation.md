<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](conversation.md) | [简体中文](../../i18n/zh-Hans-CN/api/backend/conversation.md) | [繁體中文](../../i18n/zh-Hant-TW/api/backend/conversation.md)

# Conversation Management API

> Multi-turn AI conversation interfaces based on resume context, supporting asynchronous MQ interaction, attachment upload, and message pagination

---

## Table of Contents

1. [Create Conversation](#1-create-conversation)
2. [Send Message](#2-send-message)
3. [Get Conversation Details](#3-get-conversation-details)
4. [Get Conversation List](#4-get-conversation-list)
5. [Close Conversation](#5-close-conversation)
6. [Delete Conversation](#6-delete-conversation)
7. [Upload Attachment](#7-upload-attachment)
8. [Stream AI Reply](#8-stream-ai-reply)
9. [Async Message Flow Description](#9-async-message-flow-description)
10. [Error Code Description](#10-error-code-description)

---

## 1. Create Conversation

### Basic Information

| Item | Value |
|------|-------|
| **Interface Name** | Create Conversation |
| **Interface Path** | `POST /api/v1/conversations` |
| **Authentication Required** | Yes |
| **Content-Type** | `application/json` |

### Request Structure

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | String | No | Conversation title, defaults to "New Conversation" if not provided |
| `resumeVersionId` | String (UUID) | No | Associated resume version ID. If provided, `jobId` must also be provided |
| `jobId` | String (UUID) | No | Associated job ID. If provided, `resumeVersionId` must also be provided |

#### Request Example

```json
{
  "title": "Optimize Work Experience",
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
  "jobId": "550e8400-e29b-41d4-a716-446655440010"
}
```

### Response Structure

#### Success Response (200)

| Field | Type | Description |
|-------|------|-------------|
| `conversationId` | String (UUID) | Conversation unique identifier |
| `userId` | String (UUID) | User ID |
| `title` | String | Conversation title |
| `status` | String | Status: ACTIVE, CLOSED |
| `resumeVersionId` | String (UUID) | Associated resume version ID |
| `jobId` | String (UUID) | Associated job ID |
| `messages` | Array | Message list. Creating a conversation immediately adds a preset `USER` message and starts the initial AI request |
| `createdAt` | String (ISO 8601) | Creation time |
| `updatedAt` | String (ISO 8601) | Update time |
| `aiReply` | Object | Durable request state: `requestId`, `status`, `errorCode`, timestamps, and correlated message sequences |

#### Response Example

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "conversationId": "550e8400-e29b-41d4-a716-446655440003",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Optimize Work Experience",
    "status": "ACTIVE",
    "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
    "jobId": "550e8400-e29b-41d4-a716-446655440010",
    "messages": [
      {
        "messageId": "550e8400-e29b-41d4-a716-446655440004",
        "role": "USER",
        "content": "Compare the current job posting with my resume and tell me the match score.",
        "sequence": 1,
        "fileUrl": null,
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

---

## 2. Send Message

### Basic Information

| Item | Value |
|------|-------|
| **Interface Name** | Send Message |
| **Interface Path** | `POST /api/v1/conversations/{conversationId}/messages` |
| **Authentication Required** | Yes |
| **Content-Type** | `application/json` |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `conversationId` | String (UUID) | Yes | Conversation unique identifier |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `content` | String | Yes | Message content |
| `fileUrls` | List<String> | No | Up to three managed attachment URLs returned by this conversation's upload endpoint. Ownership and storage location are validated before they are forwarded to AI |

#### Request Example

```json
{
  "content": "帮我优化一下项目经验部分",
  "fileUrls": []
}
```

### Response Structure

#### Success Response (200)

Returns the updated conversation including the new user message and `aiReply.status=PENDING` with a unique `aiReply.requestId`. The AI reply is asynchronous. Clients must poll the conversation resource and finish waiting only when that same request ID reaches `COMPLETED`, `FAILED`, or `TIMED_OUT`.

While a reply is `PENDING`, a second send returns HTTP `409`. This prevents two user turns from racing against the same aggregate state.

If the conversation title is still the default value when a message is added, the system automatically sets the title to the first 30 characters of that message content. In the current create flow, the preset initial message can set this title before the user's next message.

#### Response Example

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "conversationId": "550e8400-e29b-41d4-a716-446655440003",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "帮我优化一下项目经验部分",
    "status": "ACTIVE",
    "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
    "messages": [
      {
        "messageId": "550e8400-e29b-41d4-a716-446655440004",
        "role": "USER",
        "content": "帮我优化一下项目经验部分",
        "sequence": 1,
        "fileUrl": null,
        "createdAt": "2024-01-15T10:31:00"
      }
    ],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:31:00"
  }
}
```

---

## 3. Get Conversation Details

### Basic Information

| Item | Value |
|------|-------|
| **Interface Name** | Get Conversation Details |
| **Interface Path** | `GET /api/v1/conversations/{conversationId}` |
| **Authentication Required** | Yes |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `conversationId` | String (UUID) | Yes | Conversation unique identifier |

#### Query Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `page` | Integer | No | Message page number, starting from 0 |
| `size` | Integer | No | Messages per page; defaults to 50 and is capped at 100 |

### Response Structure

#### Success Response (200)

Returns conversation information with one bounded message page (sorted by `sequence` ascending), durable `aiReply` state, context usage, and the latest `compactionRequestId`. A client must correlate completion with `aiReply.requestId`; the presence of an older assistant message is not evidence that the newest request completed.

---

## 4. Get Conversation List

### Basic Information

| Item | Value |
|------|-------|
| **Interface Name** | Get Conversation List |
| **Interface Path** | `GET /api/v1/conversations` |
| **Authentication Required** | Yes |

### Response Structure

#### Success Response (200)

Returns lightweight summaries for the current user, ordered by `updatedAt` descending. Each item excludes message details and includes `lastMessagePreview` plus the durable `aiReply` state.

#### Response Example

```json
{
  "code": 200,
  "message": "Success",
  "data": [
    {
      "conversationId": "550e8400-e29b-41d4-a716-446655440003",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "帮我优化一下项目经验部分",
      "status": "ACTIVE",
      "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:31:00"
    }
  ]
}
```

---

## 5. Close Conversation

### Basic Information

| Item | Value |
|------|-------|
| **Interface Name** | Close Conversation |
| **Interface Path** | `PUT /api/v1/conversations/{conversationId}/close` |
| **Authentication Required** | Yes |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `conversationId` | String (UUID) | Yes | Conversation unique identifier |

### Response Structure

#### Success Response (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

---

## 6. Delete Conversation

### Basic Information

| Item | Value |
|------|-------|
| **Interface Name** | Delete Conversation |
| **Interface Path** | `DELETE /api/v1/conversations/{conversationId}` |
| **Authentication Required** | Yes |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `conversationId` | String (UUID) | Yes | Conversation unique identifier |

### Response Structure

#### Success Response (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

---

## 7. Upload Attachment

### Basic Information

| Item | Value |
|------|-------|
| **Interface Name** | Upload Conversation Attachment |
| **Interface Path** | `POST /api/v1/conversations/{conversationId}/files` |
| **Authentication Required** | Yes |
| **Content-Type** | `multipart/form-data` |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `conversationId` | String (UUID) | Yes | Conversation unique identifier |

#### Request Body (form-data)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | Attachment file (e.g. AI-generated optimized resume) |

### Response Structure

#### Success Response (200)

Returns a temporary URL from the configured storage backend after successful file upload.

```json
{
  "code": 200,
  "message": "Success",
  "data": "https://storage.example.com/conversations/550e8400-e29b-41d4-a716-446655440003/xxxx_resume.pdf?X-Amz-Algorithm=..."
}
```

---

## 8. Stream AI Reply

### Basic Information

| Item | Value |
|------|-------|
| **Interface Name** | Stream AI Reply |
| **Interface Path** | `GET /api/v1/conversations/{conversationId}/stream` |
| **Authentication Required** | Yes |
| **Content-Type** | `text/plain` (streaming response) |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `conversationId` | String (UUID) | Yes | Conversation unique identifier |

#### Request Headers

| Field | Required | Description |
|-------|----------|-------------|
| `Authorization` | Yes | `Bearer {JWT Token}` |

### Response Structure

#### Success Response (200)

Returns a `text/plain` streaming response. The HTTP connection remains open until the AI reply is generated.

This endpoint remains available for backward compatibility. The current frontend uses the durable `aiReply` state returned by `GET /api/v1/conversations/{conversationId}` because it survives refresh, reconnect, and backend restart.

> **Note: Current implementation is pseudo-streaming.**
> The AI Service generates the complete reply synchronously and sends it back via MQ as a single event.
> The backend holds the HTTP connection open, waits for the MQ reply, and then writes the complete content
> into the response stream in one go. The frontend experiences a loading state followed by the full reply
> arriving at once.
>
> To upgrade to true token-by-token streaming in the future:
> 1. Update the AI Service layer to emit token chunks through the configured LiteLLM-compatible provider.
> 2. Expose a streaming endpoint or streaming MQ event path from the AI Service.
> 3. Have the backend proxy each chunk to the frontend through the existing `StreamingResponseBody` infrastructure.
> 4. The current MQ full-reply path can be kept for non-streaming scenarios.

#### Calling Sequence

1. Call `POST /api/v1/conversations/{conversationId}/messages` to send the user message.
2. Immediately call `GET /api/v1/conversations/{conversationId}/stream` to wait for the AI reply.
3. The connection stays open (default timeout: 60 seconds).
4. When the AI reply is ready, the complete text is written to the stream and the connection closes.

#### Response Example (Streaming)

```text
Based on your resume, I suggest optimizing your work experience section from the following aspects...
```

#### Timeout Behavior

If the AI reply is not generated within 60 seconds, the stream will close and return a timeout message:

```text
AI reply timed out. Please try again later.
AI 回复超时，请稍后重试。
```

### Error Codes

| Status Code | Meaning | Trigger Scenario |
|-------------|---------|------------------|
| `401` | Not authenticated | Missing or expired JWT Token |
| `400` | Conversation business error | Conversation does not exist, access denied, or other localized conversation-domain error |

### Retry a Failed or Timed-out Reply

`POST /api/v1/conversations/{conversationId}/ai-replies/retry` retries the same user turn with a new request ID. Retry is accepted only when the current state is `FAILED` or `TIMED_OUT`; it does not append another user message.

---

## 9. Async Message Flow Description

### 8.1 Conversation AI Request Flow

After the user calls the [Send Message] interface, the backend executes the following asynchronous flow:

1. Save the user message and a request-scoped `PENDING` reply state in the same database transaction
2. If the conversation title is still the default value, automatically generate the title from the message content
3. Assemble `ConversationRequestCommand`, including history messages, current message, resumeVersionId, resumeText, primaryJobText, relatedJobTexts, init flag, and locale
4. Save the versioned request envelope to the transactional outbox, including `requestId`, `eventId`, `schemaVersion`, and `occurredAt`
5. The outbox relay claims due rows, publishes with RabbitMQ confirms, and applies bounded retry or `DEAD` state
6. Python AI service consumes the message and generates a reply once per request ID
7. AI service publishes the correlated result with publisher confirms and acknowledges the request only after result publication succeeds
8. `AiResultMessageListener` atomically completes only the matching pending request and appends one `ASSISTANT` message; duplicate and stale results are ignored
9. If the AI result includes `resumeModification.modified=true`, the backend creates or updates an `AI_OPTIMIZED` resume version and appends the optimized Markdown to the assistant message content

### 8.2 Data Format Sent to AI Service

**Request Message (`ConversationRequestCommand`)**:

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
  "locale": "zh-TW",
  "requestId": "7f785b8f-137f-4588-bd8d-f72a3f39cf62",
  "schemaVersion": 1,
  "eventId": "87fa5f67-c64b-45cf-b00b-2a63f59cf481",
  "occurredAt": "2026-07-18T12:00:00Z"
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
| `requestId` | String (UUID) | End-to-end idempotency and correlation ID for this user turn |
| `schemaVersion` | Integer | Event contract version |
| `eventId` | String (UUID) | Unique envelope event ID |
| `occurredAt` | String (ISO 8601) | Event creation time |

### 8.3 Data Format for Receiving AI Replies

**Response Message (`AiResultEvent`, type=`CONVERSATION_REPLY`)**:

```json
{
  "referenceId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "CONVERSATION_REPLY",
  "status": "COMPLETED",
  "schemaVersion": 1,
  "eventId": "4c0cb55a-29a8-4235-9c9a-97758edc0993",
  "requestId": "7f785b8f-137f-4588-bd8d-f72a3f39cf62",
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

| Field | Type | Description |
|-------|------|-------------|
| `referenceId` | String | Conversation ID |
| `type` | String | Fixed as `CONVERSATION_REPLY` |
| `status` | String | `COMPLETED` or `FAILED` |
| `requestId` | String (UUID) | Must match the active pending request before state can change |
| `data.content` | String | AI reply text |
| `data.fileUrl` | String | AI generated file URL (optional) |
| `data.resumeModification.modified` | Boolean | Whether the AI rewrote or optimized the resume |
| `data.resumeModification.markdown` | String | Full optimized resume Markdown when `modified=true` |
| `errorMessage` | String | Failure reason (exists when `status=FAILED`) |

---

## 10. Error Code Description

### Common Error Codes

| Status Code | Meaning | Trigger Scenario |
|-------------|---------|------------------|
| `200` | Success | Request processed successfully |
| `400` | Request or business error | Message content empty, UUID format error, invalid pagination parameters, conversation not found, access denied, closed conversation |
| `401` | Not authenticated | Missing JWT Token or Token expired |
| `500` | Internal server error | File upload failure, MQ send exception |

### Business Error Examples

**Send message to closed conversation (400)**:

```json
{
  "code": 400,
  "message": "Cannot add message to a closed conversation",
  "data": null
}
```

**Access conversation not belonging to you (400)**:

```json
{
  "code": 400,
  "message": "Access denied",
  "data": null
}
```

**Conversation does not exist (400)**:

```json
{
  "code": 400,
  "message": "Conversation not found",
  "data": null
}
```

**Invalid resume version (400)**:

```json
{
  "code": 400,
  "message": "Invalid resume version or access denied",
  "data": null
}
```

---

## DTO Definitions

### CreateConversationRequest (Create Conversation Request)

```java
{
  "title": String,          // Optional, conversation title
  "resumeVersionId": String, // Optional, but must be provided with jobId
  "jobId": String           // Optional, but must be provided with resumeVersionId
}
```

### SendMessageRequest (Send Message Request)

```java
{
  "content": String,         // Required, message content
  "fileUrls": List<String>   // Optional, reserved attachment URL list; current backend sends an empty list to AI
}
```

### ConversationResponse (Conversation Response)

```java
{
  "conversationId": String,   // Conversation ID
  "userId": String,           // User ID
  "title": String,            // Title
  "status": String,           // ACTIVE / CLOSED
  "resumeVersionId": String,  // Associated resume version ID
  "jobId": String,            // Associated job ID
  "messages": MessageResponse[], // Message list (may be paginated)
  "createdAt": OffsetDateTime, // Creation time
  "updatedAt": OffsetDateTime, // Update time
  "aiReply": {
    "requestId": String,
    "status": String,           // IDLE / PENDING / COMPLETED / FAILED / TIMED_OUT
    "errorCode": String,
    "startedAt": OffsetDateTime,
    "completedAt": OffsetDateTime,
    "userMessageSequence": Integer,
    "assistantMessageSequence": Integer
  }
}
```

### MessageResponse (Message Response)

```java
{
  "messageId": String,        // Message ID
  "role": String,             // USER / ASSISTANT / SYSTEM
  "content": String,          // Content
  "sequence": int,            // Sequence number
  "fileUrl": String,          // Associated file URL (AI generated files, etc.)
  "createdAt": OffsetDateTime // Creation time
}
```


---

## Notes

### Frontend Completion Tracking

The frontend polls `GET /v1/conversations/{conversationId}` with adaptive intervals and an abortable request. Polling follows the exact `aiReply.requestId`, resumes after refresh, and presents retry controls for `FAILED` and `TIMED_OUT` states.
