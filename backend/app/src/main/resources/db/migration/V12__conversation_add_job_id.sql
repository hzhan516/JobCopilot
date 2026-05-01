-- 为对话表增加职位 ID 字段及索引
-- Add job_id column and index to conversations table
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS job_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_conversations_job_id ON conversations(job_id);
