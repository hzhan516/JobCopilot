<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](embedding.md) | [简体中文](../../i18n/zh-Hans-CN/api/backend/embedding.md) | [繁體中文](../../i18n/zh-Hant-TW/api/backend/embedding.md)

# Embedding / Job Vector Module API Documentation

This module provides batch vector data ingestion capabilities for the AI layer. It is primarily used for migrating pre-computed job embeddings into the `job_vectors` table.

---

## 1. Client to Backend Interfaces

### 1.1 Batch Upsert Job Vectors

**Endpoint:** `POST /api/v1/job-vectors/batch`
**Description:** Batch upsert job vectors into the database. If a `jobId` already exists, its vector data and metadata will be updated; otherwise, a new record is inserted.

**Authentication:** Not required (intended for internal AI service use)

**Request Header:**
```http
Content-Type: application/json
```

**Request Body (`BatchJobVectorUpsertRequest`):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `items` | List | Yes | List of job vector items to upsert |

**Item Fields (`JobVectorItem`):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `jobId` | String | Yes | Unique job identifier |
| `embedding` | List<Float> | Yes | Embedding vector (dimension must match model, default 1536) |
| `title` | String | No | Job title |
| `description` | String | No | Job description |
| `requirements` | List<String> | No | List of job requirements |
| `rawContent` | String | No | Raw text content used for embedding generation |
| `sourceFile` | String | No | Source file path or identifier |
| `modelVersion` | String | No | Model version used for embedding generation (default: `gemini-embedding-001`) |

**Request Example:**
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

**Success Response (200):**

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

**Response Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `total` | Integer | Total number of items received |
| `success` | Integer | Number of items successfully persisted |
| `failed` | Integer | Number of items that failed to persist |
| `failedJobIds` | List<String> | List of job IDs that failed |

**Error Response (500):**

```json
{
  "code": 500,
  "message": "Internal server error",
  "data": null
}
```

---

## 2. Data Model

### job_vectors Table (Extended)

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(64) | Primary key |
| `job_id` | VARCHAR(64) | Unique job identifier |
| `embedding` | vector(1536) | pgvector embedding array |
| `status` | VARCHAR(32) | Vector status: `PENDING`, `COMPLETED`, `FAILED` |
| `title` | TEXT | Job title |
| `description` | TEXT | Job description |
| `requirements` | JSONB | List of requirements |
| `raw_content` | TEXT | Raw text content |
| `source_file` | VARCHAR(255) | Source file identifier |
| `model_version` | VARCHAR(50) | Embedding model version |
| `created_at` | TIMESTAMP | Creation time |
| `updated_at` | TIMESTAMP | Last update time |

**Indexes:**
- `idx_job_vectors_fts` — GIN full-text search index on `raw_content`
- `idx_job_vectors_embedding_hnsw` — HNSW approximate nearest neighbor index on `embedding`
