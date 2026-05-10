<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/job.md) | [简体中文](job.md) | [繁體中文](../../../zh-Hant-TW/api/backend/job.md)

# 职位智能解析模块 (Link-to-Match) API 文档

> 所有 `userId` 字段已统一为 UUID 字符串格式（标准 36 字符，如 `550e8400-e29b-41d4-a716-446655440000`）。

本模块提供求职者提交心仪职位链接，并利用后端通过 RabbitMQ 异步触发 Python AI 服务抓取网页、解析为结构化数据的能力。整个流程遵循 DDD 六边形架构，Java 后端仅通过 MQ 与 AI 服务交互，完全消除了 HTTP 同步直调的耦合。

---

## 1. 客户端与后端交互接口 (Client to Backend)

### 1.1 提交职位链接 (Submit Job Link)
**Endpoint:** `POST /api/v1/jobs`
**描述:** 接收用户提交的职位URL，并将异步解析请求发布到 RabbitMQ。立即返回任务的初始状态。

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
  "imageCheckEnabled": true,
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
    "imageCheckEnabled": true,
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

### 1.6 获取历史匹配列表 (Get Match History)
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

### 1.7 职位智能匹配 (Match Jobs)
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

### 1.8 向量搜索职位 (Vector Search Jobs)
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

### 1.9 职位评分 (Score Job)
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
| `finalScore` | Float | 最终融合得分（0.0-1.0）；70% LLM + 30% 数据集模型 |
| `breakdown.skillScore` | Float | 技能匹配得分 |
| `breakdown.experienceScore` | Float | 经验匹配得分 |
| `breakdown.overallScore` | Float | LLM 综合得分 |

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
  "imageCheckEnabled": true
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
**Routing Key:** `ai.req.model.incremental`
**Queue:** `ai.queue.model.incremental`

**描述:** `scoreJob()` 完成后，后端将评分结果异步发送到 AI 服务用于增量模型训练。此消息为**发后即忘**（fire-and-forget），不期待结果回调。

**Message Body:**
```json
{
  "messageId": "msg-uuid-v4",
  "jobId": "job-uuid",
  "resumeVersionId": "resume-uuid",
  "resume": {
    "skills": ["Python", "AWS", "Kubernetes"],
    "experience": [
      {"title": "Senior Engineer", "summary": "Built microservices...", "company": "TechCorp"}
    ]
  },
  "job": {
    "title": "Software Engineer",
    "description": "We are looking for...",
    "requirements": ["Python", "AWS"]
  },
  "suitable": true,
  "finalScore": 0.85,
  "timestamp": "2026-05-09T16:00:00Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `messageId` | String | 唯一消息 ID，用于去重 |
| `jobId` | String | 职位唯一标识 |
| `resumeVersionId` | String | 简历版本唯一标识 |
| `resume.skills` | List<String> | 解析后的简历技能 |
| `resume.experience` | List<Map> | 解析后的简历经验条目 |
| `job.title` | String | 职位标题 |
| `job.description` | String | 职位描述 |
| `job.requirements` | List<String> | 职位要求 |
| `suitable` | Boolean | 简历是否适合该职位 |
| `finalScore` | Float | 最终融合得分（0.0-1.0） |
| `timestamp` | String | ISO 8601 时间戳 |

> **注意：**
> - 如果 `status` 为 `FAILED`，则 `errorMessage` 必须包含导致失败的具体原因文本，且 `data` 可以为空。
> - AI 服务处理完成后，必须将结果发送到 `backend.res.job.parse` 路由键，由 `AiResultMessageListener` 接收并交由 `JobFacade.handleJobProcessResult` 处理。
