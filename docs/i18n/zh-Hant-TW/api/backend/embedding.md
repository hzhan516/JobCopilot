<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/embedding.md) | [简体中文](../../../zh-Hans-CN/api/backend/embedding.md) | [繁體中文](embedding.md)

# 嵌入向量 / 職位向量模組 API 文件

本模組為 AI 層提供批次向量資料寫入能力，主要用於將預先計算好的職位嵌入向量遷移到 `job_vectors` 資料表中。

---

## 1. 客戶端與後端互動介面

### 1.1 批次 Upsert 職位向量

**Endpoint:** `POST /api/v1/job-vectors/batch`
**描述:** 批次 Upsert 職位向量到資料庫。若 `jobId` 已存在，則更新其向量資料與中繼資料；否則插入新記錄。

**認證:** 不需要（供內部 AI 服務使用）

**Request Header:**
```http
Content-Type: application/json
```

**Request Body (`BatchJobVectorUpsertRequest`):**

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `items` | List | 是 | 待 Upsert 的職位向量項目列表 |

**項目欄位 (`JobVectorItem`):**

| 欄位 | 類型 | 必填 | 說明 |
|------|------|------|------|
| `jobId` | String | 是 | 唯一職位識別碼 |
| `embedding` | List<Float> | 是 | 嵌入向量（維度須與模型一致，預設 1536） |
| `title` | String | 否 | 職位標題 |
| `description` | String | 否 | 職位描述 |
| `requirements` | List<String> | 否 | 職位要求列表 |
| `rawContent` | String | 否 | 用於生成嵌入的原始文字內容 |
| `sourceFile` | String | 否 | 來源檔案路徑或識別碼 |
| `modelVersion` | String | 否 | 嵌入生成所用模型版本（預設：`gemini-embedding-001`） |

**請求範例:**
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

**成功回應 (200):**

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "total": 50,
    "success": 50,
    "failed": 0,
    "failedJobIds": []
  }
}
```

**回應欄位說明:**

| 欄位 | 類型 | 說明 |
|------|------|------|
| `total` | Integer | 接收項目總數 |
| `success` | Integer | 成功持久化數量 |
| `failed` | Integer | 失敗數量 |
| `failedJobIds` | List<String> | 失敗的職位 ID 列表 |

**錯誤回應 (500):**

```json
{
  "code": 500,
  "message": "Internal server error",
  "data": null
}
```

---

## 2. 資料模型

### job_vectors 資料表（擴展後）

| 欄位 | 類型 | 說明 |
|------|------|------|
| `id` | VARCHAR(64) | 主鍵 |
| `job_id` | VARCHAR(64) | 唯一職位識別碼 |
| `embedding` | vector(1536) | pgvector 嵌入向量 |
| `status` | VARCHAR(32) | 向量狀態：`PENDING`、`COMPLETED`、`FAILED` |
| `title` | TEXT | 職位標題 |
| `description` | TEXT | 職位描述 |
| `requirements` | JSONB | 要求列表 |
| `raw_content` | TEXT | 原始文字內容 |
| `source_file` | VARCHAR(255) | 來源檔案識別碼 |
| `model_version` | VARCHAR(50) | 嵌入模型版本 |
| `created_at` | TIMESTAMP | 建立時間 |
| `updated_at` | TIMESTAMP | 更新時間 |

**索引:**
- `idx_job_vectors_fts` — GIN 全文搜尋索引，基於 `raw_content`
- `idx_job_vectors_embedding_hnsw` — HNSW 近似最近鄰索引，基於 `embedding`
