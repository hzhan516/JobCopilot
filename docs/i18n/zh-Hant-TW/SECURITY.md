# 安全策略

> English version: [SECURITY.md](../../../SECURITY.md)

## 支援的版本

| 版本    | 是否支援 |
|--------:|:--------:|
| `main` / 最新發布版本 | ✅ |

## 回報安全漏洞

如果您在 JobCopilot 中發現了安全漏洞，請私下向專案維護者回報。**請勿**公開提交 Issue。

**首選管道：** 使用 GitHub 私密漏洞回報：
`https://github.com/jobcopilot/jobcopilot/security/advisories/new`

如果您的 fork 或鏡像無法使用私密漏洞回報，請透過私密管道聯絡維護者，並提供足夠資訊以便重現和評估問題。

我們承諾在 **48 小時內** 回覆，並與您一起驗證、評估優先順序並修復問題。

## 安全實踐

### 認證與授權
- 所有服務間通訊在生產環境中都需要 `X-Internal-API-Key` 請求頭驗證。
- 後端 JWT 權杖使用可設定的密鑰；生產建置中不存在硬編碼的預設密鑰。
- 重新整理權杖透過 `HttpOnly`、`SameSite=Strict`、`Secure` Cookie 傳輸。

### 資料保護
- 檔案上傳經過 MIME 類型驗證和檔名清理（防止路徑遍歷攻擊）。
- S3/MinIO 預簽名 URL 的可設定過期時間（預設 7 天，可透過 `storage.presigned-url-expiration-hours` 覆蓋）。
- 敏感日誌（權杖、憑證）會被去識別化或排除在應用程式日誌之外。

### 限流與資源保護
- Embedding 介面強制執行批次大小限制（`EMBEDDING_MAX_BATCH_SIZE`）和單條文字長度上限（`EMBEDDING_MAX_TEXT_LENGTH`）。
- 驗證碼驗證使用加固的客戶端 IP 偵測機制（忽略來自不可信來源的 `X-Forwarded-For`）。

### 基礎設施
- Actuator 端點僅公開暴露 `/health` 和 `/info`；其餘所有 actuator 路徑均被拒絕存取。
- MQ 消費者實作冪等性檢查，防止重複處理。
- 非開發環境在啟動時強制要求設定 `MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY` 和 `INTERNAL_API_KEY`。

## 揭露政策

- 回報者在 48 小時內收到確認。
- 修補程式在內部準備並驗證。
- 修復在下一個修補版本中發佈。
- 公開揭露（CVE/安全公告）在 30 天後與回報者協調進行，或經雙方同意提前揭露。

## 已知限制

- 開發環境設定（`ENV=dev`）出於本地除錯目的有意放寬安全檢查。**切勿將 `dev` 部署到生產環境。**
- Vertex AI 的 Google Cloud 憑證應透過金鑰管理系統（Kubernetes secrets、Vault 等）掛載，而不是提交到程式碼倉庫中。

---

*最後更新：2026-06-05*
