<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/authentication.md) | [简体中文](../../../zh-Hans-CN/api/backend/authentication.md) | [繁體中文](authentication.md)

# 認證 API

> 使用者註冊、登入和 Token 管理相關介面

---

## 目錄

1. [郵箱註冊](#1-郵箱註冊)
2. [郵箱登入](#2-郵箱登入)
3. [Google 登入](#3-google-登入)
4. [傳送驗證碼](#4-傳送驗證碼)
5. [查詢郵箱驗證開關](#5-查詢郵箱驗證開關)
6. [取得滑動驗證碼挑戰](#6-取得滑動驗證碼挑戰)
7. [驗證滑動驗證碼](#7-驗證滑動驗證碼)

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
| `verificationCode` | String | 否 | 6位數字 | 開啟郵箱驗證時必填 |
| `captchaToken` | String | 是 | 非空 | 滑動驗證碼驗證 token（從 `/v1/auth/captcha/verify` 獲取） |

#### 請求範例

```json
{
  "email": "user@example.com",
  "password": "123456",
  "verificationCode": "123456",
  "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
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

#### 400 - 驗證碼無效或已過期

```json
{
  "code": 400,
  "message": "Invalid or expired verification code",
  "data": null
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
    "password": "123456",
    "verificationCode": "123456",
    "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
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
| `captchaToken` | String | 是 | 非空 | 滑動驗證碼驗證 token |

#### 請求範例

```json
{
  "email": "user@example.com",
  "password": "123456",
  "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
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
    "password": "123456",
    "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
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
| `captchaToken` | String | 是 | 非空 | 滑動驗證碼驗證 token |

#### 請求範例

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIs...",
  "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
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
    "idToken": "eyJhbGciOiJSUzI1NiIs...",
    "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

---

## 4. 傳送驗證碼

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 傳送驗證碼 |
| **介面路徑** | `POST /api/v1/auth/send-verification-code` |
| **是否需要認證** | 否 |

### 請求結構

#### Request Body

| 欄位 | 類型 | 必填 | 限制 | 說明 |
|------|------|------|------|------|
| `email` | String | 是 | 郵箱格式 | 目標郵箱地址 |
| `captchaToken` | String | 是 | 非空 | 滑動驗證碼驗證 token（預檢，不消耗） |

#### 請求範例

```json
{
  "email": "user@example.com",
  "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 回應結構

#### 成功回應 (200)

```json
{
  "code": 200,
  "message": "驗證碼已傳送，請查收郵件",
  "data": null
}
```

### 錯誤回應

#### 400 - 郵箱格式不合法

```json
{
  "code": 400,
  "message": "請求參數無效",
  "data": null
}
```

#### 409 - 郵箱已被註冊

```json
{
  "code": 409,
  "message": "該郵箱已被註冊",
  "data": null
}
```

#### 429 - 冷卻中

```json
{
  "code": 429,
  "message": "請稍後再重新取得驗證碼",
  "data": null
}
```

### cURL 範例

```bash
curl -X POST http://localhost:8080/api/v1/auth/send-verification-code \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

---

## 5. 查詢郵箱驗證開關

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 查詢郵箱驗證開關 |
| **介面路徑** | `GET /api/v1/auth/email-verification-enabled` |
| **是否需要認證** | 否 |

### 回應結構

#### 成功回應 (200)

| 欄位 | 類型 | 說明 |
|------|------|------|
| `data` | Boolean | `true` 表示郵箱驗證已開啟，`false` 表示已關閉 |

#### 回應範例

```json
{
  "code": 200,
  "message": "成功",
  "data": true
}
```

### cURL 範例

```bash
curl -X GET http://localhost:8080/api/v1/auth/email-verification-enabled
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

## 6. 取得滑動驗證碼挑戰

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 取得滑動驗證碼挑戰 |
| **介面路徑** | `GET /api/v1/auth/captcha` |
| **是否需要認證** | 否 |

### 回應結構

#### 成功回應 (200)

| 欄位 | 類型 | 說明 |
|------|------|------|
| `captchaId` | String (UUID) | 挑戰唯一識別 |
| `targetX` | Integer | 滑軌上的目標位置（像素） |

#### 回應範例

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "captchaId": "550e8400-e29b-41d4-a716-446655440000",
    "targetX": 156
  }
}
```

### 錯誤回應

#### 429 - 速率限制

同一 IP 請求過多挑戰時回傳（預設：每分鐘超過 20 次）。

```json
{
  "code": 429,
  "message": "Too many requests, please try again later",
  "data": null
}
```

### cURL 範例

```bash
curl -X GET http://localhost:8080/api/v1/auth/captcha
```

---

## 7. 驗證滑動驗證碼

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 驗證滑動驗證碼 |
| **介面路徑** | `POST /api/v1/auth/captcha/verify` |
| **是否需要認證** | 否 |

### 請求結構

#### Request Body

| 欄位 | 類型 | 必填 | 限制 | 說明 |
|------|------|------|------|------|
| `captchaId` | String | 是 | 非空 | 從 `GET /v1/auth/captcha` 獲取的挑戰 ID |
| `offsetX` | Integer | 是 | ≥0 | 使用者拖動偏移量（滑塊中心位置，像素） |

#### 請求範例

```json
{
  "captchaId": "550e8400-e29b-41d4-a716-446655440000",
  "offsetX": 156
}
```

### 回應結構

#### 成功回應 (200)

| 欄位 | 類型 | 說明 |
|------|------|------|
| `captchaToken` | String (UUID) | 一次性 token，有效期 5 分鐘（300 秒） |

#### 回應範例

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "captchaToken": "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
  }
}
```

### 錯誤回應

#### 400 - 驗證碼無效或已過期

```json
{
  "code": 400,
  "message": "CAPTCHA has expired or too many failed attempts",
  "data": null
}
```

#### 400 - 偏移量不匹配

```json
{
  "code": 400,
  "message": "CAPTCHA verification failed, please try again",
  "data": null
}
```

### cURL 範例

```bash
curl -X POST http://localhost:8080/api/v1/auth/captcha/verify \
  -H "Content-Type: application/json" \
  -d '{
    "captchaId": "550e8400-e29b-41d4-a716-446655440000",
    "offsetX": 156
  }'
```

---

## 介面彙總

| 介面 | 方法 | 路徑 | 認證 |
|------|------|------|------|
| 郵箱註冊 | POST | `/api/v1/auth/register/email` | 否 |
| 郵箱登入 | POST | `/api/v1/auth/login/email` | 否 |
| Google 登入 | POST | `/api/v1/auth/login/google` | 否 |
| 傳送驗證碼 | POST | `/api/v1/auth/send-verification-code` | 否 |
| 查詢驗證開關 | GET | `/api/v1/auth/email-verification-enabled` | 否 |
| 取得滑動驗證碼挑戰 | GET | `/api/v1/auth/captcha` | 否 |
| 驗證滑動驗證碼 | POST | `/api/v1/auth/captcha/verify` | 否 |
