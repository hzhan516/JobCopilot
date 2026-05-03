<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/ai-mq-interfaces.md) | [简体中文](../../../zh-Hans-CN/api/backend/ai-mq-interfaces.md) | [繁體中文](ai-mq-interfaces.md)

# AI 服務互動 API 與 MQ 事件契約彙總

> 本文件集中彙總 Java 後端與 Python AI 服務之間的所有 REST API（如有）和 RabbitMQ 訊息佇列契約。
> 各模組的詳細說明請同時參考 `job.md`、`conversation.md`、`resume.md`。

---

## 1. 互動原則

- **單向非同步**：Java 後端透過 MQ 發送任務請求，Python AI 服務處理完成後透過 MQ 返回結果。
- **無直接 HTTP 耦合**：除健康檢查外，Java 後端不直接 HTTP 呼叫 AI 服務；所有耗時操作均走訊息佇列。
- **統一 Exchange**：所有 MQ 訊息共用 `ai.direct.exchange`（DirectExchange）。
- **事務發件箱（Outbox）**：後端**不直接**發送 MQ 訊息，而是將訊息與業務資料在同一個本地資料庫交易中持久化到 `outbox_message` 表。`OutboxRelayScheduler` 每 2 秒輪詢 PENDING 記錄並非同步投遞到 RabbitMQ。
- **死信佇列（DLQ）**：全部 10 個業務佇列均配置了 `x-dead-letter-exchange: ai.dlx.exchange`。當 Python 消費者以 `nack(requeue=false)` 拒絕訊息時，訊息會自動路由到 `ai.dlq.queue`，避免靜默丟失。

---

## 1.5 Outbox 模式詳解

後端採用**事務發件箱（Transactional Outbox）**模式，保證資料庫寫入與 MQ 訊息發布的原子性。

### 流程

1. **業務交易**：在 `@Transactional` 方法中，後端：
   - 將業務資料儲存到 PostgreSQL（例如狀態為 `PROCESSING` 的 `JobMatchResult`）。
   - 將 MQ 命令序列化為 JSON，並以 `status = PENDING` 插入 `outbox_message` 表。

2. **Outbox 轉發**：`OutboxRelayScheduler` 每 2 秒執行一次（`@Scheduled(fixedDelay = 2000)`）：
   - 從 `outbox_message` 查詢所有 `PENDING` 記錄。
   - 對每條記錄呼叫 `rabbitTemplate.convertAndSend(exchange, routingKey, payload)`。
   - 成功：更新記錄為 `status = SENT` 並設定 `sentAt`。
   - 失敗：更新記錄為 `status = FAILED` 並記錄錯誤日誌。

3. **清理**：`OutboxCleanupScheduler` 每天凌晨 3:00 執行（`@Scheduled(cron = "0 0 3 * * ?")`）：
   - 物理刪除 `status = SENT` 且 `sentAt < 目前時間 - 7 天` 的記錄。
   - 防止 `outbox_message` 表無限膨脹。

### 收益

- **原子性**：如果業務交易回滾，Outbox 記錄也會回滾。不會出現「訊息已發出但資料庫未提交」的不一致。
- **持久性**：即使 RabbitMQ 暫時不可用，訊息仍保留在 Outbox 表中，由轉發排程器重試。
- **可觀測性**：`outbox_message` 表作為所有非同步發送到 AI 服務的訊息的審計日誌。

---

## 2. MQ 拓撲總覽

| 方向 | 任務類型 | Routing Key | Queue | 生產者 | 消費者 |
|------|----------|-------------|-------|--------|--------|
| Request → AI | 職位剖析 | `ai.req.job.parse` | `ai.queue.job.parse` | Java Backend | Python AI |
| Response ← AI | 職位剖析結果 | `backend.res.job.parse` | `backend.queue.job.parse` | Python AI | Java Backend |
| Request → AI | 履歷剖析 | `ai.req.resume.parse` | `ai.queue.resume.parse` | Java Backend | Python AI |
| Response ← AI | 履歷剖析結果 | `backend.res.resume.parse` | `backend.queue.resume.parse` | Python AI | Java Backend |
| Request → AI | 向量生成 | `ai.req.vector.gen` | `ai.queue.vector.gen` | Java Backend | Python AI |
| Response ← AI | 向量生成結果 | `backend.res.vector.gen` | `backend.queue.vector.gen` | Python AI | Java Backend |
| Request → AI | 對話請求 | `ai.req.conversation` | `ai.queue.conversation` | Java Backend | Python AI |
| Response ← AI | 對話回覆結果 | `backend.res.conversation` | `backend.queue.conversation` | Python AI | Java Backend |
| Request → AI | 職位精排 | `ai.req.job.rank` | `ai.queue.job.rank` | Java Backend | Python AI |
| Response ← AI | 職位精排結果 | `backend.res.job.rank` | `backend.queue.job.rank` | Python AI | Java Backend |
| DLX → DLQ | 死信 | `dlq.routing.key` | `ai.dlq.queue` | 業務佇列（自動轉發） | 維運/監控（可手動消費） |

> **Outbox 說明**：上表中的 "Java Backend" 生產者實際上是 `OutboxRelayScheduler`，它從 `outbox_message` 表讀取記錄後轉發到 RabbitMQ。原始業務方法（如 `JobApplicationService.submitJob`）僅寫入 Outbox 表。

> **DLQ 說明**：`ai.dlx.exchange`（死信交換機）和 `ai.dlq.queue` 由 Spring AMQP 在啟動時自動宣告，無需手動配置 RabbitMQ。

---

## 3. 請求命令（Backend → AI）

### 3.1 JobParseCommand — 職位剖析請求

**發送時機**：使用者提交職位連結後，`JobApplicationService.submitJob()` 將命令寫入 Outbox 表。

```json
{
  "jobId": "job-uuid-1234",
  "url": "https://www.linkedin.com/jobs/view/12345",
  "imageCheckEnabled": true
}
```

| 欄位 | 類型 | 說明 |
|------|------|------|
| `jobId` | String | 職位唯一識別 |
| `url` | String | 職位詳情頁 URL |
| `imageCheckEnabled` | boolean | 是否啟用視覺驗證 |

---

### 3.2 ResumeParseCommand — 履歷剖析請求

**發送時機**：使用者上傳履歷後，`ResumeApplicationService.handleUpload()` 將命令寫入 Outbox 表。

```json
{
  "resumeId": "resume-uuid-5678",
  "fileUrl": "https://minio.example.com/resumes/xxx.pdf",
  "fileType": "PDF"
}
```

| 欄位 | 類型 | 說明 |
|------|------|------|
| `resumeId` | String | 履歷版本唯一識別 |
| `fileUrl` | String | 履歷檔案在 MinIO 上的 URL |
| `fileType` | String | 檔案類型，如 PDF、DOCX |

---

### 3.3 VectorGenCommand — 向量生成請求

**發送時機**：職位/履歷剖析完成後，觸發非同步向量生成；寫入 Outbox 表。

```json
{
  "referenceId": "entity-uuid",
  "entityType": "JOB",
  "text": "職位或履歷的純文本內容"
}
```

| 欄位 | 類型 | 說明 |
|------|------|------|
| `referenceId` | String | 關聯實體 ID（jobId 或 resumeVersionId） |
| `entityType` | String | `JOB` / `RESUME` |
| `text` | String | 待生成向量的文本 |

---

### 3.4 ConversationRequestCommand — 對話 AI 請求

**發送時機**：使用者發送對話訊息後，`ConversationApplicationService.sendMessage()` 將命令寫入 Outbox 表。

```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "messageHistory": [
    { "role": "USER", "content": "幫我優化一下專案經驗部分", "fileUrl": null }
  ],
  "currentMessage": "幫我優化一下專案經驗部分",
  "fileUrls": ["https://minio.example.com/resumes/xxx.pdf"],
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002"
}
```

| 欄位 | 類型 | 說明 |
|------|------|------|
| `conversationId` | String | 對話 ID |
| `userId` | String | 使用者 ID |
| `messageHistory` | List<Map> | 歷史訊息列表（role, content, fileUrl） |
| `currentMessage` | String | 當前使用者發送的最新訊息 |
| `fileUrls` | List<String> | 使用者引用的外部檔案 URL 列表 |
| `resumeVersionId` | String | 關聯履歷版本 ID（選用） |

---

## 4. 結果事件（AI → Backend）

### 統一事件結構：AiResultEvent

所有 AI 回呼均使用以下統一結構，透過 `type` 欄位區分業務類型：

```json
{
  "referenceId": "關聯實體ID",
  "type": "JOB_PARSE | RESUME_PARSE | VECTOR_GEN | CONVERSATION_REPLY",
  "status": "COMPLETED | FAILED",
  "data": { ... },
  "errorMessage": null,
  "eventType": "內部路由標記"
}
```

| 欄位 | 類型 | 說明 |
|------|------|------|
| `referenceId` | String | 關聯實體 ID |
| `type` | String | 事件類型 |
| `status` | String | `COMPLETED` 或 `FAILED` |
| `data` | Map<String, Object> | 業務資料載荷 |
| `errorMessage` | String | 失敗原因（`status=FAILED` 時必填） |
| `eventType` | String | 內部子路由標記（如 VECTOR_GEN 區分 JOB/RESUME） |

---

### 4.1 職位剖析結果（type = JOB_PARSE）

**消費端**：`AiResultMessageListener.onJobParseResult()` → `JobFacade.handleJobProcessResult()`

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

### 4.2 履歷剖析結果（type = RESUME_PARSE）

**消費端**：`AiResultMessageListener.onResumeParseResult()` → `ResumeFacade.handleParseResult()`

```json
{
  "referenceId": "resume-uuid-5678",
  "type": "RESUME_PARSE",
  "status": "COMPLETED",
  "data": {
    "parsedContent": { "name": "...", "skills": [...], "experience": [...] },
    "summary": "5年Java開發經驗..."
  },
  "errorMessage": null,
  "eventType": "RESUME"
}
```

---

### 4.3 向量生成結果（type = VECTOR_GEN）

**消費端**：`AiResultMessageListener.onVectorGenResult()`

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

### 4.4 對話回覆結果（type = CONVERSATION_REPLY）

**消費端**：`AiResultMessageListener.onConversationReply()` → `ConversationFacade.saveAiReply()`

```json
{
  "referenceId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "CONVERSATION_REPLY",
  "status": "COMPLETED",
  "data": {
    "content": "根據您的履歷，我建議從以下幾個方面優化工作經驗...",
    "fileUrl": "https://minio.example.com/conversations/xxx/optimized_resume.pdf"
  },
  "errorMessage": null,
  "eventType": null
}
```

| data 子欄位 | 類型 | 說明 |
|-------------|------|------|
| `content` | String | AI 回覆的文本內容 |
| `fileUrl` | String | AI 生成檔案的 URL（選用） |

---

## 5. 檔案上傳與 MinIO

### 5.1 後端檔案上傳 API

前端或 AI 層可將生成的檔案流上傳至後端，由後端轉存到 MinIO：

- **履歷上傳**：`POST /api/v1/resumes`（`multipart/form-data`）
- **對話附件上傳**：`POST /api/v1/conversations/{conversationId}/files`（`multipart/form-data`）

### 5.2 MinIO 儲存路徑約定

| 業務 | 物件鍵前綴範例 |
|------|----------------|
| 履歷檔案 | `resumes/{uuid}_{filename}` |
| 對話附件 | `conversations/{conversationId}/{uuid}_{filename}` |

### 5.3 預簽名 URL

`MinioFileStorageService.generatePresignedUrl()` 為上傳成功的檔案生成臨時存取 URL（預設 7 天有效期）。

---

## 6. 狀態碼與錯誤處理

- `COMPLETED`：AI 處理成功，`data` 中包含有效結果。
- `FAILED`：AI 處理失敗，`errorMessage` 必須包含具體錯誤文本，`data` 可為空。
- 消費端對異常進行 try-catch 包裹，避免 MQ 訊息無限重試導致佇列阻塞。
