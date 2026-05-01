<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](conversation.en_US.md) | [简体中文](conversation.zh-Hans-CN.md) | [繁體中文](conversation.zh-Hant-TW.md)

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
| `resumeVersionId` | String (UUID) | 否 | 关联的简历版本 ID |
| `jobId` | String (UUID) | 否 | 关联的职位 ID |

#### 请求示例

```json
{
  "title": "优化工作经验",
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002"
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
| `messages` | Array | 消息列表（新建时为空） |
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
    "messages": [],
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
| `fileUrls` | List<String> | 否 | 关联文件 URL 列表（如简历、附件等） |

#### 请求示例

```json
{
  "content": "帮我优化一下项目经验部分",
  "fileUrls": ["https://minio.example.com/resumes/xxx.pdf"]
}
```

### 响应结构

#### 成功响应 (200)

返回更新后的完整对话信息，包含新增的用户消息。**注意**：AI 回复通过异步 MQ 处理，不会立即出现在响应中，前端需要通过轮询或 WebSocket 获取最新回复。

若创建对话时未指定标题，且这是该对话的**第一条消息**，系统会自动将对话标题设置为消息内容的前 30 个字符。

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

返回文件上传成功后的 MinIO 预签名 URL。

```json
{
  "code": 200,
  "message": "Success",
  "data": "https://minio.example.com/conversations/550e8400-e29b-41d4-a716-446655440003/xxxx_resume.pdf?X-Amz-Algorithm=..."
}
```

---

## 8. 异步消息流说明

### 8.1 对话 AI 请求流

当用户调用【发送消息】接口后，后端会执行以下异步流程：

1. 保存用户消息（`role=USER`）到数据库
2. 若对话标题为默认值且是首条消息，自动生成标题
3. 组装 `ConversationRequestCommand`，包含历史消息、当前消息、fileUrls、resumeVersionId
4. 通过 RabbitMQ 发送到 `ai.req.conversation` 队列
5. Python AI 服务消费该消息，生成回复
6. AI 服务将结果发送到 `backend.res.conversation` 队列
7. `AiResultMessageListener` 监听到 `CONVERSATION_REPLY` 类型事件，保存 AI 回复（`role=ASSISTANT`）到数据库

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
  "fileUrls": ["https://minio.example.com/resumes/xxx.pdf"],
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `conversationId` | String | 对话 ID |
| `userId` | String | 用户 ID |
| `messageHistory` | List<Map> | 历史消息列表（role, content, fileUrl） |
| `currentMessage` | String | 当前用户发送的最新消息 |
| `fileUrls` | List<String> | 用户引用的外部文件 URL 列表 |
| `resumeVersionId` | String | 关联简历版本 ID（可选） |

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
| `errorMessage` | String | 失败原因（`status=FAILED` 时存在） |

---

## 9. 错误码说明

### 通用错误码

| 状态码 | 含义 | 触发场景 |
|--------|------|----------|
| `200` | 成功 | 请求处理成功 |
| `400` | 请求参数错误 | 消息内容为空、UUID 格式错误、分页参数非法 |
| `401` | 未认证 | 缺少 JWT Token 或 Token 已过期 |
| `403` | 权限不足 | 尝试操作不属于自己的对话 |
| `404` | 资源不存在 | 对话 ID 不存在、简历版本 ID 不存在 |
| `409` | 业务冲突 | 向已关闭的对话发送消息 |
| `500` | 服务器内部错误 | 文件上传失败、MQ 发送异常 |

### 业务错误示例

**向已关闭对话发送消息 (409)**：

```json
{
  "code": 409,
  "message": "Cannot add message to a closed conversation",
  "data": null
}
```

**访问不属于自己的对话 (403)**：

```json
{
  "code": 403,
  "message": "Access denied",
  "data": null
}
```

**对话不存在 (404)**：

```json
{
  "code": 404,
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
  "resumeVersionId": String, // 可选，简历版本 ID
  "jobId": String           // 可选，关联职位 ID
}
```

### SendMessageRequest (发送消息请求)

```java
{
  "content": String,         // 必填，消息内容
  "fileUrls": List<String>   // 可选，关联文件 URL 列表
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
  "createdAt": LocalDateTime, // 创建时间
  "updatedAt": LocalDateTime  // 更新时间
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
  "createdAt": LocalDateTime  // 创建时间
}
```


---

## 备注

### 前端流式接口调用

前端 `chatService.ts` 中存在对 `/v1/conversations/{conversationId}/stream` 的调用，但当前后端 `ConversationController` 中**未实现**该端点。如需支持流式 AI 回复，请后续补充对应的 Controller 方法。
