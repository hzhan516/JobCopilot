-- V17: Create HNSW approximate nearest neighbor index for job_vectors
-- V17: 为 job_vectors 创建 HNSW 近似最近邻索引
--
-- This index dramatically accelerates vector similarity search (embedding <=>)
-- compared to exact scans. It is created ONLY when the embedding dimension
-- is within pgvector's HNSW limit (2000). For dimensions above 2000, exact
-- scan is the only option.
-- 该索引可极大加速向量相似度搜索（embedding <=>），相比精确扫描性能提升显著。
-- 仅在嵌入维度不超过 pgvector HNSW 限制（2000）时创建。超过 2000 维时只能使用精确扫描。
--
-- IMPORTANT: This migration is idempotent (IF NOT EXISTS). If the HNSW index
-- was already created via init-db.sh in dev environments, this will safely skip.
-- 重要：本迁移是幂等的（IF NOT EXISTS）。如果开发环境已通过 init-db.sh 创建过
-- HNSW 索引，此处会安全跳过。

DO
$$
    BEGIN
        -- pgvector HNSW limit is 2000 dimensions
        -- pgvector HNSW 限制为 2000 维
        IF (SELECT atttypmod
            FROM pg_attribute
            WHERE attrelid = 'job_vectors'::regclass
              AND attname = 'embedding') <= 2000 THEN
            CREATE INDEX IF NOT EXISTS idx_job_vectors_embedding_hnsw
                ON job_vectors
                USING hnsw (embedding vector_cosine_ops)
                WITH (m = 16, ef_construction = 64);
        ELSE
            RAISE NOTICE 'Skipping HNSW index: embedding dimension exceeds 2000.';
            RAISE NOTICE '跳过 HNSW 索引：嵌入维度超过 2000。';
        END IF;
    END
$$;
