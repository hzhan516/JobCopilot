<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](tracking.md) | [简体中文](../../i18n/zh-Hans-CN/api/backend/tracking.md) | [繁體中文](../../i18n/zh-Hant-TW/api/backend/tracking.md)

# Application Tracking API Documentation

This document describes the complete API for the application tracking module, including status updates, event history, applied date handling, and statistics features.

---

## 1. Status Handling Description

Supported statuses are `PENDING`, `APPLIED`, `SCREENING`, `INTERVIEWING`, `OFFER`, `ACCEPTED`, `REJECTED`, and `WITHDRAWN`.

- Create requests may include an optional initial `status`. If omitted, the backend uses `PENDING`.
- Updating a record may set any valid status value. When the new status differs from the current status, the backend appends a `TrackingEvent`.
- If a record is created or updated to `APPLIED` without `appliedAt`, the backend sets `appliedAt` to the current date.
- `appliedAt` cannot be a future date.

---

## 2. Client-Backend Interaction Interfaces

### 2.1 Create Tracking Record
**Endpoint:** `POST /api/v1/trackings`  
**Description:** Create a new job application tracking record. The initial status is optional and defaults to `PENDING`.

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

### 2.2 Get Tracking List
**Endpoint:** `GET /api/v1/trackings?status=INTERVIEWING`  
**Description:** Get all tracking records for the current user, with optional status filtering.

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

### 2.3 Get Tracking Detail
**Endpoint:** `GET /api/v1/trackings/{id}`  
**Description:** Get details by tracking ID.

### 2.4 Update Tracking Record (with Status Update)
**Endpoint:** `PUT /api/v1/trackings/{id}`  
**Description:** Update the tracking record's company, job title, applied date, notes, and status. When the submitted status differs from the current status, the backend records a `TrackingEvent`.

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

### 2.5 Delete Tracking Record
**Endpoint:** `DELETE /api/v1/trackings/{id}`  
**Description:** Delete the specified tracking record.

**Response:** `200 OK` (body is null)

### 2.6 Get Statistics
**Endpoint:** `GET /api/v1/trackings/stats`  
**Description:** Get statistical distribution and success rate of all tracking records for the current user. `appliedCount` includes `APPLIED`, `SCREENING`, and `INTERVIEWING`; `offerCount` includes `OFFER` and `ACCEPTED`; `successRate` is `offerCount / totalApplications * 100`.

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

## 3. Error Codes

| HTTP Status | Business Code | Description |
|-------------|------------|------|
| 400 | 400 | Request validation or parameter format error |
| 500 | 500 | Application or database exception |

---
