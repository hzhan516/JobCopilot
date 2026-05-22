<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/tracking.md) | [简体中文](tracking.md) | [繁體中文](../../../zh-Hant-TW/api/backend/tracking.md)

# 求职申请跟踪 (Application Tracking) API 文档

本文档描述求职申请跟踪模块的完整 API，包含状态更新、事件历史、投递日期处理和统计功能。

---

## 1. 状态处理说明

支持的状态包括 `PENDING`、`APPLIED`、`SCREENING`、`INTERVIEWING`、`OFFER`、`ACCEPTED`、`REJECTED` 和 `WITHDRAWN`。

- 创建请求可以包含可选的初始 `status`。如果未提供，后端使用 `PENDING`。
- 更新记录时可以设置任意合法状态值。当新状态与当前状态不同时，后端会追加一条 `TrackingEvent`。
- 如果记录创建或更新为 `APPLIED` 且没有 `appliedAt`，后端会将 `appliedAt` 设置为当前日期。
- `appliedAt` 不能是未来日期。

---

## 2. 客户端与后端交互接口

### 2.1 创建跟踪记录
**Endpoint:** `POST /api/v1/trackings`  
**描述:** 创建一条新的求职申请跟踪记录。初始状态可选，默认值为 `PENDING`。

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
  "status": "APPLIED",
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
  "status": "APPLIED",
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

### 2.4 更新跟踪记录（含状态更新）
**Endpoint:** `PUT /api/v1/trackings/{id}`  
**描述:** 更新跟踪记录的公司、职位名称、投递日期、备注和状态。当提交的状态与当前状态不同时，后端会记录一条 `TrackingEvent`。

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
**描述:** 获取当前用户所有跟踪记录的统计分布和成功率。`appliedCount` 包含 `APPLIED`、`SCREENING` 和 `INTERVIEWING`；`offerCount` 包含 `OFFER` 和 `ACCEPTED`；`successRate` 为 `offerCount / totalApplications * 100`。

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
| 400 | 400 | 请求校验或参数格式错误 |
| 500 | 500 | 应用或数据库异常 |


---
