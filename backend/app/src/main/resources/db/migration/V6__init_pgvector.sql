-- V6__init_pgvector.sql
-- Database schema for pgvector extension and document vectors

CREATE EXTENSION IF NOT EXISTS vector;

-- Table for storing resume version embeddings
CREATE TABLE IF NOT EXISTS resume_vectors (
    id VARCHAR(64) PRIMARY KEY,
    resume_version_id VARCHAR(64) NOT NULL,
    embedding vector(1536),
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resume_vectors_version_id ON resume_vectors(resume_version_id);
CREATE INDEX idx_resume_vectors_status ON resume_vectors(status);

-- Table for storing job embeddings
CREATE TABLE IF NOT EXISTS job_vectors (
    id VARCHAR(64) PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    embedding vector(1536),
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_job_vectors_job_id ON job_vectors(job_id);
CREATE INDEX idx_job_vectors_status ON job_vectors(status);
