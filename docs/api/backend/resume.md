# 简历管理 API

> 简历上传、下载和管理相关接口

---

## 目录

1. [上传简历](#1-上传简历)
2. [下载简历](#2-下载简历)

---

## 1. 上传简历

### 基本信息

| 项目 | 值 |
|------|-----|
| **接口名称** | 上传简历 |
| **接口路径** | `POST /v1/resumes` |
| **是否需要认证** | 是 |
| **Content-Type** | `multipart/form-data` |

### 请求结构

#### Request Parameters

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| `file` | File | 是 | - | 简历文件（PDF/DOC/DOCX格式） |
| `title` | String | 否 | - | 简历标题，可选 |

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
| `resumeId` | String (UUID) | 简历唯一标识 |
| `fileName` | String | 上传的文件名 |
| `fileSize` | Long | 文件大小（字节） |
| `status` | String | 简历处理状态 |
| `uploadedAt` | String (ISO 8601) | 上传时间 |

#### 响应示例

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "resumeId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "fileName": "resume.pdf",
    "fileSize": 102456,
    "status": "PENDING",
    "uploadedAt": "2026-04-07T11:30:00.000+00:00"
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
| **接口路径** | `GET /v1/resumes/{resumeId}/download` |
| **是否需要认证** | 是 |

### 请求结构

#### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `resumeId` | String (UUID) | 是 | 简历唯一标识 |

#### Query Parameters

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `format` | String | 否 | `original` | 导出格式：`original`（原始格式）、`pdf`、`docx`、`html`、`md`、`txt` |

**说明**：当前版本仅支持返回原始文件，格式转换功能将在后续版本实现。

#### 请求示例 (cURL)

```bash
# 下载原始文件
curl -X GET http://localhost:8080/api/v1/resumes/a1b2c3d4-e5f6-7890-abcd-ef1234567890/download \
  -H "Authorization: Bearer <your_access_token>" \
  --output resume.pdf

# 指定导出格式（当前返回原始文件，文件名会修改）
curl -X GET "http://localhost:8080/api/v1/resumes/a1b2c3d4-e5f6-7890-abcd-ef1234567890/download?format=pdf" \
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

#### 401 - 未认证

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 404 - 简历不存在

```json
{
  "code": 404,
  "message": "Resume not found",
  "data": null
}
```

#### 403 - 无权限访问

```json
{
  "code": 403,
  "message": "Access denied",
  "data": null
}
```

---

## 接口汇总

| 接口 | 方法 | 路径 | 认证 |
|------|------|------|------|
| 上传简历 | POST | `/v1/resumes` | 是 |
| 下载简历 | GET | `/v1/resumes/{resumeId}/download` | 是 |

---

## 简历状态说明

| 状态值 | 说明 |
|--------|------|
| `PENDING` | 待处理（刚上传） |
| `PROCESSING` | 解析中 |
| `COMPLETED` | 解析完成 |
| `FAILED` | 解析失败 |

---

*文档版本: 1.0.0*
