-- V15: Add ai_optimized_version_id to conversations table
-- V15: 为对话表添加 ai_optimized_version_id 字段
--
-- Note: This column already exists in init.sql (dev bootstrap) but was
-- missing from the Flyway migration chain after a previous V14 was replaced.
-- 注意：该列在 init.sql（开发环境启动脚本）中已存在，但在之前的 V14
-- 被替换后，Flyway 迁移链中缺失了此变更。

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS ai_optimized_version_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_conversations_ai_optimized_version_id
    ON conversations (ai_optimized_version_id);
