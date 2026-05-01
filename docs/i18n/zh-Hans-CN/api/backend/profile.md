# 用户资料 API (Profile API)

> 用户个人资料管理接口。

---

## 接口基础信息

所有端点前缀为 `/api`（通过 `server.servlet.context-path` 配置）。

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/profile` | `GET` | 获取当前用户资料 |
| `/api/v1/profile` | `PUT` | 更新个人资料字段 |
| `/api/v1/profile/avatar` | `PUT` | 单独更新头像 URL |

---

## 认证方式

所有接口需在请求头中携带有效的 JWT access token：

```
Authorization: Bearer <accessToken>
```

用户身份通过 `@CurrentUser` 从 token 中解析。

---

## GET /api/v1/profile

获取当前登录用户的个人资料。

### 请求

- **Headers**: `Authorization: Bearer <accessToken>`
- **Body**: 无

### 响应

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

### 错误响应

| HTTP 状态 | Code | 说明 |
|-----------|------|------|
| `401` | `401` | Token 缺失或无效 |
| `404` | `404` | 未找到该用户的资料 |

---

## PUT /api/v1/profile

更新当前用户的个人资料。所有字段均为可选 —— 仅提供的非空字段会被更新。

### 请求

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

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fullName` | `string` | 否 | 用户全名 |
| `phone` | `string` | 否 | 手机号 |
| `targetPosition` | `string` | 否 | 目标职位 |
| `preferredLocation` | `string` | 否 | 期望工作地点 |

### 响应

返回更新后的完整资料，结构与 `GET /api/v1/profile` 相同。

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

### 错误响应

| HTTP 状态 | Code | 说明 |
|-----------|------|------|
| `401` | `401` | Token 缺失或无效 |
| `404` | `404` | 未找到用户资料 |

---

## PUT /api/v1/profile/avatar

仅更新头像 URL。该独立端点简化了前端头像上传流程（先上传到存储服务，再调用此接口保存 URL）。

### 请求

- **Headers**: `Authorization: Bearer <accessToken>`, `Content-Type: application/json`
- **Body**:

```json
{
  "avatarUrl": "https://storage.example.com/avatars/new-avatar.png"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `avatarUrl` | `string` | 是 | 已上传头像图片的公开 URL |

### 响应

返回更新后的完整资料，结构与 `GET /api/v1/profile` 相同。

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

### 错误响应

| HTTP 状态 | Code | 说明 |
|-----------|------|------|
| `401` | `401` | Token 缺失或无效 |
| `404` | `404` | 未找到用户资料 |

---

## 数据模型

### ProfileResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `string (UUID)` | 用户唯一标识 |
| `fullName` | `string` | 用户全名 |
| `avatarUrl` | `string` | 头像图片 URL |
| `phone` | `string` | 手机号 |
| `targetPosition` | `string` | 目标职位 |
| `preferredLocation` | `string` | 期望工作地点 |
| `createdAt` | `string (ISO-8601)` | 资料创建时间 |
| `updatedAt` | `string (ISO-8601)` | 最后更新时间 |

---

## 架构说明

Profile 模块严格遵循 **六边形架构 / DDD** 分层：

```
Trigger (ProfileController)
    └── 调用 API (ProfileFacade)
        └── App 实现 (ProfileFacadeImpl)
            └── 调用 App Service (ProfileApplicationService)
                └── 调用 Domain Port (UserProfileRepository)
                    └── Infrastructure 实现 (UserProfileRepositoryImpl)
```

- **领域实体** `UserProfile` 封装了更新逻辑（`updateProfile`、`updateAvatar`）。
- **应用服务** 负责事务边界和用例编排。
- **门面层** 负责 API DTO 与领域实体之间的转换。

---

## 其他语言

- [English (英文)](../../../../api/backend/profile.md)
- [繁體中文 (Traditional Chinese)](../../zh-Hant-TW/api/backend/profile.md)
