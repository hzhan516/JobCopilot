<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/conversation.md) | [简体中文](conversation.md) | [繁體中文](../../../zh-Hant-TW/api/backend/conversation.md)

# 对话管理 API

> 基于简历上下文的多轮 AI 对话接口，支持异步 MQ 交互、附件上传与消息分页

---

## 目录

1. [创建对话](#1-创建对话)
2. [发送消息](#2-发送消息)
3. [获取对话详情](#3-获取对话详情)
4. [获取对话列表](#4-获取对话列表)
5. [关闭对话](#5-关闭对话)
6. [删除对话](#6-删除对话)
7. [上传附件](#7-上传附件)
8. [异步消息流说明](#8-异步消息流说明)
9. [错误码说明](#9-错误码说明)

---

## 1. 创建对话

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 创建对话 |
| **接口路径** | `POST /api/v1/conversations` |
| **是否需要认证** | 是 |
| **Content-Type** | `application/json` |

### 请求结构

#### Request Body

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `title` | String | 否 | 对话标题，不传则默认为 "New Conversation" |
| `resumeVersionId` | String (UUID) | 否 | 关联的简历版本 ID；若提供，必须同时提供 `jobId` |
| `jobId` | String (UUID) | 否 | 关联的职位 ID；若提供，必须同时提供 `resumeVersionId` |

#### 请求示例

```json
{
  "title": "优化工作经验",
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
  "jobId": "550e8400-e29b-41d4-a716-446655440010"
}
```

### 响应结构

#### 成功响应 (200)

| 字段 | 类型 | 说明 |
|------|------|------|
| `conversationId` | String (UUID) | 对话唯一标识 |
| `userId` | String (UUID) | 用户 ID |
| `title` | String | 对话标题 |
| `status` | String | 状态：ACTIVE, CLOSED |
| `resumeVersionId` | String (UUID) | 关联简历版本 ID |
| `jobId` | String (UUID) | 关联职位 ID |
| `messages` | Array | 消息列表。创建对话会立即加入一条预设 `USER` 消息并启动首次 AI 请求 |
| `createdAt` | String (ISO 8601) | 创建时间 |
| `updatedAt` | String (ISO 8601) | 更新时间 |

#### 响应示例

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "conversationId": "550e8400-e29b-41d4-a716-446655440003",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "优化工作经验",
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

## 2. 发送消息

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 发送消息 |
| **接口路径** | `POST /api/v1/conversations/{conversationId}/messages` |
| **是否需要认证** | 是 |
| **Content-Type** | `application/json` |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `conversationId` | String (UUID) | 是 | 对话唯一标识 |

#### Request Body

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content` | String | 是 | 消息内容 |
| `fileUrls` | List<String> | 否 | 保留的附件 URL 列表。目前后端接受此字段，但不会持久化或转发用户提供的值到 AI 请求 |

#### 请求示例

```json
{
  "content": "帮我优化一下项目经验部分",
  "fileUrls": []
}
```

### 响应结构

#### 成功响应 (200)

返回更新后的完整对话信息，包含新增的用户消息。**注意**：AI 回复通过异步 MQ 处理，不会立即出现在响应中，前端会调用流式端点等待最新回复。

若新增消息时对话标题仍为默认值，系统会自动将标题设置为该消息内容的前 30 个字符。当前创建对话流程中的预设初始消息可能会先设置这个标题。

#### 响应示例

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

## 3. 获取对话详情

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 获取对话详情 |
| **接口路径** | `GET /api/v1/conversations/{conversationId}` |
| **是否需要认证** | 是 |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `conversationId` | String (UUID) | 是 | 对话唯一标识 |

#### Query Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | Integer | 否 | 消息页码，从 0 开始 |
| `size` | Integer | 否 | 每页消息数量，默认返回全部 |

### 响应结构

#### 成功响应 (200)

返回完整的对话信息，包括所有消息列表（按 `sequence` 升序排列）。若传入了 `page` 和 `size`，仅返回指定分页范围的消息。若 AI 已回复，消息列表中会包含 `role=ASSISTANT` 的消息，且可能带有 `fileUrl`。

---

## 4. 获取对话列表

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 获取对话列表 |
| **接口路径** | `GET /api/v1/conversations` |
| **是否需要认证** | 是 |

### 响应结构

#### 成功响应 (200)

返回当前用户的所有对话列表（不包含消息详情）。

#### 响应示例

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

## 5. 关闭对话

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 关闭对话 |
| **接口路径** | `PUT /api/v1/conversations/{conversationId}/close` |
| **是否需要认证** | 是 |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `conversationId` | String (UUID) | 是 | 对话唯一标识 |

### 响应结构

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

---

## 6. 删除对话

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 删除对话 |
| **接口路径** | `DELETE /api/v1/conversations/{conversationId}` |
| **是否需要认证** | 是 |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `conversationId` | String (UUID) | 是 | 对话唯一标识 |

### 响应结构

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

---

## 7. 上传附件

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 上传对话附件 |
| **接口路径** | `POST /api/v1/conversations/{conversationId}/files` |
| **是否需要认证** | 是 |
| **Content-Type** | `multipart/form-data` |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `conversationId` | String (UUID) | 是 | 对话唯一标识 |

#### Request Body (form-data)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | File | 是 | 附件文件（如 AI 生成的优化简历） |

### 响应结构

#### 成功响应 (200)

返回文件上传成功后，由当前配置的存储后端生成的临时 URL。

```json
{
  "code": 200,
  "message": "Success",
  "data": "https://storage.example.com/conversations/550e8400-e29b-41d4-a716-446655440003/xxxx_resume.pdf?X-Amz-Algorithm=..."
}
```

---

## 8. 异步消息流说明

### 8.1 对话 AI 请求流

当用户调用【发送消息】接口后，后端会执行以下异步流程：

1. 保存用户消息（`role=USER`）到数据库
2. 若对话标题仍为默认值，根据消息内容自动生成标题
3. 组装 `ConversationRequestCommand`，包含历史消息、当前消息、resumeVersionId、resumeText、primaryJobText、relatedJobTexts、init flag 与 locale
4. 通过 RabbitMQ 使用 routing key `ai.req.conversation` 发送到 queue `ai.queue.conversation`
5. Python AI 服务消费该消息，生成回复
6. AI 服务使用 routing key `backend.res.conversation` 将结果发送到 queue `backend.queue.conversation`
7. `AiResultMessageListener` 监听到 `CONVERSATION_REPLY` 类型事件，保存 AI 回复（`role=ASSISTANT`）到数据库
8. 若 AI 结果包含 `resumeModification.modified=true`，后端会创建或更新 `AI_OPTIMIZED` 简历版本，并将优化后的 Markdown 追加到助理回复内容

### 8.2 发送给 AI 服务的数据格式

**请求消息 (`ConversationRequestCommand`)**：

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
  "locale": "zh-CN"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `conversationId` | String | 对话 ID |
| `userId` | String | 用户 ID |
| `messageHistory` | List<Map> | 历史消息列表（role, content, fileUrl） |
| `currentMessage` | String | 当前用户发送的最新消息 |
| `fileUrls` | List<String> | 发送给 AI 服务的文件 URL 列表；当前后端上下文请求会发送空列表 |
| `resumeVersionId` | String | 关联简历版本 ID（可选） |
| `resumeText` | String | 从所选简历版本或对话 AI 工作副本加载的简历 Markdown/text |
| `primaryJobText` | String | 从对话关联职位加载的当前职位文本 |
| `relatedJobTexts` | List<String> | 同一用户其他职位文本，最多 5 条 |
| `init` | Boolean | 是否为对话初始化请求 |
| `locale` | String | 后端根据当前请求解析出的语言环境 |

### 8.3 接收 AI 回复的数据格式

**响应消息 (`AiResultEvent`，type=`CONVERSATION_REPLY`)**：

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

| 字段 | 类型 | 说明 |
|------|------|------|
| `referenceId` | String | 对话 ID |
| `type` | String | 固定为 `CONVERSATION_REPLY` |
| `status` | String | `COMPLETED` 或 `FAILED` |
| `data.content` | String | AI 回复文本 |
| `data.fileUrl` | String | AI 生成文件的 URL（可选） |
| `data.resumeModification.modified` | Boolean | AI 是否产生了简历修改 |
| `data.resumeModification.markdown` | String | `modified=true` 时的完整优化简历 Markdown |
| `errorMessage` | String | 失败原因（`status=FAILED` 时存在） |

---

## 9. 错误码说明

### 通用错误码

| 状态码 | 含义 | 触发场景 |
|--------|------|----------|
| `200` | 成功 | 请求处理成功 |
| `400` | 请求或业务错误 | 消息内容为空、UUID 格式错误、分页参数非法、对话不存在、权限不足、对话已关闭 |
| `401` | 未认证 | 缺少 JWT Token 或 Token 已过期 |
| `500` | 服务器内部错误 | 文件上传失败、MQ 发送异常 |

### 业务错误示例

**向已关闭对话发送消息 (400)**：

```json
{
  "code": 400,
  "message": "Cannot add message to a closed conversation",
  "data": null
}
```

**访问不属于自己的对话 (400)**：

```json
{
  "code": 400,
  "message": "Access denied",
  "data": null
}
```

**对话不存在 (400)**：

```json
{
  "code": 400,
  "message": "Conversation not found",
  "data": null
}
```

**无效的简历版本 (400)**：

```json
{
  "code": 400,
  "message": "Invalid resume version or access denied",
  "data": null
}
```

---

## DTO 定义

### CreateConversationRequest (创建对话请求)

```java
{
  "title": String,          // 可选，对话标题
  "resumeVersionId": String, // 可选，但必须与 jobId 同时提供
  "jobId": String           // 可选，但必须与 resumeVersionId 同时提供
}
```

### SendMessageRequest (发送消息请求)

```java
{
  "content": String,         // 必填，消息内容
  "fileUrls": List<String>   // 可选，保留的附件 URL 列表；当前后端发送给 AI 时使用空列表
}
```

### ConversationResponse (对话响应)

```java
{
  "conversationId": String,   // 对话 ID
  "userId": String,           // 用户 ID
  "title": String,            // 标题
  "status": String,           // ACTIVE / CLOSED
  "resumeVersionId": String,  // 关联简历版本 ID
  "jobId": String,            // 关联职位 ID
  "messages": MessageResponse[], // 消息列表（可能已分页）
  "createdAt": OffsetDateTime, // 创建时间
  "updatedAt": OffsetDateTime  // 更新时间
}
```

### MessageResponse (消息响应)

```java
{
  "messageId": String,        // 消息 ID
  "role": String,             // USER / ASSISTANT / SYSTEM
  "content": String,          // 内容
  "sequence": int,            // 序号
  "fileUrl": String,          // 关联文件 URL（AI 生成文件等）
  "createdAt": OffsetDateTime // 创建时间
}
```


---

## 8. 流式获取 AI 回复

### 基本信息

| 项目 | 值 |
|------|-------|
| **接口名称** | 流式获取 AI 回复 |
| **接口路径** | `GET /api/v1/conversations/{conversationId}/stream` |
| **需要认证** | 是 |
| **Content-Type** | `text/plain`（流式响应） |

### 请求结构

#### 路径参数

| 字段 | 类型 | 必填 | 说明 |
|-------|------|----------|-------------|
| `conversationId` | String (UUID) | 是 | 对话唯一标识 |

#### 请求头

| 字段 | 必填 | 说明 |
|-------|----------|-------------|
| `Authorization` | 是 | `Bearer {JWT Token}` |

### 响应结构

#### 成功响应 (200)

返回 `text/plain` 流式响应。HTTP 连接保持打开状态，直到 AI 回复生成完毕。

> **注意：当前实现为伪流式传输。**
> AI Service 同步生成完整回复后，通过 MQ 一次性将结果发回后端。
> 后端保持 HTTP 连接等待 MQ 回复，然后将完整内容一次性写入响应流。
> 前端会体验到 loading 状态，随后完整回复一次性到达。
>
> **后续升级为真正流式的路径：**
> 1. 更新 AI Service 层，通过当前配置的 LiteLLM-compatible provider 输出 token chunks。
> 2. 在 AI Service 中新增流式端点，或新增流式 MQ event path。
> 3. 后端将每个 chunk 通过现有 `StreamingResponseBody` 架构实时透传给前端。
> 4. 当前的 MQ 完整回复路径可保留用于非流式场景。

#### 调用时序

1. 调用 `POST /api/v1/conversations/{conversationId}/messages` 发送用户消息。
2. 立即调用 `GET /api/v1/conversations/{conversationId}/stream` 等待 AI 回复。
3. 连接保持打开（默认超时 60 秒）。
4. AI 回复就绪后，完整文本写入流并关闭连接。

#### 响应示例（流式）

```text
根据您的简历，我建议从以下几个方面优化工作经验...
```

#### 超时行为

如果 AI 回复在 60 秒内未生成，流将关闭并返回超时提示：

```text
AI reply timed out. Please try again later.
AI 回复超时，请稍后重试。
```

### 错误码

| 状态码 | 含义 | 触发场景 |
|-------------|---------|------------------|
| `401` | 未认证 | 缺少或已过期 JWT Token |
| `400` | 对话业务错误 | 对话不存在、权限不足，或其他本地化对话领域错误 |

---

## 备注

### 前端流式接口调用

前端 `chatService.ts` 通过 `fetch` + `ReadableStream` 调用 `GET /v1/conversations/{conversationId}/stream`。
详见 [8. 流式获取 AI 回复](#8-流式获取-ai-回复) 的后端接口文档。
