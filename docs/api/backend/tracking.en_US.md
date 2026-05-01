<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](tracking.en_US.md) | [简体中文](tracking.zh-Hans-CN.md) | [繁體中文](tracking.zh-Hant-TW.md)

# Application Tracking API Documentation

This document describes the complete API for the application tracking module, including status transition control, event history, and statistics features.

---

## 1. Status Transition Description

```
PENDING -> APPLIED
APPLIED -> SCREENING, REJECTED
SCREENING -> INTERVIEWING, REJECTED
INTERVIEWING -> OFFER, REJECTED
OFFER -> ACCEPTED, REJECTED
```

Any illegal status transition will return `400 Bad Request` with the message `"Status transition from X to Y is not allowed"`.

---

## 2. Client-Backend Interaction Interfaces

### 2.1 Create Tracking Record
**Endpoint:** `POST /api/v1/trackings`  
**Description:** Create a new job application tracking record with initial status `PENDING`.

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
  "updatedAt": "2026-04-16T10:00:00",
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

### 2.3 Get Tracking Detail
**Endpoint:** `GET /api/v1/trackings/{id}`  
**Description:** Get details by tracking ID.

### 2.4 Update Tracking Record (with Status Transition)
**Endpoint:** `PUT /api/v1/trackings/{id}`  
**Description:** Update the tracking record's notes and status. Status changes will trigger a `TrackingEvent` record.

**Request Body (`UpdateTrackingRequest`):**
```json
{
  "status": "INTERVIEWING",
  "notes": "Technical interview scheduled"
}
```

### 2.5 Delete Tracking Record
**Endpoint:** `DELETE /api/v1/trackings/{id}`  
**Description:** Delete the specified tracking record.

**Response:** `200 OK` (body is null)

### 2.6 Get Statistics
**Endpoint:** `GET /api/v1/trackings/stats`  
**Description:** Get statistical distribution and success rate of all tracking records for the current user.

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
| 400 | 400 | Illegal status transition (TrackingException) |
| 400 | 400 | Tracking not found (ID does not exist or does not belong to current user) |
| 500 | 500 | Database exception |

---

## Notes

### Frontend API Call Path Inconsistency

The paths called in frontend `trackingService.ts` are inconsistent with the backend `TrackingController` definitions:

| Feature | Frontend Path | Backend Path |
|------|----------|----------|
| Get applications | `GET /v1/tracking/applications` | `GET /v1/trackings` |
| Get application detail | `GET /v1/tracking/applications/{id}` | `GET /v1/trackings/{id}` |
| Create application | `POST /v1/tracking/applications` | `POST /v1/trackings` |
| Update application status | `PATCH /v1/tracking/applications/{id}/status` | `PUT /v1/trackings/{id}` |
| Delete application | `DELETE /v1/tracking/applications/{id}` | `DELETE /v1/trackings/{id}` |

To align frontend and backend, please synchronize modifications to either frontend `trackingService.ts` or backend `TrackingController` interface design.
