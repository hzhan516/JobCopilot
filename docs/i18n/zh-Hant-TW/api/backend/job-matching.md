<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/job-matching.md) | [简体中文](../../../zh-Hans-CN/api/backend/job-matching.md) | [繁體中文](job-matching.md)

# 職位智慧匹配 (Job Matching) API 文件

本文件描述職位智慧匹配模組的非同步召回 + 精排流程。Java 後端透過本地 PGVector 完成召回，再透過 RabbitMQ 將召回結果非同步發送給 Python AI 服務進行精排，最終持久化到資料庫。

---

## 1. 非同步流程說明

```
使用者請求 -> JobController.matchJobs()
    -> MatchingFacade.matchJobs()
        -> MatchingApplicationService.startJobMatch()
            1. 建立 PROCESSING 狀態記錄 (job_match_results)
            2. 查詢 resume_vectors 取得履歷向量
            3. VectorSearchPort 本地召回 topK 職位 (job_vectors)
            4. 發送 JobRankCommand 到 ai.req.job.rank
            5. 立即回傳 JobMatchResponse(matchId, status=PROCESSING)

Python AI 服務 -> 消費 ai.req.job.rank -> 精排 -> 發送結果到 backend.res.job.rank

JobRankResultListener -> 消費 backend.res.job.rank
    -> MatchingFacade.saveJobRankResult()
        -> MatchingApplicationService.saveMatchResult()
            -> 更新 job_match_results 為 COMPLETED + rankedResults
```

---

## 2. 客戶端與後端互動介面

### 2.1 啟動非同步職位匹配
**端點:** `POST /api/v1/jobs/match`  
**說明:** 啟動非同步匹配流程，回傳 `matchId` 和 `PROCESSING` 狀態。

**請求標頭:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**請求主體 (`JobMatchRequest`):**
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

**回應主體 (`JobMatchResponse`):**
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

### 2.2 查詢匹配結果
**端點:** `GET /api/v1/jobs/match/{matchId}`  
**說明:** 根據 `matchId` 查詢目前匹配狀態。若已完成，回傳精排後的職位列表。

**回應主體 (`JobMatchResponse`) - 已完成範例:**
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

### 2.3 查詢匹配歷史
**端點:** `GET /api/v1/jobs/match/history`  
**說明:** 取得目前使用者的所有歷史匹配記錄（按時間倒序）。

**回應主體 (`List<JobMatchHistoryResponse>`):**
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

## 3. MQ 資料格式

### 3.1 Ranking 請求 (Java -> Python)
**交換器:** `ai.direct.exchange`  
**路由鍵:** `ai.req.job.rank`  
**佇列:** `ai.queue.job.rank`

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

### 3.2 Ranking 回應 (Python -> Java)
**交換器:** `ai.direct.exchange`  
**路由鍵:** `backend.res.job.rank`  
**佇列:** `backend.queue.job.rank`

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

## 4. 錯誤碼

| HTTP 狀態碼 | 業務錯誤碼 | 說明 |
|-------------|------------|------|
| 400 | 400 | 履歷向量不存在或 embedding 為 null |
| 400 | 400 | Match result not found (matchId 不存在) |
| 500 | 500 | MQ 發送失敗或資料庫異常 |
