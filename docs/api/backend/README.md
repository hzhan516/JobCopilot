# Backend API 文档

本文档包含 Resume Assistant 后端 API 的完整接口定义。

## 基础信息

| 项目 | 说明 |
|------|------|
| 基础路径 | `/api` |
| API 版本 | `v1` |
| 内容类型 | `application/json` (除文件上传外) |
| 认证方式 | JWT Bearer Token |

## 认证方式

API 使用 JWT (JSON Web Token) 进行认证。除了在 `/api/v1/auth/**` 路径下的接口外，其他所有接口都需要在请求头中携带有效的访问令牌：

```
Authorization: Bearer <access_token>
```

## 全局响应格式

所有 API 响应都遵循统一的格式：

```json
{
  "code": 200,
  "message": "Success",
  "data": {}
}
```

### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | Integer | 状态码，200 表示成功，其他表示错误 |
| `message` | String | 响应消息 |
| `data` | Object | 响应数据，成功时包含具体数据，错误时可能包含详细错误信息 |

## 错误处理

### 错误响应格式

```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

### 常见错误码

| 状态码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未认证或认证失败 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如邮箱已存在） |
| 500 | 服务器内部错误 |

---

## API 端点列表

### 1. 认证模块 (Auth)

#### 1.1 邮箱注册

- **URL**: `POST /api/v1/auth/register/email`
- **认证**: 不需要
- **Content-Type**: `application/json`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | 邮箱地址，需符合邮箱格式 |
| `password` | String | 是 | 密码，长度 6-32 字符 |

**请求示例**:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**成功响应** (200):

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400000
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | UUID | 用户唯一标识 |
| `email` | String | 用户邮箱 |
| `accessToken` | String | 访问令牌 |
| `refreshToken` | String | 刷新令牌 |
| `expiresIn` | Long | 访问令牌有效期（毫秒） |

---

#### 1.2 邮箱登录

- **URL**: `POST /api/v1/auth/login/email`
- **认证**: 不需要
- **Content-Type**: `application/json`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | 邮箱地址 |
| `password` | String | 是 | 密码 |

**请求示例**:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**成功响应** (200):

与邮箱注册响应格式相同。

---

### 2. 简历模块 (Resume)

#### 2.1 上传简历

- **URL**: `POST /api/v1/resumes`
- **认证**: 需要
- **Content-Type**: `multipart/form-data`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | File | 是 | 简历文件（支持 PDF、DOC、DOCX 等格式） |
| `title` | String | 否 | 简历标题，不传则使用文件名 |

**请求示例**:

```http
POST /api/v1/resumes
Authorization: Bearer <access_token>
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="resume.pdf"
Content-Type: application/pdf

<文件内容>
------WebKitFormBoundary
Content-Disposition: form-data; name="title"

我的简历
------WebKitFormBoundary--
```

**成功响应** (200):

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "groupId": "550e8400-e29b-41d4-a716-446655440000",
    "originalVersionId": "550e8400-e29b-41d4-a716-446655440001",
    "title": "我的简历",
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `groupId` | UUID | 简历组 ID |
| `originalVersionId` | UUID | 原始版本 ID（可直接用于下载） |
| `title` | String | 简历标题 |
| `createdAt` | LocalDateTime | 创建时间 |

---

#### 2.2 下载简历

- **URL**: `GET /api/v1/resumes/{resumeId}/download`
- **认证**: 需要
- **Content-Type**: 根据文件类型返回

**路径参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `versionId` | UUID | 版本 ID（上传返回的 originalVersionId） |

**查询参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `format` | String | 否 | 导出格式：`original`（默认）、`pdf`、`docx`、`html`、`md`、`txt` |

**请求示例**:

```http
GET /api/v1/resumes/550e8400-e29b-41d4-a716-446655440000/download?format=pdf
Authorization: Bearer <access_token>
```

**成功响应** (200):

返回文件流，Content-Disposition 头包含文件名。

**注意**: 当前版本仅支持返回原始文件，格式转换功能待实现。

#### 2.3 获取用户所有简历组

- **URL**: `GET /api/v1/resumes/groups`
- **认证**: 需要
- **Content-Type**: `application/json`

**成功响应** (200):

返回当前用户的所有简历组列表。

#### 2.4 获取简历组详情

- **URL**: `GET /api/v1/resumes/groups/{groupId}`
- **认证**: 需要
- **Content-Type**: `application/json`

**路径参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `groupId` | UUID | 简历组唯一标识 |

**成功响应** (200):

返回简历组详情，包含各个版本的摘要信息。

#### 2.5 删除简历组

- **URL**: `DELETE /api/v1/resumes/groups/{groupId}`
- **认证**: 需要
- **Content-Type**: `application/json`

**路径参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `groupId` | UUID | 简历组唯一标识 |

**成功响应** (200):

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

#### 2.6 获取简历组版本列表

- **URL**: `GET /api/v1/resumes/groups/{groupId}/versions`
- **认证**: 需要
- **Content-Type**: `application/json`

**路径参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `groupId` | UUID | 简历组唯一标识 |

**成功响应** (200):

返回该组下所有版本的详细信息列表。

#### 2.7 删除简历版本

- **URL**: `DELETE /api/v1/resumes/versions/{versionId}`
- **认证**: 需要
- **Content-Type**: `application/json`

**路径参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `versionId` | UUID | 版本唯一标识 |

**成功响应** (200):

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

### 3. 对话模块 (Conversation)

详见 [conversation.md](conversation.md)

#### 3.1 创建对话
- **URL**: `POST /api/v1/conversations`
- **认证**: 需要

#### 3.2 发送消息
- **URL**: `POST /api/v1/conversations/{conversationId}/messages`
- **认证**: 需要

#### 3.3 获取对话详情
- **URL**: `GET /api/v1/conversations/{conversationId}`
- **认证**: 需要
- **分页**: 支持 `?page=0&size=20` 对消息列表分页

#### 3.4 获取对话列表
- **URL**: `GET /api/v1/conversations`
- **认证**: 需要

#### 3.5 关闭对话
- **URL**: `PUT /api/v1/conversations/{conversationId}/close`
- **认证**: 需要

#### 3.6 删除对话
- **URL**: `DELETE /api/v1/conversations/{conversationId}`
- **认证**: 需要

#### 3.7 上传附件
- **URL**: `POST /api/v1/conversations/{conversationId}/files`
- **认证**: 需要
- **Content-Type**: `multipart/form-data`

---

### 接口汇总

| 接口 | 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|------|
| 上传简历 | POST | `/api/v1/resumes` | 上传简历文件 | 是 |
| 下载简历 | GET | `/api/v1/resumes/{versionId}/download` | 下载简历文件 | 是 |
| 获取所有组 | GET | `/api/v1/resumes/groups` | 获取用户所有简历组 | 是 |
| 获取组详情 | GET | `/api/v1/resumes/groups/{groupId}` | 获取简历组详情 | 是 |
| 删除组 | DELETE | `/api/v1/resumes/groups/{groupId}` | 删除简历组 | 是 |
| 获取版本列表 | GET | `/api/v1/resumes/groups/{groupId}/versions` | 获取组内所有版本 | 是 |
| 获取版本详情 | GET | `/api/v1/resumes/versions/{versionId}` | 获取单个版本详情 | 是 |
| 删除版本 | DELETE | `/api/v1/resumes/versions/{versionId}` | 删除简历版本 | 是 |
| 编辑版本 | PUT | `/api/v1/resumes/versions/{versionId}` | 编辑版本内容 | 是 |
| 创建对话 | POST | `/api/v1/conversations` | 创建新对话 | 是 |
| 发送消息 | POST | `/api/v1/conversations/{conversationId}/messages` | 发送对话消息 | 是 |
| 获取对话 | GET | `/api/v1/conversations/{conversationId}` | 获取对话详情（支持消息分页） | 是 |
| 获取对话列表 | GET | `/api/v1/conversations` | 获取所有对话 | 是 |
| 关闭对话 | PUT | `/api/v1/conversations/{conversationId}/close` | 关闭对话 | 是 |
| 删除对话 | DELETE | `/api/v1/conversations/{conversationId}` | 删除对话 | 是 |
| 上传附件 | POST | `/api/v1/conversations/{conversationId}/files` | 上传对话附件 | 是 |

---

## DTO 详细定义

### 请求 DTO

#### RegisterByEmailRequest (邮箱注册请求)

```java
{
  "email": String,      // 必填，邮箱格式
  "password": String    // 必填，6-32 字符
}
```

#### LoginByEmailRequest (邮箱登录请求)

```java
{
  "email": String,      // 必填，邮箱格式
  "password": String    // 必填
}
```

#### ResumeUploadRequest (简历上传请求)

```java
{
  "file": MultipartFile,  // 必填，简历文件
  "title": String         // 可选，简历标题
}
```

#### ResumeEditRequest (简历编辑请求)

```java
{
  "versionId": UUID,   // 必填，版本 ID
  "content": String    // 必填，简历内容
}
```

### 响应 DTO

#### AuthResponse (认证响应)

```java
{
  "userId": UUID,        // 用户 ID
  "email": String,       // 用户邮箱
  "accessToken": String, // 访问令牌
  "refreshToken": String,// 刷新令牌
  "expiresIn": Long      // 有效期（毫秒）
}
```

#### ResumeUploadResponse (简历上传响应)

```java
{
  "groupId": UUID,              // 简历组 ID
  "originalVersionId": UUID,    // 原始版本 ID（可直接用于下载）
  "title": String,              // 简历标题
  "createdAt": LocalDateTime    // 创建时间
}
```

#### ResumeGroupResponse (简历组响应)

```java
{
  "groupId": UUID,              // 组 ID
  "title": String,              // 标题
  "isDefault": boolean,         // 是否为默认组
  "createdAt": LocalDateTime,   // 创建时间
  "updatedAt": LocalDateTime,   // 更新时间
  "originalVersion": VersionSummary,    // 原始版本
  "convertedVersion": VersionSummary,   // 转换版本
  "aiOptimizedVersion": VersionSummary  // AI 优化版本
}
```

**VersionSummary**:

```java
{
  "versionId": UUID,        // 版本 ID
  "status": String,         // 状态
  "createdAt": LocalDateTime,// 创建时间
  "exists": boolean         // 是否存在
}
```

#### ResumeVersionResponse (简历版本响应)

```java
{
  "versionId": UUID,          // 版本 ID
  "groupId": UUID,            // 所属组 ID
  "versionType": String,      // 版本类型
  "status": String,           // 状态
  "originalFileName": String, // 原始文件名
  "fileType": String,         // 文件类型
  "fileSize": long,           // 文件大小
  "content": String,          // 内容
  "editable": boolean,        // 是否可编辑
  "createdAt": LocalDateTime, // 创建时间
  "updatedAt": LocalDateTime  // 更新时间
}
```

#### ApiResponse<T> (通用响应包装)

```java
{
  "code": Integer,    // 状态码
  "message": String,  // 消息
  "data": T           // 数据
}
```

---

## 校验规则

### 邮箱注册/登录

| 字段 | 规则 |
|------|------|
| `email` | 必填，必须符合邮箱格式 |
| `password` | 必填，长度 6-32 字符（注册时） |

### 简历上传

| 字段 | 规则 |
|------|------|
| `file` | 必填，必须是有效的文件 |

### 简历编辑

| 字段 | 规则 |
|------|------|
| `versionId` | 必填 |
| `content` | 必填，不能为空 |

---

## 国际化支持

API 支持国际化响应，通过请求头 `Accept-Language` 指定语言：

- `zh-CN` - 简体中文（默认）
- `en` - 英文

示例:

```http
Accept-Language: en
```

---

## 备注

1. 所有时间字段均采用 ISO 8601 格式（如：`2024-01-15T10:30:00`）
2. UUID 格式为标准 36 字符 UUID 字符串（如：`550e8400-e29b-41d4-a716-446655440000`）
3. 文件上传大小限制请参考具体部署配置
4. 当前已实现的端点已列出，Facade 中定义的其他方法将在后续版本中实现
