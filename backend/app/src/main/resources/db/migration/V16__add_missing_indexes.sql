-- V16: Add missing performance indexes
-- V16: 补充缺失的性能索引
--
-- These indexes improve query performance for common access patterns
-- identified during schema review. All use IF NOT EXISTS for idempotency.
-- 以下索引针对 schema 评审中发现的常见查询模式进行优化。
-- 全部使用 IF NOT EXISTS 保证幂等性。

-- Messages ordering index (used by ConversationRepositoryImpl message sorting)
-- 消息排序索引（ConversationRepositoryImpl 按 sequence 排序消息时使用）
CREATE INDEX IF NOT EXISTS idx_messages_sequence ON messages (conversation_id, sequence);

-- Composite index for job score lookups by job + resume version
-- 职位评分复合索引（按职位和简历版本查询）
CREATE INDEX IF NOT EXISTS idx_job_scores_job_resume ON job_scores (job_id, resume_version_id);

-- Composite index for conversation list filtering by user and status
-- 对话列表复合索引（按用户和状态过滤）
CREATE INDEX IF NOT EXISTS idx_conversations_user_status ON conversations (user_id, status);

-- Composite index for vector status + creation time (cleanup / pagination queries)
-- 向量状态+创建时间复合索引（清理和分页查询）
CREATE INDEX IF NOT EXISTS idx_resume_vectors_status_created ON resume_vectors (status, created_at);
CREATE INDEX IF NOT EXISTS idx_job_vectors_status_created ON job_vectors (status, created_at);
