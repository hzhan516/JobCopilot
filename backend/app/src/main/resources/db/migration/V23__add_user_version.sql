-- V23__add_user_version.sql
-- Add optimistic locking version column to users table
-- 为用户表添加乐观锁版本号字段

ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Comment
COMMENT ON COLUMN users.version IS 'Optimistic locking version / 乐观锁版本号';
