<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](job-matching.md) | [简体中文](../../i18n/zh-Hans-CN/api/backend/job-matching.md) | [繁體中文](../../i18n/zh-Hant-TW/api/backend/job-matching.md)

# Job Intelligent Matching API Documentation

This document describes the asynchronous recall + ranking flow of the job intelligent matching module. The Java backend completes recall through local PGVector, then asynchronously sends recall results to the Python AI service for ranking via RabbitMQ, and finally persists to the database.

---

## 1. Asynchronous Flow Description

```
User request -> JobController.matchJobs()
    -> MatchingFacade.matchJobs()
        -> MatchingApplicationService.startJobMatch()
            1. Create PROCESSING status record (job_match_results)
            2. Query resume_vectors to get resume vector
            3. VectorSearchPort local recall topK jobs (job_vectors)
            4. Send JobRankCommand to ai.req.job.rank
            5. Immediately return JobMatchResponse(matchId, status=PROCESSING)

Python AI service -> Consume ai.req.job.rank -> Rank -> Send result to backend.res.job.rank

JobRankResultListener -> Consume backend.res.job.rank
    -> MatchingFacade.saveJobRankResult()
        -> MatchingApplicationService.saveMatchResult()
            -> Update job_match_results to COMPLETED + rankedResults
```

---

## 2. Client-Backend Interaction Interfaces

### 2.1 Start Asynchronous Job Matching
**Endpoint:** `POST /api/v1/jobs/match`  
**Description:** Start the asynchronous matching flow, returning `matchId` and `PROCESSING` status.

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body (`JobMatchRequest`):**
```json
{
  "resumeVersionId": "resume-version-id-001",
  "query": "Senior Java Developer in San Francisco",
  "topK": 10,
  "filters": {
    "location": "San Francisco"
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

### 2.2 Query Matching Result
**Endpoint:** `GET /api/v1/jobs/match/{matchId}`  
**Description:** Query the current matching status by `matchId`. If completed, returns the ranked job list.

**Response Body (`JobMatchResponse`) - Completed Example:**
```json
{
  "matchId": "match-uuid-1234",
  "status": "COMPLETED",
  "matches": [
    {
      "jobId": "job-001",
      "title": "Senior Java Developer",
      "company": "Tech Corp",
      "matchScore": 0.92,
      "matchFactors": {
        "skillMatch": 0.95,
        "experienceMatch": 0.90,
        "locationMatch": 0.88
      },
      "description": "Looking for an experienced Java developer..."
    }
  ],
  "total": 1,
  "recallTime": 12,
  "rankTime": 45
}
```

### 2.3 Query Matching History
**Endpoint:** `GET /api/v1/jobs/match/history`  
**Description:** Get all historical matching records for the current user (sorted by time descending).

**Response Body (`List<JobMatchHistoryResponse>`):**
```json
[
  {
    "matchId": "match-uuid-1234",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "resumeVersionId": "resume-version-id-001",
    "query": "Senior Java Developer",
    "status": "COMPLETED",
    "matches": [...],
    "total": 5,
    "recallTime": 15,
    "rankTime": 120,
    "modelVersion": "v1.0",
    "createdAt": "2026-04-16T10:00:00",
    "completedAt": "2026-04-16T10:00:02"
  }
]
```

---

## 3. MQ Data Format

### 3.1 Ranking Request (Java -> Python)
**Exchange:** `ai.direct.exchange`  
**Routing Key:** `ai.req.job.rank`  
**Queue:** `ai.queue.job.rank`

```json
{
  "matchId": "match-uuid-1234",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "resumeVersionId": "resume-version-id-001",
  "resumeText": "...",
  "query": "Senior Java Developer",
  "recalledJobIds": ["job-001", "job-002"],
  "jobDetails": {
    "job-001": {
      "title": "Senior Java Developer",
      "company": "Tech Corp",
      "description": "..."
    }
  }
}
```

### 3.2 Ranking Response (Python -> Java)
**Exchange:** `ai.direct.exchange`  
**Routing Key:** `backend.res.job.rank`  
**Queue:** `backend.queue.job.rank`

```json
{
  "matchId": "match-uuid-1234",
  "status": "COMPLETED",
  "rankTimeMs": 150,
  "rankedResults": [
    {
      "jobId": "job-001",
      "title": "Senior Java Developer",
      "company": "Tech Corp",
      "matchScore": 0.92,
      "matchFactors": {
        "skillMatch": 0.95,
        "experienceMatch": 0.90,
        "locationMatch": 0.88
      },
      "description": "Looking for an experienced Java developer..."
    }
  ]
}
```

---

## 4. Error Codes

| HTTP Status | Business Code | Description |
|-------------|------------|------|
| 400 | 400 | Resume vector does not exist or embedding is null |
| 400 | 400 | Match result not found (matchId does not exist) |
| 500 | 500 | MQ send failure or database exception |
