# 职位智能匹配 (Job Matching) API 文档

本文档描述职位智能匹配模块的异步召回 + 精排流程。Java 后端通过本地 PGVector 完成召回，再通过 RabbitMQ 将召回结果异步发送给 Python AI 服务进行精排，最终持久化到数据库。

---

## 1. 异步流程说明

```
用户请求 -> JobController.matchJobs()
    -> MatchingApplicationService.startJobMatch()
        1. 创建 PROCESSING 状态记录 (job_match_results)
        2. 查询 resume_vectors 获取简历向量
        3. PgVectorSearchService 本地召回 topK 职位 (job_vectors)
        4. 发送 JobRankCommand 到 ai.req.job.rank
        5. 立即返回 JobMatchResponse(matchId, status=PROCESSING)

Python AI 服务 -> 消费 ai.req.job.rank -> 精排 -> 发送结果到 backend.res.job.rank

JobRankResultListener -> 消费 backend.res.job.rank
    -> JobFacade.saveJobRankResult()
        -> MatchingApplicationService.saveMatchResult()
            -> 更新 job_match_results 为 COMPLETED + rankedResults
```

---

## 2. 客户端与后端交互接口

### 2.1 启动异步职位匹配
**Endpoint:** `POST /api/v1/jobs/match`  
**描述:** 启动异步匹配流程，返回 `matchId` 和 `PROCESSING` 状态。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body (`JobMatchRequest`):**
```json
{
  "userId": "resume-version-id-001",
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

### 2.2 查询匹配结果
**Endpoint:** `GET /api/v1/jobs/match/{matchId}`  
**描述:** 根据 `matchId` 查询当前匹配状态。若已完成，返回精排后的职位列表。

**Response Body (`JobMatchResponse`) - 已完成示例:**
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
        "educationMatch": 0.88
      },
      "description": "Looking for an experienced Java developer..."
    }
  ],
  "total": 1,
  "recallTime": 12,
  "rankTime": 45
}
```

### 2.3 查询匹配历史
**Endpoint:** `GET /api/v1/jobs/match/history`  
**描述:** 获取当前用户的所有历史匹配记录（按时间倒序）。

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

## 3. MQ 数据格式

### 3.1 Ranking 请求 (Java -> Python)
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

### 3.2 Ranking 响应 (Python -> Java)
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
        "educationMatch": 0.88
      },
      "description": "Looking for an experienced Java developer..."
    }
  ]
}
```

---

## 4. 错误码

| HTTP 状态码 | 业务错误码 | 说明 |
|-------------|------------|------|
| 400 | 400 | 简历向量不存在或 embedding 为 null |
| 400 | 400 | Match result not found (matchId 不存在) |
| 500 | 500 | MQ 发送失败或数据库异常 |
