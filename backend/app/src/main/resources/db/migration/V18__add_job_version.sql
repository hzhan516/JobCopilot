-- V18__add_job_version.sql
-- Add optimistic locking version column to jobs table
-- 为职位表添加乐观锁版本号字段

ALTER TABLE jobs ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Comment
COMMENT ON COLUMN jobs.version IS 'Optimistic locking version / 乐观锁版本号';
