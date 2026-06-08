<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/job.md) | [简体中文](job.md) | [繁體中文](../../../zh-Hant-TW/api/backend/job.md)

# 职位智能解析模块 (Link-to-Match) API 文档

> 所有 `userId` 字段已统一为 UUID 字符串格式（标准 36 字符，如 `550e8400-e29b-41d4-a716-446655440000`）。

本模块提供求职者提交心仪职位链接，并利用后端通过 RabbitMQ 异步触发 Python AI 服务抓取网页、解析为结构化数据的能力。整个流程遵循 DDD 六边形架构，Java 后端仅通过 MQ 与 AI 服务交互，完全消除了 HTTP 同步直调的耦合。

---

## 1. 客户端与后端交互接口 (Client to Backend)

### 1.1 提交职位链接 (Submit Job Link)
**Endpoint:** `POST /api/v1/jobs`
**描述:** 接收用户提交的职位 URL 和可选的职位截图，并将异步解析请求发布到 RabbitMQ。立即返回任务的初始状态。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: multipart/form-data
```

**Form Fields:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `url` | String | 是 | 职位发布页面 URL。 |
| `screenshot` | File | 否 | 可选的职位截图。后端会将其转成 Base64，作为 AI 解析的 fallback 输入。 |

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
**注意:** `status` 状态枚举可能为 `PENDING`, `SCRAPING`, `PARSING`, `COMPLETED`, `FAILED`。最终状态需要通过 1.2 接口轮询获取。

### 1.2 获取职位详情 (Get Job Details)
**Endpoint:** `GET /api/v1/jobs/{jobId}`
**描述:** 根据职位ID获取职位解析状态及详情。

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

### 1.3 获取职位列表 (List Jobs)
**Endpoint:** `GET /api/v1/jobs`
**描述:** 获取当前登录用户提交的所有职位列表。

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

### 1.4 职位智能匹配 (Match Jobs)
**Endpoint:** `POST /api/v1/jobs/match`
**描述:** 启动异步职位匹配流程。返回处理中状态，需通过 1.5 接口轮询结果。

### 1.5 查询匹配结果 (Get Match Result)
**Endpoint:** `GET /api/v1/jobs/match/{matchId}`
**描述:** 根据匹配任务ID查询异步匹配结果。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
```

**Response Body (`JobMatchResponse`):**
与 1.4 响应格式相同。

### 1.6 从列表隐藏职位 (Hide Job from List)
**Endpoint:** `DELETE /api/v1/jobs/{jobId}`
**描述:** 将职位从用户可见的职位列表中隐藏，但保留数据库记录。后端会设置 `hidden_at`，而 `GET /api/v1/jobs` 只返回 `hidden_at` 为 null 的职位。

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

### 1.7 获取历史匹配列表 (Get Match History)
**Endpoint:** `GET /api/v1/jobs/match/history`
**描述:** 获取当前登录用户的历史匹配记录列表。

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

### 1.8 职位智能匹配 (Match Jobs)
**Endpoint:** `POST /api/v1/jobs/match`
**描述:** 根据用户简历或查询条件，调用 AI 服务获取匹配的职位推荐列表。

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

异步完成后查询结果：
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

### 1.9 向量搜索职位 (Vector Search Jobs)
**Endpoint:** `POST /api/v1/jobs/vector-search`
**描述:** 对 `job_vectors` 表执行近似最近邻（ANN）向量搜索，基于提供的查询向量或查询文本返回语义最相似的职位。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body (`VectorSearchRequest`):**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `queryText` | String | 否 | 查询文本。当 `queryEmbedding` 未提供时用于生成向量。 |
| `queryEmbedding` | List<Float> | 否 | 预计算的查询嵌入向量，优先于 `queryText`。 |
| `limit` | Integer | 否 | 返回最大数量。默认：10，最大：100。 |
| `filters` | Map<String, String> | 否 | 过滤条件（预留扩展）。 |

**请求示例（使用 queryText）:**
```json
{
  "queryText": "Senior Java Developer with Spring Boot experience",
  "limit": 10
}
```

**请求示例（使用 queryEmbedding）:**
```json
{
  "queryEmbedding": [0.0123, -0.0456, 0.0789, "..."],
  "limit": 5
}
```

**成功响应 (200):**

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

**响应字段说明 (`VectorSearchResponse`):**

| 字段 | 类型 | 说明 |
|------|------|------|
| `jobId` | String | 职位唯一标识符 |
| `title` | String | 职位标题 |
| `company` | String | 公司名称 |
| `description` | String | 职位描述 |
| `requirements` | List<String> | 职位要求列表 |
| `similarity` | Float | 相似度得分（0-1，越大越相似） |
| `matchFactors` | Map<String, Object> | 匹配因子详情（预留扩展） |

**错误响应:**

- `400` — `queryText` 和 `queryEmbedding` 均未提供
- `401` — 未认证（JWT Token 无效或缺失）
- `503` — AI 服务不可用（提供了 `queryText` 但嵌入生成失败）

### 1.10 职位评分 (Score Job)
**Endpoint:** `POST /api/v1/jobs/{jobId}/score`
**描述:** 针对单个职位调用 AI 服务 suitability 端点进行简历匹配评分。评分结果会保存到历史记录，同时后端会异步发送评分标签到 AI 服务用于增量模型训练。

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
  "summary": "简历与职位关键要求（如 Java、Spring Boot）匹配度较高。",
  "finalScore": 0.85,
  "breakdown": {
    "skillScore": 0.9,
    "experienceScore": 0.8,
    "overallScore": 0.85
  }
}
```

**响应字段说明:**

| 字段 | 类型 | 说明 |
|------|------|------|
| `suitable` | Boolean | 候选人整体是否适合该职位 |
| `summary` | String | 1-2 句简洁的评分说明 |
| `finalScore` | Float | 最终融合得分（0.0-1.0）。有自适应数据集模型时使用模型分数，否则回退到旧版 LLM/数据集加权分数。 |
| `breakdown.skillScore` | Float | 技能匹配得分 |
| `breakdown.experienceScore` | Float | 经验匹配得分 |
| `breakdown.overallScore` | Float | LLM 综合得分 |

---

### 1.11 更新职位 (Update Job)
**Endpoint:** `PUT /api/v1/jobs/{jobId}`
**描述:** 更新当前用户拥有的职位解析内容。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body (`UpdateJobRequest`):**
```json
{
  "title": "Senior Software Engineer",
  "company": "Tech Corp",
  "salary": "$120k-$150k",
  "location": "Remote",
  "description": "Updated job description...",
  "requirements": ["Java", "Spring Boot", "AWS"]
}
```

**Response Body (`JobResponse`):**
与 1.2 响应格式相同。

### 1.12 记录职位行为 (Track Job Action)
**Endpoint:** `POST /api/v1/jobs/{jobId}/track`
**描述:** 记录用户对职位的操作，例如 `CLICK`、`APPLY` 或 `REJECT`。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
```

**Query Parameters:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `action` | String | Yes | 操作类型，例如 `CLICK`、`APPLY` 或 `REJECT`。 |
| `resumeVersionId` | UUID String | No | 与本次操作关联的简历版本 ID。 |

**Response Body:**
```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

### 1.13 获取评分历史 (Get Score History)
**Endpoint:** `GET /api/v1/jobs/scores/history`
**描述:** 获取当前用户保存的职位评分历史。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
```

**Response Body (`List<JobScoreHistoryResponse>`):**
```json
[
  {
    "id": "score-record-uuid",
    "jobId": "job-uuid-1234",
    "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
    "suitable": true,
    "finalScore": 0.85,
    "skillScore": 0.9,
    "experienceScore": 0.8,
    "overallScore": 0.85,
    "summary": "The resume matches key requirements.",
    "createdAt": "2026-04-15T10:30:00Z"
  }
]
```

---

## 2. 后端与 AI 服务层交互接口 (Backend to Python AI Service via MQ)

为了遵循系统架构，Java 后端不再直接通过 HTTP 同步调用 AI 服务，而是通过 **RabbitMQ** 发布异步任务请求，并监听 AI 服务的处理结果回调。

### 2.1 职位解析请求 (Backend -> AI Service)
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

### 2.2 职位解析结果回调 (AI Service -> Backend)
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

### 2.3 评分标签请求 (Backend -> AI Service)
**Exchange:** `ai.direct.exchange`
**Routing Key:** `ai.req.feedback`
**Queue:** `ai.queue.feedback`

**描述:** `scoreJob()` 完成后，后端将评分结果异步发送到 AI 服务用于增量模型训练。此消息为**发后即忘**（fire-and-forget），不期待结果回调。

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

> **注意：**
> - 如果 `status` 为 `FAILED`，则 `errorMessage` 必须包含导致失败的具体原因文本，且 `data` 可以为空。
> - AI 服务处理完成后，必须将结果发送到 `backend.res.job.parse` 路由键，由 `AiResultMessageListener` 接收并交由 `JobFacade.handleJobProcessResult` 处理。
