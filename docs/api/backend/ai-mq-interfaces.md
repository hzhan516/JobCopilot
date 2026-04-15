# AI 服务消息队列模块 (AI Message Queue Module) API 文档

为了支持架构设计中的微服务解耦与可扩展性，**所有与 AI 相关的请求**（如简历解析、职位抓取、视觉验证等）**全部由同步 HTTP 调用改为 RabbitMQ 异步通信**。

后端 (Spring Boot) 作为 Producer 发出请求，作为 Consumer 接收 AI 服务的处理结果。

---

## 1. 消息中间件配置

- **Host/Port**: `localhost:5672` (生产内部网络一般为 `resume-rabbitmq:5672`)
- **Credentials**: `guest` / `guest`
- **Exchange (Topic)**: `ai.topic.exchange`
- **消息格式**: JSON (`application/json`)
- **最大消息大小限制**: 10MB (10485760 bytes)。大型文件通过 OSS/S3 URL 传递，避免在 MQ 中传输超大 Base64 数据。

---

## 2. 职位解析模块 (Job Process) MQ 接口定义

当用户提交一个心仪的职位 URL 链接时，触发被动解析流程。

### 2.1 Backend -> AI Service (请求)
**Routing Key:** `ai.job.process.req`
**Queue:** `q.ai.job.process.req`

**Message Body (`JobProcessRequestEvent`):**
```json
{
  "jobId": "job-uuid-1234",
  "url": "https://www.linkedin.com/jobs/view/12345",
  "imageCheckEnabled": true
}
```

### 2.2 AI Service -> Backend (响应)
**Routing Key:** `ai.job.process.res`
**Queue:** `q.ai.job.process.res`

**Message Body (`JobProcessResultEvent`):**
```json
{
  "jobId": "job-uuid-1234",
  "success": true,
  "parsedContent": {
    "title": "Software Engineer",
    "company": "Tech Corp",
    "description": "Full job description...",
    "requirements": ["Java", "Spring Boot", "AWS"]
  },
  "errorMessage": null
}
```

> **注意：**
> 如果 `success` 为 `false`，则 `errorMessage` 必须包含导致失败的具体原因文本，且 `parsedContent` 可以为空。

---

## 3. 简历解析模块 (Resume Parse) MQ 接口定义

当用户上传一份新简历时，后台先持久化 ORIGINAL 版本，然后触发解析。

### 3.1 Backend -> AI Service (请求)
**Routing Key:** `ai.req.resume.parse`
**Queue:** `ai.queue.resume.parse`

**Message Body (`ResumeParseCommand`):**
```json
{
  "resumeId": "resume-uuid-1234",
  "fileUrl": "http://minio.../resume.pdf",
  "format": "application/pdf"
}
```

### 3.2 AI Service -> Backend (响应)
**Routing Key:** `backend.res.resume.parse`
**Queue:** `backend.queue.resume.parse`

**Message Body (`AiResultEvent`):**
```json
{
  "referenceId": "resume-uuid-1234",
  "type": "RESUME_PARSE",
  "status": "COMPLETED",
  "data": {
    "name": "John Doe",
    "email": "john@example.com",
    "skills": ["Java", "Spring"],
    "experience": []
  },
  "errorMessage": null
}
```

---

## 4. 向量生成模块 (Vector Generation) MQ 接口定义

当简历被成功解析，或职位被成功解析后，通过该模块将 JSON Text 转化为 pgvector 能够存储的向量数据。

### 4.1 Backend -> AI Service (请求)
**Routing Key:** `ai.req.vector.gen`
**Queue:** `ai.queue.vector.gen`

**Message Body (`VectorGenCommand`):**
```json
{
  "referenceId": "resume-or-job-uuid",
  "entityType": "RESUME", 
  "text": "Full parsed JSON string or raw job description text"
}
```

### 4.2 AI Service -> Backend (响应)
**Routing Key:** `backend.res.vector.gen`
**Queue:** `backend.queue.vector.gen`

**Message Body (`AiResultEvent`):**
```json
{
  "referenceId": "resume-or-job-uuid",
  "type": "VECTOR_GEN",
  "status": "COMPLETED",
  "data": {
    "embedding": [0.123, -0.456, 0.789]
  },
  "errorMessage": null
}
```
