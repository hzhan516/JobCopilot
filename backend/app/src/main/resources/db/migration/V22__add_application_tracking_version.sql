-- V22__add_application_tracking_version.sql
-- Add optimistic locking version column to application_trackings table
-- 为求职申请跟踪表添加乐观锁版本号字段

ALTER TABLE application_trackings ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Comment
COMMENT ON COLUMN application_trackings.version IS 'Optimistic locking version / 乐观锁版本号';
