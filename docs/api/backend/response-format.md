<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](response-format.md) | [简体中文](../../i18n/zh-Hans-CN/api/backend/response-format.md) | [繁體中文](../../i18n/zh-Hant-TW/api/backend/response-format.md)

# Response Format Specification

> Unified API response format description for all interfaces

---

## Overview

The JobCopilot API adopts a unified response format. Regardless of whether the request succeeds or fails, the following structure is returned:

```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

---

## Response Structure

### Field Description

| Field | Type | Required | Description |
|------|------|------|------|
| `code` | Integer | Yes | Business status code |
| `message` | String | Yes | Prompt message |
| `data` | Object | No | Business data; may be null on failure |

### Status Codes (code)

| Status Code | Meaning | Description |
|--------|------|------|
| 200 | Success | Request processed successfully |
| 400 | Bad Request | Parameter validation failed or business logic error |
| 401 | Unauthorized | Token missing or invalid |
| 403 | Forbidden | No permission to access this resource |
| 404 | Not Found | Requested resource does not exist |
| 500 | Server Error | Internal server error |

---

## Success Response Examples

### Response with Data

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "userId": "d71774e0-e238-4191-b71c-33478e44b4b6",
    "email": "user@example.com",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400
  }
}
```

### Success Response with Message Only

```json
{
  "code": 200,
  "message": "Operation completed successfully",
  "data": null
}
```

---

## Error Response Examples

### Parameter Validation Error

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": {
    "email": "Email is required",
    "password": "Password must be between 6 and 32 characters"
  }
}
```

### Business Logic Error

```json
{
  "code": 400,
  "message": "Email already exists",
  "data": null
}
```

### Authentication Error

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

### Permission Error

```json
{
  "code": 403,
  "message": "Access denied",
  "data": null
}
```

### Resource Not Found

```json
{
  "code": 404,
  "message": "User not found",
  "data": null
}
```

### Server Error

```json
{
  "code": 500,
  "message": "System busy, please try again later",
  "data": null
}
```

---

## Paginated Response Format

For list query interfaces, the response data contains pagination information:

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "list": [
      { ... },
      { ... }
    ],
    "page": 0,
    "size": 10,
    "total": 100,
    "totalPages": 10
  }
}
```

### Pagination Field Description

| Field | Type | Description |
|------|------|------|
| `list` | Array | Data list |
| `page` | Integer | Current page number (starting from 0, Spring Data default) |
| `size` | Integer | Items per page |
| `total` | Long | Total record count |
| `totalPages` | Integer | Total page count |

---

## Time Format

All time fields use the ISO 8601 format uniformly:

```
2026-04-06T22:45:00.000+00:00
```

---

## Error Handling Recommendations

### Frontend Processing Flow

1. **Check HTTP Status Code**
   - 2xx: Continue processing response body
   - 4xx/5xx: Enter error handling

2. **Parse Response Body**
   - Check `code` field
   - code = 200: Process `data`
   - code != 200: Display `message` error information

3. **Special Handling**
   - code = 401: Token expired, use Refresh Token to refresh or redirect to login page
   - code = 403: Prompt user that they have no permission
   - code = 500: Prompt that server is busy, retry later

### Sample Code (JavaScript)

```javascript
async function apiRequest(url, options) {
  try {
    const response = await fetch(url, options);
    const result = await response.json();
    
    if (result.code === 200) {
      return result.data;
    } else if (result.code === 401) {
      // Token expired, refresh or redirect to login
      window.location.href = '/login';
    } else {
      // Display error message
      alert(result.message);
    }
  } catch (error) {
    alert('Network error');
  }
}
```
