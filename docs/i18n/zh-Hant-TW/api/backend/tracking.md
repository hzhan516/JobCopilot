<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/tracking.md) | [简体中文](../../../zh-Hans-CN/api/backend/tracking.md) | [繁體中文](tracking.md)

# 求職申請追蹤 (Application Tracking) API 文件

本文件描述求職申請追蹤模組的完整 API，包含狀態流轉控制、事件歷史和統計功能。

---

## 1. 狀態流轉說明

```
PENDING -> APPLIED
APPLIED -> SCREENING, REJECTED
SCREENING -> INTERVIEWING, REJECTED
INTERVIEWING -> OFFER, REJECTED
OFFER -> ACCEPTED, REJECTED
```

任何非法的狀態流轉都會回傳 `400 Bad Request` 並提示 `"Status transition from X to Y is not allowed"`。

---

## 2. 客戶端與後端互動介面

### 2.1 建立追蹤記錄
**端點:** `POST /api/v1/trackings`  
**說明:** 建立一條新的求職申請追蹤記錄，初始狀態為 `PENDING`。

**請求標頭:**
```http
Authorization: Bearer <user-jwt-token>
Content-Type: application/json
```

**請求主體 (`CreateTrackingRequest`):**
```json
{
  "jobId": "job-001",
  "companyName": "Tech Corp",
  "jobTitle": "Senior Java Developer",
  "appliedAt": "2026-04-10",
  "notes": "Referral from friend"
}
```

**回應主體 (`TrackingResponse`):**
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
  "updatedAt": "2026-04-16T10:00:00",
  "notes": "Referral from friend",
  "events": []
}
```

### 2.2 取得追蹤列表
**端點:** `GET /api/v1/trackings?status=INTERVIEWING`  
**說明:** 取得目前使用者的所有追蹤記錄，支援按狀態篩選。

**回應主體 (`List<TrackingResponse>`):**
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
    "updatedAt": "2026-04-16T10:00:00",
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

### 2.3 取得追蹤詳情
**端點:** `GET /api/v1/trackings/{id}`  
**說明:** 根據追蹤 ID 取得詳情。

### 2.4 更新追蹤記錄（含狀態流轉）
**端點:** `PUT /api/v1/trackings/{id}`  
**說明:** 更新追蹤記錄的備註和狀態。狀態變更會觸發 `TrackingEvent` 記錄。

**請求主體 (`UpdateTrackingRequest`):**
```json
{
  "status": "INTERVIEWING",
  "notes": "Technical interview scheduled"
}
```

### 2.5 刪除追蹤記錄
**端點:** `DELETE /api/v1/trackings/{id}`  
**說明:** 刪除指定的追蹤記錄。

**回應:** `200 OK` (body 為 null)

### 2.6 取得統計資訊
**端點:** `GET /api/v1/trackings/stats`  
**說明:** 取得目前使用者所有追蹤記錄的統計分布和成功率。

**回應主體 (`TrackingStatsResponse`):**
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

## 3. 錯誤碼

| HTTP 狀態碼 | 業務錯誤碼 | 說明 |
|-------------|------------|------|
| 400 | 400 | 非法狀態流轉 (TrackingException) |
| 400 | 400 | Tracking not found (ID 不存在或不屬於目前使用者) |
| 500 | 500 | 資料庫異常 |

---
