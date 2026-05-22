<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/ai-mq-interfaces.md) | [简体中文](ai-mq-interfaces.md) | [繁體中文](../../../zh-Hant-TW/api/backend/ai-mq-interfaces.md)

# AI 服务交互 API 与 MQ 事件契约汇总

> 本文档集中汇总 Java 后端与 Python AI 服务之间的所有 REST API（如有）和 RabbitMQ 消息队列契约。
> 各模块的详细说明请同时参考 `job.md`、`conversation.md`、`resume.md`。

---

## 1. 交互原则

- **单向异步**：Java 后端通过 MQ 发送任务请求，Python AI 服务处理完成后通过 MQ 返回结果。
- **AI 处理优先走异步流程**：解析、对话、排序等耗时任务通过 MQ 处理。后端也会通过同步 REST 调用 AI 服务的轻量 embedding、适配度评分和模型重算端点。
- **统一 Exchange**：所有 MQ 消息共用 `ai.direct.exchange`（DirectExchange）。
- **事务发件箱（Outbox）**：后端**不直接**发送 MQ 消息，而是将消息与业务数据在同一个本地数据库事务中持久化到 `outbox_message` 表。`OutboxRelayScheduler` 每 2 秒轮询 PENDING 记录并异步投递到 RabbitMQ。
- **死信队列（DLQ）**：所有工作流队列均配置了 `x-dead-letter-exchange: ai.dlx.exchange`。当 Python 消费者以 `nack(requeue=false)` 拒绝消息时，消息会自动路由到 `ai.dlq.queue`，避免静默丢失。

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
| Request → AI | 用户反馈 | `ai.req.feedback` | `ai.queue.feedback` | Java Backend | Python AI Worker |
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
  "imageCheckEnabled": false,
  "screenshotBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `jobId` | String | 职位唯一标识 |
| `url` | String | 职位详情页 URL |
| `imageCheckEnabled` | boolean | 是否启用视觉验证；当前 HTTP 职位提交路径会设置为 `false` |
| `screenshotBase64` | String | 可选，用户上传的截图 Base64，用于 AI 解析 fallback |

---

### 3.2 ResumeParseCommand — 简历解析请求

**发送时机**：用户上传简历后，`ResumeApplicationService.handleUpload()` 将命令写入 Outbox 表。

```json
{
  "resumeId": "resume-uuid-5678",
  "fileUrl": "https://storage.example.com/resumes/xxx.pdf",
  "format": "application/pdf"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `resumeId` | String | 简历版本唯一标识 |
| `fileUrl` | String | 当前配置的存储后端生成的临时文件 URL |
| `format` | String | 上传文件的 content type，例如 `application/pdf` 或 DOCX MIME type |

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
| `relatedJobTexts` | List<String> | 最多 5 条其他已完成职位文本，用于上下文 |
| `init` | Boolean | 是否为对话初始化请求 |
| `locale` | String | 使用者界面语言环境，例如 `en`、`zh-CN` 或 `zh-TW` |

---

### 3.4 JobRankCommand — 职位精排请求

**发送时机**：后端向量检索召回可见职位后，`MatchingApplicationService.startMatching()` 将精排命令写入 Outbox 表。

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

| 字段 | 类型 | 说明 |
|------|------|------|
| `matchId` | String | 职位匹配任务 ID |
| `userId` | String | 用户 ID |
| `resumeVersionId` | String | 简历版本 ID |
| `resumeText` | String | 已解析简历文本或当前简历内容 |
| `query` | String | 用户查询文本，可为空 |
| `recalledJobIds` | List<String> | 后端向量召回并过滤隐藏职位后的职位 ID |
| `jobDetails` | Map<String, Object> | 每个召回职位的详情，包含 title、company、description 与 semanticMatch |

---

### 3.5 UserFeedbackCommand — 增量训练反馈标签

**发送时机**：`JobApplicationService.scoreJob()` 完成后，评分结果被序列化并写入 Outbox 表，路由键为 `ai.req.feedback`。

**说明**：此消息为**发后即忘**（fire-and-forget）。AI worker 消费后将带标签的特征样本写入 Redis，并用于定时 LightGBM 重新训练，不会向后端发送结果回调。

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

| 字段 | 类型 | 说明 |
|------|------|------|
| `matchId` | String | 唯一反馈消息 ID |
| `userId` | UUID | 产生评分的用户 |
| `resumeVersionId` | String | 简历版本唯一标识 |
| `jobId` | String | 职位唯一标识 |
| `feedbackType` | String | 反馈标签；当前评分流程适合时发送 `APPLY`，否则发送 `IGNORE` |
| `score` | Double | 最终融合得分（0.0-1.0） |
| `context` | String | JSON 字符串，包含简历/职位上下文以及可选的 `llmOverallScore`、`semanticMatch`、`datasetScore`、`llmModel` |
| `timestamp` | String | ISO 8601 时间戳 |

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

| data 子字段 | 类型 | 说明 |
|-------------|------|------|
| `content` | String | AI 回复的文本内容 |
| `fileUrl` | String | AI 生成文件的 URL（可选） |
| `resumeModification.modified` | Boolean | AI 是否改写或优化了简历 |
| `resumeModification.markdown` | String | `modified=true` 时返回的完整优化后简历 Markdown |

---

### 4.4 职位精排结果（type = JOB_RANK）

**消费端**：`AiResultMessageListener.onJobRankResult()` → `MatchingFacade.saveJobRankResult()`

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

| data 子字段 | 类型 | 说明 |
|-------------|------|------|
| `rankTimeMs` | Integer | AI 精排耗时，单位毫秒 |
| `rankedResults` | List<Map> | 精排后的职位列表 |
| `rankedResults[].jobId` | String | 职位 ID |
| `rankedResults[].matchScore` | Float | 最终精排分数 |
| `rankedResults[].matchFactors` | Object | 技能、经验和地点匹配拆分 |
| `rankedResults[].matchReason` | String | 可选，LLM 为高排名职位生成的匹配说明 |

---

## 5. 文件上传与存储

### 5.1 后端文件上传 API

前端或 AI 层可将生成的文件流上传至后端，由后端通过当前配置的存储后端保存：

- **简历上传**：`POST /api/v1/resumes`（`multipart/form-data`）
- **对话附件上传**：`POST /api/v1/conversations/{conversationId}/files`（`multipart/form-data`）

### 5.2 存储路径约定

| 业务 | 对象键前缀示例 |
|------|----------------|
| 简历文件 | `resumes/{uuid}_{filename}` |
| 对话附件 | `conversations/{conversationId}/{uuid}_{filename}` |

### 5.3 预签名 URL

`FileStorageService.generatePresignedUrl()` 为上传成功的文件生成临时访问 URL（默认 7 天有效期）。

---

## 6. 状态码与错误处理

- `COMPLETED`：AI 处理成功，`data` 中包含有效结果。
- `FAILED`：AI 处理失败，`errorMessage` 必须包含具体错误文本，`data` 可为空。
- 消费端对异常进行 try-catch 包裹，避免 MQ 消息无限重试导致队列阻塞。
