<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/conversation.md) | [简体中文](../../../zh-Hans-CN/api/backend/conversation.md) | [繁體中文](conversation.md)

# 對話管理 API

> 基於履歷上下文的多輪 AI 對話介面，支援非同步 MQ 互動、附件上傳與訊息分頁

---

## 目錄

1. [建立對話](#1-建立對話)
2. [發送訊息](#2-發送訊息)
3. [取得對話詳情](#3-取得對話詳情)
4. [取得對話列表](#4-取得對話列表)
5. [關閉對話](#5-關閉對話)
6. [刪除對話](#6-刪除對話)
7. [上傳附件](#7-上傳附件)
8. [非同步訊息流說明](#8-非同步訊息流說明)
9. [錯誤碼說明](#9-錯誤碼說明)

---

## 1. 建立對話

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 建立對話 |
| **介面路徑** | `POST /api/v1/conversations` |
| **是否需要認證** | 是 |
| **Content-Type** | `application/json` |

### 請求結構

#### Request Body

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `title` | String | 否 | 對話標題，不傳則預設為 "New Conversation" |
| `resumeVersionId` | String (UUID) | 否 | 關聯的履歷版本 ID |
| `jobId` | String (UUID) | 否 | 關聯的職位 ID |

#### 請求範例

```json
{
  "title": "優化工作經驗",
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002"
}
```

### 回應結構

#### 成功回應 (200)

| 欄位 | 類型 | 說明 |
|------|------|------|
| `conversationId` | String (UUID) | 對話唯一識別 |
| `userId` | String (UUID) | 使用者 ID |
| `title` | String | 對話標題 |
| `status` | String | 狀態：ACTIVE, CLOSED |
| `resumeVersionId` | String (UUID) | 關聯履歷版本 ID |
| `messages` | Array | 訊息列表（建立時為空） |
| `createdAt` | String (ISO 8601) | 建立時間 |
| `updatedAt` | String (ISO 8601) | 更新時間 |

#### 回應範例

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "conversationId": "550e8400-e29b-41d4-a716-446655440003",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "優化工作經驗",
    "status": "ACTIVE",
    "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
    "messages": [],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

---

## 2. 發送訊息

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 發送訊息 |
| **介面路徑** | `POST /api/v1/conversations/{conversationId}/messages` |
| **是否需要認證** | 是 |
| **Content-Type** | `application/json` |

### 請求結構

#### Path Parameters

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `conversationId` | String (UUID) | 是 | 對話唯一識別 |

#### Request Body

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `content` | String | 是 | 訊息內容 |
| `fileUrls` | List<String> | 否 | 關聯檔案 URL 列表（如履歷、附件等） |

#### 請求範例

```json
{
  "content": "幫我優化一下專案經驗部分",
  "fileUrls": ["https://minio.example.com/resumes/xxx.pdf"]
}
```

### 回應結構

#### 成功回應 (200)

回傳更新後的完整對話資訊，包含新增的使用者訊息。**注意**：AI 回覆透過非同步 MQ 處理，不會立即出現在回應中，前端需要透過輪詢或 WebSocket 取得最新回覆。

若建立對話時未指定標題，且這是該對話的**第一條訊息**，系統會自動將對話標題設定為訊息內容的前 30 個字元。

#### 回應範例

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "conversationId": "550e8400-e29b-41d4-a716-446655440003",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "幫我優化一下專案經驗部分",
    "status": "ACTIVE",
    "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
    "messages": [
      {
        "messageId": "550e8400-e29b-41d4-a716-446655440004",
        "role": "USER",
        "content": "幫我優化一下專案經驗部分",
        "sequence": 1,
        "fileUrl": null,
        "createdAt": "2024-01-15T10:31:00"
      }
    ],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:31:00"
  }
}
```

---

## 3. 取得對話詳情

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 取得對話詳情 |
| **介面路徑** | `GET /api/v1/conversations/{conversationId}` |
| **是否需要認證** | 是 |

### 請求結構

#### Path Parameters

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `conversationId` | String (UUID) | 是 | 對話唯一識別 |

#### Query Parameters

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `page` | Integer | 否 | 訊息頁碼，從 0 開始 |
| `size` | Integer | 否 | 每頁訊息數量，預設回傳全部 |

### 回應結構

#### 成功回應 (200)

回傳完整的對話資訊，包括所有訊息列表（按 `sequence` 升序排列）。若傳入了 `page` 和 `size`，僅回傳指定分頁範圍的訊息。若 AI 已回覆，訊息列表中會包含 `role=ASSISTANT` 的訊息，且可能帶有 `fileUrl`。

---

## 4. 取得對話列表

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 取得對話列表 |
| **介面路徑** | `GET /api/v1/conversations` |
| **是否需要認證** | 是 |

### 回應結構

#### 成功回應 (200)

回傳當前使用者的所有對話列表（不包含訊息詳情）。

#### 回應範例

```json
{
  "code": 200,
  "message": "Success",
  "data": [
    {
      "conversationId": "550e8400-e29b-41d4-a716-446655440003",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "幫我優化一下專案經驗部分",
      "status": "ACTIVE",
      "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:31:00"
    }
  ]
}
```

---

## 5. 關閉對話

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 關閉對話 |
| **介面路徑** | `PUT /api/v1/conversations/{conversationId}/close` |
| **是否需要認證** | 是 |

### 請求結構

#### Path Parameters

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `conversationId` | String (UUID) | 是 | 對話唯一識別 |

### 回應結構

#### 成功回應 (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

---

## 6. 刪除對話

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 刪除對話 |
| **介面路徑** | `DELETE /api/v1/conversations/{conversationId}` |
| **是否需要認證** | 是 |

### 請求結構

#### Path Parameters

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `conversationId` | String (UUID) | 是 | 對話唯一識別 |

### 回應結構

#### 成功回應 (200)

```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

---

## 7. 上傳附件

### 基本資訊

| 項目 | 值 |
|------|-----|
| **介面名稱** | 上傳對話附件 |
| **介面路徑** | `POST /api/v1/conversations/{conversationId}/files` |
| **是否需要認證** | 是 |
| **Content-Type** | `multipart/form-data` |

### 請求結構

#### Path Parameters

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `conversationId` | String (UUID) | 是 | 對話唯一識別 |

#### Request Body (form-data)

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `file` | File | 是 | 附件檔案（如 AI 生成的最佳化履歷） |

### 回應結構

#### 成功回應 (200)

回傳檔案上傳成功後的 MinIO 預簽名 URL。

```json
{
  "code": 200,
  "message": "Success",
  "data": "https://minio.example.com/conversations/550e8400-e29b-41d4-a716-446655440003/xxxx_resume.pdf?X-Amz-Algorithm=..."
}
```

---

## 8. 非同步訊息流說明

### 8.1 對話 AI 請求流

當使用者呼叫【發送訊息】介面後，後端會執行以下非同步流程：

1. 儲存使用者訊息（`role=USER`）到資料庫
2. 若對話標題為預設值且是首條訊息，自動生成標題
3. 組裝 `ConversationRequestCommand`，包含歷史訊息、當前訊息、fileUrls、resumeVersionId
4. 透過 RabbitMQ 發送到 `ai.req.conversation` 佇列
5. Python AI 服務消費該訊息，生成回覆
6. AI 服務將結果發送到 `backend.res.conversation` 佇列
7. `AiResultMessageListener` 監聽到 `CONVERSATION_REPLY` 類型事件，儲存 AI 回覆（`role=ASSISTANT`）到資料庫

### 8.2 發送給 AI 服務的資料格式

**請求訊息 (`ConversationRequestCommand`)**：

```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "messageHistory": [
    { "role": "USER", "content": "幫我優化一下專案經驗部分", "fileUrl": null }
  ],
  "currentMessage": "幫我優化一下專案經驗部分",
  "fileUrls": ["https://minio.example.com/resumes/xxx.pdf"],
  "resumeVersionId": "550e8400-e29b-41d4-a716-446655440002"
}
```

| 欄位 | 類型 | 說明 |
|------|------|------|
| `conversationId` | String | 對話 ID |
| `userId` | String | 使用者 ID |
| `messageHistory` | List<Map> | 歷史訊息列表（role, content, fileUrl） |
| `currentMessage` | String | 當前使用者發送的最新訊息 |
| `fileUrls` | List<String> | 使用者引用的外部檔案 URL 列表 |
| `resumeVersionId` | String | 關聯履歷版本 ID（選用） |

### 8.3 接收 AI 回覆的資料格式

**回應訊息 (`AiResultEvent`，type=`CONVERSATION_REPLY`)**：

```json
{
  "referenceId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "CONVERSATION_REPLY",
  "status": "COMPLETED",
  "data": {
    "content": "根據您的履歷，我建議從以下幾個方面優化工作經驗...",
    "fileUrl": "https://minio.example.com/conversations/xxx/optimized_resume.pdf"
  },
  "errorMessage": null,
  "eventType": null
}
```

| 欄位 | 類型 | 說明 |
|------|------|------|
| `referenceId` | String | 對話 ID |
| `type` | String | 固定為 `CONVERSATION_REPLY` |
| `status` | String | `COMPLETED` 或 `FAILED` |
| `data.content` | String | AI 回覆文本 |
| `data.fileUrl` | String | AI 生成檔案的 URL（選用） |
| `errorMessage` | String | 失敗原因（`status=FAILED` 時存在） |

---

## 9. 錯誤碼說明

### 通用錯誤碼

| 狀態碼 | 含義 | 觸發場景 |
|--------|------|----------|
| `200` | 成功 | 請求處理成功 |
| `400` | 請求參數錯誤 | 訊息內容為空、UUID 格式錯誤、分頁參數非法 |
| `401` | 未認證 | 缺少 JWT Token 或 Token 已過期 |
| `403` | 權限不足 | 嘗試操作不屬於自己的對話 |
| `404` | 資源不存在 | 對話 ID 不存在、履歷版本 ID 不存在 |
| `409` | 業務衝突 | 向已關閉的對話發送訊息 |
| `500` | 伺服器內部錯誤 | 檔案上傳失敗、MQ 發送異常 |

### 業務錯誤範例

**向已關閉對話發送訊息 (409)**：

```json
{
  "code": 409,
  "message": "Cannot add message to a closed conversation",
  "data": null
}
```

**存取不屬於自己的對話 (403)**：

```json
{
  "code": 403,
  "message": "Access denied",
  "data": null
}
```

**對話不存在 (404)**：

```json
{
  "code": 404,
  "message": "Conversation not found",
  "data": null
}
```

**無效的履歷版本 (400)**：

```json
{
  "code": 400,
  "message": "Invalid resume version or access denied",
  "data": null
}
```

---

## DTO 定義

### CreateConversationRequest (建立對話請求)

```java
{
  "title": String,          // 選用，對話標題
  "resumeVersionId": String, // 選用，履歷版本 ID
  "jobId": String           // 選用，關聯職位 ID
}
```

### SendMessageRequest (發送訊息請求)

```java
{
  "content": String,         // 必填，訊息內容
  "fileUrls": List<String>   // 選用，關聯檔案 URL 列表
}
```

### ConversationResponse (對話回應)

```java
{
  "conversationId": String,   // 對話 ID
  "userId": String,           // 使用者 ID
  "title": String,            // 標題
  "status": String,           // ACTIVE / CLOSED
  "resumeVersionId": String,  // 關聯履歷版本 ID
  "jobId": String,            // 關聯職位 ID
  "messages": MessageResponse[], // 訊息列表（可能已分頁）
  "createdAt": LocalDateTime, // 建立時間
  "updatedAt": LocalDateTime  // 更新時間
}
```

### MessageResponse (訊息回應)

```java
{
  "messageId": String,        // 訊息 ID
  "role": String,             // USER / ASSISTANT / SYSTEM
  "content": String,          // 內容
  "sequence": int,            // 序號
  "fileUrl": String,          // 關聯檔案 URL（AI 生成檔案等）
  "createdAt": LocalDateTime  // 建立時間
}
```


---

## 8. 串流取得 AI 回覆

### 基本資訊

| 項目 | 值 |
|------|-------|
| **介面名稱** | 串流取得 AI 回覆 |
| **介面路徑** | `GET /api/v1/conversations/{conversationId}/stream` |
| **需要認證** | 是 |
| **Content-Type** | `text/plain`（串流回應） |

### 請求結構

#### 路徑參數

| 欄位 | 類型 | 必填 | 說明 |
|-------|------|----------|-------------|
| `conversationId` | String (UUID) | 是 | 對話唯一識別碼 |

#### 請求標頭

| 欄位 | 必填 | 說明 |
|-------|----------|-------------|
| `Authorization` | 是 | `Bearer {JWT Token}` |

### 回應結構

#### 成功回應 (200)

回傳 `text/plain` 串流回應。HTTP 連線保持開啟，直到 AI 回覆生成完畢。

> **注意：當前實作為偽串流傳輸。**
> AI Service 同步生成完整回覆後，透過 MQ 一次性將結果發回後端。
> 後端保持 HTTP 連線等待 MQ 回覆，然後將完整內容一次性寫入回應流。
> 前端會體驗到 loading 狀態，隨後完整回覆一次性到達。
>
> **後續升級為真正串流的路徑：**
> 1. 更新 AI Service 層，使用 Gemini 的 `generate_content_stream()` 串流 API。
> 2. 在 AI Service 中新增 REST 串流端點（例如 `/api/v1/conversation/stream`）。
> 3. 後端透過 `WebClient` 或 `RestTemplate` 直接呼叫 AI Service 的串流端點，
>    將每個 chunk 即時透傳給前端，現有的 `StreamingResponseBody` 架構可復用。
> 4. MQ 路徑可保留用於非串流場景，或廢棄。

#### 呼叫時序

1. 呼叫 `POST /api/v1/conversations/{conversationId}/messages` 傳送使用者訊息。
2. 立即呼叫 `GET /api/v1/conversations/{conversationId}/stream` 等待 AI 回覆。
3. 連線保持開啟（預設逾時 60 秒）。
4. AI 回覆就緒後，完整文字寫入流並關閉連線。

#### 回應範例（串流）

```text
根據您的履歷，我建議從以下幾個方面優化工作經驗...
```

#### 逾時行為

如果 AI 回覆在 60 秒內未生成，流將關閉並回傳逾時提示：

```text
AI reply timed out. Please try again later.
AI 回复超时，请稍后重试。
```

### 錯誤碼

| 狀態碼 | 含義 | 觸發場景 |
|-------------|---------|------------------|
| `401` | 未認證 | 缺少或已過期 JWT Token |
| `403` | 權限不足 | 嘗試存取不屬於您的對話 |
| `404` | 資源不存在 | 對話 ID 不存在 |

---

## 備註

### 前端串流介面呼叫

前端 `chatService.ts` 透過 `fetch` + `ReadableStream` 呼叫 `GET /v1/conversations/{conversationId}/stream`。
詳見 [8. 串流取得 AI 回覆](#8-串流取得-ai-回覆) 的後端介面文件。
