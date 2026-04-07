# Resume Assistant API 文档

> 智能求职助手后端 API 文档

## 概述

本文档描述了 Resume Assistant 后端服务的 REST API 接口，包括认证、用户管理、简历管理、职位管理和对话等功能模块。

### 基础信息

| 项目 | 值 |
|------|-----|
| **Base URL** | `http://localhost:8080/api` |
| **Content-Type** | `application/json` |
| **编码** | UTF-8 |

### 响应格式

所有 API 响应均使用统一的响应格式：

```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | Integer | 状态码，200 表示成功 |
| `message` | String | 提示信息 |
| `data` | Object | 业务数据 |

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未认证/登录过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 认证方式

除登录和注册接口外，其他接口需要在请求头中携带 JWT Token：

```
Authorization: Bearer <accessToken>
```

---

## 文档目录

| 模块 | 文档 | 说明 |
|------|------|------|
| 认证 | [authentication.md](authentication.md) | 用户注册、登录、Token 刷新 |
| 响应格式 | [response-format.md](response-format.md) | 统一响应格式详细说明 |

---

## 快速开始

### 1. 注册账号

```bash
curl -X POST http://localhost:8080/api/v1/auth/register/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "123456"
  }'
```

### 2. 登录获取 Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "123456"
  }'
```

### 3. 使用 Token 访问受保护接口

```bash
curl -X GET http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer <your_access_token>"
```

---

*文档版本: 1.0.0*
