<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/job.md) | [简体中文](../../../zh-Hans-CN/api/backend/job.md) | [繁體中文](job.md)

# 職位智慧剖析模組 (Link-to-Match) API 文件

> 所有 `userId` 欄位已統一為 UUID 字串格式（標準 36 字元，如 `550e8400-e29b-41d4-a716-446655440000`）。

本模組提供求職者提交心儀職位連結，並利用後端透過 RabbitMQ 非同步觸發 Python AI 服務抓取網頁、剖析為結構化資料的能力。整個流程遵循 DDD 六邊形架構，Java 後端僅透過 MQ 與 AI 服務互動，完全消除了 HTTP 同步直調的耦合。

---

## 1. 客戶端與後端互動介面 (Client to Backend)

### 1.1 提交職位連結 (Submit Job Link)
**Endpoint:** `POST /api/v1/jobs`
**描述:** 接收使用者提交的職位 URL，並將非同步剖析請求發佈到 RabbitMQ。立即返回任務的初始狀態。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body (`SubmitJobRequest`):**
```json
{
  "url": "https://www.linkedin.com/jobs/view/12345",
  "imageCheckEnabled": true
}
```

**Response Body (`JobResponse`):**
```json
{
  "id": "job-uuid-1234",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "originalUrl": "https://www.linkedin.com/jobs/view/12345",
  "status": "PENDING",
  "parsedContent": null,
  "imageCheckEnabled": true,
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
  "imageCheckEnabled": true,
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
    "imageCheckEnabled": true,
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

### 1.6 取得歷史配對列表 (Get Match History)
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

### 1.7 職位智慧配對 (Match Jobs)
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
  "userId": "550e8400-e29b-41d4-a716-446655440000",
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
  "imageCheckEnabled": true
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

> **注意：**
> - 如果 `status` 為 `FAILED`，則 `errorMessage` 必須包含導致失敗的具體原因文本，且 `data` 可以為空。
> - AI 服務處理完成後，必須將結果發送到 `backend.res.job.parse` 路由鍵，由 `AiResultMessageListener` 接收並交由 `JobFacade.handleJobProcessResult` 處理。
