<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/README.md) | [简体中文](../../../zh-Hans-CN/api/backend/README.md) | [繁體中文](README.md)

# Backend API 文件

本文件包含 Resume Assistant 後端 API 的完整介面定義。

## 基本資訊

| 項目 | 說明 |
|------|------|
| 基礎路徑 | `/api` |
| API 版本 | `v1` |
| 內容類型 | `application/json` (除檔案上傳外) |
| 認證方式 | JWT Bearer Token |

## 認證方式

API 使用 JWT (JSON Web Token) 進行認證。除了在 `/api/v1/auth/**` 路徑下的介面外，其他所有介面都需要在請求頭中攜帶有效的存取權杖：

```
Authorization: Bearer <access_token>
```

## 全域回應格式

所有 API 回應都遵循統一的格式：

```json
{
  "code": 200,
  "message": "Success",
  "data": {}
}
```

### 回應欄位說明

| 欄位 | 類型 | 說明 |
|------|------|------|
| `code` | Integer | 狀態碼，200 表示成功，其他表示錯誤 |
| `message` | String | 回應訊息 |
| `data` | Object | 回應資料，成功時包含具體資料，錯誤時可能包含詳細錯誤資訊 |

## 錯誤處理

### 錯誤回應格式

```json
{
  "code": 400,
  "message": "錯誤描述",
  "data": null
}
```

### 常見錯誤碼

| 狀態碼 | 說明 |
|--------|------|
| 400 | 請求參數錯誤 |
| 401 | 未認證或認證失敗 |
| 403 | 權限不足 |
| 404 | 資源不存在 |
| 409 | 資源衝突（如郵箱已存在） |
| 500 | 伺服器內部錯誤 |

---

## API 端點列表

### 1. 認證模組 (Auth)

#### 1.1 郵箱註冊

- **URL**: `POST /api/v1/auth/register/email`
- **認證**: 不需要
- **Content-Type**: `application/json`

**請求參數**:

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `email` | String | 是 | 郵箱地址，需符合郵箱格式 |
| `password` | String | 是 | 密碼，長度 6-32 字元 |

**請求範例**:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**成功回應** (201):

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

**回應欄位說明**:

| 欄位 | 類型 | 說明 |
|------|------|------|
| `userId` | UUID | 使用者唯一識別 |
| `email` | String | 使用者郵箱 |
| `accessToken` | String | 存取權杖 |
| `refreshToken` | String | 重新整理權杖 |
| `expiresIn` | Long | 存取權杖有效期（秒） |

---

#### 1.2 郵箱登入

- **URL**: `POST /api/v1/auth/login/email`
- **認證**: 不需要
- **Content-Type**: `application/json`

**請求參數**:

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `email` | String | 是 | 郵箱地址 |
| `password` | String | 是 | 密碼 |

**請求範例**:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**成功回應** (200):

與郵箱註冊回應格式相同。

---

### 2. 履歷模組 (Resume)

#### 2.1 上傳履歷

- **URL**: `POST /api/v1/resumes`
- **認證**: 需要
- **Content-Type**: `multipart/form-data`

**請求參數**:

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `file` | File | 是 | 履歷檔案（支援 PDF、DOC、DOCX 等格式） |
| `title` | String | 否 | 履歷標題，不傳則使用檔案名稱 |

**請求範例**:

```http
POST /api/v1/resumes
Authorization: Bearer <access_token>
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="resume.pdf"
Content-Type: application/pdf

<檔案內容>
------WebKitFormBoundary
Content-Disposition: form-data; name="title"

我的履歷
------WebKitFormBoundary--
```

**成功回應** (200):

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "groupId": "550e8400-e29b-41d4-a716-446655440000",
    "originalVersionId": "550e8400-e29b-41d4-a716-446655440001",
    "title": "我的履歷",
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

**回應欄位說明**:

| 欄位 | 類型 | 說明 |
|------|------|------|
| `groupId` | UUID | 履歷組 ID |
| `originalVersionId` | UUID | 原始版本 ID（可直接用於下載） |
| `title` | String | 履歷標題 |
| `createdAt` | LocalDateTime | 建立時間 |

---

#### 2.2 下載履歷

- **URL**: `GET /api/v1/resumes/{versionId}/download`
- **認證**: 需要
- **Content-Type**: 根據檔案類型回傳

**路徑參數**:

| 欄位 | 類型 | 說明 |
|------|------|------|
| `versionId` | UUID | 版本 ID（上傳回傳的 originalVersionId） |

**查詢參數**:

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `format` | String | 否 | 匯出格式：`original`（預設）、`pdf`、`docx`、`html`、`md`、`txt` |

**請求範例**:

```http
GET /api/v1/resumes/550e8400-e29b-41d4-a716-446655440000/download?format=pdf
Authorization: Bearer <access_token>
```

**成功回應** (200):

回傳檔案流，Content-Disposition 頭包含檔案名稱。

**注意**: 當前版本僅支援回傳原始檔案，格式轉換功能待實現。

#### 2.3 取得使用者所有履歷組

- **URL**: `GET /api/v1/resumes/groups`
- **認證**: 需要
- **Content-Type**: `application/json`

**成功回應** (200):

回傳當前使用者的所有履歷組列表。

#### 2.4 取得履歷組詳情

- **URL**: `GET /api/v1/resumes/groups/{groupId}`
- **認證**: 需要
- **Content-Type**: `application/json`

**路徑參數**:

| 欄位 | 類型 | 說明 |
|------|------|------|
| `groupId` | UUID | 履歷組唯一識別 |

**成功回應** (200):

回傳履歷組詳情，包含各個版本的摘要資訊。

#### 2.5 刪除履歷組

- **URL**: `DELETE /api/v1/resumes/groups/{groupId}`
- **認證**: 需要
- **Content-Type**: `application/json`

**路徑參數**:

| 欄位 | 類型 | 說明 |
|------|------|------|
| `groupId` | UUID | 履歷組唯一識別 |

**成功回應** (200):

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

#### 2.6 取得履歷組版本列表

- **URL**: `GET /api/v1/resumes/groups/{groupId}/versions`
- **認證**: 需要
- **Content-Type**: `application/json`

**路徑參數**:

| 欄位 | 類型 | 說明 |
|------|------|------|
| `groupId` | UUID | 履歷組唯一識別 |

**成功回應** (200):

回傳該組下所有版本的詳細資訊列表。

#### 2.7 刪除履歷版本

- **URL**: `DELETE /api/v1/resumes/versions/{versionId}`
- **認證**: 需要
- **Content-Type**: `application/json`

**路徑參數**:

| 欄位 | 類型 | 說明 |
|------|------|------|
| `versionId` | UUID | 版本唯一識別 |

**成功回應** (200):

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

### 3. 職位模組 (Job)

詳見 [job.md](job.md) 和 [job-matching.md](job-matching.md)

本模組提供職位連結提交、非同步剖析、智慧配對和歷史查詢功能。

#### 3.1 提交職位連結
- **URL**: `POST /api/v1/jobs`
- **認證**: 需要

#### 3.2 取得職位詳情
- **URL**: `GET /api/v1/jobs/{jobId}`
- **認證**: 需要

#### 3.3 取得職位列表
- **URL**: `GET /api/v1/jobs`
- **認證**: 需要

#### 3.4 啟動職位配對
- **URL**: `POST /api/v1/jobs/match`
- **認證**: 需要

#### 3.5 查詢配對結果
- **URL**: `GET /api/v1/jobs/match/{matchId}`
- **認證**: 需要

#### 3.6 取得配對歷史
- **URL**: `GET /api/v1/jobs/match/history`
- **認證**: 需要

---

### 4. 對話模組 (Conversation)

詳見 [conversation.md](conversation.md)

#### 4.1 建立對話
- **URL**: `POST /api/v1/conversations`
- **認證**: 需要

#### 4.2 發送訊息
- **URL**: `POST /api/v1/conversations/{conversationId}/messages`
- **認證**: 需要

#### 4.3 取得對話詳情
- **URL**: `GET /api/v1/conversations/{conversationId}`
- **認證**: 需要
- **分頁**: 支援 `?page=0&size=20` 對訊息列表分頁

#### 4.4 取得對話列表
- **URL**: `GET /api/v1/conversations`
- **認證**: 需要

#### 4.5 關閉對話
- **URL**: `PUT /api/v1/conversations/{conversationId}/close`
- **認證**: 需要

#### 4.6 刪除對話
- **URL**: `DELETE /api/v1/conversations/{conversationId}`
- **認證**: 需要

#### 4.7 上傳附件
- **URL**: `POST /api/v1/conversations/{conversationId}/files`
- **認證**: 需要
- **Content-Type**: `multipart/form-data`

---

### 5. 求職追蹤模組 (Tracking)

詳見 [tracking.md](tracking.md)

本模組提供求職申請的狀態流轉、事件記錄和統計分析功能。

#### 5.1 建立追蹤記錄
- **URL**: `POST /api/v1/trackings`
- **認證**: 需要

#### 5.2 取得追蹤列表
- **URL**: `GET /api/v1/trackings?status=INTERVIEWING`
- **認證**: 需要

#### 5.3 取得追蹤詳情
- **URL**: `GET /api/v1/trackings/{id}`
- **認證**: 需要

#### 5.4 更新追蹤記錄
- **URL**: `PUT /api/v1/trackings/{id}`
- **認證**: 需要

#### 5.5 刪除追蹤記錄
- **URL**: `DELETE /api/v1/trackings/{id}`
- **認證**: 需要

#### 5.6 取得統計資訊
- **URL**: `GET /api/v1/trackings/stats`
- **認證**: 需要

---

### 介面彙總

| 介面 | 方法 | 路徑 | 說明 | 認證 |
|------|------|------|------|------|
| 郵箱註冊 | POST | `/api/v1/auth/register/email` | 使用者郵箱註冊 | 否 |
| 郵箱登入 | POST | `/api/v1/auth/login/email` | 使用者郵箱登入 | 否 |
| Google 登入 | POST | `/api/v1/auth/login/google` | Google OAuth 登入 | 否 |
| 上傳履歷 | POST | `/api/v1/resumes` | 上傳履歷檔案 | 是 |
| 下載履歷 | GET | `/api/v1/resumes/{versionId}/download` | 下載履歷檔案（支援格式轉換） | 是 |
| 取得所有組 | GET | `/api/v1/resumes/groups` | 取得使用者所有履歷組 | 是 |
| 取得組詳情 | GET | `/api/v1/resumes/groups/{groupId}` | 取得履歷組詳情 | 是 |
| 刪除組 | DELETE | `/api/v1/resumes/groups/{groupId}` | 刪除履歷組 | 是 |
| 取得版本列表 | GET | `/api/v1/resumes/groups/{groupId}/versions` | 取得組內所有版本 | 是 |
| 取得版本詳情 | GET | `/api/v1/resumes/versions/{versionId}` | 取得單個版本詳情 | 是 |
| 刪除版本 | DELETE | `/api/v1/resumes/versions/{versionId}` | 刪除履歷版本 | 是 |
| 編輯版本 | PUT | `/api/v1/resumes/versions/{versionId}` | 編輯版本內容 | 是 |
| 提交職位 | POST | `/api/v1/jobs` | 提交職位連結非同步剖析 | 是 |
| 取得職位詳情 | GET | `/api/v1/jobs/{jobId}` | 取得職位剖析狀態 | 是 |
| 取得職位列表 | GET | `/api/v1/jobs` | 取得使用者所有職位 | 是 |
| 啟動職位配對 | POST | `/api/v1/jobs/match` | 啟動非同步職位配對 | 是 |
| 查詢配對結果 | GET | `/api/v1/jobs/match/{matchId}` | 查詢配對任務結果 | 是 |
| 取得配對歷史 | GET | `/api/v1/jobs/match/history` | 取得歷史配對記錄 | 是 |
| 建立對話 | POST | `/api/v1/conversations` | 建立新對話 | 是 |
| 發送訊息 | POST | `/api/v1/conversations/{conversationId}/messages` | 發送對話訊息 | 是 |
| 取得對話 | GET | `/api/v1/conversations/{conversationId}` | 取得對話詳情（支援訊息分頁） | 是 |
| 取得對話列表 | GET | `/api/v1/conversations` | 取得所有對話 | 是 |
| 關閉對話 | PUT | `/api/v1/conversations/{conversationId}/close` | 關閉對話 | 是 |
| 刪除對話 | DELETE | `/api/v1/conversations/{conversationId}` | 刪除對話 | 是 |
| 上傳附件 | POST | `/api/v1/conversations/{conversationId}/files` | 上傳對話附件 | 是 |
| 建立追蹤 | POST | `/api/v1/trackings` | 建立求職追蹤記錄 | 是 |
| 取得追蹤列表 | GET | `/api/v1/trackings` | 取得追蹤記錄列表 | 是 |
| 取得追蹤詳情 | GET | `/api/v1/trackings/{id}` | 取得追蹤詳情 | 是 |
| 更新追蹤 | PUT | `/api/v1/trackings/{id}` | 更新追蹤（含狀態流轉） | 是 |
| 刪除追蹤 | DELETE | `/api/v1/trackings/{id}` | 刪除追蹤記錄 | 是 |
| 取得統計 | GET | `/api/v1/trackings/stats` | 取得追蹤統計 | 是 |

---

## DTO 詳細定義

### 請求 DTO

#### RegisterByEmailRequest (郵箱註冊請求)

```java
{
  "email": String,      // 必填，郵箱格式
  "password": String    // 必填，6-32 字元
}
```

#### LoginByEmailRequest (郵箱登入請求)

```java
{
  "email": String,      // 必填，郵箱格式
  "password": String    // 必填
}
```

#### ResumeUploadRequest (履歷上傳請求)

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `file` | MultipartFile | 是 | 履歷檔案（PDF/DOCX/MD/TXT） |
| `title` | String | 否 | 履歷標題，不傳則使用檔案名稱 |

#### ResumeEditRequest (履歷編輯請求)

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `versionId` | UUID | 是 | 版本 ID（與路徑參數一致） |
| `content` | String | 是 | 履歷內容（Markdown 格式） |

```java
{
  "versionId": UUID,   // 必填，版本 ID
  "content": String    // 必填，履歷內容
}
```

### 回應 DTO

#### AuthResponse (認證回應)

```java
{
  "userId": UUID,        // 使用者 ID
  "email": String,       // 使用者郵箱
  "accessToken": String, // 存取權杖
  "refreshToken": String,// 重新整理權杖
  "expiresIn": Long      // 有效期（秒）
}
```

#### ResumeUploadResponse (履歷上傳回應)

```java
{
  "groupId": UUID,              // 履歷組 ID
  "originalVersionId": UUID,    // 原始版本 ID（可直接用於下載）
  "title": String,              // 履歷標題
  "createdAt": LocalDateTime    // 建立時間
}
```

#### ResumeGroupResponse (履歷組回應)

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

**VersionSummary**:

```java
{
  "versionId": UUID,        // 版本 ID
  "status": String,         // 狀態
  "createdAt": LocalDateTime,// 建立時間
  "exists": boolean         // 是否存在
}
```

#### ResumeVersionResponse (履歷版本回應)

```java
{
  "versionId": UUID,          // 版本 ID
  "groupId": UUID,            // 所屬組 ID
  "versionType": String,      // 版本類型
  "status": String,           // 狀態
  "originalFileName": String, // 原始檔案名稱
  "fileType": String,         // 檔案類型
  "fileSize": long,           // 檔案大小
  "content": String,          // 內容
  "editable": boolean,        // 是否可編輯
  "createdAt": LocalDateTime, // 建立時間
  "updatedAt": LocalDateTime  // 更新時間
}
```

#### ApiResponse<T> (通用回應包裝)

```java
{
  "code": Integer,    // 狀態碼
  "message": String,  // 訊息
  "data": T           // 資料
}
```

---

## 驗證規則

### 郵箱註冊/登入

| 欄位 | 規則 |
|------|------|
| `email` | 必填，必須符合郵箱格式 |
| `password` | 必填，長度 6-32 字元（註冊時） |

### 履歷上傳

| 欄位 | 規則 |
|------|------|
| `file` | 必填，必須是有效的檔案 |

### 履歷編輯

| 欄位 | 規則 |
|------|------|
| `versionId` | 必填 |
| `content` | 必填，不能為空 |

---

## 國際化支援

API 支援國際化回應，透過請求頭 `Accept-Language` 指定語言：

- `zh-CN` - 簡體中文（預設）
- `en` - 英文

範例:

```http
Accept-Language: en
```

---

## 相關文件

- [認證模組詳細文件](authentication.md)
- [履歷模組詳細文件](resume.md)
- [職位模組詳細文件](job.md)
- [職位配對模組詳細文件](job-matching.md)
- [對話模組詳細文件](conversation.md)
- [求職追蹤模組詳細文件](tracking.md)
- [AI / MQ 互動介面文件](ai-mq-interfaces.md)
- [回應格式與錯誤碼說明](response-format.md)

---

## 備註

1. 所有時間欄位均採用 ISO 8601 格式（如：`2024-01-15T10:30:00`）
2. UUID 格式為標準 36 字元 UUID 字串（如：`550e8400-e29b-41d4-a716-446655440000`）
3. 檔案上傳大小限制請參考具體部署設定
4. 當前已實現的端點已列出，Facade 中定義的其他方法將在後續版本中實現
