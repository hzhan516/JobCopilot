-- V19__add_resume_group_version.sql
-- Add optimistic locking version column to resume_groups table
-- 为简历组表添加乐观锁版本号字段

ALTER TABLE resume_groups ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Comment
COMMENT ON COLUMN resume_groups.version IS 'Optimistic locking version / 乐观锁版本号';
