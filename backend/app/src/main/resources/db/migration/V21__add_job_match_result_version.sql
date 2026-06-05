-- V21__add_job_match_result_version.sql
-- Add optimistic locking version column to job_match_results table
-- 为职位匹配结果表添加乐观锁版本号字段

ALTER TABLE job_match_results ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Comment
COMMENT ON COLUMN job_match_results.version IS 'Optimistic locking version / 乐观锁版本号';
