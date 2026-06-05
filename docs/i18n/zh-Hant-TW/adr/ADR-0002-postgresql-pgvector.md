<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../adr/ADR-0002-postgresql-pgvector.md) | [简体中文](../../zh-Hans-CN/adr/ADR-0002-postgresql-pgvector.md) | [繁體中文](ADR-0002-postgresql-pgvector.md)

# ADR-0002: 採用 PostgreSQL + pgvector 作為向量與結構化資料統一存儲

| 屬性 | 內容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | 後端架構團隊 |
| **Affected Modules** | `backend/infrastructure/persistence/`, `backend/domain/*/repository/` |

---

## 1. Context / 背景

ResumeAssistant 核心功能涉及兩類資料的持久化：

| 資料類型 | 示例 | 查询模式 |
|----------|------|----------|
| **結構化資料** | 用戶資訊、簡歷元資料、职位描述、對話記錄 | 精確匹配（`user_id = ?`）、範围查询、排序分页 |
| **向量資料** | 簡歷 embedding、职位描述 embedding、對話上下文 embedding | 近似最近鄰搜尋（ANN, Approximate Nearest Neighbor） |

### 1.1 候選方案

| 方案 | 說明 | 評估維度 |
|------|------|----------|
| **A. PostgreSQL + pgvector** | 在現有關係資料庫上通過擴展支持向量類型與 ivfflat/hnsw 索引 | 運維複雜度低，但 ANN 性能非顶級 |
| **B. PostgreSQL + 专用向量庫** | 結構資料存 PG，向量存 Milvus / Pinecone / Weaviate | 性能最優，但引入第二資料源、同步複雜度、成本 |
| **C. SQLite + 本地向量庫** | 如 `faiss` 本地索引，適用於單機版 | 不滿足多用戶並發與分布式部署需求 |

---

## 2. Decision / 決策

**採用方案 A：PostgreSQL + pgvector 作為統一的結構化與向量資料存儲。**

### 2.1 技術選型详情

| 組件 | 版本 | 用途 |
|------|------|------|
| PostgreSQL | 15+ | 關係型資料主存儲 |
| pgvector 擴展 | 0.5.1+ | 提供 `vector` 資料類型、距離運算符、HNSW / ivfflat 索引 |
| hibernate-vector | 6.6.33.Final | Hibernate ORM 原生向量類型映射（`@Vector` 注解） |
| pgvector JDBC | 0.1.6 | 底層 JDBC 向量類型支持 |

### 2.2 領域層 Port 定義

```java
// domain/src/main/java/.../resume/repository/ResumeVectorRepository.java
public interface ResumeVectorRepository {
    /**
     * 存儲簡歷的向量表示
     * Store vector embedding of a resume
     */
    void saveEmbedding(ResumeId id, float[] embedding);

    /**
     * 基於向量相似度搜尋最匹配的簡歷
     * Find top-k most similar resumes by cosine distance
     */
    List<ResumeMatch> findNearest(float[] queryVector, int topK);
}
```

### 2.3 Infrastructure 實現

```java
// infrastructure/src/main/java/.../persistence/embedding/ResumeVectorPgRepository.java
@Repository
public class ResumeVectorPgRepository implements ResumeVectorRepository {
    
    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ResumeMatch> findNearest(float[] queryVector, int topK) {
        // 使用 pgvector 的 <-> (Euclidean) 或 <=> (Cosine) 運算符
        String sql = """
            SELECT r.id, r.title, 1 - (e.vector <=> :query) as similarity
            FROM resume_embeddings e
            JOIN resumes r ON r.id = e.resume_id
            ORDER BY e.vector <=> :query
            LIMIT :topK
            """;
        // 通過 Hibernate-vector 自動完成 Java float[] ↔ pgvector 類型轉換
        return em.createNativeQuery(sql, ResumeMatch.class)
                 .setParameter("query", queryVector)
                 .setParameter("topK", topK)
                 .getResultList();
    }
}
```

### 2.4 索引策略

| 索引類型 | 場景 | 設定 |
|----------|------|------|
| **HNSW** | 高維向量（768/1536 維）、高並發查询、允許一定構建時間 | `CREATE INDEX ON resume_embeddings USING hnsw (vector vector_cosine_ops);` |
| **ivfflat** | 向量量級極大（百萬級+）、可接受牺牲少量召回率換取構建速度 | `CREATE INDEX ON resume_embeddings USING ivfflat (vector vector_cosine_ops) WITH (lists = 100);` |

當前階段（萬級向量）優先使用 **HNSW**，單表構建時間在秒級，查询延迟 < 10ms。

---

## 3. Consequences / 後果

### 3.1 Positive / 正面

| 收益 | 說明 |
|------|------|
| **單一資料源（SSOT）** | 簡歷元資料與 embedding 在同一事務內寫入，不存在兩庫同步延迟或資料不一致問題。 |
| **ACID 保障** | 簡歷保存 + 向量生成 + 向量存儲可在同一 `@Transactional` 边界內完成，失败自動回滚。 |
| **運維極簡** | 仅需維护 PostgreSQL 一个容器/实例，備份、监控、權限管理統一。 |
| **成本可控** | 無需額外付費向量庫（如 Pinecone 按用量计費），自建場景零增量成本。 |
| **Hibernate 原生支持** | `hibernate-vector` 允許在 Entity 中直接声明 `@Vector(dimensions = 1536)`，無需手動 JDBC 類型轉換。 |

### 3.2 Negative / 負面

| 成本 | 說明 |
|------|------|
| **ANN 性能天花板** | pgvector 的 HNSW 在千萬級向量時性能下降明顯，低於专用向量庫（Milvus 在同等硬件下通常快 2-5x）。 |
| **維度限制** | pgvector 支持最高 16,000 維，當前 embedding 模型输出 1536 維，充裕但需持續监控模型升級。 |
| **索引構建阻塞** | `CREATE INDEX CONCURRENTLY` 需顯式使用，否則大表建索引會鎖表。 |
| **備份體積膨胀** | 向量資料為高維浮點陣列，備份檔案顯著大於純結構化資料，需評估壓缩策略。 |

### 3.3 Risks / 風險與缓解

| 風險 | 缓解措施 |
|------|----------|
| 向量表資料量增長到百萬級，查询性能劣化 | **容量規划**：當前設计保留 `ResumeVectorRepository` Port 介面，未來可透明替換為 Milvus Adapter，無需改動 domain 層。 |
| embedding 模型變更導致向量維度變化 | **Schema 版本化**：向量表增加 `model_version` 字段，不同模型版本的向量分表或分區存儲。 |
| pgvector 擴展與 PostgreSQL 主版本升級衝突 | **容器化隔離**：PostgreSQL 通過 Docker 部署，升級時整體替換镜像，避免宿主擴展衝突。 |

---

## 4. Compliance / 合規驗證

- **性能基線**：每次版本發布前在 staging 环境執行向量搜尋基準測試（1萬 / 10萬 / 100萬 向量量級），延迟 P99 < 50ms。
- **索引监控**：通過 PostgreSQL `pg_stat_user_indexes` 监控 HNSW 索引命中率，低於 90% 時触發告警。
- **架構门禁**：`code-analyzer` 掃描確保 `domain` 層無直接引用 `org.postgresql` 或 `com.pgvector` 包。

---

## 5. Related / 相關決策

- ADR-0001 — 六边形架構（`ResumeVectorRepository` 作為 Driven Port 的物理隔離）
- ADR-0004 — Embedding 服務抽象為 `EmbeddingPort`（向量生成與存儲解耦）
- ADR-0006 — Docker Compose 三層網路架構（PostgreSQL 位於 `data-network`）

---

## 6. Notes / 備注

> pgvector 是開源的 PostgreSQL 擴展，由 Andrew Kane 維护。截至 2025 年，它已被 AWS RDS、Azure Database for PostgreSQL、Google Cloud SQL 等託管服務原生支持，消除了自運維擴展的兼容性顾慮。
>
> 本項目未對 ivfflat `probes` 進行調優，因為 HNSW 預設值已覆蓋當前資料量。若負載假設發生變化，調優 `ef_search` / `ef_construction` 並記錄參數值。

---

*End of ADR-0002*
