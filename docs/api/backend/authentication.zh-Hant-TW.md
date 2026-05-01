<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](authentication.en_US.md) | [简体中文](authentication.zh-Hans-CN.md) | [繁體中文](authentication.zh-Hant-TW.md)

# 認證 API

> 使用者註冊、登入和 Token 管理相關介面

---

## 目錄

1. [郵箱註冊](#1-郵箱註冊)
2. [郵箱登入](#2-郵箱登入)
3. [Google 登入](#3-google-登入)

---

## 1. 郵箱註冊

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 郵箱註冊 |
| **介面路徑** | `POST /api/v1/auth/register/email` |
| **是否需要認證** | 否 |

### 請求結構

#### Request Body

| 欄位 | 類型 | 必填 | 限制 | 說明 |
|------|------|------|------|------|
| `email` | String | 是 | 郵箱格式 | 使用者郵箱地址 |
| `password` | String | 是 | 6-32位 | 使用者密碼 |

#### 請求範例

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

### 回應結構

#### 成功回應 (201)

| 欄位 | 類型 | 說明 |
|------|------|------|
| `userId` | String (UUID) | 使用者唯一識別 |
| `email` | String | 使用者郵箱 |
| `accessToken` | String | 存取權杖（有效期24小時） |
| `refreshToken` | String | 重新整理權杖（有效期7天） |
| `expiresIn` | Long | accessToken 有效期（秒） |

#### 回應範例

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

### 錯誤回應

#### 400 - 參數驗證錯誤

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

#### 409 - 郵箱已存在

```json
{
  "code": 409,
  "message": "Email already exists",
  "data": null
}
```

### 呼叫範例 (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/auth/register/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "123456"
  }'
```

---

## 2. 郵箱登入

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 郵箱登入 |
| **介面路徑** | `POST /api/v1/auth/login/email` |
| **是否需要認證** | 否 |

### 請求結構

#### Request Body

| 欄位 | 類型 | 必填 | 限制 | 說明 |
|------|------|------|------|------|
| `email` | String | 是 | 郵箱格式 | 使用者郵箱地址 |
| `password` | String | 是 | 非空 | 使用者密碼 |

#### 請求範例

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

### 回應結構

#### 成功回應 (200)

| 欄位 | 類型 | 說明 |
|------|------|------|
| `userId` | String (UUID) | 使用者唯一識別 |
| `email` | String | 使用者郵箱 |
| `accessToken` | String | 存取權杖（有效期24小時） |
| `refreshToken` | String | 重新整理權杖（有效期7天） |
| `expiresIn` | Long | accessToken 有效期（秒） |

#### 回應範例

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

### 錯誤回應

#### 401 - 郵箱或密碼錯誤

```json
{
  "code": 401,
  "message": "Invalid credentials",
  "data": null
}
```

#### 400 - 參數驗證錯誤

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

### 呼叫範例 (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "123456"
  }'
```

---

## 3. Google 登入

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | Google 登入 |
| **介面路徑** | `POST /api/v1/auth/login/google` |
| **是否需要認證** | 否 |

### 請求結構

#### Request Body

| 欄位 | 類型 | 必填 | 限制 | 說明 |
|------|------|------|------|------|
| `idToken` | String | 是 | 非空 | Google ID Token |

#### 請求範例

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIs..."
}
```

### 回應結構

#### 成功回應 (200)

與郵箱登入回應格式相同（首次登入會自動註冊並建立使用者）。

#### 回應範例

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

### 錯誤回應

#### 401 - ID Token 無效或驗證失敗

```json
{
  "code": 401,
  "message": "Invalid credentials",
  "data": null
}
```

### 呼叫範例 (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login/google \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "eyJhbGciOiJSUzI1NiIs..."
  }'
```

---

## Token 使用說明

### Access Token

- **有效期**: 24小時 (86400秒)
- **用途**: 存取需要認證的 API 介面
- **傳遞方式**: HTTP Header `Authorization: Bearer <token>`

### Refresh Token

- **有效期**: 7天 (604800秒)
- **用途**: Access Token 過期後取得新的 Token 對
- **儲存建議**: 安全儲存，用於 Token 重新整理機制

### Token 結構 (JWT)

```
Header.Payload.Signature
```

Payload 包含以下欄位：

| 欄位 | 說明 |
|------|------|
| `sub` | 使用者ID |
| `iat` | 簽發時間 |
| `exp` | 過期時間 |

---

## 介面彙總

| 介面 | 方法 | 路徑 | 認證 |
|------|------|------|------|
| 郵箱註冊 | POST | `/api/v1/auth/register/email` | 否 |
| 郵箱登入 | POST | `/api/v1/auth/login/email` | 否 |
| Google 登入 | POST | `/api/v1/auth/login/google` | 否 |
