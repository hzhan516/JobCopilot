<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](README.md) | [简体中文](../../i18n/zh-Hans-CN/api/backend/README.md) | [繁體中文](../../i18n/zh-Hant-TW/api/backend/README.md)

# Backend API Documentation

This document contains the complete interface definitions for the JobCopilot backend API.

## Basic Information

| Item | Description |
|------|-------------|
| Base Path | `/api` |
| API Version | `v1` |
| Content-Type | `application/json` (except for file uploads) |
| Authentication | JWT Bearer Token |

## Authentication

The API uses JWT (JSON Web Token) for authentication. Except for endpoints under `/api/v1/auth/**`, all other endpoints require a valid access token in the request header:

```
Authorization: Bearer <access_token>
```

## Global Response Format

All API responses follow a unified format:

```json
{
  "code": 200,
  "message": "Success",
  "data": {}
}
```

### Response Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `code` | Integer | Status code; 200 indicates success, others indicate errors |
| `message` | String | Response message |
| `data` | Object | Response data; contains specific data on success, and may contain detailed error information on failure |

## Error Handling

### Error Response Format

```json
{
  "code": 400,
  "message": "Error description",
  "data": null
}
```

### Common Error Codes

| Status Code | Description |
|-------------|-------------|
| 400 | Request parameter error |
| 401 | Not authenticated or authentication failed |
| 403 | Insufficient permissions |
| 404 | Resource not found |
| 409 | Resource conflict (e.g., email already exists) |
| 500 | Internal server error |

---

## API Endpoint List

### 1. Authentication Module (Auth)

#### 1.1 Email Registration

- **URL**: `POST /api/v1/auth/register/email`
- **Authentication**: Not required
- **Content-Type**: `application/json`

**Request Parameters**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | String | Yes | Email address; must conform to email format |
| `password` | String | Yes | Password; 6-32 characters |
| `verificationCode` | String | No | 6-digit code; required when email verification is enabled |
| `captchaToken` | String | Yes | CAPTCHA verification token from `/v1/auth/captcha/verify` |

**Request Example**:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "verificationCode": "123456",
  "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Success Response** (201):

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

**Response Field Descriptions**:

| Field | Type | Description |
|-------|------|-------------|
| `userId` | UUID | User unique identifier |
| `email` | String | User email |
| `accessToken` | String | Access token |
| `refreshToken` | String | Refresh token |
| `expiresIn` | Long | Access token validity period (seconds) |

---

#### 1.2 Email Login

- **URL**: `POST /api/v1/auth/login/email`
- **Authentication**: Not required
- **Content-Type**: `application/json`

**Request Parameters**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | String | Yes | Email address |
| `password` | String | Yes | Password |
| `captchaToken` | String | Yes | CAPTCHA verification token |

**Request Example**:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "captchaToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Success Response** (200):

Same response format as email registration.

---

### 2. Resume Module (Resume)

#### 2.1 Upload Resume

- **URL**: `POST /api/v1/resumes`
- **Authentication**: Required
- **Content-Type**: `multipart/form-data`

**Request Parameters**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | Resume file (supports PDF, DOC, DOCX, etc.) |
| `title` | String | No | Resume title; uses filename if not provided |

**Request Example**:

```http
POST /api/v1/resumes
Authorization: Bearer <access_token>
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="resume.pdf"
Content-Type: application/pdf

<file content>
------WebKitFormBoundary
Content-Disposition: form-data; name="title"

My Resume
------WebKitFormBoundary--
```

**Success Response** (200):

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

**Response Field Descriptions**:

| Field | Type | Description |
|-------|------|-------------|
| `groupId` | UUID | Resume group ID |
| `originalVersionId` | UUID | Original version ID (can be used directly for download) |
| `title` | String | Resume title |
| `createdAt` | LocalDateTime | Creation time |

---

#### 2.2 Download Resume

- **URL**: `GET /api/v1/resumes/{versionId}/download`
- **Authentication**: Required
- **Content-Type**: Returned based on file type

**Path Parameters**:

| Field | Type | Description |
|-------|------|-------------|
| `versionId` | UUID | Version ID (originalVersionId returned by upload) |

**Query Parameters**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `format` | String | No | Export format: `original` (default), `pdf`, `docx`, `html`, `md`, `txt` |

**Request Example**:

```http
GET /api/v1/resumes/550e8400-e29b-41d4-a716-446655440000/download?format=pdf
Authorization: Bearer <access_token>
```

**Success Response** (200):

Returns file stream; Content-Disposition header contains the filename.

**Note**: The current version only supports returning the original file; format conversion will be implemented in a future version.

#### 2.3 Get All Resume Groups for User

- **URL**: `GET /api/v1/resumes/groups`
- **Authentication**: Required
- **Content-Type**: `application/json`

**Success Response** (200):

Returns a list of all resume groups for the current user.

#### 2.4 Get Resume Group Details

- **URL**: `GET /api/v1/resumes/groups/{groupId}`
- **Authentication**: Required
- **Content-Type**: `application/json`

**Path Parameters**:

| Field | Type | Description |
|-------|------|-------------|
| `groupId` | UUID | Resume group unique identifier |

**Success Response** (200):

Returns resume group details, including summary information for each version.

#### 2.5 Delete Resume Group

- **URL**: `DELETE /api/v1/resumes/groups/{groupId}`
- **Authentication**: Required
- **Content-Type**: `application/json`

**Path Parameters**:

| Field | Type | Description |
|-------|------|-------------|
| `groupId` | UUID | Resume group unique identifier |

**Success Response** (200):

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

#### 2.6 Get Resume Group Version List

- **URL**: `GET /api/v1/resumes/groups/{groupId}/versions`
- **Authentication**: Required
- **Content-Type**: `application/json`

**Path Parameters**:

| Field | Type | Description |
|-------|------|-------------|
| `groupId` | UUID | Resume group unique identifier |

**Success Response** (200):

Returns a detailed list of all versions under this group.

#### 2.7 Delete Resume Version

- **URL**: `DELETE /api/v1/resumes/versions/{versionId}`
- **Authentication**: Required
- **Content-Type**: `application/json`

**Path Parameters**:

| Field | Type | Description |
|-------|------|-------------|
| `versionId` | UUID | Version unique identifier |

**Success Response** (200):

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

### 3. Job Module (Job)

See [job.md](job.md) and [job-matching.md](job-matching.md)

This module provides job link submission, async parsing, intelligent matching, and history query functions.

#### 3.1 Submit Job Link
- **URL**: `POST /api/v1/jobs`
- **Authentication**: Required

#### 3.2 Get Job Details
- **URL**: `GET /api/v1/jobs/{jobId}`
- **Authentication**: Required

#### 3.3 Get Job List
- **URL**: `GET /api/v1/jobs`
- **Authentication**: Required

#### 3.4 Start Job Matching
- **URL**: `POST /api/v1/jobs/match`
- **Authentication**: Required

#### 3.5 Query Match Results
- **URL**: `GET /api/v1/jobs/match/{matchId}`
- **Authentication**: Required

#### 3.6 Get Match History
- **URL**: `GET /api/v1/jobs/match/history`
- **Authentication**: Required

#### 3.7 Vector Search Jobs
- **URL**: `POST /api/v1/jobs/vector-search`
- **Authentication**: Required

---

### 6. Embedding Module (Embedding)

See [embedding.md](embedding.md)

This module provides batch vector data ingestion capabilities for the AI layer.

#### 6.1 Batch Upsert Job Vectors
- **URL**: `POST /api/v1/job-vectors/batch`
- **Authentication**: Not required (intended for internal AI service use)

---

### 4. Conversation Module (Conversation)

See [conversation.md](conversation.md)

#### 4.1 Create Conversation
- **URL**: `POST /api/v1/conversations`
- **Authentication**: Required

#### 4.2 Send Message
- **URL**: `POST /api/v1/conversations/{conversationId}/messages`
- **Authentication**: Required

#### 4.3 Get Conversation Details
- **URL**: `GET /api/v1/conversations/{conversationId}`
- **Authentication**: Required
- **Pagination**: Supports `?page=0&size=20` for paginating the message list

#### 4.4 Get Conversation List
- **URL**: `GET /api/v1/conversations`
- **Authentication**: Required

#### 4.5 Close Conversation
- **URL**: `PUT /api/v1/conversations/{conversationId}/close`
- **Authentication**: Required

#### 4.6 Delete Conversation
- **URL**: `DELETE /api/v1/conversations/{conversationId}`
- **Authentication**: Required

#### 4.7 Upload Attachment
- **URL**: `POST /api/v1/conversations/{conversationId}/files`
- **Authentication**: Required
- **Content-Type**: `multipart/form-data`

---

### 5. Job Tracking Module (Tracking)

See [tracking.md](tracking.md)

This module provides application status flow, event recording, and statistical analysis functions.

#### 5.1 Create Tracking Record
- **URL**: `POST /api/v1/trackings`
- **Authentication**: Required

#### 5.2 Get Tracking List
- **URL**: `GET /api/v1/trackings?status=INTERVIEWING`
- **Authentication**: Required

#### 5.3 Get Tracking Details
- **URL**: `GET /api/v1/trackings/{id}`
- **Authentication**: Required

#### 5.4 Update Tracking Record
- **URL**: `PUT /api/v1/trackings/{id}`
- **Authentication**: Required

#### 5.5 Delete Tracking Record
- **URL**: `DELETE /api/v1/trackings/{id}`
- **Authentication**: Required

#### 5.6 Get Statistics
- **URL**: `GET /api/v1/trackings/stats`
- **Authentication**: Required

---

### Endpoint Summary

| Endpoint | Method | Path | Description | Auth |
|----------|--------|------|-------------|------|
| Email Registration | POST | `/api/v1/auth/register/email` | User email registration | No |
| Email Login | POST | `/api/v1/auth/login/email` | User email login | No |
| Google Login | POST | `/api/v1/auth/login/google` | Google OAuth login | No |
| Send Verification Code | POST | `/api/v1/auth/send-verification-code` | Send email verification code | No |
| Check Verification Enabled | GET | `/api/v1/auth/email-verification-enabled` | Check if email verification is enabled | No |
| Get CAPTCHA Challenge | GET | `/api/v1/auth/captcha` | Get slider CAPTCHA challenge | No |
| Verify CAPTCHA | POST | `/api/v1/auth/captcha/verify` | Verify drag result and issue token | No |
| Upload Resume | POST | `/api/v1/resumes` | Upload resume file | Yes |
| Download Resume | GET | `/api/v1/resumes/{versionId}/download` | Download resume file (supports format conversion) | Yes |
| Get All Groups | GET | `/api/v1/resumes/groups` | Get all resume groups for user | Yes |
| Get Group Details | GET | `/api/v1/resumes/groups/{groupId}` | Get resume group details | Yes |
| Delete Group | DELETE | `/api/v1/resumes/groups/{groupId}` | Delete resume group | Yes |
| Get Version List | GET | `/api/v1/resumes/groups/{groupId}/versions` | Get all versions in group | Yes |
| Get Version Details | GET | `/api/v1/resumes/versions/{versionId}` | Get single version details | Yes |
| Delete Version | DELETE | `/api/v1/resumes/versions/{versionId}` | Delete resume version | Yes |
| Edit Version | PUT | `/api/v1/resumes/versions/{versionId}` | Edit version content | Yes |
| Submit Job | POST | `/api/v1/jobs` | Submit job link for async parsing | Yes |
| Get Job Details | GET | `/api/v1/jobs/{jobId}` | Get job parsing status | Yes |
| Get Job List | GET | `/api/v1/jobs` | Get all jobs for user | Yes |
| Start Job Matching | POST | `/api/v1/jobs/match` | Start async job matching | Yes |
| Query Match Results | GET | `/api/v1/jobs/match/{matchId}` | Query match task results | Yes |
| Get Match History | GET | `/api/v1/jobs/match/history` | Get historical match records | Yes |
| Vector Search Jobs | POST | `/api/v1/jobs/vector-search` | ANN vector search for jobs | Yes |
| Score Job | POST | `/api/v1/jobs/{jobId}/score` | Score a job against a resume | Yes |
| Get Job Dataset | GET | `/api/v1/job-dataset` | Query training dataset (internal) | No |
| Batch Upsert Job Vectors | POST | `/api/v1/job-vectors/batch` | Batch upsert job vectors (AI layer) | No |
| Create Conversation | POST | `/api/v1/conversations` | Create new conversation | Yes |
| Send Message | POST | `/api/v1/conversations/{conversationId}/messages` | Send conversation message | Yes |
| Get Conversation | GET | `/api/v1/conversations/{conversationId}` | Get conversation details (supports message pagination) | Yes |
| Get Conversation List | GET | `/api/v1/conversations` | Get all conversations | Yes |
| Close Conversation | PUT | `/api/v1/conversations/{conversationId}/close` | Close conversation | Yes |
| Delete Conversation | DELETE | `/api/v1/conversations/{conversationId}` | Delete conversation | Yes |
| Upload Attachment | POST | `/api/v1/conversations/{conversationId}/files` | Upload conversation attachment | Yes |
| Create Tracking | POST | `/api/v1/trackings` | Create job tracking record | Yes |
| Get Tracking List | GET | `/api/v1/trackings` | Get tracking record list | Yes |
| Get Tracking Details | GET | `/api/v1/trackings/{id}` | Get tracking details | Yes |
| Update Tracking | PUT | `/api/v1/trackings/{id}` | Update tracking (includes status flow) | Yes |
| Delete Tracking | DELETE | `/api/v1/trackings/{id}` | Delete tracking record | Yes |
| Get Statistics | GET | `/api/v1/trackings/stats` | Get tracking statistics | Yes |

---

## DTO Detailed Definitions

### Request DTOs

#### RegisterByEmailRequest (Email Registration Request)

```java
{
  "email": String,              // Required, email format
  "password": String,           // Required, 6-32 characters
  "verificationCode": String,   // Optional, 6 digits; required when email verification is enabled
  "captchaToken": String        // Required, obtained from /v1/auth/captcha/verify
}
```

#### SendVerificationCodeRequest (Send Verification Code Request)

```java
{
  "email": String,         // Required, email format
  "captchaToken": String   // Required, CAPTCHA verification token (peeked, not consumed)
}
```

#### LoginByEmailRequest (Email Login Request)

```java
{
  "email": String,         // Required, email format
  "password": String,      // Required
  "captchaToken": String   // Required, CAPTCHA verification token
}
```

#### ResumeUploadRequest (Resume Upload Request)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | MultipartFile | Yes | Resume file (PDF/DOCX/MD/TXT) |
| `title` | String | No | Resume title; uses filename if not provided |

#### ResumeEditRequest (Resume Edit Request)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `versionId` | UUID | Yes | Version ID (consistent with path parameter) |
| `content` | String | Yes | Resume content (Markdown format) |

```java
{
  "versionId": UUID,   // Required, version ID
  "content": String    // Required, resume content
}
```

#### VectorSearchRequest (Vector Search Request)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `queryText` | String | No | Query text for embedding generation |
| `queryEmbedding` | List<Float> | No | Pre-computed embedding vector |
| `limit` | Integer | No | Max results (default: 10, max: 100) |
| `filters` | Map<String, String> | No | Filter conditions (reserved) |

```java
{
  "queryText": String,           // Optional, used when queryEmbedding is absent
  "queryEmbedding": List<Float>, // Optional, takes precedence over queryText
  "limit": Integer,              // Optional, default 10
  "filters": Map<String, String> // Optional
}
```

#### BatchJobVectorUpsertRequest (Batch Job Vector Upsert Request)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `items` | List<JobVectorItem> | Yes | List of job vectors to upsert |

**JobVectorItem:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `jobId` | String | Yes | Unique job identifier |
| `embedding` | List<Float> | Yes | Embedding vector |
| `title` | String | No | Job title |
| `description` | String | No | Job description |
| `requirements` | List<String> | No | Job requirements |
| `rawContent` | String | No | Raw text content |
| `sourceFile` | String | No | Source file identifier |
| `modelVersion` | String | No | Model version (default: `gemini-embedding-001`) |

```java
{
  "items": [
    {
      "jobId": String,           // Required
      "embedding": List<Float>,  // Required
      "title": String,           // Optional
      "description": String,     // Optional
      "requirements": List<String>, // Optional
      "rawContent": String,      // Optional
      "sourceFile": String,      // Optional
      "modelVersion": String     // Optional
    }
  ]
}
```

### Response DTOs

#### AuthResponse (Authentication Response)

```java
{
  "userId": UUID,        // User ID
  "email": String,       // User email
  "accessToken": String, // Access token
  "refreshToken": String,// Refresh token
  "expiresIn": Long      // Validity period (seconds)
}
```

#### ResumeUploadResponse (Resume Upload Response)

```java
{
  "groupId": UUID,              // Resume group ID
  "originalVersionId": UUID,    // Original version ID (can be used directly for download)
  "title": String,              // Resume title
  "createdAt": LocalDateTime    // Creation time
}
```

#### ResumeGroupResponse (Resume Group Response)

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

**VersionSummary**:

```java
{
  "versionId": UUID,        // Version ID
  "status": String,         // Status
  "createdAt": LocalDateTime,// Creation time
  "exists": boolean         // Whether it exists
}
```

#### ResumeVersionResponse (Resume Version Response)

```java
{
  "versionId": UUID,          // Version ID
  "groupId": UUID,            // Belonging group ID
  "versionType": String,      // Version type
  "status": String,           // Status
  "originalFileName": String, // Original filename
  "fileType": String,         // File type
  "fileSize": long,           // File size
  "content": String,          // Content
  "editable": boolean,        // Whether editable
  "createdAt": LocalDateTime, // Creation time
  "updatedAt": LocalDateTime  // Update time
}
```

#### ApiResponse<T> (Generic Response Wrapper)

```java
{
  "code": Integer,    // Status code
  "message": String,  // Message
  "data": T           // Data
}
```

#### VectorSearchResponse (Vector Search Response)

```java
{
  "jobId": String,              // Job ID
  "title": String,              // Job title
  "company": String,            // Company name
  "description": String,        // Job description
  "requirements": List<String>, // Requirements list
  "similarity": Float,          // Similarity score (0-1)
  "matchFactors": Map<String, Object> // Match factors
}
```

#### BatchJobVectorUpsertResponse (Batch Upsert Response)

```java
{
  "total": Integer,        // Total items received
  "success": Integer,      // Successfully persisted count
  "failed": Integer,       // Failed count
  "failedJobIds": List<String> // Failed job IDs
}
```

---

## Validation Rules

### Email Registration/Login

| Field | Rule |
|-------|------|
| `email` | Required, must conform to email format |
| `password` | Required, length 6-32 characters (for registration) |
| `verificationCode` | Optional when disabled; Required (6 digits) when email verification is enabled |
| `captchaToken` | Required for all auth endpoints (register, login, Google login, send verification code) |

### Resume Upload

| Field | Rule |
|-------|------|
| `file` | Required, must be a valid file |

### Resume Edit

| Field | Rule |
|-------|------|
| `versionId` | Required |
| `content` | Required, cannot be empty |

---

## Internationalization Support

The API supports internationalized responses; specify the language via the `Accept-Language` request header:

- `zh-CN` - Simplified Chinese (default)
- `en` - English

Example:

```http
Accept-Language: en
```

---

## Related Documents

- [Authentication Module Detailed Documentation](authentication.md)
- [Resume Module Detailed Documentation](resume.md)
- [Job Module Detailed Documentation](job.md)
- [Job Matching Module Detailed Documentation](job-matching.md)
- [Conversation Module Detailed Documentation](conversation.md)
- [Job Tracking Module Detailed Documentation](tracking.md)
- [Embedding Module Detailed Documentation](embedding.md)
- [AI / MQ Interaction Interface Documentation](ai-mq-interfaces.md)
- [Response Format and Error Code Description](response-format.md)

---

## Notes

1. All time fields use ISO 8601 format (e.g., `2024-01-15T10:30:00`)
2. UUID format is the standard 36-character UUID string (e.g., `550e8400-e29b-41d4-a716-446655440000`)
3. File upload size limits refer to the specific deployment configuration
4. Currently implemented endpoints are listed; other methods defined in the Facade will be implemented in subsequent versions
