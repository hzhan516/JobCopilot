<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/job.md) | [简体中文](../../../zh-Hans-CN/api/backend/job.md) | [繁體中文](job.md)

# 職位智慧剖析模組 (Link-to-Match) API 文件

> 所有 `userId` 欄位已統一為 UUID 字串格式（標準 36 字元，如 `550e8400-e29b-41d4-a716-446655440000`）。

本模組提供求職者提交心儀職位連結，並利用後端透過 RabbitMQ 非同步觸發 Python AI 服務抓取網頁、剖析為結構化資料的能力。整個流程遵循 DDD 六邊形架構，Java 後端僅透過 MQ 與 AI 服務互動，完全消除了 HTTP 同步直調的耦合。

---

## 1. 客戶端與後端互動介面 (Client to Backend)

### 1.1 提交職位連結 (Submit Job Link)
**Endpoint:** `POST /api/v1/jobs`
**描述:** 接收使用者提交的職位 URL 和可選的職位截圖，並將非同步剖析請求發佈到 RabbitMQ。立即返回任務的初始狀態。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: multipart/form-data
```

**Form Fields:**

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `url` | String | 是 | 職位發布頁面 URL。 |
| `screenshot` | File | 否 | 可選的職位截圖。後端會將其轉成 Base64，作為 AI 解析的 fallback 輸入。 |

**Request Example:**
```bash
curl -X POST "http://localhost/api/v1/jobs" \
  -H "Authorization: Bearer <user-jwt-token>" \
  -F "url=https://www.linkedin.com/jobs/view/12345" \
  -F "screenshot=@job-posting.png"
```

**Response Body (`JobResponse`):**
```json
{
  "id": "job-uuid-1234",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "originalUrl": "https://www.linkedin.com/jobs/view/12345",
  "status": "PENDING",
  "parsedContent": null,
  "imageCheckEnabled": false,
  "errorMessage": null
}
```
**注意:** `status` 狀態列舉可能為 `PENDING`, `SCRAPING`, `PARSING`, `COMPLETED`, `FAILED`。最終狀態需要透過 1.2 介面輪詢取得。

### 1.2 取得職位詳情 (Get Job Details)
**Endpoint:** `GET /api/v1/jobs/{jobId}`
**描述:** 根據職位 ID 取得職位剖析狀態及詳情。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
```

**Response Body (`JobResponse`):**
```json
{
  "id": "job-uuid-1234",
  "userId": "user-uuid",
  "originalUrl": "https://www.linkedin.com/jobs/view/12345",
  "status": "COMPLETED",
  "parsedContent": {
    "title": "Software Engineer",
    "company": "Tech Corp",
    "description": "Full job description...",
    "requirements": ["Java", "Spring Boot", "AWS"]
  },
  "imageCheckEnabled": false,
  "errorMessage": null
}
```

### 1.3 取得職位列表 (List Jobs)
**Endpoint:** `GET /api/v1/jobs`
**描述:** 取得當前登入使用者提交的所有職位列表。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
```

**Response Body (`List<JobResponse>`):**
```json
[
  {
    "id": "job-uuid-1234",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "originalUrl": "https://www.linkedin.com/jobs/view/12345",
    "status": "COMPLETED",
    "parsedContent": null,
    "imageCheckEnabled": false,
    "errorMessage": null
  }
]
```

### 1.4 職位智慧配對 (Match Jobs)
**Endpoint:** `POST /api/v1/jobs/match`
**描述:** 啟動非同步職位配對流程。回傳處理中狀態，需透過 1.5 介面輪詢結果。

### 1.5 查詢配對結果 (Get Match Result)
**Endpoint:** `GET /api/v1/jobs/match/{matchId}`
**描述:** 根據配對任務 ID 查詢非同步配對結果。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
```

**Response Body (`JobMatchResponse`):**
與 1.4 回應格式相同。

### 1.6 從列表隱藏職位 (Hide Job from List)
**Endpoint:** `DELETE /api/v1/jobs/{jobId}`
**描述:** 將職位從使用者可見的職位列表中隱藏，但保留資料庫記錄。後端會設定 `hidden_at`，而 `GET /api/v1/jobs` 只回傳 `hidden_at` 為 null 的職位。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
```

**Response Body:**
```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

### 1.7 取得歷史配對列表 (Get Match History)
**Endpoint:** `GET /api/v1/jobs/match/history`
**描述:** 取得當前登入使用者的歷史配對記錄列表。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
```

**Response Body (`List<JobMatchHistoryResponse>`):**
```json
[
  {
    "matchId": "match-uuid-1234",
    "status": "COMPLETED",
    "total": 5,
    "createdAt": "2026-04-15T10:30:00"
  }
]
```

---

### 1.8 職位智慧配對 (Match Jobs)
**Endpoint:** `POST /api/v1/jobs/match`
**描述:** 根據使用者履歷或查詢條件，呼叫 AI 服務取得配對的職位推薦列表。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body (`JobMatchRequest`):**
```json
{
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
  "query": "Java Spring Boot",
  "topK": 10,
  "filters": {
    "location": "Remote"
  }
}
```

**Response Body (`JobMatchResponse`):**
```json
{
  "matchId": "match-uuid-1234",
  "status": "PROCESSING",
  "matches": [],
  "total": 0,
  "recallTime": null,
  "rankTime": null
}
```

非同步完成後查詢結果：
```json
{
  "matchId": "match-uuid-1234",
  "status": "COMPLETED",
  "matches": [
    {
      "jobId": "mock-job-1",
      "title": "Senior Java Developer",
      "company": "Tech Corp",
      "matchScore": 0.92,
      "matchFactors": {
        "skillMatch": 0.95,
        "experienceMatch": 0.90,
        "locationMatch": 0.88
      },
      "description": "Looking for an experienced Java developer with Spring Boot skills."
    }
  ],
  "total": 1,
  "recallTime": 120,
  "rankTime": 45
}
```

---

### 1.9 向量搜尋職位 (Vector Search Jobs)
**Endpoint:** `POST /api/v1/jobs/vector-search`
**描述:** 對 `job_vectors` 資料表執行近似最近鄰（ANN）向量搜尋，基於提供的查詢向量或查詢文字返回語義最相似的職位。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body (`VectorSearchRequest`):**

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `queryText` | String | 否 | 查詢文字。當 `queryEmbedding` 未提供時用於生成向量。 |
| `queryEmbedding` | List<Float> | 否 | 預先計算的查詢嵌入向量，優先於 `queryText`。 |
| `limit` | Integer | 否 | 返回最大數量。預設：10，最大：100。 |
| `filters` | Map<String, String> | 否 | 過濾條件（預留擴展）。 |

**請求範例（使用 queryText）:**
```json
{
  "queryText": "Senior Java Developer with Spring Boot experience",
  "limit": 10
}
```

**請求範例（使用 queryEmbedding）:**
```json
{
  "queryEmbedding": [0.0123, -0.0456, 0.0789, "..."],
  "limit": 5
}
```

**成功回應 (200):**

```json
{
  "code": 200,
  "message": "Success",
  "data": [
    {
      "jobId": "job-921716",
      "title": "Senior Java Developer",
      "company": "Tech Corp",
      "description": "Looking for an experienced Java developer...",
      "requirements": ["Java", "Spring Boot", "AWS"],
      "similarity": 0.92,
      "matchFactors": {
        "similarity": 0.92
      }
    }
  ]
}
```

**回應欄位說明 (`VectorSearchResponse`):**

| 欄位 | 類型 | 說明 |
|------|------|------|
| `jobId` | String | 職位唯一識別碼 |
| `title` | String | 職位標題 |
| `company` | String | 公司名稱 |
| `description` | String | 職位描述 |
| `requirements` | List<String> | 職位要求列表 |
| `similarity` | Float | 相似度得分（0-1，越大越相似） |
| `matchFactors` | Map<String, Object> | 配對因子詳情（預留擴展） |

**錯誤回應:**

- `400` — `queryText` 和 `queryEmbedding` 均未提供
- `401` — 未認證（JWT Token 無效或缺失）
- `503` — AI 服務不可用（提供了 `queryText` 但嵌入生成失敗）

---

### 1.10 職位評分 (Score Job)
**Endpoint:** `POST /api/v1/jobs/{jobId}/score`
**描述:** 針對單一職位與履歷進行匹配評分，呼叫 AI 服務的適配度端點。評分結果會儲存至歷史記錄，並非同步發送評分標籤給 AI 服務以供增量模型訓練。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body (`JobScoreRequest`):**
```json
{
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002"
}
```

**Response Body (`JobScoreResponse`):**
```json
{
  "suitable": true,
  "summary": "履歷符合 Java、Spring Boot 等關鍵要求。",
  "finalScore": 0.85,
  "breakdown": {
    "skillScore": 0.9,
    "experienceScore": 0.8,
    "overallScore": 0.85
  }
}
```

**回應欄位說明：**

| 欄位 | 類型 | 說明 |
|------|------|------|
| `suitable` | Boolean | 候選人是否整體適合該職位 |
| `summary` | String | 1-2 句簡潔的評估說明 |
| `finalScore` | Float | 最終融合得分（0.0-1.0）。有自適應資料集模型時使用模型分數，否則回退到舊版 LLM/資料集加權分數。 |
| `breakdown.skillScore` | Float | 技能匹配得分 |
| `breakdown.experienceScore` | Float | 經驗匹配得分 |
| `breakdown.overallScore` | Float | LLM 綜合得分 |

---

## 2. 後端與 AI 服務層互動介面 (Backend to Python AI Service via MQ)

為了遵循系統架構，Java 後端不再直接透過 HTTP 同步呼叫 AI 服務，而是透過 **RabbitMQ** 發佈非同步任務請求，並監聽 AI 服務的處理結果回呼。

### 2.1 職位剖析請求 (Backend -> AI Service)
**Exchange:** `ai.direct.exchange`
**Routing Key:** `ai.req.job.parse`
**Queue:** `ai.queue.job.parse`

**Message Body (`JobParseCommand`):**
```json
{
  "jobId": "job-uuid-1234",
  "url": "https://www.linkedin.com/jobs/view/12345",
  "imageCheckEnabled": false,
  "screenshotBase64": "base64-encoded-image-or-null"
}
```

### 2.2 職位剖析結果回呼 (AI Service -> Backend)
**Exchange:** `ai.direct.exchange`
**Routing Key:** `backend.res.job.parse`
**Queue:** `backend.queue.job.parse`

**Message Body (`AiResultEvent`):**
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

### 2.3 評分標籤請求 (Backend -> AI Service)
**Exchange:** `ai.direct.exchange`
**Routing Key:** `ai.req.feedback`
**Queue:** `ai.queue.feedback`

**描述:** 當 `scoreJob()` 完成後，後端將評分結果非同步發送至 AI 服務進行增量模型訓練。此為 **發送後即忘（fire-and-forget）** 訊息，不期望結果回呼。

**Message Body:**
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

| 欄位 | 類型 | 說明 |
|------|------|------|
| `matchId` | String | 唯一反饋訊息 ID |
| `userId` | UUID | 產生評分的使用者 |
| `resumeVersionId` | String | 履歷版本唯一識別 |
| `jobId` | String | 職位唯一識別 |
| `feedbackType` | String | 反饋標籤；目前評分流程適合時發送 `APPLY`，否則發送 `IGNORE` |
| `score` | Double | 最終融合得分（0.0-1.0） |
| `context` | String | JSON 字串，包含履歷/職位上下文以及可選的 `llmOverallScore`、`semanticMatch`、`datasetScore`、`llmModel` |
| `timestamp` | String | ISO 8601 時間戳 |

> **注意：**
> - 如果 `status` 為 `FAILED`，則 `errorMessage` 必須包含導致失敗的具體原因文本，且 `data` 可以為空。
> - AI 服務處理完成後，必須將結果發送到 `backend.res.job.parse` 路由鍵，由 `AiResultMessageListener` 接收並交由 `JobFacade.handleJobProcessResult` 處理。
