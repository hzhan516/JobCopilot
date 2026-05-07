<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/ai-mq-interfaces.md) | [简体中文](ai-mq-interfaces.md) | [繁體中文](../../../zh-Hant-TW/api/backend/ai-mq-interfaces.md)

# AI 服务交互 API 与 MQ 事件契约汇总

> 本文档集中汇总 Java 后端与 Python AI 服务之间的所有 REST API（如有）和 RabbitMQ 消息队列契约。
> 各模块的详细说明请同时参考 `job.md`、`conversation.md`、`resume.md`。

---

## 1. 交互原则

- **单向异步**：Java 后端通过 MQ 发送任务请求，Python AI 服务处理完成后通过 MQ 返回结果。
- **无直接 HTTP 耦合**：除健康检查外，Java 后端不直接 HTTP 调用 AI 服务；所有耗时操作均走消息队列。
- **统一 Exchange**：所有 MQ 消息共用 `ai.direct.exchange`（DirectExchange）。
- **事务发件箱（Outbox）**：后端**不直接**发送 MQ 消息，而是将消息与业务数据在同一个本地数据库事务中持久化到 `outbox_message` 表。`OutboxRelayScheduler` 每 2 秒轮询 PENDING 记录并异步投递到 RabbitMQ。
- **死信队列（DLQ）**：全部 8 个业务队列均配置了 `x-dead-letter-exchange: ai.dlx.exchange`。当 Python 消费者以 `nack(requeue=false)` 拒绝消息时，消息会自动路由到 `ai.dlq.queue`，避免静默丢失。

---

## 1.5 Outbox 模式详解

后端采用**事务发件箱（Transactional Outbox）**模式，保证数据库写入与 MQ 消息发布的原子性。

### 流程

1. **业务事务**：在 `@Transactional` 方法中，后端：
   - 将业务数据保存到 PostgreSQL（例如状态为 `PROCESSING` 的 `JobMatchResult`）。
   - 将 MQ 命令序列化为 JSON，并以 `status = PENDING` 插入 `outbox_message` 表。

2. **Outbox 转发**：`OutboxRelayScheduler` 每 2 秒执行一次（`@Scheduled(fixedDelay = 2000)`）：
   - 从 `outbox_message` 查询所有 `PENDING` 记录。
   - 对每条记录调用 `rabbitTemplate.convertAndSend(exchange, routingKey, payload)`。
   - 成功：更新记录为 `status = SENT` 并设置 `sentAt`。
   - 失败：更新记录为 `status = FAILED` 并记录错误日志。

3. **清理**：`OutboxCleanupScheduler` 每天凌晨 3:00 执行（`@Scheduled(cron = "0 0 3 * * ?")`）：
   - 物理删除 `status = SENT` 且 `sentAt < 当前时间 - 7 天` 的记录。
   - 防止 `outbox_message` 表无限膨胀。

### 收益

- **原子性**：如果业务事务回滚，Outbox 记录也会回滚。不会出现"消息已发出但数据库未提交"的不一致。
- **持久性**：即使 RabbitMQ 临时不可用，消息仍保留在 Outbox 表中，由转发调度器重试。
- **可观测性**：`outbox_message` 表作为所有异步发送到 AI 服务的消息的审计日志。

---

## 2. MQ 拓扑总览

| 方向 | 任务类型 | Routing Key | Queue | 生产者 | 消费者 |
|------|----------|-------------|-------|--------|--------|
| Request → AI | 职位解析 | `ai.req.job.parse` | `ai.queue.job.parse` | Java Backend | Python AI |
| Response ← AI | 职位解析结果 | `backend.res.job.parse` | `backend.queue.job.parse` | Python AI | Java Backend |
| Request → AI | 简历解析 | `ai.req.resume.parse` | `ai.queue.resume.parse` | Java Backend | Python AI |
| Response ← AI | 简历解析结果 | `backend.res.resume.parse` | `backend.queue.resume.parse` | Python AI | Java Backend |
| Request → AI | 对话请求 | `ai.req.conversation` | `ai.queue.conversation` | Java Backend | Python AI |
| Response ← AI | 对话回复结果 | `backend.res.conversation` | `backend.queue.conversation` | Python AI | Java Backend |
| Request → AI | 职位精排 | `ai.req.job.rank` | `ai.queue.job.rank` | Java Backend | Python AI |
| Response ← AI | 职位精排结果 | `backend.res.job.rank` | `backend.queue.job.rank` | Python AI | Java Backend |
| DLX → DLQ | 死信 | `dlq.routing.key` | `ai.dlq.queue` | 业务队列（自动转发） | 运维/监控（可手动消费） |

> **Outbox 说明**：上表中的 "Java Backend" 生产者实际上是 `OutboxRelayScheduler`，它从 `outbox_message` 表读取记录后转发到 RabbitMQ。原始业务方法（如 `JobApplicationService.submitJob`）仅写入 Outbox 表。

> **DLQ 说明**：`ai.dlx.exchange`（死信交换机）和 `ai.dlq.queue` 由 Spring AMQP 在启动时自动声明，无需手动配置 RabbitMQ。

---

## 3. 请求命令（Backend → AI）

### 3.1 JobParseCommand — 职位解析请求

**发送时机**：用户提交职位链接后，`JobApplicationService.submitJob()` 将命令写入 Outbox 表。

```json
{
  "jobId": "job-uuid-1234",
  "url": "https://www.linkedin.com/jobs/view/12345",
  "imageCheckEnabled": true
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `jobId` | String | 职位唯一标识 |
| `url` | String | 职位详情页 URL |
| `imageCheckEnabled` | boolean | 是否启用视觉验证 |

---

### 3.2 ResumeParseCommand — 简历解析请求

**发送时机**：用户上传简历后，`ResumeApplicationService.handleUpload()` 将命令写入 Outbox 表。

```json
{
  "resumeId": "resume-uuid-5678",
  "fileUrl": "https://minio.example.com/resumes/xxx.pdf",
  "fileType": "PDF"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `resumeId` | String | 简历版本唯一标识 |
| `fileUrl` | String | 简历文件在 MinIO 上的 URL |
| `fileType` | String | 文件类型，如 PDF、DOCX |

---

### 3.3 ConversationRequestCommand — 对话 AI 请求

**发送时机**：用户发送对话消息后，`ConversationApplicationService.sendMessage()` 将命令写入 Outbox 表。

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

---

## 4. 结果事件（AI → Backend）

### 统一事件结构：AiResultEvent

所有 AI 回调均使用以下统一结构，通过 `type` 字段区分业务类型：

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

| 字段 | 类型 | 说明 |
|------|------|------|
| `referenceId` | String | 关联实体 ID |
| `type` | String | 事件类型 |
| `status` | String | `COMPLETED` 或 `FAILED` |
| `data` | Map<String, Object> | 业务数据载荷 |
| `errorMessage` | String | 失败原因（`status=FAILED` 时必填） |
| `eventType` | String | 内部子路由标记（如 VECTOR_GEN 区分 JOB/RESUME） |

---

### 4.1 职位解析结果（type = JOB_PARSE）

**消费端**：`AiResultMessageListener.onJobParseResult()` → `JobFacade.handleJobProcessResult()`

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

### 4.2 简历解析结果（type = RESUME_PARSE）

**消费端**：`AiResultMessageListener.onResumeParseResult()` → `ResumeFacade.handleParseResult()`

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

### 4.3 对话回复结果（type = CONVERSATION_REPLY）

**消费端**：`AiResultMessageListener.onConversationReply()` → `ConversationFacade.saveAiReply()`

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

| data 子字段 | 类型 | 说明 |
|-------------|------|------|
| `content` | String | AI 回复的文本内容 |
| `fileUrl` | String | AI 生成文件的 URL（可选） |

---

## 5. 文件上传与 MinIO

### 5.1 后端文件上传 API

前端或 AI 层可将生成的文件流上传至后端，由后端转存到 MinIO：

- **简历上传**：`POST /api/v1/resumes`（`multipart/form-data`）
- **对话附件上传**：`POST /api/v1/conversations/{conversationId}/files`（`multipart/form-data`）

### 5.2 MinIO 存储路径约定

| 业务 | 对象键前缀示例 |
|------|----------------|
| 简历文件 | `resumes/{uuid}_{filename}` |
| 对话附件 | `conversations/{conversationId}/{uuid}_{filename}` |

### 5.3 预签名 URL

`MinioFileStorageService.generatePresignedUrl()` 为上传成功的文件生成临时访问 URL（默认 7 天有效期）。

---

## 6. 状态码与错误处理

- `COMPLETED`：AI 处理成功，`data` 中包含有效结果。
- `FAILED`：AI 处理失败，`errorMessage` 必须包含具体错误文本，`data` 可为空。
- 消费端对异常进行 try-catch 包裹，避免 MQ 消息无限重试导致队列阻塞。
