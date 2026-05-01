<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/resume.md) | [简体中文](../../../zh-Hans-CN/api/backend/resume.md) | [繁體中文](resume.md)

# 履歷管理 API

> 履歷上傳、下載和管理相關介面

---

## 目錄

1. [上傳履歷](#1-上傳履歷)
2. [下載履歷](#2-下載履歷)
3. [取得使用者所有履歷組](#3-取得使用者所有履歷組)
4. [取得履歷組詳情](#4-取得履歷組詳情)
5. [刪除履歷組](#5-刪除履歷組)
6. [取得履歷組版本清單](#6-取得履歷組版本清單)
7. [取得單個版本詳情](#7-取得單個版本詳情)
8. [刪除履歷版本](#8-刪除履歷版本)
9. [編輯版本內容](#9-編輯版本內容)
10. [Facade 介面方法](#10-facade-介面方法)

---

## 1. 上傳履歷

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 上傳履歷 |
| **介面路徑** | `POST /api/v1/resumes` |
| **是否需要認證** | 是 |
| **Content-Type** | `multipart/form-data` |

### 請求結構

#### Request Parameters

| 欄位 | 型別 | 必填 | 限制 | 說明 |
|------|------|------|------|------|
| `file` | File | 是 | 最大 10MB | 履歷檔案（PDF/DOCX/MD/TXT 格式） |
| `title` | String | 否 | - | 履歷標題，未傳送則使用檔案名稱 |

**支援的檔案型別**:
- `application/pdf` - PDF 檔案
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document` - DOCX 檔案
- `text/markdown` - Markdown 檔案
- `text/plain` - 純文字檔案

#### 請求範例 (cURL)

```bash
curl -X POST http://localhost:8080/api/v1/resumes \
  -H "Authorization: Bearer <your_access_token>" \
  -F "file=@/path/to/resume.pdf" \
  -F "title=My Resume"
```

### 回應結構

#### 成功回應 (200)

| 欄位 | 型別 | 說明 |
|------|------|------|
| `groupId` | String (UUID) | 履歷組唯一識別碼 |
| `originalVersionId` | String (UUID) | 原始版本 ID（可用於直接下載） |
| `title` | String | 履歷標題 |
| `createdAt` | String (ISO 8601) | 建立時間 |

#### 回應範例

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

### 錯誤回應

#### 400 - 檔案不能為空

```json
{
  "code": 400,
  "message": "File is required",
  "data": null
}
```

#### 400 - 檔案大小超過限制

```json
{
  "code": 400,
  "message": "File size exceeds limit",
  "data": null
}
```

#### 400 - 不支援的檔案型別

```json
{
  "code": 400,
  "message": "Invalid file type",
  "data": null
}
```

#### 401 - 未認證

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

---

## 2. 下載履歷

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 下載履歷 |
| **介面路徑** | `GET /api/v1/resumes/{versionId}/download` |
| **是否需要認證** | 是 |

### 請求結構

#### Path Parameters

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `versionId` | String (UUID) | 是 | 版本 ID（上傳回傳的 `originalVersionId`） |

#### Query Parameters

| 欄位 | 型別 | 必填 | 預設值 | 說明 |
|------|------|------|--------|------|
| `format` | String | 否 | `original` | 匯出格式：`original`（原始格式）、`pdf`、`docx`、`html`、`md`、`txt` |

**重要說明**: 
- `versionId` 參數是上傳介面回傳的 `originalVersionId`
- 不要使用 `groupId` 作為下載參數
- 目前版本僅支援回傳原始檔案，格式轉換功能將在後續版本實作

#### 請求範例 (cURL)

```bash
# 使用上傳回傳的 originalVersionId 直接下載
curl -X GET http://localhost:8080/api/v1/resumes/550e8400-e29b-41d4-a716-446655440001/download \
  -H "Authorization: Bearer <your_access_token>" \
  --output resume.pdf

# 指定匯出格式（目前回傳原始檔案，檔案名稱會修改）
curl -X GET "http://localhost:8080/api/v1/resumes/550e8400-e29b-41d4-a716-446655440001/download?format=pdf" \
  -H "Authorization: Bearer <your_access_token>" \
  --output resume.pdf
```

### 回應結構

#### 成功回應 (200)

回傳檔案串流，Content-Type 根據檔案型別自動設定（如 `application/pdf`）。

**回應 Headers:**

| Header | 說明 |
|--------|------|
| `Content-Type` | 檔案 MIME 型別 |
| `Content-Disposition` | 附件下載提示，包含檔案名稱 |
| `Content-Length` | 檔案大小 |

### 錯誤回應

#### 400 - 版本不存在或格式錯誤

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

#### 401 - 未認證

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 無權限存取

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 3. 取得使用者所有履歷組

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 取得使用者所有履歷組 |
| **介面路徑** | `GET /api/v1/resumes/groups` |
| **是否需要認證** | 是 |
| **Content-Type** | `application/json` |

### 請求結構

無需請求參數，透過目前登入使用者身份取得。

#### 請求範例 (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/groups \
  -H "Authorization: Bearer <your_access_token>"
```

### 回應結構

#### 成功回應 (200)

回傳目前使用者的所有履歷組清單。

| 欄位 | 型別 | 說明 |
|------|------|------|
| `groupId` | String (UUID) | 組 ID |
| `title` | String | 履歷標題 |
| `isDefault` | Boolean | 是否為預設履歷組 |
| `createdAt` | String (ISO 8601) | 建立時間 |
| `updatedAt` | String (ISO 8601) | 更新時間 |
| `originalVersion` | VersionSummary | 原始版本摘要 |
| `convertedVersion` | VersionSummary | 轉換版本摘要 |
| `aiOptimizedVersion` | VersionSummary | AI 最佳化版本摘要 |

**VersionSummary 結構**:

| 欄位 | 型別 | 說明 |
|------|------|------|
| `versionId` | String (UUID) | 版本 ID |
| `status` | String | 版本狀態：PENDING, PROCESSING, COMPLETED, FAILED |
| `createdAt` | String (ISO 8601) | 建立時間 |
| `exists` | Boolean | 該版本是否存在 |

#### 回應範例

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

### 錯誤回應

#### 401 - 未認證

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

---

## 4. 取得履歷組詳情

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 取得履歷組詳情 |
| **介面路徑** | `GET /api/v1/resumes/groups/{groupId}` |
| **是否需要認證** | 是 |

### 請求結構

#### Path Parameters

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `groupId` | String (UUID) | 是 | 履歷組唯一識別碼（上傳介面回傳的 groupId） |

#### 請求範例 (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/groups/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <your_access_token>"
```

### 回應結構

#### 成功回應 (200)

| 欄位 | 型別 | 說明 |
|------|------|------|
| `groupId` | String (UUID) | 組 ID |
| `title` | String | 履歷標題 |
| `isDefault` | Boolean | 是否為預設履歷組 |
| `createdAt` | String (ISO 8601) | 建立時間 |
| `updatedAt` | String (ISO 8601) | 更新時間 |
| `originalVersion` | VersionSummary | 原始版本摘要 |
| `convertedVersion` | VersionSummary | 轉換版本摘要 |
| `aiOptimizedVersion` | VersionSummary | AI 最佳化版本摘要 |

#### 回應範例

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

### 錯誤回應

#### 400 - 組不存在

```json
{
  "code": 400,
  "message": "group.not.found",
  "data": null
}
```

#### 401 - 未認證

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 無權限存取

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 5. 刪除履歷組

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 刪除履歷組 |
| **介面路徑** | `DELETE /api/v1/resumes/groups/{groupId}` |
| **是否需要認證** | 是 |

### 請求結構

#### Path Parameters

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `groupId` | String (UUID) | 是 | 履歷組唯一識別碼 |

#### 請求範例 (cURL)

```bash
curl -X DELETE http://localhost:8080/api/v1/resumes/groups/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <your_access_token>"
```

### 回應結構

#### 成功回應 (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

### 錯誤回應

#### 400 - 組不存在

```json
{
  "code": 400,
  "message": "group.not.found",
  "data": null
}
```

#### 401 - 未認證

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 無權限存取

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 6. 取得履歷組版本清單

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 取得履歷組版本清單 |
| **介面路徑** | `GET /api/v1/resumes/groups/{groupId}/versions` |
| **是否需要認證** | 是 |

### 請求結構

#### Path Parameters

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `groupId` | String (UUID) | 是 | 履歷組唯一識別碼 |

#### 請求範例 (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/groups/550e8400-e29b-41d4-a716-446655440000/versions \
  -H "Authorization: Bearer <your_access_token>"
```

### 回應結構

#### 成功回應 (200)

回傳該組下所有版本的詳細資訊清單。

**ResumeVersionResponse 結構**:

| 欄位 | 型別 | 說明 |
|------|------|------|
| `versionId` | String (UUID) | 版本 ID |
| `groupId` | String (UUID) | 所屬組 ID |
| `versionType` | String | 版本型別：ORIGINAL, CONVERTED, AI_OPTIMIZED |
| `status` | String | 狀態：PENDING, PROCESSING, COMPLETED, FAILED |
| `originalFileName` | String | 原始檔案名稱 |
| `fileType` | String | 檔案型別 |
| `fileSize` | Number | 檔案大小（位元組） |
| `content` | String | 內容（文字格式，Markdown） |
| `editable` | Boolean | 是否可編輯 |
| `createdAt` | String (ISO 8601) | 建立時間 |
| `updatedAt` | String (ISO 8601) | 更新時間 |

#### 回應範例

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

### 錯誤回應

#### 400 - 組不存在

```json
{
  "code": 400,
  "message": "group.not.found",
  "data": null
}
```

#### 401 - 未認證

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 無權限存取

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 7. 取得單個版本詳情

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 取得單個版本詳情 |
| **介面路徑** | `GET /api/v1/resumes/versions/{versionId}` |
| **是否需要認證** | 是 |

### 請求結構

#### Path Parameters

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `versionId` | String (UUID) | 是 | 版本唯一識別碼 |

#### 請求範例 (cURL)

```bash
curl -X GET http://localhost:8080/api/v1/resumes/versions/550e8400-e29b-41d4-a716-446655440001 \
  -H "Authorization: Bearer <your_access_token>"
```

### 回應結構

#### 成功回應 (200)

| 欄位 | 型別 | 說明 |
|------|------|------|
| `versionId` | String (UUID) | 版本 ID |
| `groupId` | String (UUID) | 所屬組 ID |
| `versionType` | String | 版本型別：ORIGINAL, CONVERTED, AI_OPTIMIZED |
| `status` | String | 狀態：PENDING, PROCESSING, COMPLETED, FAILED |
| `originalFileName` | String | 原始檔案名稱 |
| `fileType` | String | 檔案型別 |
| `fileSize` | Number | 檔案大小（位元組） |
| `content` | String | 內容（文字格式，Markdown） |
| `editable` | Boolean | 是否可編輯 |
| `createdAt` | String (ISO 8601) | 建立時間 |
| `updatedAt` | String (ISO 8601) | 更新時間 |

#### 回應範例

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

### 錯誤回應

#### 400 - 版本不存在

```json
{
  "code": 400,
  "message": "version.not.found",
  "data": null
}
```

#### 401 - 未認證

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 無權限存取

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 8. 刪除履歷版本

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 刪除履歷版本 |
| **介面路徑** | `DELETE /api/v1/resumes/versions/{versionId}` |
| **是否需要認證** | 是 |

### 請求結構

#### Path Parameters

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `versionId` | String (UUID) | 是 | 版本唯一識別碼 |

**注意**: 
- 只能刪除 `CONVERTED` 或 `AI_OPTIMIZED` 類型的版本
- `ORIGINAL` 類型版本不允許單獨刪除，必須透過刪除整個履歷組來刪除

#### 請求範例 (cURL)

```bash
curl -X DELETE http://localhost:8080/api/v1/resumes/versions/550e8400-e29b-41d4-a716-446655440002 \
  -H "Authorization: Bearer <your_access_token>"
```

### 回應結構

#### 成功回應 (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

### 錯誤回應

#### 400 - 版本不存在

```json
{
  "code": 400,
  "message": "version.not.found",
  "data": null
}
```

#### 400 - 原版不能單獨刪除

```json
{
  "code": 400,
  "message": "version.original.cannot.delete",
  "data": null
}
```

#### 401 - 未認證

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 無權限存取

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 9. 編輯版本內容

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 編輯版本內容 |
| **介面路徑** | `PUT /api/v1/resumes/versions/{versionId}` |
| **是否需要認證** | 是 |
| **Content-Type** | `application/json` |

### 請求結構

#### Path Parameters

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `versionId` | String (UUID) | 是 | 版本唯一識別碼 |

#### Request Body

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `content` | String | 是 | 履歷內容（Markdown 格式） |

**注意**: 請求主體中的 `versionId` 會被忽略，以路徑參數為準。

#### 請求範例 (cURL)

```bash
curl -X PUT http://localhost:8080/api/v1/resumes/versions/550e8400-e29b-41d4-a716-446655440002 \
  -H "Authorization: Bearer <your_access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "# Updated Resume\n\n## Experience\nSoftware Engineer at XYZ Corp..."
  }'
```

### 回應結構

#### 成功回應 (200)

回傳更新後的版本詳情，結構與取得單個版本詳情相同。

#### 回應範例

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

### 錯誤回應

#### 400 - 參數驗證失敗

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

#### 401 - 未認證

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

#### 403 - 無權限存取

```json
{
  "code": 403,
  "message": "access.denied",
  "data": null
}
```

---

## 10. Facade 介面方法

ResumeFacade 定義了完整的履歷管理介面，以下是介面方法清單：

| 方法 | 說明 | 實作狀態 |
|------|------|----------|
| `uploadResume` | 上傳履歷 | ✅ 已實作 |
| `downloadResume` | 下載履歷 | ✅ 已實作 |
| `getResumeGroups` | 取得使用者的所有履歷組 | ✅ 已實作 |
| `getResumeGroup` | 取得單個履歷組詳情 | ✅ 已實作 |
| `deleteResumeGroup` | 刪除履歷組 | ✅ 已實作 |
| `getVersionsByGroup` | 取得履歷組下的所有版本 | ✅ 已實作 |
| `getVersion` | 取得單個版本詳情 | ✅ 已實作 |
| `deleteVersion` | 刪除履歷版本 | ✅ 已實作 |
| `editVersion` | 編輯版本內容 | ✅ 已實作 |
| `setDefaultGroup` | 設定預設履歷組 | ⏳ MVP 未實作 |
| `createAiVersion` | 建立 AI 最佳化版本 | ⏳ MVP 未實作 |
| `rollbackToVersion` | 回滾到指定版本 | ⏳ MVP 未實作 |

---

## DTO 定義

### ResumeUploadRequest (上傳請求)

```java
{
  "file": MultipartFile,  // 必填，履歷檔案
  "title": String         // 可選，履歷標題
}
```

### ResumeUploadResponse (上傳回應)

```java
{
  "groupId": UUID,              // 履歷組 ID
  "originalVersionId": UUID,    // 原始版本 ID（可直接用於下載）
  "title": String,              // 履歷標題
  "createdAt": LocalDateTime    // 建立時間
}
```

### ResumeEditRequest (編輯請求)

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `versionId` | UUID | 是 | 版本 ID（會被路徑參數覆寫） |
| `content` | String | 是 | 履歷內容（Markdown 格式） |

```java
{
  "versionId": UUID,   // 必填，版本 ID（會被路徑參數覆寫）
  "content": String    // 必填，履歷內容（Markdown）
}
```

### ResumeGroupResponse (履歷組回應)

```java
{
  "groupId": UUID,              // 組 ID
  "title": String,              // 標題
  "isDefault": boolean,         // 是否為預設組
  "createdAt": LocalDateTime,   // 建立時間
  "updatedAt": LocalDateTime,   // 更新時間
  "originalVersion": VersionSummary,    // 原始版本
  "convertedVersion": VersionSummary,   // 轉換版本
  "aiOptimizedVersion": VersionSummary  // AI 最佳化版本
}
```

### ResumeVersionResponse (履歷版本回應)

```java
{
  "versionId": UUID,          // 版本 ID
  "groupId": UUID,            // 所屬組 ID
  "versionType": String,      // 版本型別：ORIGINAL, CONVERTED, AI_OPTIMIZED
  "status": String,           // 狀態：PENDING, PROCESSING, COMPLETED, FAILED
  "originalFileName": String, // 原始檔案名稱
  "fileType": String,         // 檔案型別
  "fileSize": long,           // 檔案大小
  "content": String,          // 內容（文字格式）
  "editable": boolean,        // 是否可編輯
  "createdAt": LocalDateTime, // 建立時間
  "updatedAt": LocalDateTime  // 更新時間
}
```

---

## 履歷狀態說明

| 狀態值 | 說明 |
|--------|------|
| `PENDING` | 待處理（剛上傳） |
| `PROCESSING` | 剖析中 |
| `COMPLETED` | 剖析完成 |
| `FAILED` | 剖析失敗 |

## 版本型別說明

| 型別值 | 說明 |
|--------|------|
| `ORIGINAL` | 原始上傳版本 |
| `CONVERTED` | 格式轉換版本（轉換為 Markdown） |
| `AI_OPTIMIZED` | AI 最佳化版本 |

---

## 介面彙總

| 介面 | 方法 | 路徑 | 說明 | 認證 |
|------|------|------|------|------|
| 上傳履歷 | POST | `/api/v1/resumes` | 上傳履歷檔案 | 是 |
| 下載履歷 | GET | `/api/v1/resumes/{versionId}/download` | 下載履歷檔案 | 是 |
| 取得所有組 | GET | `/api/v1/resumes/groups` | 取得使用者所有履歷組 | 是 |
| 取得組詳情 | GET | `/api/v1/resumes/groups/{groupId}` | 取得履歷組詳情 | 是 |
| 刪除組 | DELETE | `/api/v1/resumes/groups/{groupId}` | 刪除履歷組 | 是 |
| 取得版本清單 | GET | `/api/v1/resumes/groups/{groupId}/versions` | 取得組內所有版本 | 是 |
| 取得版本詳情 | GET | `/api/v1/resumes/versions/{versionId}` | 取得單個版本詳情 | 是 |
| 刪除版本 | DELETE | `/api/v1/resumes/versions/{versionId}` | 刪除履歷版本 | 是 |
| 編輯版本 | PUT | `/api/v1/resumes/versions/{versionId}` | 編輯版本內容 | 是 |

---

## 使用流程範例

### 簡單流程：上傳後直接下載

```bash
# 1. 登入取得 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.data.accessToken')

# 2. 上傳履歷
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/resumes \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@resume.pdf" \
  -F "title=My Resume")
  
ORIGINAL_VERSION_ID=$(echo $RESPONSE | jq -r '.data.originalVersionId')
echo "Original Version ID: $ORIGINAL_VERSION_ID"

# 3. 直接使用 originalVersionId 下載
curl -X GET "http://localhost:8080/api/v1/resumes/$ORIGINAL_VERSION_ID/download" \
  -H "Authorization: Bearer $TOKEN" \
  --output downloaded_resume.pdf
```

### 完整流程：編輯後下載

```bash
# 1. 登入取得 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.data.accessToken')

# 2. 上傳履歷
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/resumes \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@resume.pdf" \
  -F "title=My Resume")
  
GROUP_ID=$(echo $RESPONSE | jq -r '.data.groupId')
echo "Group ID: $GROUP_ID"

# 3. 取得組詳情
GROUP_INFO=$(curl -s "http://localhost:8080/api/v1/resumes/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN")

# 4. 提取轉換版本 ID（可編輯的 Markdown 版本）
CONVERTED_VERSION_ID=$(echo $GROUP_INFO | jq -r '.data.convertedVersion.versionId')
echo "Converted Version ID: $CONVERTED_VERSION_ID"

# 5. 取得版本詳情檢視目前內容
VERSION_DETAIL=$(curl -s "http://localhost:8080/api/v1/resumes/versions/$CONVERTED_VERSION_ID" \
  -H "Authorization: Bearer $TOKEN")
echo "Current Content: $(echo $VERSION_DETAIL | jq -r '.data.content')"

# 6. 編輯版本內容
curl -X PUT "http://localhost:8080/api/v1/resumes/versions/$CONVERTED_VERSION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "# Updated Resume\n\n## Experience\nSoftware Engineer..."
  }'

# 7. 使用版本 ID 下載
curl -X GET "http://localhost:8080/api/v1/resumes/$CONVERTED_VERSION_ID/download" \
  -H "Authorization: Bearer $TOKEN" \
  --output converted_resume.md

# 8. 刪除履歷組（清理）
curl -X DELETE "http://localhost:8080/api/v1/resumes/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 常見問題

### Q: 上傳成功後下載提示 "version.not.found"？

**A**: 請使用上傳介面回傳的 `originalVersionId` 進行下載，而不是 `groupId`。

### Q: 如何取得版本 ID？

**A**: 
1. **推薦**: 直接使用上傳介面回傳的 `originalVersionId`
2. 呼叫 `GET /api/v1/resumes/groups/{groupId}` 取得組詳情，其中包含各個版本的摘要資訊
3. 呼叫 `GET /api/v1/resumes/groups/{groupId}/versions` 取得該組下所有版本清單
4. 呼叫 `GET /api/v1/resumes/versions/{versionId}` 取得單個版本詳情

### Q: 哪些版本可以編輯？

**A**: 
- `CONVERTED`（轉換版）和 `AI_OPTIMIZED`（AI 最佳化版）可以編輯
- `ORIGINAL`（原版）不可編輯，保護原始上傳檔案
- 可透過 `editable` 欄位判斷是否可編輯

---

*文件版本: 1.3.0*
