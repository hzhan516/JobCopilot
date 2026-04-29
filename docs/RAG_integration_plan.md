# RAG (Retrieval-Augmented Generation) 集成方案讨论稿

> 目标：明确前端、后端、AI 服务三层在 RAG 体系中的职责与下一步行动。

---

## 一、RAG 是什么 & 为什么我们需要它

**定义**：RAG = **检索（Retrieve）+ 生成（Generate）**。AI 回答用户问题时，先从一个向量化知识库里检索最相关的文档片段，再把这些片段作为上下文交给 LLM 生成答案。

**为什么我们需要**：
- 当前对话模块直接把整份简历/职位 JD 硬塞进 Prompt，token 浪费严重，长文档会截断
- 职位匹配每次查询实时 embed 200 条公开数据，API 调用成本高、响应慢
- 没有外部知识支撑，AI 无法回答通用问题（如"Marketing Coordinator 需要什么技能"）

**最终目标**：用户提交的每一个职位 URL，解析后自动进入知识库；用户提问时，系统只检索最相关的片段回答，而不是读全文。

---

## 二、RAG 架构全景

```
用户提问 / 简历上传 / 职位提交
       ↓
┌──────────────────────────────────────────┐
│  前端 (React)                              │
│  - 展示 RAG 检索来源（高亮引用了哪段 JD）   │
│  - 对话流式展示，附带引用标签               │
└────────────┬───────────────────────────────┘
             │ HTTP / REST
┌────────────▼───────────────────────────────┐
│  后端 (Java Spring Boot / DDD)              │
│  - 向量存储 & 检索 (pgvector)              │
│  - 文档分块 & 索引管理                     │
│  - 召回服务：query → top-k chunks          │
│  - MQ 调度：把检索结果发给 AI 服务          │
└────────────┬───────────────────────────────┘
             │ RabbitMQ
┌────────────▼───────────────────────────────┐
│  AI 服务 (Python FastAPI)                   │
│  - 文本 Embedding 生成                     │
│  - 文档解析 & 分块                         │
│  - Prompt 组装：检索片段 + 用户问题 → LLM   │
│  - 答案生成 & 结果回传                     │
└──────────────────────────────────────────┘
```

---

## 三、分部分现状 & 行动意见

---

### 3.1 前端 (React + TypeScript)

#### 现状
- 对话界面只展示 AI 文本回复，没有显示「AI 参考了哪段内容」
- 职位详情页展示完整 JD，没有「相关简历段落高亮」或「匹配度依据」
- 用户无法感知 RAG 的存在

#### 需要做什么
| 优先级 | 事项 | 说明 |
|--------|------|------|
| 🔴 P1 | **对话引用标签** | AI 回复底部展示引用的 chunk 来源（如 "引用自：职位 JD — 技能要求"），可点击展开 |
| 🔴 P1 | **职位-简历匹配高亮** | 在职位详情页或对比页，高亮简历中与该职位要求匹配的段落 |
| 🟡 P2 | **检索来源面板** | 对话界面侧边栏展示本次检索召回的 top-k 文档片段列表 |
| 🟢 P3 | **知识库管理入口** | 管理员/用户查看已入库的职位知识库，支持手动删除/刷新 |

#### 接口依赖（需要后端提供）
- `GET /api/v1/rag/context?query=...&resumeVersionId=...` → 返回检索到的 chunks 列表
- AI 回复中增加 `citations: [{source, chunkId, snippet}]` 字段

---

### 3.2 后端 (Java / DDD 六边形架构)

#### 现状 ✅（已具备）
- `pgvector` 扩展已启用，有 `job_vectors` / `resume_vectors` 表
- `PgVectorSearchService` 已能用 `<=>` 做余弦相似度检索
- MQ 链路完整：`VectorGenCommand` → AI Service → `AiResultEvent`
- 职位匹配流程：向量召回 → MQ 精排，本质已是 RAG Pipeline

#### 缺口 ❌（需要补）
| 缺口 | 影响 | 解决方式 |
|------|------|----------|
| **无文档分块表** | 一个简历/职位 = 一个整体向量，粒度太粗 | 新建 `resume_chunks` / `job_chunks` 表 |
| **无 chunk 级检索接口** | 对话时无法只召回「工作经验」段落 | 新增 `ChunkRepository` + `ChunkSearchService` |
| **无 HNSW 索引** | 数据量大了以后向量扫描慢 | 给 embedding 列加 `USING hnsw` 索引 |
| **无混合检索** | 纯向量检索，精确关键词召回不全 | PostgreSQL `tsvector` 全文索引 + 向量融合 (RRF) |
| **MQ 契约未覆盖 chunk** | `VectorGenCommand` 只发整体文本 | 扩展为支持多 chunk 批量生成 & 回传 |

#### 行动意见
| 优先级 | 任务 | 技术要点 | 预计工时 |
|--------|------|----------|----------|
| 🔴 P1 | **Flyway V11: 创建 chunk 表** | `resume_chunks(id, resume_version_id, section_type, content, embedding vector(1536), status)`；job_chunks 同理 | 0.5d |
| 🔴 P1 | **新增 Chunk 领域层** | `Chunk` 实体、`ChunkRepository` 接口、`ChunkSearchService` 领域服务 | 1d |
| 🔴 P1 | **新增 Chunk 基础设施实现** | JPA Entity、RepositoryImpl、HNSW 索引创建 SQL | 1d |
| 🟡 P2 | **扩展 MQ 契约** | `VectorGenCommand` 支持 `chunkIndex`、`totalChunks`；`AiResultEvent.data` 增加 `chunkId` | 0.5d |
| 🟡 P2 | **新增 RAG 查询 API** | `RagFacade` → `RagApplicationService` → `ChunkSearchService`，返回 `RagContextResponse` | 1d |
| 🟢 P3 | **HNSW 索引迁移** | 等 chunk 表数据量 > 1万 时再执行，现在加也可以预留 | 0.25d |
| 🟢 P3 | **混合检索 (RRF)** | 后期优化项，先用纯向量验证链路 | — |

#### 数据库设计（Chunk 表）

```sql
-- resume_chunks: 简历分块向量存储
CREATE TABLE IF NOT EXISTS resume_chunks (
    id VARCHAR(64) PRIMARY KEY,
    resume_version_id VARCHAR(64) NOT NULL,
    section_type VARCHAR(32) NOT NULL,  -- SKILLS / EXPERIENCE / EDUCATION / PROJECTS / SUMMARY
    chunk_index INT NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    embedding vector(1536),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resume_chunks_version ON resume_chunks(resume_version_id);
CREATE INDEX idx_resume_chunks_section ON resume_chunks(section_type);

-- job_chunks: 职位 JD 分块向量存储（URL 抓取的职位自动分块入库）
CREATE TABLE IF NOT EXISTS job_chunks (
    id VARCHAR(64) PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    section_type VARCHAR(32) NOT NULL,  -- OVERVIEW / RESPONSIBILITIES / REQUIREMENTS / BENEFITS
    chunk_index INT NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    embedding vector(1536),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_job_chunks_job_id ON job_chunks(job_id);
CREATE INDEX idx_job_chunks_section ON job_chunks(section_type);
```

---

### 3.3 AI 服务 (Python FastAPI)

#### 现状 ✅（已具备）
- `vector_service.py`：Google Vertex AI Gemini 生成 embedding，`RETRIEVAL_DOCUMENT` task type
- `job_parser.py` / `resume_parser.py`：文本解析能力
- `conversation_service.py`：对话 Prompt 组装，但**目前是全篇硬塞**

#### 缺口 ❌（需要补）
| 缺口 | 影响 | 解决方式 |
|------|------|----------|
| **无文档分块逻辑** | 简历/JD 进来后没有切成段落 | 新增 `chunking_service.py` |
| **无 chunk 级 embedding** | 只能生成整体向量 | `vector_service.py` 支持批量 chunk embed |
| **对话 Prompt 未接入检索** | AI 看不到最相关的片段 | 修改 `conversation_service.py`，从 `retrieved_chunks` 读取 |
| **URL 抓取职位未自动入库** | 用户提交职位后只解析，没进知识库 | `job_orchestrator.py` 解析后触发分块 → embed → MQ 回传 |
| **200 条数据逐条实时 embed** | 每次查询调 200 次 API，慢且贵 | 预 embed 一次存库，查询只 embed query |

#### 行动意见
| 优先级 | 任务 | 技术要点 | 预计工时 |
|--------|------|----------|----------|
| 🔴 P1 | **新增 `chunking_service.py`** | 按 section 分块：简历分 5 类，JD 分 4 类；每块不超过 512 tokens | 1d |
| 🔴 P1 | **修改 `vector_service.py`** | 支持 `generate_embeddings_batch(chunks: list[str]) -> list[list[float]]` | 0.5d |
| 🔴 P1 | **修改 `conversation_service.py`** | Prompt 模板增加 `retrieved_chunks` 占位；优先引用 chunk 内容回答 | 1d |
| 🔴 P1 | **修改 `job_orchestrator.py`** | 职位 URL 解析完成后，调用 chunking → embed → 通过 MQ 回传 `job_chunks` | 1d |
| 🟡 P2 | **预 embed 200 条公开数据** | 写一次性脚本，批量 embed 后通过 MQ 或直接 HTTP 写入后端 `job_chunks` | 0.5d |
| 🟡 P2 | **新增 `rag_context_handler.py`** | 消费后端发来的 RAG 检索请求，返回组装好的 context（如果需要 AI 层做二次过滤） | 1d |
| 🟢 P3 | **运行时知识库自积累** | 从对话中提取高频问答对，向量化后存入 `faq_chunks` | — |

#### 分块策略（Chunking Strategy）

```python
# 简历分块规则
RESUME_SECTIONS = {
    "SUMMARY":     r"(?i)(summary|profile|objective|about me)",
    "SKILLS":      r"(?i)(skills|technologies|tools|competencies)",
    "EXPERIENCE":  r"(?i)(experience|work history|employment)",
    "EDUCATION":   r"(?i)(education|academic|degree)",
    "PROJECTS":    r"(?i)(projects|portfolio)",
}

# 职位 JD 分块规则
JOB_SECTIONS = {
    "OVERVIEW":         r"(?i)(about us|company|overview|who we are)",
    "RESPONSIBILITIES": r"(?i)(responsibilities|what you.*do|role|duties)",
    "REQUIREMENTS":     r"(?i)(requirements|qualifications|what you.*need|skills required)",
    "BENEFITS":         r"(?i)(benefits|perks|compensation|we offer)",
}
```

每块 **max_tokens ≈ 512**（约 2000 字符），超长段落按句子边界二次切分。

---

## 四、RAG 数据流：用户提交职位 URL → 入库 → 被检索

```
用户粘贴 LinkedIn/Indeed URL
       ↓
前端: POST /api/v1/jobs/submit {url}
       ↓
后端: JobApplicationService.submitJob()
      → 保存 job 记录 (status=PENDING)
      → MQ: JobParseCommand → AI Service
       ↓
AI Service: job_parser.py 抓取 & 解析 JD 文本
      → chunking_service.py 分块 (4 section types)
      → vector_service.py 批量 embed (每块一个 1536 维向量)
      → MQ: AiResultEvent(type=JOB_PARSE) 回传结构化数据
      → MQ: VectorGenCommand (批量, 每块一个) 或扩展新事件
       ↓
后端: AiResultMessageListener
      → 保存 parsed job 数据到 jobs 表
      → 保存 chunks 到 job_chunks 表 (embedding + content + section_type)
       ↓
[知识库就绪]

用户提问: "我的 AWS 经验够不够这个职位？"
       ↓
后端: ConversationController
      → 先把问题 embed → 查 resume_chunks (用户简历) + job_chunks (该职位)
      → 召回 top-5 相关 chunks
      → 组装 RAG context → MQ: ConversationRequestCommand (带 retrievedChunks)
       ↓
AI Service: conversation_service.py
      → Prompt 中注入 retrievedChunks
      → 只基于相关片段生成答案
      → 回传 AI 回复 (带 citations)
       ↓
前端: 展示回复 + 可点击的引用来源
```

---

## 五、URL 持续补充知识库机制

**核心原则**：每个用户提交的职位 URL，解析后自动成为 RAG 知识库的一份子。

| 阶段 | 机制 | 说明 |
|------|------|------|
| **冷启动** | 预 embed 200 条公开数据 | 作为种子库，保证系统上线就能用 |
| **运行时增长** | 用户每提交一个 URL → 解析 → 分块 → embed → 入库 | 知识库随用户使用自动扩大 |
| **去重** | URL 去重：已有相同 URL 不重复解析 | 需要 job.url 加 UNIQUE 约束 |
| **过期清理** | 职位 JD 下架后，自动标记 chunks 为 `EXPIRED` | 可配定时任务扫描 |

---

## 六、风险 & 决策点

| 决策 | 选项 | 建议 |
|------|------|------|
| **Embedding 模型** | 继续 Gemini `text-embedding-005`？还是换 `all-MiniLM-L6-v2` 本地跑？ | **继续 Gemini**，统一运维，本地模型增加部署复杂度 |
| **Chunk 存储粒度** | 每 chunk 单独一行 vs 一个 JSONB 列存所有 chunks？ | **单独一行**，方便 HNSW 索引和按 section 过滤检索 |
| **预 embed 200 条** | 由 AI Service 脚本直接写 DB？还是走 MQ 让后端写？ | **走 MQ**，保持"AI 不直连 DB"的架构原则 |
| **对话 RAG 时机** | 先上 resume_chunks（简历对话）还是先上 job_chunks（职位对话）？ | **先 resume_chunks**，用户上传简历后立刻有对话价值 |

---

## 七、MVP 排期建议（4 周）

| Week | 后端 | AI 服务 | 前端 |
|------|------|---------|------|
| **W1** | Flyway V11 + Chunk 领域层 + Repository | `chunking_service.py` + 批量 embed | — |
| **W2** | RAG 查询 API + MQ 契约扩展 | `conversation_service.py` 接入 retrievedChunks | 对话引用标签 UI |
| **W3** | 预 embed 200 条数据脚本 | `job_orchestrator.py` 自动入库流程 | 职位-简历匹配高亮 |
| **W4** | 集成测试 + HNSW 索引 | 运行时自积累 (FAQ) 预留 | 检索来源面板 |

---

*文档版本: v1 | 日期: 2026-04-29*
