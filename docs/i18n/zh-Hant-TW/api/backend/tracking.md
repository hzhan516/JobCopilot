<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/tracking.md) | [简体中文](../../../zh-Hans-CN/api/backend/tracking.md) | [繁體中文](tracking.md)

# 求職申請追蹤 (Application Tracking) API 文件

本文件描述求職申請追蹤模組的完整 API，包含狀態更新、事件歷史、投遞日期處理和統計功能。

---

## 1. 狀態處理說明

支援的狀態包含 `PENDING`、`APPLIED`、`SCREENING`、`INTERVIEWING`、`OFFER`、`ACCEPTED`、`REJECTED` 和 `WITHDRAWN`。

- 建立請求可以包含可選的初始 `status`。如果未提供，後端使用 `PENDING`。
- 更新記錄時可以設定任意合法狀態值。當新狀態與目前狀態不同時，後端會追加一條 `TrackingEvent`。
- 如果記錄建立或更新為 `APPLIED` 且沒有 `appliedAt`，後端會將 `appliedAt` 設為目前日期。
- `appliedAt` 不能是未來日期。

---

## 2. 客戶端與後端互動介面

### 2.1 建立追蹤記錄
**端點:** `POST /api/v1/trackings`  
**說明:** 建立一條新的求職申請追蹤記錄。初始狀態可選，預設值為 `PENDING`。

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
  "status": "APPLIED",
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
  "status": "APPLIED",
  "appliedAt": "2026-04-10",
  "createdAt": "2026-04-16T10:00:00Z",
  "updatedAt": "2026-04-16T10:00:00Z",
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

### 2.3 取得追蹤詳情
**端點:** `GET /api/v1/trackings/{id}`  
**說明:** 根據追蹤 ID 取得詳情。

### 2.4 更新追蹤記錄（含狀態更新）
**端點:** `PUT /api/v1/trackings/{id}`  
**說明:** 更新追蹤記錄的公司、職位名稱、投遞日期、備註和狀態。當提交的狀態與目前狀態不同時，後端會記錄一條 `TrackingEvent`。

**請求主體 (`UpdateTrackingRequest`):**
```json
{
  "companyName": "Tech Corp",
  "jobTitle": "Senior Java Developer",
  "status": "INTERVIEWING",
  "appliedAt": "2026-04-10",
  "notes": "Technical interview scheduled"
}
```

### 2.5 刪除追蹤記錄
**端點:** `DELETE /api/v1/trackings/{id}`  
**說明:** 刪除指定的追蹤記錄。

**回應:** `200 OK` (body 為 null)

### 2.6 取得統計資訊
**端點:** `GET /api/v1/trackings/stats`  
**說明:** 取得目前使用者所有追蹤記錄的統計分布和成功率。`appliedCount` 包含 `APPLIED`、`SCREENING` 和 `INTERVIEWING`；`offerCount` 包含 `OFFER` 和 `ACCEPTED`；`successRate` 為 `offerCount / totalApplications * 100`。

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
| 400 | 400 | 請求校驗或參數格式錯誤 |
| 500 | 500 | 應用或資料庫異常 |

---
