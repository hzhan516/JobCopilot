-- Outbox 消息表 / Outbox message table
-- 用于事务发件箱模式，保证数据库写入与 MQ 消息发布的原子性
-- Used for the Transactional Outbox pattern to ensure atomicity between DB writes and MQ publishing
CREATE TABLE IF NOT EXISTS outbox_message
(
    id
    VARCHAR
(
    36
) PRIMARY KEY DEFAULT gen_random_uuid
(
),
    exchange VARCHAR
(
    255
) NOT NULL,
    routing_key VARCHAR
(
    255
) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR
(
    20
) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP
    );

-- 索引：按状态查询 PENDING 记录（OutboxRelayScheduler 轮询用）
-- Index for querying PENDING records (used by OutboxRelayScheduler polling)
CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_message(status);

-- 索引：按状态 + 发送时间查询过期记录（OutboxCleanupScheduler 清理用）
-- Index for querying expired records by status + sent time (used by OutboxCleanupScheduler)
CREATE INDEX IF NOT EXISTS idx_outbox_status_sent_at ON outbox_message(status, sent_at);
