# 响应格式规范

> 所有 API 接口的统一响应格式说明

---

## 概述

Resume Assistant API 采用统一的响应格式，无论请求成功或失败，都会返回以下结构：

```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

---

## 响应结构

### 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `code` | Integer | 是 | 业务状态码 |
| `message` | String | 是 | 提示信息 |
| `data` | Object | 否 | 业务数据，失败时可能为 null |

### 状态码 (code)

| 状态码 | 含义 | 说明 |
|--------|------|------|
| 200 | 成功 | 请求处理成功 |
| 400 | 请求错误 | 参数校验失败或业务逻辑错误 |
| 401 | 未认证 | Token 缺失或无效 |
| 403 | 无权限 | 没有访问该资源的权限 |
| 404 | 不存在 | 请求的资源不存在 |
| 500 | 服务器错误 | 服务器内部错误 |

---

## 成功响应示例

### 带数据的响应

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

### 仅消息的成功响应

```json
{
  "code": 200,
  "message": "Operation completed successfully",
  "data": null
}
```

---

## 错误响应示例

### 参数校验错误

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

### 业务逻辑错误

```json
{
  "code": 400,
  "message": "Email already exists",
  "data": null
}
```

### 认证错误

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

### 权限错误

```json
{
  "code": 403,
  "message": "Access denied",
  "data": null
}
```

### 资源不存在

```json
{
  "code": 404,
  "message": "User not found",
  "data": null
}
```

### 服务器错误

```json
{
  "code": 500,
  "message": "System busy, please try again later",
  "data": null
}
```

---

## 分页响应格式

对于列表查询接口，响应数据包含分页信息：

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

### 分页字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `list` | Array | 数据列表 |
| `page` | Integer | 当前页码（从 0 开始，Spring Data 默认） |
| `size` | Integer | 每页条数 |
| `total` | Long | 总记录数 |
| `totalPages` | Integer | 总页数 |

---

## 时间格式

所有时间字段统一使用 ISO 8601 格式：

```
2026-04-06T22:45:00.000+00:00
```

---

## 错误处理建议

### 前端处理流程

1. **检查 HTTP 状态码**
   - 2xx: 继续处理响应体
   - 4xx/5xx: 进入错误处理

2. **解析响应体**
   - 检查 `code` 字段
   - code = 200: 处理 `data` 数据
   - code != 200: 显示 `message` 错误信息

3. **特殊处理**
   - code = 401: Token 过期，使用 Refresh Token 刷新或跳转登录页
   - code = 403: 提示用户无权限
   - code = 500: 提示服务器繁忙，稍后重试

### 示例代码 (JavaScript)

```javascript
async function apiRequest(url, options) {
  try {
    const response = await fetch(url, options);
    const result = await response.json();
    
    if (result.code === 200) {
      return result.data;
    } else if (result.code === 401) {
      // Token 过期，刷新或跳转登录
      window.location.href = '/login';
    } else {
      // 显示错误信息
      alert(result.message);
    }
  } catch (error) {
    alert('Network error');
  }
}
```
