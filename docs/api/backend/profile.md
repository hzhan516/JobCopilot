<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](profile.md) | [简体中文](../../i18n/zh-Hans-CN/api/backend/profile.md) | [繁體中文](../../i18n/zh-Hant-TW/api/backend/profile.md)

# Profile API

> User profile management endpoints.

---

## Base URL

All endpoints are prefixed with `/api` (via `server.servlet.context-path`).

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/profile` | `GET` | Get current user profile |
| `/api/v1/profile` | `PUT` | Update profile fields |
| `/api/v1/profile/avatar` | `PUT` | Update avatar URL only |

---

## Authentication

All endpoints require a valid JWT access token in the `Authorization: Bearer <token>` header.
The user identity is resolved from the token via `@CurrentUser`.

---

## GET /api/v1/profile

Retrieve the profile of the currently authenticated user.

### Request

- **Headers**: `Authorization: Bearer <accessToken>`
- **Body**: none

### Response

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "Alice Zhang",
    "avatarUrl": "https://example.com/avatar.png",
    "phone": "+1-555-0199",
    "targetPosition": "Software Engineer",
    "preferredLocation": "San Francisco, CA",
    "createdAt": "2025-04-01T10:30:00",
    "updatedAt": "2025-04-28T14:20:00"
  }
}
```

### Error Responses

| HTTP Status | Code | Description |
|-------------|------|-------------|
| `401` | `401` | Missing or invalid token |
| `404` | `404` | Profile not found for the authenticated user |

---

## PUT /api/v1/profile

Update the current user's profile. All fields are optional — only provided non-null values are applied.

### Request

- **Headers**: `Authorization: Bearer <accessToken>`, `Content-Type: application/json`
- **Body**:

```json
{
  "fullName": "Alice Zhang",
  "phone": "+1-555-0199",
  "targetPosition": "Senior Software Engineer",
  "preferredLocation": "Remote"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `fullName` | `string` | No | User's full name |
| `phone` | `string` | No | Phone number |
| `targetPosition` | `string` | No | Desired job title |
| `preferredLocation` | `string` | No | Preferred work location |

### Response

Returns the updated profile with the same schema as `GET /api/v1/profile`.

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "Alice Zhang",
    "avatarUrl": "https://example.com/avatar.png",
    "phone": "+1-555-0199",
    "targetPosition": "Senior Software Engineer",
    "preferredLocation": "Remote",
    "createdAt": "2025-04-01T10:30:00",
    "updatedAt": "2025-04-30T22:05:00"
  }
}
```

### Error Responses

| HTTP Status | Code | Description |
|-------------|------|-------------|
| `401` | `401` | Missing or invalid token |
| `404` | `404` | Profile not found |

---

## PUT /api/v1/profile/avatar

Update only the avatar URL. This dedicated endpoint simplifies front-end avatar upload flows (upload to storage first, then call this endpoint to persist the URL).

### Request

- **Headers**: `Authorization: Bearer <accessToken>`, `Content-Type: application/json`
- **Body**:

```json
{
  "avatarUrl": "https://storage.example.com/avatars/new-avatar.png"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `avatarUrl` | `string` | Yes | Public URL of the uploaded avatar image |

### Response

Returns the updated profile with the same schema as `GET /api/v1/profile`.

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "Alice Zhang",
    "avatarUrl": "https://storage.example.com/avatars/new-avatar.png",
    "phone": "+1-555-0199",
    "targetPosition": "Senior Software Engineer",
    "preferredLocation": "Remote",
    "createdAt": "2025-04-01T10:30:00",
    "updatedAt": "2025-04-30T22:10:00"
  }
}
```

### Error Responses

| HTTP Status | Code | Description |
|-------------|------|-------------|
| `401` | `401` | Missing or invalid token |
| `404` | `404` | Profile not found |

---

## Data Model

### ProfileResponse

| Field | Type | Description |
|-------|------|-------------|
| `userId` | `string (UUID)` | Unique user identifier |
| `fullName` | `string` | User's full name |
| `avatarUrl` | `string` | URL to the avatar image |
| `phone` | `string` | Phone number |
| `targetPosition` | `string` | Target job position |
| `preferredLocation` | `string` | Preferred work location |
| `createdAt` | `string (ISO-8601)` | Profile creation timestamp |
| `updatedAt` | `string (ISO-8601)` | Last update timestamp |

---

## Architecture

The Profile module follows **Hexagonal / DDD** layering:

```
Trigger (ProfileController)
    └── calls API (ProfileFacade)
        └── implemented by App (ProfileFacadeImpl)
            └── calls App Service (ProfileApplicationService)
                └── calls Domain Port (UserProfileRepository)
                    └── implemented by Infrastructure (UserProfileRepositoryImpl)
```

- **Domain entity** `UserProfile` encapsulates update logic (`updateProfile`, `updateAvatar`).
- **App Service** handles transaction boundaries and orchestration.
- **Facade** converts API DTOs ↔ Domain entities.

---

## i18n

- [简体中文 (Simplified Chinese)](../../i18n/zh-Hans-CN/api/backend/profile.md)
- [繁體中文 (Traditional Chinese)](../../i18n/zh-Hant-TW/api/backend/profile.md)
