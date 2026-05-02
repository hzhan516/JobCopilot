<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](resume.md) | [简体中文](../../i18n/zh-Hans-CN/api/backend/resume.md) | [繁體中文](../../i18n/zh-Hant-TW/api/backend/resume.md)

# Resume Management API

> APIs related to resume upload, download, and management

---

## Table of Contents

1. [Upload Resume](#1-upload-resume)
2. [Download Resume](#2-download-resume)
3. [Get All Resume Groups for User](#3-get-all-resume-groups-for-user)
4. [Get Resume Group Details](#4-get-resume-group-details)
5. [Delete Resume Group](#5-delete-resume-group)
6. [Get Resume Group Version List](#6-get-resume-group-version-list)
7. [Get Single Version Details](#7-get-single-version-details)
8. [Delete Resume Version](#8-delete-resume-version)
9. [Edit Version Content](#9-edit-version-content)
10. [Create Version Copy](#10-create-version-copy)
11. [Facade Interface Methods](#11-facade-interface-methods)

---

## 1. Upload Resume

### Basic Information

| Item | Value |
|------|-------|
| **Endpoint Name** | Upload Resume |
| **Endpoint Path** | `POST /api/v1/resumes` |
| **Authentication Required** | Yes |
| **Content-Type** | `multipart/form-data` |

### Request Structure

#### Request Parameters

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `file` | File | Yes | Max 10MB | Resume file (PDF/DOCX/MD/TXT format) |
| `title` | String | No | - | Resume title; uses filename if not provided |

**Supported File Types**:
- `application/pdf` - PDF file
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document` - DOCX file
- `text/markdown` - Markdown file
- `text/plain` - Plain text file

#### Request Example (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/resumes \
  -H "Authorization: Bearer <your_access_token>" \
  -F "file=@/path/to/resume.pdf" \
  -F "title=My Resume"
```

### Response Structure

#### Success Response (200)

| Field | Type | Description |
|-------|------|-------------|
| `groupId` | String (UUID) | Resume group unique identifier |
| `originalVersionId` | String (UUID) | Original version ID (can be used directly for download) |
| `title` | String | Resume title |
| `createdAt` | String (ISO 8601) | Creation time |

#### Response Example

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

### Error Responses

#### 400 - File Cannot Be Empty

```json
{
  "code": 400,
  "message": "File is required",
  "data": null
}
```

#### 400 - File Size Exceeds Limit

```json
{
  "code": 400,
  "message": "File size exceeds limit",
  "data": null
}
```

#### 400 - Unsupported File Type

```json
{
  "code": 400,
  "message": "Invalid file type",
  "data": null
}
```

#### 401 - Not Authenticated

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

---

## 2. Download Resume

### Basic Information

| Item | Value |
|------|-------|
| **Endpoint Name** | Download Resume |
| **Endpoint Path** | `GET /api/v1/resumes/{versionId}/download` |
| **Authentication Required** | Yes |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `versionId` | String (UUID) | Yes | Version ID (originalVersionId returned by upload) |

#### Query Parameters

| Field | Type | Required | Default Value | Description |
|-------|------|----------|---------------|-------------|
| `format` | String | No | `original` | Export format: `original` (original format), `pdf`, `docx`, `html`, `md`, `txt` |

**Important Notes**:
- The `versionId` parameter is the `originalVersionId` returned by the upload endpoint
- Do not use `groupId` as the download parameter
- The current version only supports returning the original file; format conversion will be implemented in a future version

#### Request Example (cURL)

```bash
# Download directly using the originalVersionId returned by upload
curl -X GET http://localhost:8080/api/v1/resumes/550e8400-e29b-41d4-a716-446655440001/download \
  -H "Authorization: Bearer <your_access_token>" \
  --output resume.pdf

# Specify export format (currently returns original file, filename will be modified)
curl -X GET "http://localhost:8080/api/v1/resumes/550e8400-e29b-41d4-a716-446655440001/download?format=pdf" \
  -H "Authorization: Bearer <your_access_token>" \
  --output resume.pdf
```

### Response Structure

#### Success Response (200)

Returns file stream; Content-Type is automatically set based on file type (e.g., `application/pdf`).

**Response Headers:**

| Header | Description |
|--------|-------------|
| `Content-Type` | File MIME type |
| `Content-Disposition` | Attachment download hint, contains filename |
| `Content-Length` | File size |

### Error Responses

#### 400 - Version Does Not Exist or Format Error

```json
{
  "code": 400,
  "message": "version.not.found",
  "data": null
}
```

Or

```json
{
  "code": 400,
  "message": "Parameter 'versionId' must be a valid UUID",
  "data": null
}
```

#### 401 - Not Authenticated

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - No Permission to Access

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 3. Get All Resume Groups for User

### Basic Information

| Item | Value |
|------|-------|
| **Endpoint Name** | Get All Resume Groups for User |
| **Endpoint Path** | `GET /api/v1/resumes/groups` |
| **Authentication Required** | Yes |
| **Content-Type** | `application/json` |

### Request Structure

No request parameters needed; retrieved based on the currently logged-in user's identity.

#### Request Example (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/groups \
  -H "Authorization: Bearer <your_access_token>"
```

### Response Structure

#### Success Response (200)

Returns a list of all resume groups for the current user.

| Field | Type | Description |
|-------|------|-------------|
| `groupId` | String (UUID) | Group ID |
| `title` | String | Resume title |
| `isDefault` | Boolean | Whether it is the default resume group |
| `createdAt` | String (ISO 8601) | Creation time |
| `updatedAt` | String (ISO 8601) | Update time |
| `originalVersion` | VersionSummary | Original version summary |
| `convertedVersion` | VersionSummary | Converted version summary |
| `aiOptimizedVersion` | VersionSummary | AI-optimized version summary |

**VersionSummary Structure:**

| Field | Type | Description |
|-------|------|-------------|
| `versionId` | String (UUID) | Version ID |
| `status` | String | Version status: PENDING, PROCESSING, COMPLETED, FAILED |
| `createdAt` | String (ISO 8601) | Creation time |
| `exists` | Boolean | Whether this version exists |

#### Response Example

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

### Error Responses

#### 401 - Not Authenticated

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

---

## 4. Get Resume Group Details

### Basic Information

| Item | Value |
|------|-------|
| **Endpoint Name** | Get Resume Group Details |
| **Endpoint Path** | `GET /api/v1/resumes/groups/{groupId}` |
| **Authentication Required** | Yes |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `groupId` | String (UUID) | Yes | Resume group unique identifier (groupId returned by upload endpoint) |

#### Request Example (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/groups/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <your_access_token>"
```

### Response Structure

#### Success Response (200)

| Field | Type | Description |
|-------|------|-------------|
| `groupId` | String (UUID) | Group ID |
| `title` | String | Resume title |
| `isDefault` | Boolean | Whether it is the default resume group |
| `createdAt` | String (ISO 8601) | Creation time |
| `updatedAt` | String (ISO 8601) | Update time |
| `originalVersion` | VersionSummary | Original version summary |
| `convertedVersion` | VersionSummary | Converted version summary |
| `aiOptimizedVersion` | VersionSummary | AI-optimized version summary |

#### Response Example

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

### Error Responses

#### 400 - Group Does Not Exist

```json
{
  "code": 400,
  "message": "group.not.found",
  "data": null
}
```

#### 401 - Not Authenticated

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - No Permission to Access

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 5. Delete Resume Group

### Basic Information

| Item | Value |
|------|-------|
| **Endpoint Name** | Delete Resume Group |
| **Endpoint Path** | `DELETE /api/v1/resumes/groups/{groupId}` |
| **Authentication Required** | Yes |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `groupId` | String (UUID) | Yes | Resume group unique identifier |

#### Request Example (cURL)

```bash
curl -X DELETE http://localhost:8080/api/v1/resumes/groups/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <your_access_token>"
```

### Response Structure

#### Success Response (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

### Error Responses

#### 400 - Group Does Not Exist

```json
{
  "code": 400,
  "message": "group.not.found",
  "data": null
}
```

#### 401 - Not Authenticated

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - No Permission to Access

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 6. Get Resume Group Version List

### Basic Information

| Item | Value |
|------|-------|
| **Endpoint Name** | Get Resume Group Version List |
| **Endpoint Path** | `GET /api/v1/resumes/groups/{groupId}/versions` |
| **Authentication Required** | Yes |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `groupId` | String (UUID) | Yes | Resume group unique identifier |

#### Request Example (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/groups/550e8400-e29b-41d4-a716-446655440000/versions \
  -H "Authorization: Bearer <your_access_token>"
```

### Response Structure

#### Success Response (200)

Returns a detailed list of all versions under this group.

**ResumeVersionResponse Structure:**

| Field | Type | Description |
|-------|------|-------------|
| `versionId` | String (UUID) | Version ID |
| `groupId` | String (UUID) | Belonging group ID |
| `versionType` | String | Version type: ORIGINAL, CONVERTED, AI_OPTIMIZED |
| `status` | String | Status: PENDING, PROCESSING, COMPLETED, FAILED |
| `originalFileName` | String | Original filename |
| `fileType` | String | File type |
| `fileSize` | Number | File size (bytes) |
| `content` | String | Content (text format, Markdown) |
| `editable` | Boolean | Whether editable |
| `createdAt` | String (ISO 8601) | Creation time |
| `updatedAt` | String (ISO 8601) | Update time |

#### Response Example

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

### Error Responses

#### 400 - Group Does Not Exist

```json
{
  "code": 400,
  "message": "group.not.found",
  "data": null
}
```

#### 401 - Not Authenticated

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - No Permission to Access

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 7. Get Single Version Details

### Basic Information

| Item | Value |
|------|-------|
| **Endpoint Name** | Get Single Version Details |
| **Endpoint Path** | `GET /api/v1/resumes/versions/{versionId}` |
| **Authentication Required** | Yes |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `versionId` | String (UUID) | Yes | Version unique identifier |

#### Request Example (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/versions/550e8400-e29b-41d4-a716-446655440001 \
  -H "Authorization: Bearer <your_access_token>"
```

### Response Structure

#### Success Response (200)

| Field | Type | Description |
|-------|------|-------------|
| `versionId` | String (UUID) | Version ID |
| `groupId` | String (UUID) | Belonging group ID |
| `versionType` | String | Version type: ORIGINAL, CONVERTED, AI_OPTIMIZED |
| `status` | String | Status: PENDING, PROCESSING, COMPLETED, FAILED |
| `originalFileName` | String | Original filename |
| `fileType` | String | File type |
| `fileSize` | Number | File size (bytes) |
| `content` | String | Content (text format, Markdown) |
| `editable` | Boolean | Whether editable |
| `createdAt` | String (ISO 8601) | Creation time |
| `updatedAt` | String (ISO 8601) | Update time |

#### Response Example

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

### Error Responses

#### 400 - Version Does Not Exist

```json
{
  "code": 400,
  "message": "version.not.found",
  "data": null
}
```

#### 401 - Not Authenticated

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - No Permission to Access

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 8. Delete Resume Version

### Basic Information

| Item | Value |
|------|-------|
| **Endpoint Name** | Delete Resume Version |
| **Endpoint Path** | `DELETE /api/v1/resumes/versions/{versionId}` |
| **Authentication Required** | Yes |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `versionId` | String (UUID) | Yes | Version unique identifier |

**Notes**:
- Only `CONVERTED` or `AI_OPTIMIZED` type versions can be deleted
- `ORIGINAL` type versions cannot be deleted individually; must delete the entire resume group

#### Request Example (cURL)

```bash
curl -X DELETE http://localhost:8080/api/v1/resumes/versions/550e8400-e29b-41d4-a716-446655440002 \
  -H "Authorization: Bearer <your_access_token>"
```

### Response Structure

#### Success Response (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

### Error Responses

#### 400 - Version Does Not Exist

```json
{
  "code": 400,
  "message": "version.not.found",
  "data": null
}
```

#### 400 - Original Version Cannot Be Deleted Individually

```json
{
  "code": 400,
  "message": "version.original.cannot.delete",
  "data": null
}
```

#### 401 - Not Authenticated

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - No Permission to Access

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 9. Edit Version Content

### Basic Information

| Item | Value |
|------|-------|
| **Endpoint Name** | Edit Version Content |
| **Endpoint Path** | `PUT /api/v1/resumes/versions/{versionId}` |
| **Authentication Required** | Yes |
| **Content-Type** | `application/json` |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `versionId` | String (UUID) | Yes | Version unique identifier |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `content` | String | Yes | Resume content (Markdown format) |

**Note**: The `versionId` in the request body will be ignored; the path parameter takes precedence.

**Auto Vector Re-generation**: When editing a `CONVERTED` or `AI_OPTIMIZED` version, the system automatically triggers an asynchronous vector re-generation request after the content is persisted. This ensures that subsequent job-matching recall uses the latest resume embedding. If the MQ message fails to publish, the edit still succeeds; the vector will be re-synced on the next available opportunity.

#### Request Example (cURL)

```bash
curl -X PUT http://localhost:8080/api/v1/resumes/versions/550e8400-e29b-41d4-a716-446655440002 \
  -H "Authorization: Bearer <your_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "# Updated Resume\n\n## Experience\nSoftware Engineer at XYZ Corp..."
  }'
```

### Response Structure

#### Success Response (200)

Returns updated version details; structure is the same as Get Single Version Details.

#### Response Example

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

### Error Responses

#### 400 - Parameter Validation Failed

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": {
    "content": "Content is required"
  }
}
```

#### 400 - Version Does Not Exist

```json
{
  "code": 400,
  "message": "version.not.found",
  "data": null
}
```

#### 401 - Not Authenticated

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - No Permission to Access

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 10. Create Version Copy

### Basic Information

| Item | Value |
|------|-------|
| **Endpoint Name** | Create Version Copy |
| **Endpoint Path** | `POST /api/v1/resumes/groups/{groupId}/versions` |
| **Authentication Required** | Yes |
| **Content-Type** | `application/json` |

### Request Structure

#### Path Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `groupId` | String (UUID) | Yes | Resume group unique identifier |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sourceVersionId` | String (UUID) | No | Source version ID; if empty, copies from current ACTIVE CONVERTED version |

**Version Chain Limit**: Each resume group maintains a maximum of 50 CONVERTED versions (including ARCHIVED). When the limit is exceeded, the oldest ARCHIVED version is automatically deleted before the new copy is created.

**Auto-archive**: When a new CONVERTED version is created, the existing ACTIVE CONVERTED version of the same group is automatically archived.

#### Request Example (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/resumes/groups/550e8400-e29b-41d4-a716-446655440000/versions \
  -H "Authorization: Bearer <your_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceVersionId": "550e8400-e29b-41d4-a716-446655440002"
  }'
```

### Response Structure

#### Success Response (200)

Returns the newly created version details; structure is the same as Get Single Version Details.

#### Response Example

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "versionId": "550e8400-e29b-41d4-a716-446655440003",
    "groupId": "550e8400-e29b-41d4-a716-446655440000",
    "versionType": "CONVERTED",
    "status": "ACTIVE",
    "originalFileName": null,
    "fileType": "text/markdown",
    "fileSize": 0,
    "content": "# Resume Content\n\nCopied from source version...",
    "editable": true,
    "createdAt": "2024-01-15T10:40:00",
    "updatedAt": "2024-01-15T10:40:00"
  }
}
```

### Error Responses

#### 400 - Group Does Not Exist

```json
{
  "code": 400,
  "message": "group.not.found",
  "data": null
}
```

#### 400 - Source Version Mismatch

```json
{
  "code": 400,
  "message": "version.group.mismatch",
  "data": null
}
```

#### 401 - Not Authenticated

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - No Permission to Access

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 11. Facade Interface Methods

ResumeFacade defines the complete resume management interface; below is the list of interface methods:

| Method | Description | Implementation Status |
|--------|-------------|----------------------|
| `uploadResume` | Upload resume | Implemented |
| `downloadResume` | Download resume | Implemented |
| `getResumeGroups` | Get all resume groups for user | Implemented |
| `getResumeGroup` | Get single resume group details | Implemented |
| `deleteResumeGroup` | Delete resume group | Implemented |
| `getVersionsByGroup` | Get all versions under resume group | Implemented |
| `getVersion` | Get single version details | Implemented |
| `deleteVersion` | Delete resume version | Implemented |
| `editVersion` | Edit version content | Implemented |
| `createVersion` | Create version copy | Implemented |
| `setDefaultGroup` | Set default resume group | Not implemented in MVP |
| `createAiVersion` | Create AI-optimized version | Not implemented in MVP |
| `rollbackToVersion` | Rollback to specified version | Not implemented in MVP |

---

## DTO Definitions

### ResumeUploadRequest (Upload Request)

```java
{
  "file": MultipartFile,  // Required, resume file
  "title": String         // Optional, resume title
}
```

### ResumeUploadResponse (Upload Response)

```java
{
  "groupId": UUID,              // Resume group ID
  "originalVersionId": UUID,    // Original version ID (can be used directly for download)
  "title": String,              // Resume title
  "createdAt": LocalDateTime    // Creation time
}
```

### ResumeEditRequest (Edit Request)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `versionId` | UUID | Yes | Version ID (overridden by path parameter) |
| `content` | String | Yes | Resume content (Markdown format) |

```java
{
  "versionId": UUID,   // Required, version ID (overridden by path parameter)
  "content": String    // Required, resume content (Markdown)
}
```

### ResumeGroupResponse (Resume Group Response)

```java
{
  "groupId": UUID,              // Group ID
  "title": String,              // Title
  "isDefault": boolean,         // Whether it is the default group
  "createdAt": LocalDateTime,   // Creation time
  "updatedAt": LocalDateTime,   // Update time
  "originalVersion": VersionSummary,    // Original version
  "convertedVersion": VersionSummary,   // Converted version
  "aiOptimizedVersion": VersionSummary  // AI-optimized version
}
```

### ResumeVersionResponse (Resume Version Response)

```java
{
  "versionId": UUID,          // Version ID
  "groupId": UUID,            // Belonging group ID
  "versionType": String,      // Version type: ORIGINAL, CONVERTED, AI_OPTIMIZED
  "status": String,           // Status: PENDING, PROCESSING, COMPLETED, FAILED
  "originalFileName": String, // Original filename
  "fileType": String,         // File type
  "fileSize": long,           // File size
  "content": String,          // Content (text format)
  "editable": boolean,        // Whether editable
  "createdAt": LocalDateTime, // Creation time
  "updatedAt": LocalDateTime  // Update time
}
```

---

## Resume Status Description

| Status Value | Description |
|--------------|-------------|
| `PENDING` | Pending (just uploaded) |
| `PROCESSING` | Parsing |
| `COMPLETED` | Parsing completed |
| `FAILED` | Parsing failed |

## Version Type Description

| Type Value | Description |
|------------|-------------|
| `ORIGINAL` | Original uploaded version |
| `CONVERTED` | Format-converted version (converted to Markdown) |
| `AI_OPTIMIZED` | AI-optimized version |

---

## Endpoint Summary

| Endpoint | Method | Path | Description | Auth |
|----------|--------|------|-------------|------|
| Upload Resume | POST | `/api/v1/resumes` | Upload resume file | Yes |
| Download Resume | GET | `/api/v1/resumes/{versionId}/download` | Download resume file | Yes |
| Get All Groups | GET | `/api/v1/resumes/groups` | Get all resume groups for user | Yes |
| Get Group Details | GET | `/api/v1/resumes/groups/{groupId}` | Get resume group details | Yes |
| Delete Group | DELETE | `/api/v1/resumes/groups/{groupId}` | Delete resume group | Yes |
| Get Version List | GET | `/api/v1/resumes/groups/{groupId}/versions` | Get all versions in group | Yes |
| Get Version Details | GET | `/api/v1/resumes/versions/{versionId}` | Get single version details | Yes |
| Delete Version | DELETE | `/api/v1/resumes/versions/{versionId}` | Delete resume version | Yes |
| Edit Version | PUT | `/api/v1/resumes/versions/{versionId}` | Edit version content | Yes |

---

## Usage Flow Examples

### Simple Flow: Upload and Download Directly

```bash
# 1. Login to get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.data.accessToken')

# 2. Upload resume
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/resumes \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@resume.pdf" \
  -F "title=My Resume")
  
ORIGINAL_VERSION_ID=$(echo $RESPONSE | jq -r '.data.originalVersionId')
echo "Original Version ID: $ORIGINAL_VERSION_ID"

# 3. Download directly using originalVersionId
curl -X GET "http://localhost:8080/api/v1/resumes/$ORIGINAL_VERSION_ID/download" \
  -H "Authorization: Bearer $TOKEN" \
  --output downloaded_resume.pdf
```

### Complete Flow: Edit Then Download

```bash
# 1. Login to get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.data.accessToken')

# 2. Upload resume
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/resumes \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@resume.pdf" \
  -F "title=My Resume")
  
GROUP_ID=$(echo $RESPONSE | jq -r '.data.groupId')
echo "Group ID: $GROUP_ID"

# 3. Get group details
GROUP_INFO=$(curl -s "http://localhost:8080/api/v1/resumes/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN")

# 4. Extract converted version ID (editable Markdown version)
CONVERTED_VERSION_ID=$(echo $GROUP_INFO | jq -r '.data.convertedVersion.versionId')
echo "Converted Version ID: $CONVERTED_VERSION_ID"

# 5. Get version details to view current content
VERSION_DETAIL=$(curl -s "http://localhost:8080/api/v1/resumes/versions/$CONVERTED_VERSION_ID" \
  -H "Authorization: Bearer $TOKEN")
echo "Current Content: $(echo $VERSION_DETAIL | jq -r '.data.content')"

# 6. Edit version content
curl -X PUT "http://localhost:8080/api/v1/resumes/versions/$CONVERTED_VERSION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "# Updated Resume\n\n## Experience\nSoftware Engineer..."
  }'

# 7. Download using version ID
curl -X GET "http://localhost:8080/api/v1/resumes/$CONVERTED_VERSION_ID/download" \
  -H "Authorization: Bearer $TOKEN" \
  --output converted_resume.md

# 8. Delete resume group (cleanup)
curl -X DELETE "http://localhost:8080/api/v1/resumes/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN"
```

---

## FAQ

### Q: Download shows "version.not.found" after successful upload?

**A**: Please use the `originalVersionId` returned by the upload endpoint for download, not `groupId`.

### Q: How to get the version ID?

**A**:
1. **Recommended**: Use the `originalVersionId` returned by the upload endpoint directly
2. Call `GET /api/v1/resumes/groups/{groupId}` to get group details, which includes summary info for each version
3. Call `GET /api/v1/resumes/groups/{groupId}/versions` to get all versions under this group
4. Call `GET /api/v1/resumes/versions/{versionId}` to get single version details

### Q: Which versions can be edited?

**A**:
- `CONVERTED` (converted) and `AI_OPTIMIZED` (AI-optimized) versions can be edited
- `ORIGINAL` (original) is not editable, protecting the originally uploaded file
- Can determine editability via the `editable` field

---

*Document version: 1.3.0*
