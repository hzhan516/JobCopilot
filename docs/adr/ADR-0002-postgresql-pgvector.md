<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](ADR-0002-postgresql-pgvector.md) | [简体中文](../i18n/zh-Hans-CN/adr/ADR-0002-postgresql-pgvector.md) | [繁體中文](../i18n/zh-Hant-TW/adr/ADR-0002-postgresql-pgvector.md)

# ADR-0002: Adopt PostgreSQL + pgvector as Unified Storage for Structured and Vector Data

| Attribute | Value |
|-----------|-------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | Backend Architecture Team |
| **Affected Modules** | `backend/infrastructure/persistence/`, `backend/domain/*/repository/` |

---

## 1. Context

The ResumeAssistant core functionality involves persisting two types of data:

| Data Type | Examples | Query Patterns |
|-----------|----------|----------------|
| **Structured Data** | User info, resume metadata, job descriptions, conversation records | Exact match (`user_id = ?`), range queries, sort/pagination |
| **Vector Data** | Resume embeddings, job description embeddings, conversation context embeddings | Approximate Nearest Neighbor (ANN) search |

### 1.1 Candidate Solutions

| Solution | Description | Evaluation |
|----------|-------------|------------|
| **A. PostgreSQL + pgvector** | Vector types and ivfflat/hnsw indexes via extension on the existing relational database | Low operational complexity; ANN performance not top-tier |
| **B. PostgreSQL + Dedicated Vector DB** | Structured data in PG, vectors in Milvus / Pinecone / Weaviate | Best performance, but introduces second data source, sync complexity, and cost |
| **C. SQLite + Local Vector Library** | Local index with `faiss`, suitable for standalone deployments | Does not meet multi-user concurrency or distributed deployment needs |

---

## 2. Decision

**Adopt Solution A: PostgreSQL + pgvector as the unified structured and vector data store.**

### 2.1 Technical Stack Details

| Component | Version | Purpose |
|-----------|---------|---------|
| PostgreSQL | 15+ | Relational data primary storage |
| pgvector extension | 0.5.1+ | Provides `vector` data type, distance operators, HNSW / ivfflat indexes |
| hibernate-vector | 6.6.33.Final | Hibernate ORM native vector type mapping (`@Vector` annotation) |
| pgvector JDBC | 0.1.6 | Low-level JDBC vector type support |

### 2.2 Domain Layer Port Definition

```java
// domain/src/main/java/.../resume/repository/ResumeVectorRepository.java
public interface ResumeVectorRepository {
    /**
     * Store vector embedding of a resume
     */
    void saveEmbedding(ResumeId id, float[] embedding);

    /**
     * Find top-k most similar resumes by cosine distance
     */
    List<ResumeMatch> findNearest(float[] queryVector, int topK);
}
```

### 2.3 Infrastructure Implementation

```java
// infrastructure/src/main/java/.../persistence/embedding/ResumeVectorPgRepository.java
@Repository
public class ResumeVectorPgRepository implements ResumeVectorRepository {
    
    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ResumeMatch> findNearest(float[] queryVector, int topK) {
        // Use pgvector's <-> (Euclidean) or <=> (Cosine) operators
        String sql = """
            SELECT r.id, r.title, 1 - (e.vector <=> :query) as similarity
            FROM resume_embeddings e
            JOIN resumes r ON r.id = e.resume_id
            ORDER BY e.vector <=> :query
            LIMIT :topK
            """;
        // hibernate-vector handles Java float[] ↔ pgvector conversion automatically
        return em.createNativeQuery(sql, ResumeMatch.class)
                 .setParameter("query", queryVector)
                 .setParameter("topK", topK)
                 .getResultList();
    }
}
```

### 2.4 Index Strategy

| Index Type | Scenario | Configuration |
|------------|----------|---------------|
| **HNSW** | High-dimensional vectors (768/1536 dims), high-concurrency queries, tolerable build time | `CREATE INDEX ON resume_embeddings USING hnsw (vector vector_cosine_ops);` |
| **ivfflat** | Extremely large vector scale (millions+), acceptable recall trade-off for build speed | `CREATE INDEX ON resume_embeddings USING ivfflat (vector vector_cosine_ops) WITH (lists = 100);` |

Current stage (tens of thousands of vectors) prioritizes **HNSW**; single-table build time is in seconds, query latency < 10ms.

---

## 3. Consequences

### 3.1 Positive

| Benefit | Description |
|---------|-------------|
| **Single Source of Truth (SSOT)** | Resume metadata and embeddings written in the same transaction; no cross-database sync lag or inconsistency. |
| **ACID Guarantees** | Resume save + vector generation + vector storage can complete within a single `@Transactional` boundary, with automatic rollback on failure. |
| **Minimal Operations** | Only one PostgreSQL container/instance to maintain; backup, monitoring, and access control are unified. |
| **Cost Controllable** | No additional paid vector DB (e.g., Pinecone usage-based billing); zero incremental cost for self-hosted scenarios. |
| **Native Hibernate Support** | `hibernate-vector` allows direct `@Vector(dimensions = 1536)` declaration in Entities, eliminating manual JDBC type conversion. |

### 3.2 Negative

| Cost | Description |
|------|-------------|
| **ANN Performance Ceiling** | pgvector HNSW degrades significantly at tens of millions of vectors, below dedicated vector DBs (Milvus is typically 2-5x faster on equivalent hardware). |
| **Dimension Limit** | pgvector supports up to 16,000 dimensions; current embedding model outputs 1536 dims, which is ample but requires monitoring for model upgrades. |
| **Index Build Blocking** | `CREATE INDEX CONCURRENTLY` must be used explicitly; otherwise building indexes on large tables locks the table. |
| **Backup Size Inflation** | Vector data is high-dimensional float arrays; backup files are significantly larger than pure structured data; compression strategies must be evaluated. |

### 3.3 Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Vector table grows to millions, query performance degrades | **Capacity Planning**: The `ResumeVectorRepository` Port interface is reserved; future transparent replacement with a Milvus Adapter requires no domain layer changes. |
| Embedding model change causes dimension mismatch | **Schema Versioning**: Add `model_version` field to vector tables; vectors from different model versions stored in separate tables or partitions. |
| pgvector extension conflicts with PostgreSQL major version upgrades | **Container Isolation**: PostgreSQL deployed via Docker; upgrades replace the entire image, avoiding host extension conflicts. |

---

## 4. Compliance Verification

- **Performance Baseline**: Before each release, execute vector search benchmarks on staging (10K / 100K / 1M vector scales); P99 latency < 50ms.
- **Index Monitoring**: Monitor HNSW index hit rate via PostgreSQL `pg_stat_user_indexes`; alert when below 90%.
- **Architecture Gate**: `code-analyzer` scanning ensures the `domain` layer has no direct references to `org.postgresql` or `com.pgvector` packages.

---

## 5. Related Decisions

- ADR-0001 — Hexagonal Architecture (`ResumeVectorRepository` as a Driven Port with physical isolation)
- ADR-0004 — Embedding Service Abstracted as `EmbeddingPort` (Decoupling vector generation from storage)
- ADR-0006 — Docker Compose Three-Layer Network Architecture (PostgreSQL located in `data-network`)

---

## 6. Notes

> pgvector is an open-source PostgreSQL extension maintained by Andrew Kane. As of 2025, it is natively supported by AWS RDS, Azure Database for PostgreSQL, and Google Cloud SQL, eliminating compatibility concerns for self-managed extensions.
>
> This project does not tune ivfflat `probes` because HNSW defaults already handle current volumes. If load assumptions change, tune `ef_search` / `ef_construction` and document the values.

---

*End of ADR-0002*