-- V25: Chat context window config seeds + conversation token/summary columns
-- V25: 聊天上下文窗口配置种子 + 对话 token/摘要字段

-- Seed context window & compaction threshold into DynamicConfig
INSERT INTO dynamic_config (config_key, config_value, default_value, description, category, value_type) VALUES
('chat.contextWindow', '1000000', '1000000', 'Context window size in tokens for usage indicator denominator', 'ai', 'NUMBER'),
('chat.compactThreshold', '80', '80', 'Usage ratio (%) above which compact advisory is shown', 'ai', 'NUMBER')
ON CONFLICT DO NOTHING;

-- Add token tracking and compaction columns to conversations
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS context_tokens INTEGER NOT NULL DEFAULT 0;
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS total_tokens_used BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS context_summary TEXT;
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS compacted_through_sequence INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN conversations.context_tokens IS 'Prompt tokens from the most recent AI call / 最近一次 AI 调用的 prompt tokens';
COMMENT ON COLUMN conversations.total_tokens_used IS 'Cumulative tokens consumed across all AI calls / 所有 AI 调用的累计 token 消耗';
COMMENT ON COLUMN conversations.context_summary IS 'LLM-generated summary of compacted message history / LLM 生成的压缩历史摘要';
COMMENT ON COLUMN conversations.compacted_through_sequence IS 'Last message sequence number covered by the summary / 摘要已覆盖到的最后一条消息序号';
