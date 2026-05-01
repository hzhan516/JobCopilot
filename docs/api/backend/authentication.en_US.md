<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](authentication.en_US.md) | [简体中文](authentication.zh-Hans-CN.md) | [繁體中文](authentication.zh-Hant-TW.md)

# Authentication API

> User registration, login, and token management interfaces

---

## Table of Contents

1. [Email Registration](#1-email-registration)
2. [Email Login](#2-email-login)
3. [Google Login](#3-google-login)

---

## 1. Email Registration

### Basic Information

| Item | Value |
|------|-------|
| **Interface Name** | Email Registration |
| **Interface Path** | `POST /api/v1/auth/register/email` |
| **Authentication Required** | No |

### Request Structure

#### Request Body

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `email` | String | Yes | Email format | User email address |
| `password` | String | Yes | 6-32 chars | User password |

#### Request Example

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

### Response Structure

#### Success Response (201)

| Field | Type | Description |
|-------|------|-------------|
| `userId` | String (UUID) | User unique identifier |
| `email` | String | User email |
| `accessToken` | String | Access token (valid for 24 hours) |
| `refreshToken` | String | Refresh token (valid for 7 days) |
| `expiresIn` | Long | accessToken validity period (seconds) |

#### Response Example

```json
{
  "code": 201,
  "message": "Success",
  "data": {
    "userId": "d71774e0-e238-4191-b71c-33478e44b4b6",
    "email": "user@example.com",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkNzE3NzRlMC1lMjM4LTQxOTEtYjcxYy0zMzQ3OGU0NGI0YjYiLCJpYXQiOjE3NzU1NDA5MTksImV4cCI6MTc3NTYyNzMxOX0.bb5uFoqCH9qK6m9KuV2o-3VehLaJvBWlRnMGIPQ2cyE",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkNzE3NzRlMC1lMjM4LTQxOTEtYjcxYy0zMzQ3OGU0NGI0YjYiLCJpYXQiOjE3NzU1NDA5MjAsImV4cCI6MTc3NjE0NTcyMH0.qe1vrQDzJV26lRLf3vFpifBmBxnIl3kw145HwwO0oYU",
    "expiresIn": 86400
  }
}
```

### Error Responses

#### 400 - Parameter Validation Error

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

#### 409 - Email Already Exists

```json
{
  "code": 409,
  "message": "Email already exists",
  "data": null
}
```

### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/auth/register/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "123456"
  }'
```

---

## 2. Email Login

### Basic Information

| Item | Value |
|------|-------|
| **Interface Name** | Email Login |
| **Interface Path** | `POST /api/v1/auth/login/email` |
| **Authentication Required** | No |

### Request Structure

#### Request Body

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `email` | String | Yes | Email format | User email address |
| `password` | String | Yes | Non-empty | User password |

#### Request Example

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

### Response Structure

#### Success Response (200)

| Field | Type | Description |
|-------|------|-------------|
| `userId` | String (UUID) | User unique identifier |
| `email` | String | User email |
| `accessToken` | String | Access token (valid for 24 hours) |
| `refreshToken` | String | Refresh token (valid for 7 days) |
| `expiresIn` | Long | accessToken validity period (seconds) |

#### Response Example

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "userId": "d71774e0-e238-4191-b71c-33478e44b4b6",
    "email": "user@example.com",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkNzE3NzRlMC1lMjM4LTQxOTEtYjcxYy0zMzQ3OGU0NGI0YjYiLCJpYXQiOjE3NzU1NDA5MjAsImV4cCI6MTc3NTYyNzMyMH0.8G3u4TRpCoDYM5pqe8JaGpnGBvq1Ximf0mzFV48X8M4",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkNzE3NzRlMC1lMjM4LTQxOTEtYjcxYy0zMzQ3OGU0NGI0YjYiLCJpYXQiOjE3NzU1NDA5MjAsImV4cCI6MTc3NjE0NTcyMH0.qe1vrQDzJV26lRLf3vFpifBmBxnIl3kw145HwwO0oYU",
    "expiresIn": 86400
  }
}
```

### Error Responses

#### 401 - Invalid Email or Password

```json
{
  "code": 401,
  "message": "Invalid credentials",
  "data": null
}
```

#### 400 - Parameter Validation Error

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": {
    "email": "Email is required",
    "password": "Password is required"
  }
}
```

### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "123456"
  }'
```

---

## 3. Google Login

### Basic Information

| Item | Value |
|------|-------|
| **Interface Name** | Google Login |
| **Interface Path** | `POST /api/v1/auth/login/google` |
| **Authentication Required** | No |

### Request Structure

#### Request Body

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `idToken` | String | Yes | Non-empty | Google ID Token |

#### Request Example

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIs..."
}
```

### Response Structure

#### Success Response (200)

Same response format as email login (first login will automatically register and create a user).

#### Response Example

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "userId": "d71774e0-e238-4191-b71c-33478e44b4b6",
    "email": "user@gmail.com",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400
  }
}
```

### Error Responses

#### 401 - Invalid ID Token or Verification Failed

```json
{
  "code": 401,
  "message": "Invalid credentials",
  "data": null
}
```

### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/auth/login/google \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "eyJhbGciOiJSUzI1NiIs..."
  }'
```

---

## Token Usage

### Access Token

- **Validity**: 24 hours (86400 seconds)
- **Purpose**: Access APIs requiring authentication
- **Transmission**: HTTP Header `Authorization: Bearer <token>`

### Refresh Token

- **Validity**: 7 days (604800 seconds)
- **Purpose**: Obtain a new token pair after Access Token expiration
- **Storage Recommendation**: Store securely for token refresh mechanism

### Token Structure (JWT)

```
Header.Payload.Signature
```

Payload contains the following fields:

| Field | Description |
|-------|-------------|
| `sub` | User ID |
| `iat` | Issued at |
| `exp` | Expiration time |

---

## Interface Summary

| Interface | Method | Path | Authentication |
|-----------|--------|------|----------------|
| Email Registration | POST | `/api/v1/auth/register/email` | No |
| Email Login | POST | `/api/v1/auth/login/email` | No |
| Google Login | POST | `/api/v1/auth/login/google` | No |
