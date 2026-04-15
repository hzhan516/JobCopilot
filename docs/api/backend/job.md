# 职位智能解析模块 (Link-to-Match) API 文档

本模块提供求职者提交心仪职位链接，并利用后端驱动 Python AI 服务抓取网页、解析为结构化数据的能力。

---

## 1. 客户端与后端交互接口 (Client to Backend)

### 1.1 提交职位链接 (Submit Job Link)
**Endpoint:** `POST /api/v1/jobs`
**描述:** 接收用户提交的职位URL，并触发同步的网页爬取与大模型解析流程。

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
  "code": 200,
  "message": "success",
  "data": {
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
}
```
**注意:** `status` 状态枚举可能为 `PENDING`, `SCRAPING`, `PARSING`, `COMPLETED`, `FAILED`。

### 1.2 获取职位详情 (Get Job Details)
**Endpoint:** `GET /api/v1/jobs/{jobId}`
**描述:** 根据职位ID获取职位解析状态及详情。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
```

**Response Body (`JobResponse`):**
结构同上 1.1 的 Response Body。

---

## 2. 后端与 AI 服务层交互接口 (Backend to Python AI Service)

为了遵循系统架构，Java后端不再直接调用 OpenAI 或 Jina 等第三方服务，而是通过 Spring `RestTemplate` 调用专门的 Python AI 服务的 HTTP 端点（预设地址由 `${ai.service.url}` 配置，如 `http://python-ai-service:8000`）。

### 2.1 网页抓取 (Scrape Web Page)
**Endpoint:** `POST {ai.service.url}/api/v1/ai/scrape`
**描述:** 由 `PythonAiServiceWebScraperAdapter` 调用，AI服务可集成 Jina Reader 等工具提取纯净 Markdown，如果需要还能返回截图。

**Request Body:**
```json
{
  "url": "https://www.linkedin.com/jobs/view/12345",
  "captureScreenshot": true
}
```

**Response Body:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "markdownText": "# Software Engineer\n\n## Company...",
    "screenshotUrl": "http://minio-or-s3/screenshot/1234.png"
  }
}
```

### 2.2 LLM 智能解析 (Parse Job Content)
**Endpoint:** `POST {ai.service.url}/api/v1/ai/parse-job`
**描述:** 由 `PythonAiServiceLlmParserAdapter` 调用，AI 服务负责使用 LLM Structured Outputs 将 Markdown 转换为结构化 JSON。

**Request Body:**
```json
{
  "markdownText": "# Software Engineer\n\n## Company..."
}
```

**Response Body:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "title": "Software Engineer",
    "company": "Tech Corp",
    "description": "Full job description...",
    "requirements": ["Java", "Spring Boot", "AWS"]
  }
}
```

### 2.3 视觉核验修复 (Vision Verification)
**Endpoint:** `POST {ai.service.url}/api/v1/ai/vision-verify-job`
**描述:** 由 `PythonAiServiceVisionVerificationAdapter` 调用，在用户开启图像校验功能时，AI 服务会结合初步解析出的 JSON 与 网页截图，再次核对有无遗漏并输出最新 JSON。

**Request Body:**
```json
{
  "parsedContent": {
    "title": "Software Engineer",
    "company": "Tech Corp",
    "description": "Full job description...",
    "requirements": ["Java"]
  },
  "screenshotUrl": "http://minio-or-s3/screenshot/1234.png"
}
```

**Response Body:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "title": "Software Engineer",
    "company": "Tech Corp",
    "description": "Full job description...",
    "requirements": ["Java", "Spring Boot", "AWS"]
  }
}
```
