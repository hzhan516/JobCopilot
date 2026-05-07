<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/embedding.md) | [简体中文](../../zh-Hans-CN/api/backend/embedding.md) | [繁體中文](embedding.md)

# Embedding / 職位向量模組 API 文件

本模組為 AI 層提供批量向量資料寫入能力，主要用於將預先計算的職位嵌入向量遷移到 `job_vectors` 資料表中。

---

## 1. 用戶端到後端介面

### 1.1 批量 Upsert 職位向量

**介面：** `POST /api/v1/job-vectors/batch`
**說明：** 批量寫入職位向量到資料庫。如果 `jobId` 已存在且內容完全相同，則跳過（去重）；如果內容不同則更新；否則插入新記錄。

**驗證：** 不需要（僅供內部 AI 服務使用）

**請求標頭：**
```http
Content-Type: application/json
```

**請求主體（`BatchJobVectorUpsertRequest`）：**

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `items` | List | 是 | 待 upsert 的職位向量項目列表 |

**項目欄位（`JobVectorItem`）：**

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `jobId` | String | 是 | 職位唯一識別碼 |
| `embedding` | List<Float> | 是 | 嵌入向量（維度須與模型一致，預設 1536） |
| `title` | String | 否 | 職位標題 |
| `description` | String | 否 | 職位描述 |
| `requirements` | List<String> | 否 | 職位要求列表 |
| `rawContent` | String | 否 | 用於生成嵌入的原始文字 |
| `sourceFile` | String | 否 | 來源檔案路徑或識別碼 |
| `modelVersion` | String | 否 | 生成嵌入的模型版本（預設：`gemini-embedding-001`） |

**請求範例：**
```json
{
  "items": [
    {
      "jobId": "job-921716",
      "embedding": [0.0123, -0.0456, 0.0789, "..."],
      "title": "Marketing Coordinator",
      "description": "A leading real estate firm in New Jersey is seeking...",
      "requirements": ["Adobe Creative Cloud", "Microsoft Office Suite"],
      "rawContent": "Marketing Coordinator...",
      "sourceFile": "normalized_jobs_sample.jsonl",
      "modelVersion": "gemini-embedding-001"
    }
  ]
}
```

**成功回應（200）：**

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "total": 50,
    "success": 48,
    "failed": 0,
    "skipped": 2,
    "failedJobIds": []
  }
}
```

**回應欄位說明：**

| 欄位 | 類型 | 說明 |
|------|------|------|
| `total` | Integer | 接收到的項目總數 |
| `success` | Integer | 成功持久化的項目數（插入或更新） |
| `failed` | Integer | 持久化失敗的項目數 |
| `skipped` | Integer | 因現有記錄內容完全相同而跳過的項目數 |
| `failedJobIds` | List<String> | 失敗的職位 ID 列表 |

**錯誤回應（500）：**

```json
{
  "code": 500,
  "message": "Internal server error",
  "data": null
}
```

---

### 1.2 批量 Upsert 履歷向量

**介面：** `POST /api/v1/resume-vectors/batch`
**說明：** 批量寫入履歷向量到資料庫。如果 `resumeVersionId` 已存在且嵌入向量相同，則跳過（去重）；如果不同則更新；否則插入新記錄。

**驗證：** 不需要（僅供內部 AI 服務使用）

**請求標頭：**
```http
Content-Type: application/json
```

**請求主體（`BatchResumeVectorUpsertRequest`）：**

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `items` | List | 是 | 待 upsert 的履歷向量項目列表 |

**項目欄位（`ResumeVectorItem`）：**

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `resumeVersionId` | String | 是 | 履歷版本唯一識別碼 |
| `embedding` | List<Float> | 是 | 嵌入向量（維度須與模型一致，預設 1536） |

**請求範例：**
```json
{
  "items": [
    {
      "resumeVersionId": "resume-v1-001",
      "embedding": [0.0123, -0.0456, 0.0789, "..."]
    }
  ]
}
```

**成功回應（200）：**

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "total": 10,
    "success": 10,
    "failed": 0,
    "skipped": 0,
    "failedResumeVersionIds": []
  }
}
```

**回應欄位說明：**

| 欄位 | 類型 | 說明 |
|------|------|------|
| `total` | Integer | 接收到的項目總數 |
| `success` | Integer | 成功持久化的項目數（插入或更新） |
| `failed` | Integer | 持久化失敗的項目數 |
| `skipped` | Integer | 因現有記錄內容完全相同而跳過的項目數 |
| `failedResumeVersionIds` | List<String> | 失敗的履歷版本 ID 列表 |

---

### 1.3 嵌入向量生成（後端 → AI 服務）

**介面：** `POST /api/v1/ai/embeddings`（由 AI 服務託管）
**說明：** 為一條或多條輸入文字生成嵌入向量。後端透過 `EmbeddingService` 呼叫此介面。

**請求主體（`EmbeddingRequest`）：**

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `texts` | List<String> | 是 | 待嵌入的輸入文字列表 |
| `model` | String | 否 | 覆蓋預設嵌入模型 |

**請求範例（單條文字，後端呼叫方式）：**
```json
{
  "texts": ["Software Engineer with 5 years of experience..."]
}
```

**回應主體（`EmbeddingResponse`）：**

| 欄位 | 類型 | 說明 |
|------|------|------|
| `embeddings` | List<List<Float>> | 嵌入向量列表，每條輸入文字對應一個 |
| `modelUsed` | String | 實際使用的模型識別碼 |
| `count` | Integer | 返回的嵌入向量數量 |

**回應範例：**
```json
{
  "embeddings": [[0.0123, -0.0456, 0.0789, "..."]],
  "modelUsed": "gemini-embedding-001",
  "count": 1
}
```

> **注意：** 後端 `EmbeddingService` 將單條文字包裝在 `texts` 陣列中發送，並從回應中提取 `embeddings[0]`。

---

### 1.4 同步向量生成（內部呼叫）

**呼叫方**：`JobApplicationService`、`ResumeApplicationService`、`ConversationApplicationService`、`MatchingApplicationService`
**被叫方**：`VectorFacade.generateAndSaveVector(referenceId, entityType, text)`

這是內部同步 REST 呼叫鏈（不涉及 MQ）：

```
應用服務 -> VectorFacade
  -> VectorApplicationService
    -> EmbeddingService.generate(text)
      -> POST /api/v1/ai/embeddings (AI 服務)
    -> 儲存到 job_vectors / resume_vectors
```

**行為：**
- 成功時：嵌入向量儲存到 `job_vectors` 或 `resume_vectors`，狀態為 `COMPLETED`
- 失敗時（AI 服務不可用或維度不匹配）：儲存狀態為 `FAILED` 的記錄，並填充 `error_message`
- 呼叫是同步的，會阻塞呼叫方交易，但異常會被捕獲以避免級聯失敗

---

## 2. 資料同步流程

### 啟動同步（AI 服務 → 後端）

AI 服務啟動時，會在背景執行緒中自動執行以下步驟：

1. 讀取 `data_pipeline/output/normalized_jobs_sample.jsonl`
2. 使用 `generate_embedding()` 為每條記錄生成嵌入向量
3. 將結果分批（預設批次大小：100）並呼叫 `POST /api/v1/job-vectors/batch`
4. 後端執行去重：如果 `jobId` 已存在且內容完全相同，則跳過

這確保了 `job_vectors` 資料表無需手動資料遷移指令碼即可填充預先計算的嵌入向量。

---

## 3. 資料模型

### job_vectors 資料表（擴展）

| 欄位名稱 | 類型 | 說明 |
|----------|------|------|
| `id` | VARCHAR(64) | 主鍵 |
| `job_id` | VARCHAR(64) | 職位唯一識別碼 |
| `embedding` | vector(1536) | pgvector 嵌入陣列 |
| `status` | VARCHAR(32) | 向量狀態：`PENDING`、`COMPLETED`、`FAILED` |
| `title` | TEXT | 職位標題 |
| `description` | TEXT | 職位描述 |
| `requirements` | JSONB | 要求列表 |
| `raw_content` | TEXT | 原始文字內容 |
| `source_file` | VARCHAR(255) | 來源檔案識別碼 |
| `model_version` | VARCHAR(50) | 嵌入模型版本 |
| `created_at` | TIMESTAMP | 建立時間 |
| `updated_at` | TIMESTAMP | 最後更新時間 |

**索引：**
- `idx_job_vectors_fts` — `raw_content` 的 GIN 全文搜尋索引
- `idx_job_vectors_embedding_hnsw` — `embedding` 的 HNSW 近似最近鄰索引

### resume_vectors 資料表

| 欄位名稱 | 類型 | 說明 |
|----------|------|------|
| `id` | VARCHAR(64) | 主鍵 |
| `resume_version_id` | VARCHAR(64) | 履歷版本唯一識別碼 |
| `embedding` | vector(1536) | pgvector 嵌入陣列 |
| `status` | VARCHAR(32) | 向量狀態：`PENDING`、`COMPLETED`、`FAILED` |
| `error_message` | TEXT | 生成失敗時的錯誤訊息 |
| `created_at` | TIMESTAMP | 建立時間 |
| `updated_at` | TIMESTAMP | 最後更新時間 |
