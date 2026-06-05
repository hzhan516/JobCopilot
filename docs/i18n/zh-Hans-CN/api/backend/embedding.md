<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../../api/backend/embedding.md) | [简体中文](embedding.md) | [繁體中文](../../zh-Hant-TW/api/backend/embedding.md)

# Embedding / 职位向量模块 API 文档

本模块为 AI 层提供批量向量数据写入能力，主要用于将预计算的职位嵌入向量迁移到 `job_vectors` 表中。

---

## 1. 客户端到后端接口

### 1.1 批量 Upsert 职位向量

**接口：** `POST /api/v1/job-vectors/batch`
**说明：** 批量写入职位向量到数据库。如果 `jobId` 已存在且内容完全相同，则跳过（去重）；如果内容不同则更新；否则插入新记录。

**鉴权：** 仅限内部服务调用。配置 `INTERNAL_API_KEY` 时必须携带 `X-Internal-API-Key`，不得公网暴露。

**请求头：**
```http
Content-Type: application/json
```

**请求体（`BatchJobVectorUpsertRequest`）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `items` | List | 是 | 待 upsert 的职位向量条目列表 |

**条目字段（`JobVectorItem`）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `jobId` | String | 是 | 职位唯一标识 |
| `embedding` | List<Float> | 是 | 嵌入向量（维度须与模型一致，默认 1536） |
| `title` | String | 否 | 职位标题 |
| `description` | String | 否 | 职位描述 |
| `requirements` | List<String> | 否 | 职位要求列表 |
| `rawContent` | String | 否 | 用于生成嵌入的原始文本 |
| `sourceFile` | String | 否 | 来源文件路径或标识 |
| `modelVersion` | String | 否 | 生成嵌入的模型版本（默认：`gemini-embedding-001`） |

**请求示例：**
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

**成功响应（200）：**

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

**响应字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `total` | Integer | 接收到的条目总数 |
| `success` | Integer | 成功持久化的条目数（插入或更新） |
| `failed` | Integer | 持久化失败的条目数 |
| `skipped` | Integer | 因现有记录内容完全相同而跳过的条目数 |
| `failedJobIds` | List<String> | 失败的职位 ID 列表 |

**错误响应（500）：**

```json
{
  "code": 500,
  "message": "Internal server error",
  "data": null
}
```

---

### 1.2 批量 Upsert 简历向量

**接口：** `POST /api/v1/resume-vectors/batch`
**说明：** 批量写入简历向量到数据库。如果 `resumeVersionId` 已存在且嵌入向量相同，则跳过（去重）；如果不同则更新；否则插入新记录。

**鉴权：** 仅限内部服务调用。配置 `INTERNAL_API_KEY` 时必须携带 `X-Internal-API-Key`，不得公网暴露。

**请求头：**
```http
Content-Type: application/json
```

**请求体（`BatchResumeVectorUpsertRequest`）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `items` | List | 是 | 待 upsert 的简历向量条目列表 |

**条目字段（`ResumeVectorItem`）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `resumeVersionId` | String | 是 | 简历版本唯一标识 |
| `embedding` | List<Float> | 是 | 嵌入向量（维度须与模型一致，默认 1536） |

**请求示例：**
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

**成功响应（200）：**

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

**响应字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `total` | Integer | 接收到的条目总数 |
| `success` | Integer | 成功持久化的条目数（插入或更新） |
| `failed` | Integer | 持久化失败的条目数 |
| `skipped` | Integer | 因现有记录内容完全相同而跳过的条目数 |
| `failedResumeVersionIds` | List<String> | 失败的简历版本 ID 列表 |

---

### 1.3 嵌入向量生成（后端 → AI 服务）

**接口：** `POST /api/v1/ai/embeddings`（由 AI 服务托管）
**说明：** 为一条或多条输入文本生成嵌入向量。后端通过 `EmbeddingService` 调用此接口。

**请求体（`EmbeddingRequest`）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `texts` | List<String> | 是 | 待嵌入的输入文本列表 |
| `model` | String | 否 | 覆盖默认嵌入模型 |

**请求示例（单条文本，后端调用方式）：**
```json
{
  "texts": ["Software Engineer with 5 years of experience..."]
}
```

**响应体（`EmbeddingResponse`）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `embeddings` | List<List<Float>> | 嵌入向量列表，每条输入文本对应一个 |
| `modelUsed` | String | 实际使用的模型标识 |
| `count` | Integer | 返回的嵌入向量数量 |

**响应示例：**
```json
{
  "embeddings": [[0.0123, -0.0456, 0.0789, "..."]],
  "modelUsed": "gemini-embedding-001",
  "count": 1
}
```

> **注意：** 后端 `EmbeddingService` 将单条文本包装在 `texts` 数组中发送，并从响应中提取 `embeddings[0]`。

---

### 1.4 同步向量生成（内部调用）

**调用方**：`JobApplicationService`、`ResumeApplicationService`、`ConversationApplicationService`、`MatchingApplicationService`
**被调方**：`VectorFacade.generateAndSaveVector(referenceId, entityType, text)`

这是内部同步 REST 调用链（不涉及 MQ）：

```
应用服务 -> VectorFacade
  -> VectorApplicationService
    -> EmbeddingService.generate(text)
      -> POST /api/v1/ai/embeddings (AI 服务)
    -> 保存到 job_vectors / resume_vectors
```

**行为：**
- 成功时：嵌入向量保存到 `job_vectors` 或 `resume_vectors`，状态为 `COMPLETED`
- 失败时（AI 服务不可用或维度不匹配）：保存状态为 `FAILED` 的记录，并填充 `error_message`
- 调用是同步的，会阻塞调用方事务，但异常会被捕获以避免级联失败

---

## 2. 数据同步流程

### 启动同步（AI 服务 → 后端）

AI 服务启动时，会在后台线程中自动执行以下步骤：

1. 读取 `data_pipeline/output/normalized_jobs_sample.jsonl`
2. 使用 `generate_embedding()` 为每条记录生成嵌入向量
3. 将结果分批（默认批次大小：100）并调用 `POST /api/v1/job-vectors/batch`
4. 后端执行去重：如果 `jobId` 已存在且内容完全相同，则跳过

这确保了 `job_vectors` 表无需手动数据迁移脚本即可填充预计算的嵌入向量。

---

## 3. 数据模型

### job_vectors 表（扩展）

| 列名 | 类型 | 说明 |
|------|------|------|
| `id` | VARCHAR(64) | 主键 |
| `job_id` | VARCHAR(64) | 职位唯一标识 |
| `embedding` | vector(1536) | pgvector 嵌入数组 |
| `status` | VARCHAR(32) | 向量状态：`PENDING`、`COMPLETED`、`FAILED` |
| `title` | TEXT | 职位标题 |
| `description` | TEXT | 职位描述 |
| `requirements` | JSONB | 要求列表 |
| `raw_content` | TEXT | 原始文本内容 |
| `source_file` | VARCHAR(255) | 来源文件标识 |
| `model_version` | VARCHAR(50) | 嵌入模型版本 |
| `created_at` | TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | 最后更新时间 |

**索引：**
- `idx_job_vectors_fts` — `raw_content` 的 GIN 全文搜索索引
- `idx_job_vectors_embedding_hnsw` — `embedding` 的 HNSW 近似最近邻索引

### resume_vectors 表

| 列名 | 类型 | 说明 |
|------|------|------|
| `id` | VARCHAR(64) | 主键 |
| `resume_version_id` | VARCHAR(64) | 简历版本唯一标识 |
| `embedding` | vector(1536) | pgvector 嵌入数组 |
| `status` | VARCHAR(32) | 向量状态：`PENDING`、`COMPLETED`、`FAILED` |
| `error_message` | TEXT | 生成失败时的错误信息 |
| `created_at` | TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | 最后更新时间 |
