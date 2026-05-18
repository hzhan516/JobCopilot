<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](job.md) | [简体中文](../../i18n/zh-Hans-CN/api/backend/job.md) | [繁體中文](../../i18n/zh-Hant-TW/api/backend/job.md)

# Job Intelligent Parsing Module (Link-to-Match) API Documentation

> All `userId` fields have been unified to UUID string format (standard 36 characters, e.g. `550e8400-e29b-41d4-a716-446655440000`).

This module provides the capability for job seekers to submit desired job links, and leverages the backend to asynchronously trigger the Python AI service via RabbitMQ to scrape web pages and parse them into structured data. The entire process follows the DDD hexagonal architecture; the Java backend only interacts with the AI service via MQ, completely eliminating HTTP synchronous direct-call coupling.

---

## 1. Client to Backend Interfaces

### 1.1 Submit Job Link
**Endpoint:** `POST /api/v1/jobs`
**Description:** Receives a job URL and an optional screenshot uploaded by the user, then publishes an asynchronous parsing request to RabbitMQ. Returns the initial status of the task immediately.

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: multipart/form-data
```

**Form Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `url` | String | Yes | Job posting URL. |
| `screenshot` | File | No | Optional job posting screenshot. The backend forwards it to the AI service as Base64 fallback input. |

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
**Note:** `status` enum may be `PENDING`, `SCRAPING`, `PARSING`, `COMPLETED`, `FAILED`. The final status needs to be polled via the 1.2 interface.

### 1.2 Get Job Details
**Endpoint:** `GET /api/v1/jobs/{jobId}`
**Description:** Gets job parsing status and details by job ID.

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

### 1.3 List Jobs
**Endpoint:** `GET /api/v1/jobs`
**Description:** Gets all job links submitted by the currently logged-in user.

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

### 1.4 Match Jobs
**Endpoint:** `POST /api/v1/jobs/match`
**Description:** Launches the asynchronous job matching process. Returns a processing status; results need to be polled via the 1.5 interface.

### 1.5 Get Match Result
**Endpoint:** `GET /api/v1/jobs/match/{matchId}`
**Description:** Queries asynchronous matching results by match task ID.

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
```

**Response Body (`JobMatchResponse`):**
Same format as the 1.4 response.

### 1.6 Hide Job from List
**Endpoint:** `DELETE /api/v1/jobs/{jobId}`
**Description:** Hides a job from user-facing job lists while preserving the database row. The backend sets `hidden_at`, and `GET /api/v1/jobs` returns only jobs whose `hidden_at` is null.

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

### 1.7 Get Match History
**Endpoint:** `GET /api/v1/jobs/match/history`
**Description:** Gets the historical match record list for the currently logged-in user.

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

### 1.8 Match Jobs (Intelligent)
**Endpoint:** `POST /api/v1/jobs/match`
**Description:** Calls the AI service to get a list of matching job recommendations based on the user's resume or query conditions.

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

Query result after async completion:
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

## 2. Backend to AI Service Interfaces (Backend to Python AI Service via MQ)

To comply with the system architecture, the Java backend no longer directly calls the AI service via HTTP synchronously; instead, it publishes asynchronous task requests via **RabbitMQ** and listens for processing result callbacks from the AI service.

### 2.1 Job Parse Request (Backend -> AI Service)
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

### 2.2 Job Parse Result Callback (AI Service -> Backend)
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

### 1.8 Vector Search Jobs
**Endpoint:** `POST /api/v1/jobs/vector-search`
**Description:** Performs approximate nearest neighbor (ANN) vector search on the `job_vectors` table. Returns the most semantically similar jobs based on the provided query embedding or query text.

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body (`VectorSearchRequest`):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `queryText` | String | No | Query text. Used to generate embedding if `queryEmbedding` is not provided. |
| `queryEmbedding` | List<Float> | No | Pre-computed query embedding vector. Takes precedence over `queryText`. |
| `limit` | Integer | No | Maximum number of results to return. Default: 10, Max: 100. |
| `filters` | Map<String, String> | No | Filter conditions (reserved for future extension). |

**Request Example (with queryText):**
```json
{
  "queryText": "Senior Java Developer with Spring Boot experience",
  "limit": 10
}
```

**Request Example (with queryEmbedding):**
```json
{
  "queryEmbedding": [0.0123, -0.0456, 0.0789, "..."],
  "limit": 5
}
```

**Success Response (200):**

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

**Response Field Descriptions (`VectorSearchResponse`):**

| Field | Type | Description |
|-------|------|-------------|
| `jobId` | String | Job unique identifier |
| `title` | String | Job title |
| `company` | String | Company name |
| `description` | String | Job description |
| `requirements` | List<String> | List of job requirements |
| `similarity` | Float | Similarity score (0-1, higher is more similar) |
| `matchFactors` | Map<String, Object> | Match factor details (reserved for extension) |

**Error Responses:**

- `400` — Missing both `queryText` and `queryEmbedding`
- `401` — Unauthorized (invalid or missing JWT token)
- `503` — AI Service unavailable (when `queryText` is provided but embedding generation fails)

---

### 1.9 Score Job
**Endpoint:** `POST /api/v1/jobs/{jobId}/score`
**Description:** Scores a single job against a resume by calling the AI service suitability endpoint. The scoring result is saved to history, and an asynchronous score label is sent to the AI service for incremental model training.

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
  "summary": "The resume matches key requirements such as Java, Spring Boot.",
  "finalScore": 0.85,
  "breakdown": {
    "skillScore": 0.9,
    "experienceScore": 0.8,
    "overallScore": 0.85
  }
}
```

**Response Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `suitable` | Boolean | Whether the candidate is generally a good fit |
| `summary` | String | 1-2 concise sentences explaining the decision |
| `finalScore` | Float | Final fused score (0.0-1.0). Uses the adaptive dataset model when available; otherwise falls back to the legacy LLM/dataset weighted score. |
| `breakdown.skillScore` | Float | Skill match score |
| `breakdown.experienceScore` | Float | Experience match score |
| `breakdown.overallScore` | Float | LLM overall score |

---

## 2. Backend to AI Service Interfaces (Backend to Python AI Service via MQ)

To comply with the system architecture, the Java backend no longer directly calls the AI service via HTTP synchronously; instead, it publishes asynchronous task requests via **RabbitMQ** and listens for processing result callbacks from the AI service.

### 2.1 Job Parse Request (Backend -> AI Service)
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

### 2.2 Job Parse Result Callback (AI Service -> Backend)
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

> **Notes:**
> - If `status` is `FAILED`, then `errorMessage` must contain the specific reason text causing the failure, and `data` can be empty.
> - After the AI service finishes processing, it must send the result to the `backend.res.job.parse` routing key, received by `AiResultMessageListener` and handled by `JobFacade.handleJobProcessResult`.

### 2.3 Score Label Request (Backend -> AI Service)
**Exchange:** `ai.direct.exchange`
**Routing Key:** `ai.req.model.incremental`
**Queue:** `ai.queue.model.incremental`

**Description:** After `scoreJob()` completes, the backend asynchronously sends the scoring result to the AI service for incremental model training. This is a **fire-and-forget** message; no result callback is expected.

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
  "llmOverallScore": 0.82,
  "finalScore": 0.85,
  "semanticMatch": 0.78,
  "datasetScore": 0.87,
  "llmModel": "configured-llm-model",
  "timestamp": "2026-05-09T16:00:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `messageId` | String | Unique message ID for deduplication |
| `jobId` | String | Job unique identifier |
| `resumeVersionId` | String | Resume version unique identifier |
| `resume.skills` | List<String> | Parsed resume skills |
| `resume.experience` | List<Map> | Parsed resume experience items |
| `job.title` | String | Job title |
| `job.description` | String | Job description |
| `job.requirements` | List<String> | Job requirements |
| `suitable` | Boolean | Whether the resume is suitable for the job |
| `llmOverallScore` | Float | LLM overall score before dataset model adjustment |
| `finalScore` | Float | Final fused score (0.0-1.0) |
| `semanticMatch` | Float | Optional semantic similarity score from vector matching |
| `datasetScore` | Float | Optional score from the adaptive incremental model |
| `llmModel` | String | Optional configured LLM model name used for the score |
| `timestamp` | String | ISO 8601 timestamp |
