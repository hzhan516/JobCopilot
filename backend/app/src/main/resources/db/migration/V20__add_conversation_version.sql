-- V20__add_conversation_version.sql
-- Add optimistic locking version column to conversations table
-- 为对话表添加乐观锁版本号字段

ALTER TABLE conversations ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Comment
COMMENT ON COLUMN conversations.version IS 'Optimistic locking version / 乐观锁版本号';
