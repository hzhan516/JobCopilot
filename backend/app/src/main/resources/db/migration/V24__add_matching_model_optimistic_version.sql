-- V24__add_matching_model_optimistic_version.sql
-- Add optimistic locking version column to job_matching_models table
-- 为匹配模型表添加乐观锁版本号字段（与业务字段 model_version 区分）

ALTER TABLE job_matching_models ADD COLUMN IF NOT EXISTS optimistic_version BIGINT NOT NULL DEFAULT 0;

-- Comment
COMMENT ON COLUMN job_matching_models.optimistic_version IS 'Optimistic locking version / 乐观锁版本号（与业务 model_version 区分）';
