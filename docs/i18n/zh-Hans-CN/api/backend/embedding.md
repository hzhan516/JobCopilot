<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/embedding.md) | [简体中文](embedding.md) | [繁體中文](../../../zh-Hant-TW/api/backend/embedding.md)

# 嵌入向量 / 职位向量模块 API 文档

本模块为 AI 层提供批量向量数据写入能力，主要用于将预计算好的职位嵌入向量迁移到 `job_vectors` 表中。

---

## 1. 客户端与后端交互接口

### 1.1 批量 Upsert 职位向量

**Endpoint:** `POST /api/v1/job-vectors/batch`
**描述:** 批量 Upsert 职位向量到数据库。若 `jobId` 已存在，则更新其向量数据与元数据；否则插入新记录。

**认证:** 不需要（供内部 AI 服务使用）

**Request Header:**
```http
Content-Type: application/json
```

**Request Body (`BatchJobVectorUpsertRequest`):**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `items` | List | 是 | 待 Upsert 的职位向量条目列表 |

**条目字段 (`JobVectorItem`):**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `jobId` | String | 是 | 唯一职位标识符 |
| `embedding` | List<Float> | 是 | 嵌入向量（维度须与模型一致，默认 1536） |
| `title` | String | 否 | 职位标题 |
| `description` | String | 否 | 职位描述 |
| `requirements` | List<String> | 否 | 职位要求列表 |
| `rawContent` | String | 否 | 用于生成嵌入的原始文本内容 |
| `sourceFile` | String | 否 | 来源文件路径或标识符 |
| `modelVersion` | String | 否 | 嵌入生成所用模型版本（默认：`gemini-embedding-001`） |

**请求示例:**
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

**成功响应 (200):**

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

**响应字段说明:**

| 字段 | 类型 | 说明 |
|------|------|------|
| `total` | Integer | 接收条目总数 |
| `success` | Integer | 成功持久化数量 |
| `failed` | Integer | 失败数量 |
| `failedJobIds` | List<String> | 失败的职位 ID 列表 |

**错误响应 (500):**

```json
{
  "code": 500,
  "message": "Internal server error",
  "data": null
}
```

---

## 2. 数据模型

### job_vectors 表（扩展后）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | VARCHAR(64) | 主键 |
| `job_id` | VARCHAR(64) | 唯一职位标识符 |
| `embedding` | vector(1536) | pgvector 嵌入向量 |
| `status` | VARCHAR(32) | 向量状态：`PENDING`、`COMPLETED`、`FAILED` |
| `title` | TEXT | 职位标题 |
| `description` | TEXT | 职位描述 |
| `requirements` | JSONB | 要求列表 |
| `raw_content` | TEXT | 原始文本内容 |
| `source_file` | VARCHAR(255) | 来源文件标识 |
| `model_version` | VARCHAR(50) | 嵌入模型版本 |
| `created_at` | TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | 更新时间 |

**索引:**
- `idx_job_vectors_fts` — GIN 全文搜索索引，基于 `raw_content`
- `idx_job_vectors_embedding_hnsw` — HNSW 近似最近邻索引，基于 `embedding`
