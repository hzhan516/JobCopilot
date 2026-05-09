<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/authentication.md) | [简体中文](authentication.md) | [繁體中文](../../../zh-Hant-TW/api/backend/authentication.md)

# 认证 API

> 用户注册、登录和 Token 管理相关接口

---

## 目录

1. [邮箱注册](#1-邮箱注册)
2. [邮箱登录](#2-邮箱登录)
3. [Google 登录](#3-google-登录)
4. [发送验证码](#4-发送验证码)
5. [查询邮箱验证开关](#5-查询邮箱验证开关)

---

## 1. 邮箱注册

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 邮箱注册 |
| **接口路径** | `POST /api/v1/auth/register/email` |
| **是否需要认证** | 否 |

### 请求结构

#### Request Body

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| `email` | String | 是 | 邮箱格式 | 用户邮箱地址 |
| `password` | String | 是 | 6-32位 | 用户密码 |
| `verificationCode` | String | 否 | 6位数字 | 开启邮箱验证时必填 |

#### 请求示例

```json
{
  "email": "user@example.com",
  "password": "123456",
  "verificationCode": "123456"
}
```

### 响应结构

#### 成功响应 (201)

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | String (UUID) | 用户唯一标识 |
| `email` | String | 用户邮箱 |
| `accessToken` | String | 访问令牌（有效期24小时） |
| `refreshToken` | String | 刷新令牌（有效期7天） |
| `expiresIn` | Long | accessToken 有效期（秒） |

#### 响应示例

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

### 错误响应

#### 400 - 参数校验错误

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

#### 400 - 验证码无效或已过期

```json
{
  "code": 400,
  "message": "Invalid or expired verification code",
  "data": null
}
```

#### 409 - 邮箱已存在

```json
{
  "code": 409,
  "message": "Email already exists",
  "data": null
}
```

### 调用示例 (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/auth/register/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "123456",
    "verificationCode": "123456"
  }'
```

---

## 2. 邮箱登录

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 邮箱登录 |
| **接口路径** | `POST /api/v1/auth/login/email` |
| **是否需要认证** | 否 |

### 请求结构

#### Request Body

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| `email` | String | 是 | 邮箱格式 | 用户邮箱地址 |
| `password` | String | 是 | 非空 | 用户密码 |

#### 请求示例

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

### 响应结构

#### 成功响应 (200)

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | String (UUID) | 用户唯一标识 |
| `email` | String | 用户邮箱 |
| `accessToken` | String | 访问令牌（有效期24小时） |
| `refreshToken` | String | 刷新令牌（有效期7天） |
| `expiresIn` | Long | accessToken 有效期（秒） |

#### 响应示例

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

### 错误响应

#### 401 - 邮箱或密码错误

```json
{
  "code": 401,
  "message": "Invalid credentials",
  "data": null
}
```

#### 400 - 参数校验错误

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

### 调用示例 (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "123456"
  }'
```

---

## 3. Google 登录

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | Google 登录 |
| **接口路径** | `POST /api/v1/auth/login/google` |
| **是否需要认证** | 否 |

### 请求结构

#### Request Body

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| `idToken` | String | 是 | 非空 | Google ID Token |

#### 请求示例

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIs..."
}
```

### 响应结构

#### 成功响应 (200)

与邮箱登录响应格式相同（首次登录会自动注册并创建用户）。

#### 响应示例

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

### 错误响应

#### 401 - ID Token 无效或验证失败

```json
{
  "code": 401,
  "message": "Invalid credentials",
  "data": null
}
```

### 调用示例 (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login/google \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "eyJhbGciOiJSUzI1NiIs..."
  }'
```

---

## 4. 发送验证码

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 发送验证码 |
| **接口路径** | `POST /api/v1/auth/send-verification-code` |
| **是否需要认证** | 否 |

### 请求结构

#### Request Body

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| `email` | String | 是 | 邮箱格式 | 目标邮箱地址 |

#### 请求示例

```json
{
  "email": "user@example.com"
}
```

### 响应结构

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "验证码已发送，请查收邮件",
  "data": null
}
```

### 错误响应

#### 400 - 邮箱格式不合法

```json
{
  "code": 400,
  "message": "请求参数无效",
  "data": null
}
```

#### 409 - 邮箱已被注册

```json
{
  "code": 409,
  "message": "该邮箱已被注册",
  "data": null
}
```

#### 429 - 冷却中

```json
{
  "code": 429,
  "message": "请稍后再重新获取验证码",
  "data": null
}
```

### cURL 示例

```bash
curl -X POST http://localhost:8080/api/v1/auth/send-verification-code \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

---

## 5. 查询邮箱验证开关

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 查询邮箱验证开关 |
| **接口路径** | `GET /api/v1/auth/email-verification-enabled` |
| **是否需要认证** | 否 |

### 响应结构

#### 成功响应 (200)

| 字段 | 类型 | 说明 |
|------|------|------|
| `data` | Boolean | `true` 表示邮箱验证已开启，`false` 表示已关闭 |

#### 响应示例

```json
{
  "code": 200,
  "message": "成功",
  "data": true
}
```

### cURL 示例

```bash
curl -X GET http://localhost:8080/api/v1/auth/email-verification-enabled
```

---

## Token 使用说明

### Access Token

- **有效期**: 24小时 (86400秒)
- **用途**: 访问需要认证的 API 接口
- **传递方式**: HTTP Header `Authorization: Bearer <token>`

### Refresh Token

- **有效期**: 7天 (604800秒)
- **用途**: Access Token 过期后获取新的 Token 对
- **存储建议**: 安全存储，用于 Token 刷新机制

### Token 结构 (JWT)

```
Header.Payload.Signature
```

Payload 包含以下字段：

| 字段 | 说明 |
|------|------|
| `sub` | 用户ID |
| `iat` | 签发时间 |
| `exp` | 过期时间 |

---

## 接口汇总

| 接口 | 方法 | 路径 | 认证 |
|------|------|------|------|
| 邮箱注册 | POST | `/api/v1/auth/register/email` | 否 |
| 邮箱登录 | POST | `/api/v1/auth/login/email` | 否 |
| Google 登录 | POST | `/api/v1/auth/login/google` | 否 |
| 发送验证码 | POST | `/api/v1/auth/send-verification-code` | 否 |
| 查询验证开关 | GET | `/api/v1/auth/email-verification-enabled` | 否 |
