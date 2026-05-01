<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](job.md) | [简体中文](../../i18n/zh-Hans-CN/api/backend/job.md) | [繁體中文](../../i18n/zh-Hant-TW/api/backend/job.md)

# Job Intelligent Parsing Module (Link-to-Match) API Documentation

> All `userId` fields have been unified to UUID string format (standard 36 characters, e.g. `550e8400-e29b-41d4-a716-446655440000`).

This module provides the capability for job seekers to submit desired job links, and leverages the backend to asynchronously trigger the Python AI service via RabbitMQ to scrape web pages and parse them into structured data. The entire process follows the DDD hexagonal architecture; the Java backend only interacts with the AI service via MQ, completely eliminating HTTP synchronous direct-call coupling.

---

## 1. Client to Backend Interfaces

### 1.1 Submit Job Link
**Endpoint:** `POST /api/v1/jobs`
**Description:** Receives a job URL submitted by the user and publishes an asynchronous parsing request to RabbitMQ. Returns the initial status of the task immediately.

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
  "imageCheckEnabled": true,
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
    "imageCheckEnabled": true,
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

### 1.6 Get Match History
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

### 1.7 Match Jobs (Intelligent)
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
  "imageCheckEnabled": true
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
