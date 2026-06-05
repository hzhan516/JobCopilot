<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../adr/ADR-0002-postgresql-pgvector.md) | [简体中文](ADR-0002-postgresql-pgvector.md) | [繁體中文](../../zh-Hant-TW/adr/ADR-0002-postgresql-pgvector.md)

# ADR-0002: 采用 PostgreSQL + pgvector 作为向量与结构化数据统一存储

| 属性 | 内容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | 后端架构团队 |
| **Affected Modules** | `backend/infrastructure/persistence/`, `backend/domain/*/repository/` |

---

## 1. Context / 背景

JobCopilot 核心功能涉及两类数据的持久化：

| 数据类型 | 示例 | 查询模式 |
|----------|------|----------|
| **结构化数据** | 用户信息、简历元数据、职位描述、对话记录 | 精确匹配（`user_id = ?`）、范围查询、排序分页 |
| **向量数据** | 简历 embedding、职位描述 embedding、对话上下文 embedding | 近似最近邻搜索（ANN, Approximate Nearest Neighbor） |

### 1.1 候选方案

| 方案 | 说明 | 评估维度 |
|------|------|----------|
| **A. PostgreSQL + pgvector** | 在现有关系数据库上通过扩展支持向量类型与 ivfflat/hnsw 索引 | 运维复杂度低，但 ANN 性能非顶级 |
| **B. PostgreSQL + 专用向量库** | 结构数据存 PG，向量存 Milvus / Pinecone / Weaviate | 性能最优，但引入第二数据源、同步复杂度、成本 |
| **C. SQLite + 本地向量库** | 如 `faiss` 本地索引，适用于单机版 | 不满足多用户并发与分布式部署需求 |

---

## 2. Decision / 决策

**采用方案 A：PostgreSQL + pgvector 作为统一的结构化与向量数据存储。**

### 2.1 技术选型详情

| 组件 | 版本 | 用途 |
|------|------|------|
| PostgreSQL | 15+ | 关系型数据主存储 |
| pgvector 扩展 | 0.5.1+ | 提供 `vector` 数据类型、距离运算符、HNSW / ivfflat 索引 |
| hibernate-vector | 6.6.33.Final | Hibernate ORM 原生向量类型映射（`@Vector` 注解） |
| pgvector JDBC | 0.1.6 | 底层 JDBC 向量类型支持 |

### 2.2 领域层 Port 定义

```java
// domain/src/main/java/.../resume/repository/ResumeVectorRepository.java
public interface ResumeVectorRepository {
    /**
     * 存储简历的向量表示
     * Store vector embedding of a resume
     */
    void saveEmbedding(ResumeId id, float[] embedding);

    /**
     * 基于向量相似度搜索最匹配的简历
     * Find top-k most similar resumes by cosine distance
     */
    List<ResumeMatch> findNearest(float[] queryVector, int topK);
}
```

### 2.3 Infrastructure 实现

```java
// infrastructure/src/main/java/.../persistence/embedding/ResumeVectorPgRepository.java
@Repository
public class ResumeVectorPgRepository implements ResumeVectorRepository {
    
    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ResumeMatch> findNearest(float[] queryVector, int topK) {
        // 使用 pgvector 的 <-> (Euclidean) 或 <=> (Cosine) 运算符
        String sql = """
            SELECT r.id, r.title, 1 - (e.vector <=> :query) as similarity
            FROM resume_embeddings e
            JOIN resumes r ON r.id = e.resume_id
            ORDER BY e.vector <=> :query
            LIMIT :topK
            """;
        // 通过 Hibernate-vector 自动完成 Java float[] ↔ pgvector 类型转换
        return em.createNativeQuery(sql, ResumeMatch.class)
                 .setParameter("query", queryVector)
                 .setParameter("topK", topK)
                 .getResultList();
    }
}
```

### 2.4 索引策略

| 索引类型 | 场景 | 配置 |
|----------|------|------|
| **HNSW** | 高维向量（768/1536 维）、高并发查询、允许一定构建时间 | `CREATE INDEX ON resume_embeddings USING hnsw (vector vector_cosine_ops);` |
| **ivfflat** | 向量量级极大（百万级+）、可接受牺牲少量召回率换取构建速度 | `CREATE INDEX ON resume_embeddings USING ivfflat (vector vector_cosine_ops) WITH (lists = 100);` |

当前阶段（万级向量）优先使用 **HNSW**，单表构建时间在秒级，查询延迟 < 10ms。

---

## 3. Consequences / 后果

### 3.1 Positive / 正面

| 收益 | 说明 |
|------|------|
| **单一数据源（SSOT）** | 简历元数据与 embedding 在同一事务内写入，不存在两库同步延迟或数据不一致问题。 |
| **ACID 保障** | 简历保存 + 向量生成 + 向量存储可在同一 `@Transactional` 边界内完成，失败自动回滚。 |
| **运维极简** | 仅需维护 PostgreSQL 一个容器/实例，备份、监控、权限管理统一。 |
| **成本可控** | 无需额外付费向量库（如 Pinecone 按用量计费），自建场景零增量成本。 |
| **Hibernate 原生支持** | `hibernate-vector` 允许在 Entity 中直接声明 `@Vector(dimensions = 1536)`，无需手动 JDBC 类型转换。 |

### 3.2 Negative / 负面

| 成本 | 说明 |
|------|------|
| **ANN 性能天花板** | pgvector 的 HNSW 在千万级向量时性能下降明显，低于专用向量库（Milvus 在同等硬件下通常快 2-5x）。 |
| **维度限制** | pgvector 支持最高 16,000 维，当前 embedding 模型输出 1536 维，充裕但需持续监控模型升级。 |
| **索引构建阻塞** | `CREATE INDEX CONCURRENTLY` 需显式使用，否则大表建索引会锁表。 |
| **备份体积膨胀** | 向量数据为高维浮点数组，备份文件显著大于纯结构化数据，需评估压缩策略。 |

### 3.3 Risks / 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| 向量表数据量增长到百万级，查询性能劣化 | **容量规划**：当前设计保留 `ResumeVectorRepository` Port 接口，未来可透明替换为 Milvus Adapter，无需改动 domain 层。 |
| embedding 模型变更导致向量维度变化 | **Schema 版本化**：向量表增加 `model_version` 字段，不同模型版本的向量分表或分区存储。 |
| pgvector 扩展与 PostgreSQL 主版本升级冲突 | **容器化隔离**：PostgreSQL 通过 Docker 部署，升级时整体替换镜像，避免宿主扩展冲突。 |

---

## 4. Compliance / 合规验证

- **性能基线**：每次版本发布前在 staging 环境执行向量搜索基准测试（1万 / 10万 / 100万 向量量级），延迟 P99 < 50ms。
- **索引监控**：通过 PostgreSQL `pg_stat_user_indexes` 监控 HNSW 索引命中率，低于 90% 时触发告警。
- **架构门禁**：`code-analyzer` 扫描确保 `domain` 层无直接引用 `org.postgresql` 或 `com.pgvector` 包。

---

## 5. Related / 相关决策

- ADR-0001 — 六边形架构（`ResumeVectorRepository` 作为 Driven Port 的物理隔离）
- ADR-0004 — Embedding 服务抽象为 `EmbeddingPort`（向量生成与存储解耦）
- ADR-0006 — Docker Compose 三层网络架构（PostgreSQL 位于 `data-network`）

---

## 6. Notes / 备注

> pgvector 是开源的 PostgreSQL 扩展，由 Andrew Kane 维护。截至 2025 年，它已被 AWS RDS、Azure Database for PostgreSQL、Google Cloud SQL 等托管服务原生支持，消除了自运维扩展的兼容性顾虑。
>
> 本项目未对 ivfflat `probes` 进行调优，因为 HNSW 默认值已覆盖当前数据量。若负载假设发生变化，调优 `ef_search` / `ef_construction` 并记录参数值。

---

*End of ADR-0002*
