<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/README.md) | [简体中文](README.md) | [繁體中文](../../../zh-Hant-TW/api/backend/README.md)

# Backend API 文档

本文档包含 JobCopilot 后端 API 的完整接口定义。

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
| `captchaToken` | String | 是 | 滑动验证码验证 token（从 `/v1/auth/captcha/verify` 获取） |

**请求示例**:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**成功响应** (201):

```json
{
  "code": 201,
  "message": "Success",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400
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
| `expiresIn` | Long | 访问令牌有效期（秒） |

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
| `captchaToken` | String | 是 | 滑动验证码验证 token |

**请求示例**:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**成功响应** (200):

与邮箱注册响应格式相同。

---

#### 1.3 Google 登录

- **URL**: `POST /api/v1/auth/login/google`
- **认证**: 不需要
- **Content-Type**: `application/json`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `idToken` | String | 是 | Google ID Token |
| `captchaToken` | String | 是 | 滑动验证码验证 token |

**请求示例**:

```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIs...",
  "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**成功响应** (200):

与邮箱登录响应格式相同。

---

#### 1.4 发送验证码

- **URL**: `POST /api/v1/auth/send-verification-code`
- **认证**: 不需要
- **Content-Type**: `application/json`

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | 邮箱地址，需符合邮箱格式 |
| `captchaToken` | String | 是 | 滑动验证码验证 token（预检，不消耗） |

**请求示例**:

```json
{
  "email": "user@example.com",
  "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**成功响应** (200):

```json
{
  "code": 200,
  "message": "验证码已发送，请查收邮件",
  "data": null
}
```

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

- **URL**: `GET /api/v1/resumes/{versionId}/download`
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

### 3. 职位模块 (Job)

详见 [job.md](job.md) 和 [job-matching.md](job-matching.md)

本模块提供职位链接提交、异步解析、智能匹配和历史查询功能。

#### 3.1 提交职位链接
- **URL**: `POST /api/v1/jobs`
- **认证**: 需要

#### 3.2 获取职位详情
- **URL**: `GET /api/v1/jobs/{jobId}`
- **认证**: 需要

#### 3.3 获取职位列表
- **URL**: `GET /api/v1/jobs`
- **认证**: 需要

#### 3.4 启动职位匹配
- **URL**: `POST /api/v1/jobs/match`
- **认证**: 需要

#### 3.5 查询匹配结果
- **URL**: `GET /api/v1/jobs/match/{matchId}`
- **认证**: 需要

#### 3.6 获取匹配历史
- **URL**: `GET /api/v1/jobs/match/history`
- **认证**: 需要

#### 3.7 向量搜索职位
- **URL**: `POST /api/v1/jobs/vector-search`
- **认证**: 需要

---

### 6. 嵌入向量模块 (Embedding)

详见 [embedding.md](embedding.md)

本模块为 AI 层提供批量向量数据写入能力。

#### 6.1 批量 Upsert 职位向量
- **URL**: `POST /api/v1/job-vectors/batch`
- **认证**: 不需要（供内部 AI 服务使用）

---

### 4. 对话模块 (Conversation)

详见 [conversation.md](conversation.md)

#### 4.1 创建对话
- **URL**: `POST /api/v1/conversations`
- **认证**: 需要

#### 4.2 发送消息
- **URL**: `POST /api/v1/conversations/{conversationId}/messages`
- **认证**: 需要

#### 4.3 获取对话详情
- **URL**: `GET /api/v1/conversations/{conversationId}`
- **认证**: 需要
- **分页**: 支持 `?page=0&size=20` 对消息列表分页

#### 4.4 获取对话列表
- **URL**: `GET /api/v1/conversations`
- **认证**: 需要

#### 4.5 关闭对话
- **URL**: `PUT /api/v1/conversations/{conversationId}/close`
- **认证**: 需要

#### 4.6 删除对话
- **URL**: `DELETE /api/v1/conversations/{conversationId}`
- **认证**: 需要

#### 4.7 上传附件
- **URL**: `POST /api/v1/conversations/{conversationId}/files`
- **认证**: 需要
- **Content-Type**: `multipart/form-data`

---

### 5. 求职跟踪模块 (Tracking)

详见 [tracking.md](tracking.md)

本模块提供求职申请的状态流转、事件记录和统计分析功能。

#### 5.1 创建跟踪记录
- **URL**: `POST /api/v1/trackings`
- **认证**: 需要

#### 5.2 获取跟踪列表
- **URL**: `GET /api/v1/trackings?status=INTERVIEWING`
- **认证**: 需要

#### 5.3 获取跟踪详情
- **URL**: `GET /api/v1/trackings/{id}`
- **认证**: 需要

#### 5.4 更新跟踪记录
- **URL**: `PUT /api/v1/trackings/{id}`
- **认证**: 需要

#### 5.5 删除跟踪记录
- **URL**: `DELETE /api/v1/trackings/{id}`
- **认证**: 需要

#### 5.6 获取统计信息
- **URL**: `GET /api/v1/trackings/stats`
- **认证**: 需要

---

### 接口汇总

| 接口 | 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|------|
| 邮箱注册 | POST | `/api/v1/auth/register/email` | 用户邮箱注册 | 否 |
| 邮箱登录 | POST | `/api/v1/auth/login/email` | 用户邮箱登录 | 否 |
| Google 登录 | POST | `/api/v1/auth/login/google` | Google OAuth 登录 | 否 |
| 发送验证码 | POST | `/api/v1/auth/send-verification-code` | 发送邮箱验证码 | 否 |
| 查询验证开关 | GET | `/api/v1/auth/email-verification-enabled` | 查询邮箱验证是否开启 | 否 |
| 获取滑动验证码挑战 | GET | `/api/v1/auth/captcha` | 获取滑动验证码挑战 | 否 |
| 验证滑动验证码 | POST | `/api/v1/auth/captcha/verify` | 验证拖动结果并颁发 token | 否 |
| 发送验证码 | POST | `/api/v1/auth/send-verification-code` | 发送邮箱验证码 | 否 |
| 查询验证开关 | GET | `/api/v1/auth/email-verification-enabled` | 查询邮箱验证是否开启 | 否 |
| 上传简历 | POST | `/api/v1/resumes` | 上传简历文件 | 是 |
| 下载简历 | GET | `/api/v1/resumes/{versionId}/download` | 下载简历文件（支持格式转换） | 是 |
| 获取所有组 | GET | `/api/v1/resumes/groups` | 获取用户所有简历组 | 是 |
| 获取组详情 | GET | `/api/v1/resumes/groups/{groupId}` | 获取简历组详情 | 是 |
| 删除组 | DELETE | `/api/v1/resumes/groups/{groupId}` | 删除简历组 | 是 |
| 获取版本列表 | GET | `/api/v1/resumes/groups/{groupId}/versions` | 获取组内所有版本 | 是 |
| 获取版本详情 | GET | `/api/v1/resumes/versions/{versionId}` | 获取单个版本详情 | 是 |
| 删除版本 | DELETE | `/api/v1/resumes/versions/{versionId}` | 删除简历版本 | 是 |
| 编辑版本 | PUT | `/api/v1/resumes/versions/{versionId}` | 编辑版本内容 | 是 |
| 提交职位 | POST | `/api/v1/jobs` | 提交职位链接异步解析 | 是 |
| 获取职位详情 | GET | `/api/v1/jobs/{jobId}` | 获取职位解析状态 | 是 |
| 获取职位列表 | GET | `/api/v1/jobs` | 获取用户所有职位 | 是 |
| 启动职位匹配 | POST | `/api/v1/jobs/match` | 启动异步职位匹配 | 是 |
| 查询匹配结果 | GET | `/api/v1/jobs/match/{matchId}` | 查询匹配任务结果 | 是 |
| 获取匹配历史 | GET | `/api/v1/jobs/match/history` | 获取历史匹配记录 | 是 |
| 向量搜索职位 | POST | `/api/v1/jobs/vector-search` | ANN 向量搜索职位 | 是 |
| 职位评分 | POST | `/api/v1/jobs/{jobId}/score` | 对职位进行简历匹配评分 | 是 |
| 获取职位数据集 | GET | `/api/v1/job-dataset` | 查询训练数据集（内部接口） | 否 |
| 批量 Upsert 职位向量 | POST | `/api/v1/job-vectors/batch` | 批量 Upsert 职位向量（AI 层） | 否 |
| 创建对话 | POST | `/api/v1/conversations` | 创建新对话 | 是 |
| 发送消息 | POST | `/api/v1/conversations/{conversationId}/messages` | 发送对话消息 | 是 |
| 获取对话 | GET | `/api/v1/conversations/{conversationId}` | 获取对话详情（支持消息分页） | 是 |
| 获取对话列表 | GET | `/api/v1/conversations` | 获取所有对话 | 是 |
| 关闭对话 | PUT | `/api/v1/conversations/{conversationId}/close` | 关闭对话 | 是 |
| 删除对话 | DELETE | `/api/v1/conversations/{conversationId}` | 删除对话 | 是 |
| 上传附件 | POST | `/api/v1/conversations/{conversationId}/files` | 上传对话附件 | 是 |
| 创建跟踪 | POST | `/api/v1/trackings` | 创建求职跟踪记录 | 是 |
| 获取跟踪列表 | GET | `/api/v1/trackings` | 获取跟踪记录列表 | 是 |
| 获取跟踪详情 | GET | `/api/v1/trackings/{id}` | 获取跟踪详情 | 是 |
| 更新跟踪 | PUT | `/api/v1/trackings/{id}` | 更新跟踪（含状态流转） | 是 |
| 删除跟踪 | DELETE | `/api/v1/trackings/{id}` | 删除跟踪记录 | 是 |
| 获取统计 | GET | `/api/v1/trackings/stats` | 获取跟踪统计 | 是 |

---

## DTO 详细定义

### 请求 DTO

#### RegisterByEmailRequest (邮箱注册请求)

```java
{
  "email": String,              // 必填，邮箱格式
  "password": String,           // 必填，6-32 字符
  "verificationCode": String,   // 可选，6 位数字；开启邮箱验证时必填
  "captchaToken": String        // 必填，从 /v1/auth/captcha/verify 获取
}
```

#### SendVerificationCodeRequest (发送验证码请求)

```java
{
  "email": String,         // 必填，邮箱格式
  "captchaToken": String   // 必填，滑动验证码验证 token（预检，不消耗）
}
```

#### LoginByEmailRequest (邮箱登录请求)

```java
{
  "email": String,         // 必填，邮箱格式
  "password": String,      // 必填
  "captchaToken": String   // 必填，滑动验证码验证 token
}
```

#### ResumeUploadRequest (简历上传请求)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | MultipartFile | 是 | 简历文件（PDF/DOCX/MD/TXT） |
| `title` | String | 否 | 简历标题，不传则使用文件名 |

#### ResumeEditRequest (简历编辑请求)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `versionId` | UUID | 是 | 版本 ID（与路径参数一致） |
| `content` | String | 是 | 简历内容（Markdown 格式） |

```java
{
  "versionId": UUID,   // 必填，版本 ID
  "content": String    // 必填，简历内容
}
```

#### VectorSearchRequest (向量搜索请求)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `queryText` | String | 否 | 查询文本，用于生成嵌入 |
| `queryEmbedding` | List<Float> | 否 | 预计算嵌入向量 |
| `limit` | Integer | 否 | 最大返回数（默认：10，最大：100） |
| `filters` | Map<String, String> | 否 | 过滤条件（预留） |

```java
{
  "queryText": String,           // 可选，queryEmbedding 为空时使用
  "queryEmbedding": List<Float>, // 可选，优先于 queryText
  "limit": Integer,              // 可选，默认 10
  "filters": Map<String, String> // 可选
}
```

#### BatchJobVectorUpsertRequest (批量职位向量 Upsert 请求)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `items` | List<JobVectorItem> | 是 | 待 Upsert 的职位向量列表 |

**JobVectorItem:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `jobId` | String | 是 | 唯一职位标识符 |
| `embedding` | List<Float> | 是 | 嵌入向量 |
| `title` | String | 否 | 职位标题 |
| `description` | String | 否 | 职位描述 |
| `requirements` | List<String> | 否 | 职位要求 |
| `rawContent` | String | 否 | 原始文本内容 |
| `sourceFile` | String | 否 | 来源文件标识 |
| `modelVersion` | String | 否 | 模型版本（默认：`gemini-embedding-001`） |

```java
{
  "items": [
    {
      "jobId": String,           // 必填
      "embedding": List<Float>,  // 必填
      "title": String,           // 可选
      "description": String,     // 可选
      "requirements": List<String>, // 可选
      "rawContent": String,      // 可选
      "sourceFile": String,      // 可选
      "modelVersion": String     // 可选
    }
  ]
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
  "expiresIn": Long      // 有效期（秒）
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

#### VectorSearchResponse (向量搜索响应)

```java
{
  "jobId": String,              // 职位 ID
  "title": String,              // 职位标题
  "company": String,            // 公司名称
  "description": String,        // 职位描述
  "requirements": List<String>, // 要求列表
  "similarity": Float,          // 相似度得分（0-1）
  "matchFactors": Map<String, Object> // 匹配因子
}
```

#### BatchJobVectorUpsertResponse (批量 Upsert 响应)

```java
{
  "total": Integer,        // 接收总数
  "success": Integer,      // 成功数
  "failed": Integer,       // 失败数
  "failedJobIds": List<String> // 失败的职位 ID
}
```

---

## 校验规则

### 邮箱注册/登录

| 字段 | 规则 |
|------|------|
| `email` | 必填，必须符合邮箱格式 |
| `password` | 必填，长度 6-32 字符（注册时） |
| `verificationCode` | 关闭时可选；开启时必须填写（6 位数字） |
| `captchaToken` | 所有认证接口必填（注册、登录、Google 登录、发送验证码） |

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

## 相关文档

- [认证模块详细文档](authentication.md)
- [简历模块详细文档](resume.md)
- [职位模块详细文档](job.md)
- [职位匹配模块详细文档](job-matching.md)
- [对话模块详细文档](conversation.md)
- [求职跟踪模块详细文档](tracking.md)
- [嵌入向量模块详细文档](embedding.md)
- [AI / MQ 交互接口文档](ai-mq-interfaces.md)
- [响应格式与错误码说明](response-format.md)

---

## 备注

1. 所有时间字段均采用 ISO 8601 格式（如：`2024-01-15T10:30:00`）
2. UUID 格式为标准 36 字符 UUID 字符串（如：`550e8400-e29b-41d4-a716-446655440000`）
3. 文件上传大小限制请参考具体部署配置
4. 当前已实现的端点已列出，Facade 中定义的其他方法将在后续版本中实现
