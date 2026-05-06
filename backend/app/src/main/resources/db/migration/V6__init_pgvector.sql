-- V6__init_pgvector.sql
-- Database schema for pgvector extension and document vectors
-- pgvector 扩展与文档向量数据库结构

CREATE EXTENSION IF NOT EXISTS vector;

-- 简历版本嵌入向量表 / Resume version embedding vectors table
CREATE TABLE IF NOT EXISTS resume_vectors
(
    id                VARCHAR(64) PRIMARY KEY,
    resume_version_id VARCHAR(64) NOT NULL UNIQUE,
    embedding         vector(1536),
    status            VARCHAR(32) NOT NULL,
    error_message     TEXT,
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引 / Indexes
CREATE INDEX IF NOT EXISTS idx_resume_vectors_version_id ON resume_vectors (resume_version_id);
CREATE INDEX IF NOT EXISTS idx_resume_vectors_status ON resume_vectors (status);

-- 职位嵌入向量表 / Job embedding vectors table
CREATE TABLE IF NOT EXISTS job_vectors
(
    id            VARCHAR(64) PRIMARY KEY,
    job_id        VARCHAR(64) NOT NULL UNIQUE,
    embedding     vector(1536),
    status        VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255)
);

-- 索引 / Indexes
CREATE INDEX IF NOT EXISTS idx_job_vectors_job_id ON job_vectors (job_id);
CREATE INDEX IF NOT EXISTS idx_job_vectors_status ON job_vectors (status);
