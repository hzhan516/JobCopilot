# 职位智能解析模块 (Link-to-Match) API 文档

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
  "jobId": "job-uuid-1234",
  "userId": "user-uuid",
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
  "jobId": "job-uuid-1234",
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

> **注意：**
> - 如果 `status` 为 `FAILED`，则 `errorMessage` 必须包含导致失败的具体原因文本，且 `data` 可以为空。
> - AI 服务处理完成后，必须将结果发送到 `backend.res.job.parse` 路由键，由 `AiResultMessageListener` 接收并交由 `JobFacade.handleJobProcessResult` 处理。
