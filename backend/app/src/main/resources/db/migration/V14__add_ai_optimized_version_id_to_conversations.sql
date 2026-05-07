ALTER TABLE conversations ADD COLUMN ai_optimized_version_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_conversations_ai_optimized_version_id ON conversations(ai_optimized_version_id);
