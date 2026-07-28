-- Durable request-correlated AI chat state (Phase 1).
ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS compaction_request_id VARCHAR(36),
    ADD COLUMN IF NOT EXISTS ai_reply_request_id VARCHAR(36),
    ADD COLUMN IF NOT EXISTS ai_reply_status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    ADD COLUMN IF NOT EXISTS ai_reply_error_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS ai_reply_started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ai_reply_completed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ai_reply_user_message_sequence INTEGER,
    ADD COLUMN IF NOT EXISTS ai_reply_assistant_message_sequence INTEGER;

CREATE INDEX IF NOT EXISTS idx_conversations_ai_reply_pending
    ON conversations (ai_reply_status, ai_reply_started_at);

-- Bounded, recoverable outbox delivery state (Phase 2).
ALTER TABLE outbox_message
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_error_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS worker_id VARCHAR(128);

UPDATE outbox_message
SET next_attempt_at = COALESCE(next_attempt_at, created_at)
WHERE status IN ('PENDING', 'FAILED');

CREATE INDEX IF NOT EXISTS idx_outbox_due
    ON outbox_message (status, next_attempt_at, created_at);

CREATE INDEX IF NOT EXISTS idx_outbox_stale_processing
    ON outbox_message (status, locked_at);
