-- ============================================
-- 扩展 job_vectors 表结构 / Extend job_vectors table schema
-- 开发阶段直接执行，无需 Flyway / Execute directly in dev, no Flyway needed
-- ============================================

ALTER TABLE job_vectors
    ADD COLUMN IF NOT EXISTS title TEXT,
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS requirements JSONB,
    ADD COLUMN IF NOT EXISTS raw_content TEXT,
    ADD COLUMN IF NOT EXISTS source_file VARCHAR (255),
    ADD COLUMN IF NOT EXISTS model_version VARCHAR (50) DEFAULT 'gemini-embedding-001';

-- 全文搜索 GIN 索引 / Full-text search GIN index
CREATE INDEX IF NOT EXISTS idx_job_vectors_fts ON job_vectors
    USING GIN (to_tsvector('english', COALESCE (raw_content, '')));

-- HNSW 近似最近邻索引 / HNSW ANN index
-- 注意：CONCURRENTLY 避免锁表，但开发环境单连接可直接执行
-- Note: CONCURRENTLY avoids table locking; safe for dev single-connection execution
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_job_vectors_embedding_hnsw ON job_vectors
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
