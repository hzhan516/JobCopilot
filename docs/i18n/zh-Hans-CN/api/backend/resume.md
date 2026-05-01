<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/resume.md) | [简体中文](resume.md) | [繁體中文](../../../zh-Hant-TW/api/backend/resume.md)

# 简历管理 API

> 简历上传、下载和管理相关接口

---

## 目录

1. [上传简历](#1-上传简历)
2. [下载简历](#2-下载简历)
3. [获取用户所有简历组](#3-获取用户所有简历组)
4. [获取简历组详情](#4-获取简历组详情)
5. [删除简历组](#5-删除简历组)
6. [获取简历组版本列表](#6-获取简历组版本列表)
7. [获取单个版本详情](#7-获取单个版本详情)
8. [删除简历版本](#8-删除简历版本)
9. [编辑版本内容](#9-编辑版本内容)
10. [Facade 接口方法](#10-facade-接口方法)

---

## 1. 上传简历

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 上传简历 |
| **接口路径** | `POST /api/v1/resumes` |
| **是否需要认证** | 是 |
| **Content-Type** | `multipart/form-data` |

### 请求结构

#### Request Parameters

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| `file` | File | 是 | 最大 10MB | 简历文件（PDF/DOCX/MD/TXT格式） |
| `title` | String | 否 | - | 简历标题，不传则使用文件名 |

**支持的文件类型**:
- `application/pdf` - PDF 文件
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document` - DOCX 文件
- `text/markdown` - Markdown 文件
- `text/plain` - 纯文本文件

#### 请求示例 (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/resumes \
  -H "Authorization: Bearer <your_access_token>" \
  -F "file=@/path/to/resume.pdf" \
  -F "title=My Resume"
```

### 响应结构

#### 成功响应 (200)

| 字段 | 类型 | 说明 |
|------|------|------|
| `groupId` | String (UUID) | 简历组唯一标识 |
| `originalVersionId` | String (UUID) | 原始版本ID（可用于直接下载） |
| `title` | String | 简历标题 |
| `createdAt` | String (ISO 8601) | 创建时间 |

#### 响应示例

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "groupId": "550e8400-e29b-41d4-a716-446655440000",
    "originalVersionId": "550e8400-e29b-41d4-a716-446655440001",
    "title": "My Resume",
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

### 错误响应

#### 400 - 文件不能为空

```json
{
  "code": 400,
  "message": "File is required",
  "data": null
}
```

#### 400 - 文件大小超过限制

```json
{
  "code": 400,
  "message": "File size exceeds limit",
  "data": null
}
```

#### 400 - 不支持的文件类型

```json
{
  "code": 400,
  "message": "Invalid file type",
  "data": null
}
```

#### 401 - 未认证

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

---

## 2. 下载简历

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 下载简历 |
| **接口路径** | `GET /api/v1/resumes/{versionId}/download` |
| **是否需要认证** | 是 |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `versionId` | String (UUID) | 是 | 版本ID（上传返回的 `originalVersionId`） |

#### Query Parameters

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `format` | String | 否 | `original` | 导出格式：`original`（原始格式）、`pdf`、`docx`、`html`、`md`、`txt` |

**重要说明**: 
- `versionId` 参数是上传接口返回的 `originalVersionId`
- 不要使用 `groupId` 作为下载参数
- 当前版本仅支持返回原始文件，格式转换功能将在后续版本实现

#### 请求示例 (cURL)

```bash
# 使用上传返回的 originalVersionId 直接下载
curl -X GET http://localhost:8080/api/v1/resumes/550e8400-e29b-41d4-a716-446655440001/download \
  -H "Authorization: Bearer <your_access_token>" \
  --output resume.pdf

# 指定导出格式（当前返回原始文件，文件名会修改）
curl -X GET "http://localhost:8080/api/v1/resumes/550e8400-e29b-41d4-a716-446655440001/download?format=pdf" \
  -H "Authorization: Bearer <your_access_token>" \
  --output resume.pdf
```

### 响应结构

#### 成功响应 (200)

返回文件流，Content-Type 根据文件类型自动设置（如 `application/pdf`）。

**响应 Headers:**

| Header | 说明 |
|--------|------|
| `Content-Type` | 文件 MIME 类型 |
| `Content-Disposition` | 附件下载提示，包含文件名 |
| `Content-Length` | 文件大小 |

### 错误响应

#### 400 - 版本不存在或格式错误

```json
{
  "code": 400,
  "message": "version.not.found",
  "data": null
}
```

或

```json
{
  "code": 400,
  "message": "Parameter 'versionId' must be a valid UUID",
  "data": null
}
```

#### 401 - 未认证

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 无权限访问

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 3. 获取用户所有简历组

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 获取用户所有简历组 |
| **接口路径** | `GET /api/v1/resumes/groups` |
| **是否需要认证** | 是 |
| **Content-Type** | `application/json` |

### 请求结构

无需请求参数，通过当前登录用户身份获取。

#### 请求示例 (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/groups \
  -H "Authorization: Bearer <your_access_token>"
```

### 响应结构

#### 成功响应 (200)

返回当前用户的所有简历组列表。

| 字段 | 类型 | 说明 |
|------|------|------|
| `groupId` | String (UUID) | 组 ID |
| `title` | String | 简历标题 |
| `isDefault` | Boolean | 是否为默认简历组 |
| `createdAt` | String (ISO 8601) | 创建时间 |
| `updatedAt` | String (ISO 8601) | 更新时间 |
| `originalVersion` | VersionSummary | 原始版本摘要 |
| `convertedVersion` | VersionSummary | 转换版本摘要 |
| `aiOptimizedVersion` | VersionSummary | AI优化版本摘要 |

**VersionSummary 结构**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `versionId` | String (UUID) | 版本 ID |
| `status` | String | 版本状态：PENDING, PROCESSING, COMPLETED, FAILED |
| `createdAt` | String (ISO 8601) | 创建时间 |
| `exists` | Boolean | 该版本是否存在 |

#### 响应示例

```json
{
  "code": 200,
  "message": "Success",
  "data": [
    {
      "groupId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "My Resume",
      "isDefault": false,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00",
      "originalVersion": {
        "versionId": "550e8400-e29b-41d4-a716-446655440001",
        "status": "COMPLETED",
        "createdAt": "2024-01-15T10:30:00",
        "exists": true
      },
      "convertedVersion": {
        "versionId": "550e8400-e29b-41d4-a716-446655440002",
        "status": "COMPLETED",
        "createdAt": "2024-01-15T10:30:00",
        "exists": true
      },
      "aiOptimizedVersion": null
    }
  ]
}
```

### 错误响应

#### 401 - 未认证

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

---

## 4. 获取简历组详情

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 获取简历组详情 |
| **接口路径** | `GET /api/v1/resumes/groups/{groupId}` |
| **是否需要认证** | 是 |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `groupId` | String (UUID) | 是 | 简历组唯一标识（上传接口返回的 groupId） |

#### 请求示例 (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/groups/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <your_access_token>"
```

### 响应结构

#### 成功响应 (200)

| 字段 | 类型 | 说明 |
|------|------|------|
| `groupId` | String (UUID) | 组 ID |
| `title` | String | 简历标题 |
| `isDefault` | Boolean | 是否为默认简历组 |
| `createdAt` | String (ISO 8601) | 创建时间 |
| `updatedAt` | String (ISO 8601) | 更新时间 |
| `originalVersion` | VersionSummary | 原始版本摘要 |
| `convertedVersion` | VersionSummary | 转换版本摘要 |
| `aiOptimizedVersion` | VersionSummary | AI优化版本摘要 |

#### 响应示例

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "groupId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "My Resume",
    "isDefault": false,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00",
    "originalVersion": {
      "versionId": "550e8400-e29b-41d4-a716-446655440001",
      "status": "COMPLETED",
      "createdAt": "2024-01-15T10:30:00",
      "exists": true
    },
    "convertedVersion": {
      "versionId": "550e8400-e29b-41d4-a716-446655440002",
      "status": "COMPLETED",
      "createdAt": "2024-01-15T10:30:00",
      "exists": true
    },
    "aiOptimizedVersion": null
  }
}
```

### 错误响应

#### 400 - 组不存在

```json
{
  "code": 400,
  "message": "group.not.found",
  "data": null
}
```

#### 401 - 未认证

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 无权限访问

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 5. 删除简历组

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 删除简历组 |
| **接口路径** | `DELETE /api/v1/resumes/groups/{groupId}` |
| **是否需要认证** | 是 |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `groupId` | String (UUID) | 是 | 简历组唯一标识 |

#### 请求示例 (cURL)

```bash
curl -X DELETE http://localhost:8080/api/v1/resumes/groups/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <your_access_token>"
```

### 响应结构

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

### 错误响应

#### 400 - 组不存在

```json
{
  "code": 400,
  "message": "group.not.found",
  "data": null
}
```

#### 401 - 未认证

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 无权限访问

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 6. 获取简历组版本列表

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 获取简历组版本列表 |
| **接口路径** | `GET /api/v1/resumes/groups/{groupId}/versions` |
| **是否需要认证** | 是 |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `groupId` | String (UUID) | 是 | 简历组唯一标识 |

#### 请求示例 (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/groups/550e8400-e29b-41d4-a716-446655440000/versions \
  -H "Authorization: Bearer <your_access_token>"
```

### 响应结构

#### 成功响应 (200)

返回该组下所有版本的详细信息列表。

**ResumeVersionResponse 结构**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `versionId` | String (UUID) | 版本 ID |
| `groupId` | String (UUID) | 所属组 ID |
| `versionType` | String | 版本类型：ORIGINAL, CONVERTED, AI_OPTIMIZED |
| `status` | String | 状态：PENDING, PROCESSING, COMPLETED, FAILED |
| `originalFileName` | String | 原始文件名 |
| `fileType` | String | 文件类型 |
| `fileSize` | Number | 文件大小（字节） |
| `content` | String | 内容（文本格式，Markdown） |
| `editable` | Boolean | 是否可编辑 |
| `createdAt` | String (ISO 8601) | 创建时间 |
| `updatedAt` | String (ISO 8601) | 更新时间 |

#### 响应示例

```json
{
  "code": 200,
  "message": "Success",
  "data": [
    {
      "versionId": "550e8400-e29b-41d4-a716-446655440001",
      "groupId": "550e8400-e29b-41d4-a716-446655440000",
      "versionType": "ORIGINAL",
      "status": "COMPLETED",
      "originalFileName": "resume.pdf",
      "fileType": "application/pdf",
      "fileSize": 102456,
      "content": null,
      "editable": false,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    },
    {
      "versionId": "550e8400-e29b-41d4-a716-446655440002",
      "groupId": "550e8400-e29b-41d4-a716-446655440000",
      "versionType": "CONVERTED",
      "status": "COMPLETED",
      "originalFileName": "resume.pdf",
      "fileType": "text/markdown",
      "fileSize": 0,
      "content": "# Resume Content...",
      "editable": true,
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  ]
}
```

### 错误响应

#### 400 - 组不存在

```json
{
  "code": 400,
  "message": "group.not.found",
  "data": null
}
```

#### 401 - 未认证

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 无权限访问

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 7. 获取单个版本详情

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 获取单个版本详情 |
| **接口路径** | `GET /api/v1/resumes/versions/{versionId}` |
| **是否需要认证** | 是 |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `versionId` | String (UUID) | 是 | 版本唯一标识 |

#### 请求示例 (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/versions/550e8400-e29b-41d4-a716-446655440001 \
  -H "Authorization: Bearer <your_access_token>"
```

### 响应结构

#### 成功响应 (200)

| 字段 | 类型 | 说明 |
|------|------|------|
| `versionId` | String (UUID) | 版本 ID |
| `groupId` | String (UUID) | 所属组 ID |
| `versionType` | String | 版本类型：ORIGINAL, CONVERTED, AI_OPTIMIZED |
| `status` | String | 状态：PENDING, PROCESSING, COMPLETED, FAILED |
| `originalFileName` | String | 原始文件名 |
| `fileType` | String | 文件类型 |
| `fileSize` | Number | 文件大小（字节） |
| `content` | String | 内容（文本格式，Markdown） |
| `editable` | Boolean | 是否可编辑 |
| `createdAt` | String (ISO 8601) | 创建时间 |
| `updatedAt` | String (ISO 8601) | 更新时间 |

#### 响应示例

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "versionId": "550e8400-e29b-41d4-a716-446655440002",
    "groupId": "550e8400-e29b-41d4-a716-446655440000",
    "versionType": "CONVERTED",
    "status": "COMPLETED",
    "originalFileName": "resume.pdf",
    "fileType": "text/markdown",
    "fileSize": 0,
    "content": "# Resume\n\n## Experience\n...",
    "editable": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

### 错误响应

#### 400 - 版本不存在

```json
{
  "code": 400,
  "message": "version.not.found",
  "data": null
}
```

#### 401 - 未认证

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 无权限访问

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 8. 删除简历版本

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 删除简历版本 |
| **接口路径** | `DELETE /api/v1/resumes/versions/{versionId}` |
| **是否需要认证** | 是 |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `versionId` | String (UUID) | 是 | 版本唯一标识 |

**注意**: 
- 只能删除 `CONVERTED` 或 `AI_OPTIMIZED` 类型的版本
- `ORIGINAL` 类型版本不允许单独删除，必须通过删除整个简历组来删除

#### 请求示例 (cURL)

```bash
curl -X DELETE http://localhost:8080/api/v1/resumes/versions/550e8400-e29b-41d4-a716-446655440002 \
  -H "Authorization: Bearer <your_access_token>"
```

### 响应结构

#### 成功响应 (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

### 错误响应

#### 400 - 版本不存在

```json
{
  "code": 400,
  "message": "version.not.found",
  "data": null
}
```

#### 400 - 原版不能单独删除

```json
{
  "code": 400,
  "message": "version.original.cannot.delete",
  "data": null
}
```

#### 401 - 未认证

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 无权限访问

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 9. 编辑版本内容

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 编辑版本内容 |
| **接口路径** | `PUT /api/v1/resumes/versions/{versionId}` |
| **是否需要认证** | 是 |
| **Content-Type** | `application/json` |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `versionId` | String (UUID) | 是 | 版本唯一标识 |

#### Request Body

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content` | String | 是 | 简历内容（Markdown 格式） |

**注意**: 请求体中的 `versionId` 会被忽略，以路径参数为准。

**自动向量重生成**: 编辑 `CONVERTED` 或 `AI_OPTIMIZED` 类型的版本时，系统在内容持久化后会自动触发异步向量重生成请求，确保后续职位匹配召回使用最新的简历嵌入向量。若 MQ 消息发送失败，编辑操作仍会成功返回；向量将在下次可用时机自动补齐。

#### 请求示例 (cURL)

```bash
curl -X PUT http://localhost:8080/api/v1/resumes/versions/550e8400-e29b-41d4-a716-446655440002 \
  -H "Authorization: Bearer <your_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "# Updated Resume\n\n## Experience\nSoftware Engineer at XYZ Corp..."
  }'
```

### 响应结构

#### 成功响应 (200)

返回更新后的版本详情，结构与获取单个版本详情相同。

#### 响应示例

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "versionId": "550e8400-e29b-41d4-a716-446655440002",
    "groupId": "550e8400-e29b-41d4-a716-446655440000",
    "versionType": "CONVERTED",
    "status": "COMPLETED",
    "originalFileName": "resume.pdf",
    "fileType": "text/markdown",
    "fileSize": 0,
    "content": "# Updated Resume\n\n## Experience\nSoftware Engineer at XYZ Corp...",
    "editable": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:35:00"
  }
}
```

### 错误响应

#### 400 - 参数校验失败

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": {
    "content": "Content is required"
  }
}
```

#### 400 - 版本不存在

```json
{
  "code": 400,
  "message": "version.not.found",
  "data": null
}
```

#### 401 - 未认证

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 无权限访问

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 10. Facade 接口方法

ResumeFacade 定义了完整的简历管理接口，以下是接口方法列表：

| 方法 | 说明 | 实现状态 |
|------|------|----------|
| `uploadResume` | 上传简历 | ✅ 已实现 |
| `downloadResume` | 下载简历 | ✅ 已实现 |
| `getResumeGroups` | 获取用户的所有简历组 | ✅ 已实现 |
| `getResumeGroup` | 获取单个简历组详情 | ✅ 已实现 |
| `deleteResumeGroup` | 删除简历组 | ✅ 已实现 |
| `getVersionsByGroup` | 获取简历组下的所有版本 | ✅ 已实现 |
| `getVersion` | 获取单个版本详情 | ✅ 已实现 |
| `deleteVersion` | 删除简历版本 | ✅ 已实现 |
| `editVersion` | 编辑版本内容 | ✅ 已实现 |
| `setDefaultGroup` | 设置默认简历组 | ⏳ MVP 未实现 |
| `createAiVersion` | 创建AI优化版本 | ⏳ MVP 未实现 |
| `rollbackToVersion` | 回滚到指定版本 | ⏳ MVP 未实现 |

---

## DTO 定义

### ResumeUploadRequest (上传请求)

```java
{
  "file": MultipartFile,  // 必填，简历文件
  "title": String         // 可选，简历标题
}
```

### ResumeUploadResponse (上传响应)

```java
{
  "groupId": UUID,              // 简历组 ID
  "originalVersionId": UUID,    // 原始版本 ID（可直接用于下载）
  "title": String,              // 简历标题
  "createdAt": LocalDateTime    // 创建时间
}
```

### ResumeEditRequest (编辑请求)

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `versionId` | UUID | 是 | 版本 ID（会被路径参数覆盖） |
| `content` | String | 是 | 简历内容（Markdown 格式） |

```java
{
  "versionId": UUID,   // 必填，版本 ID（会被路径参数覆盖）
  "content": String    // 必填，简历内容（Markdown）
}
```

### ResumeGroupResponse (简历组响应)

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

### ResumeVersionResponse (简历版本响应)

```java
{
  "versionId": UUID,          // 版本 ID
  "groupId": UUID,            // 所属组 ID
  "versionType": String,      // 版本类型：ORIGINAL, CONVERTED, AI_OPTIMIZED
  "status": String,           // 状态：PENDING, PROCESSING, COMPLETED, FAILED
  "originalFileName": String, // 原始文件名
  "fileType": String,         // 文件类型
  "fileSize": long,           // 文件大小
  "content": String,          // 内容（文本格式）
  "editable": boolean,        // 是否可编辑
  "createdAt": LocalDateTime, // 创建时间
  "updatedAt": LocalDateTime  // 更新时间
}
```

---

## 简历状态说明

| 状态值 | 说明 |
|--------|------|
| `PENDING` | 待处理（刚上传） |
| `PROCESSING` | 解析中 |
| `COMPLETED` | 解析完成 |
| `FAILED` | 解析失败 |

## 版本类型说明

| 类型值 | 说明 |
|--------|------|
| `ORIGINAL` | 原始上传版本 |
| `CONVERTED` | 格式转换版本（转换为 Markdown） |
| `AI_OPTIMIZED` | AI 优化版本 |

---

## 接口汇总

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

---

## 使用流程示例

### 简单流程：上传后直接下载

```bash
# 1. 登录获取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.data.accessToken')

# 2. 上传简历
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/resumes \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@resume.pdf" \
  -F "title=My Resume")
  
ORIGINAL_VERSION_ID=$(echo $RESPONSE | jq -r '.data.originalVersionId')
echo "Original Version ID: $ORIGINAL_VERSION_ID"

# 3. 直接使用 originalVersionId 下载
curl -X GET "http://localhost:8080/api/v1/resumes/$ORIGINAL_VERSION_ID/download" \
  -H "Authorization: Bearer $TOKEN" \
  --output downloaded_resume.pdf
```

### 完整流程：编辑后下载

```bash
# 1. 登录获取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.data.accessToken')

# 2. 上传简历
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/resumes \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@resume.pdf" \
  -F "title=My Resume")
  
GROUP_ID=$(echo $RESPONSE | jq -r '.data.groupId')
echo "Group ID: $GROUP_ID"

# 3. 获取组详情
GROUP_INFO=$(curl -s "http://localhost:8080/api/v1/resumes/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN")

# 4. 提取转换版本ID（可编辑的 Markdown 版本）
CONVERTED_VERSION_ID=$(echo $GROUP_INFO | jq -r '.data.convertedVersion.versionId')
echo "Converted Version ID: $CONVERTED_VERSION_ID"

# 5. 获取版本详情查看当前内容
VERSION_DETAIL=$(curl -s "http://localhost:8080/api/v1/resumes/versions/$CONVERTED_VERSION_ID" \
  -H "Authorization: Bearer $TOKEN")
echo "Current Content: $(echo $VERSION_DETAIL | jq -r '.data.content')"

# 6. 编辑版本内容
curl -X PUT "http://localhost:8080/api/v1/resumes/versions/$CONVERTED_VERSION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "# Updated Resume\n\n## Experience\nSoftware Engineer..."
  }'

# 7. 使用版本ID下载
curl -X GET "http://localhost:8080/api/v1/resumes/$CONVERTED_VERSION_ID/download" \
  -H "Authorization: Bearer $TOKEN" \
  --output converted_resume.md

# 8. 删除简历组（清理）
curl -X DELETE "http://localhost:8080/api/v1/resumes/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 常见问题

### Q: 上传成功后下载提示 "version.not.found"？

**A**: 请使用上传接口返回的 `originalVersionId` 进行下载，而不是 `groupId`。

### Q: 如何获取版本ID？

**A**: 
1. **推荐**: 直接使用上传接口返回的 `originalVersionId`
2. 调用 `GET /api/v1/resumes/groups/{groupId}` 获取组详情，其中包含各个版本的摘要信息
3. 调用 `GET /api/v1/resumes/groups/{groupId}/versions` 获取该组下所有版本列表
4. 调用 `GET /api/v1/resumes/versions/{versionId}` 获取单个版本详情

### Q: 哪些版本可以编辑？

**A**: 
- `CONVERTED`（转换版）和 `AI_OPTIMIZED`（AI优化版）可以编辑
- `ORIGINAL`（原版）不可编辑，保护原始上传文件
- 可通过 `editable` 字段判断是否可编辑

---

*文档版本: 1.3.0*
