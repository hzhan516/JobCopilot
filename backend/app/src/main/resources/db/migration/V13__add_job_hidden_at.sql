-- Add soft-delete marker for user-hidden jobs.
-- 保留职位数据，但让用户列表排除已隐藏职位。
ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_jobs_user_hidden_at ON jobs (user_id, hidden_at);
