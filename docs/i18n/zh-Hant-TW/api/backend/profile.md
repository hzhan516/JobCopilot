# 使用者資料 API (Profile API)

> 使用者個人資料管理介面。

---

## 介面基本資訊

所有端點前綴為 `/api`（透過 `server.servlet.context-path` 設定）。

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/v1/profile` | `GET` | 取得當前使用者資料 |
| `/api/v1/profile` | `PUT` | 更新個人資料欄位 |
| `/api/v1/profile/avatar` | `PUT` | 單獨更新頭像 URL |

---

## 認證方式

所有介面需在請求標頭中攜帶有效的 JWT access token：

```
Authorization: Bearer <accessToken>
```

使用者身分透過 `@CurrentUser` 從 token 中解析。

---

## GET /api/v1/profile

取得目前登入使用者的個人資料。

### 請求

- **Headers**: `Authorization: Bearer <accessToken>`
- **Body**: 無

### 回應

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

### 錯誤回應

| HTTP 狀態 | Code | 說明 |
|-----------|------|------|
| `401` | `401` | Token 缺失或無效 |
| `404` | `404` | 找不到該使用者的資料 |

---

## PUT /api/v1/profile

更新目前使用者的個人資料。所有欄位均為選填 —— 僅提供的非空欄位會被更新。

### 請求

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

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `fullName` | `string` | 否 | 使用者全名 |
| `phone` | `string` | 否 | 手機號碼 |
| `targetPosition` | `string` | 否 | 目標職位 |
| `preferredLocation` | `string` | 否 | 期望工作地點 |

### 回應

回傳更新後的完整資料，結構與 `GET /api/v1/profile` 相同。

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

### 錯誤回應

| HTTP 狀態 | Code | 說明 |
|-----------|------|------|
| `401` | `401` | Token 缺失或無效 |
| `404` | `404` | 找不到使用者資料 |

---

## PUT /api/v1/profile/avatar

僅更新頭像 URL。此獨立端點簡化了前端頭像上傳流程（先上傳到儲存服務，再呼叫此介面儲存 URL）。

### 請求

- **Headers**: `Authorization: Bearer <accessToken>`, `Content-Type: application/json`
- **Body**:

```json
{
  "avatarUrl": "https://storage.example.com/avatars/new-avatar.png"
}
```

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `avatarUrl` | `string` | 是 | 已上傳頭像圖片的公開 URL |

### 回應

回傳更新後的完整資料，結構與 `GET /api/v1/profile` 相同。

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

### 錯誤回應

| HTTP 狀態 | Code | 說明 |
|-----------|------|------|
| `401` | `401` | Token 缺失或無效 |
| `404` | `404` | 找不到使用者資料 |

---

## 資料模型

### ProfileResponse

| 欄位 | 類型 | 說明 |
|------|------|------|
| `userId` | `string (UUID)` | 使用者唯一識別碼 |
| `fullName` | `string` | 使用者全名 |
| `avatarUrl` | `string` | 頭像圖片 URL |
| `phone` | `string` | 手機號碼 |
| `targetPosition` | `string` | 目標職位 |
| `preferredLocation` | `string` | 期望工作地點 |
| `createdAt` | `string (ISO-8601)` | 資料建立時間 |
| `updatedAt` | `string (ISO-8601)` | 最後更新時間 |

---

## 架構說明

Profile 模組嚴格遵循 **六邊形架構 / DDD** 分層：

```
Trigger (ProfileController)
    └── 呼叫 API (ProfileFacade)
        └── App 實作 (ProfileFacadeImpl)
            └── 呼叫 App Service (ProfileApplicationService)
                └── 呼叫 Domain Port (UserProfileRepository)
                    └── Infrastructure 實作 (UserProfileRepositoryImpl)
```

- **領域實體** `UserProfile` 封裝了更新邏輯（`updateProfile`、`updateAvatar`）。
- **應用服務** 負責交易邊界與使用案例編排。
- **門面層** 負責 API DTO 與領域實體之間的轉換。

---

## 其他語言

- [English (英文)](../../../../api/backend/profile.md)
- [简体中文 (Simplified Chinese)](../../zh-Hans-CN/api/backend/profile.md)
