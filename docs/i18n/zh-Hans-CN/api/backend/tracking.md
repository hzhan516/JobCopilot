<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/tracking.md) | [简体中文](tracking.md) | [繁體中文](../../../zh-Hant-TW/api/backend/tracking.md)

# 求职申请跟踪 (Application Tracking) API 文档

本文档描述求职申请跟踪模块的完整 API，包含状态流转控制、事件历史和统计功能。

---

## 1. 状态流转说明

```
PENDING -> APPLIED
APPLIED -> SCREENING, REJECTED
SCREENING -> INTERVIEWING, REJECTED
INTERVIEWING -> OFFER, REJECTED
OFFER -> ACCEPTED, REJECTED
```

任何非法的状态流转都会返回 `400 Bad Request` 并提示 `"Status transition from X to Y is not allowed"`。

---

## 2. 客户端与后端交互接口

### 2.1 创建跟踪记录
**Endpoint:** `POST /api/v1/trackings`  
**描述:** 创建一条新的求职申请跟踪记录，初始状态为 `PENDING`。

**Request Header:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**Request Body (`CreateTrackingRequest`):**
```json
{
  "jobId": "job-001",
  "companyName": "Tech Corp",
  "jobTitle": "Senior Java Developer",
  "appliedAt": "2026-04-10",
  "notes": "Referral from friend"
}
```

**Response Body (`TrackingResponse`):**
```json
{
  "trackingId": "track-uuid-1234",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "job": {
    "id": "job-001",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "originalUrl": "https://example.com/job/001",
    "status": "COMPLETED",
    "parsedContent": {
      "title": "Senior Java Developer",
      "company": "Tech Corp",
      "description": "...",
      "requirements": ["Java", "Spring Boot"]
    },
    "imageCheckEnabled": false,
    "errorMessage": null
  },
  "companyName": "Tech Corp",
  "jobTitle": "Senior Java Developer",
  "status": "PENDING",
  "appliedAt": "2026-04-10",
  "createdAt": "2026-04-16T10:00:00Z",
  "updatedAt": "2026-04-16T10:00:00Z",
  "notes": "Referral from friend",
  "events": []
}
```

### 2.2 获取跟踪列表
**Endpoint:** `GET /api/v1/trackings?status=INTERVIEWING`  
**描述:** 获取当前用户的所有跟踪记录，支持按状态过滤。

**Response Body (`List<TrackingResponse>`):**
```json
[
  {
    "trackingId": "track-uuid-1234",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "job": null,
    "companyName": "Tech Corp",
    "jobTitle": "Senior Java Developer",
    "status": "INTERVIEWING",
    "appliedAt": "2026-04-10",
    "createdAt": "2026-04-16T10:00:00Z",
    "updatedAt": "2026-04-16T10:00:00Z",
    "notes": "First round completed",
    "events": [
      {
        "timestamp": "2026-04-10T08:00:00",
        "fromStatus": "PENDING",
        "toStatus": "APPLIED",
        "note": "Submitted application"
      },
      {
        "timestamp": "2026-04-12T09:00:00",
        "fromStatus": "APPLIED",
        "toStatus": "SCREENING",
        "note": "Passed initial screening"
      },
      {
        "timestamp": "2026-04-16T10:00:00",
        "fromStatus": "SCREENING",
        "toStatus": "INTERVIEWING",
        "note": "Scheduled technical interview"
      }
    ]
  }
]
```

### 2.3 获取跟踪详情
**Endpoint:** `GET /api/v1/trackings/{id}`  
**描述:** 根据跟踪 ID 获取详情。

### 2.4 更新跟踪记录（含状态流转）
**Endpoint:** `PUT /api/v1/trackings/{id}`  
**描述:** 更新跟踪记录的公司、职位名称、投递日期、备注和状态。状态变更会触发 `TrackingEvent` 记录。

**Request Body (`UpdateTrackingRequest`):**
```json
{
  "companyName": "Tech Corp",
  "jobTitle": "Senior Java Developer",
  "status": "INTERVIEWING",
  "appliedAt": "2026-04-10",
  "notes": "Technical interview scheduled"
}
```

### 2.5 删除跟踪记录
**Endpoint:** `DELETE /api/v1/trackings/{id}`  
**描述:** 删除指定的跟踪记录。

**Response:** `200 OK` (body 为 null)

### 2.6 获取统计信息
**Endpoint:** `GET /api/v1/trackings/stats`  
**描述:** 获取当前用户所有跟踪记录的统计分布和成功率。

**Response Body (`TrackingStatsResponse`):**
```json
{
  "totalApplications": 10,
  "pendingCount": 2,
  "appliedCount": 3,
  "interviewingCount": 2,
  "offerCount": 2,
  "rejectedCount": 1,
  "withdrawnCount": 0,
  "successRate": 20.0
}
```

---

## 3. 错误码

| HTTP 状态码 | 业务错误码 | 说明 |
|-------------|------------|------|
| 400 | 400 | 非法状态流转 (TrackingException) |
| 400 | 400 | Tracking not found (ID 不存在或不属于当前用户) |
| 500 | 500 | 数据库异常 |


---
