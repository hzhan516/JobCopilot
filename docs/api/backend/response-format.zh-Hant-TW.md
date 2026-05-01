<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](response-format.en_US.md) | [简体中文](response-format.zh-Hans-CN.md) | [繁體中文](response-format.zh-Hant-TW.md)

# 回應格式規範

> 所有 API 介面的統一回應格式說明

---

## 概述

Resume Assistant API 採用統一的回應格式，無論請求成功或失敗，都會回傳以下結構：

```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

---

## 回應結構

### 欄位說明

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `code` | Integer | 是 | 業務狀態碼 |
| `message` | String | 是 | 提示資訊 |
| `data` | Object | 否 | 業務資料，失敗時可能為 null |

### 狀態碼 (code)

| 狀態碼 | 含義 | 說明 |
|--------|------|------|
| 200 | 成功 | 請求處理成功 |
| 400 | 請求錯誤 | 參數驗證失敗或業務邏輯錯誤 |
| 401 | 未認證 | Token 缺失或無效 |
| 403 | 無權限 | 沒有存取該資源的權限 |
| 404 | 不存在 | 請求的資源不存在 |
| 500 | 伺服器錯誤 | 伺服器內部錯誤 |

---

## 成功回應範例

### 帶資料的回應

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "userId": "d71774e0-e238-4191-b71c-33478e44b4b6",
    "email": "user@example.com",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400
  }
}
```

### 僅訊息的成功回應

```json
{
  "code": 200,
  "message": "Operation completed successfully",
  "data": null
}
```

---

## 錯誤回應範例

### 參數驗證錯誤

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": {
    "email": "Email is required",
    "password": "Password must be between 6 and 32 characters"
  }
}
```

### 業務邏輯錯誤

```json
{
  "code": 400,
  "message": "Email already exists",
  "data": null
}
```

### 認證錯誤

```json
{
  "code": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

### 權限錯誤

```json
{
  "code": 403,
  "message": "Access denied",
  "data": null
}
```

### 資源不存在

```json
{
  "code": 404,
  "message": "User not found",
  "data": null
}
```

### 伺服器錯誤

```json
{
  "code": 500,
  "message": "System busy, please try again later",
  "data": null
}
```

---

## 分頁回應格式

對於列表查詢介面，回應資料包含分頁資訊：

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "list": [
      { ... },
      { ... }
    ],
    "page": 0,
    "size": 10,
    "total": 100,
    "totalPages": 10
  }
}
```

### 分頁欄位說明

| 欄位 | 型別 | 說明 |
|------|------|------|
| `list` | Array | 資料列表 |
| `page` | Integer | 目前頁碼（從 0 開始，Spring Data 預設） |
| `size` | Integer | 每頁筆數 |
| `total` | Long | 總記錄數 |
| `totalPages` | Integer | 總頁數 |

---

## 時間格式

所有時間欄位統一使用 ISO 8601 格式：

```
2026-04-06T22:45:00.000+00:00
```

---

## 錯誤處理建議

### 前端處理流程

1. **檢查 HTTP 狀態碼**
   - 2xx: 繼續處理回應主體
   - 4xx/5xx: 進入錯誤處理

2. **解析回應主體**
   - 檢查 `code` 欄位
   - code = 200: 處理 `data` 資料
   - code != 200: 顯示 `message` 錯誤資訊

3. **特殊處理**
   - code = 401: Token 過期，使用 Refresh Token 刷新或跳轉登入頁
   - code = 403: 提示使用者無權限
   - code = 500: 提示伺服器繁忙，稍後重試

### 範例程式碼 (JavaScript)

```javascript
async function apiRequest(url, options) {
  try {
    const response = await fetch(url, options);
    const result = await response.json();
    
    if (result.code === 200) {
      return result.data;
    } else if (result.code === 401) {
      // Token 過期，刷新或跳轉登入
      window.location.href = '/login';
    } else {
      // 顯示錯誤訊息
      alert(result.message);
    }
  } catch (error) {
    alert('Network error');
  }
}
```
