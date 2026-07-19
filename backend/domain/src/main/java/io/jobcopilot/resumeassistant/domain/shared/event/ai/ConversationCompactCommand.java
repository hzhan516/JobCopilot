package io.jobcopilot.resumeassistant.domain.shared.event.ai;

import java.util.List;
import java.util.Map;

/**
 * Command to trigger LLM-based summarization of older conversation history.
 * 触发基于 LLM 的旧对话历史摘要压缩的命令。
 *
 * @param conversationId        对话 ID / Conversation ID
 * @param userId                用户 ID / User ID
 * @param messageHistory        完整消息历史（AI 服务负责摘要）/ Full message history (AI summarizes)
 * @param compactedThroughSequence 当前已压缩到的序号（用于增量压缩）/ Current compacted-through sequence
 */
public record ConversationCompactCommand(
        String conversationId,
        String userId,
        List<Map<String, Object>> messageHistory,
        int compactedThroughSequence,
        String requestId,
        int schemaVersion,
        String eventId,
        String occurredAt
) {
    public ConversationCompactCommand(
            String conversationId, String userId, List<Map<String, Object>> messageHistory,
            int compactedThroughSequence, String requestId) {
        this(conversationId, userId, messageHistory, compactedThroughSequence, requestId,
                1, java.util.UUID.randomUUID().toString(), java.time.OffsetDateTime.now().toString());
    }

    public ConversationCompactCommand(
            String conversationId, String userId, List<Map<String, Object>> messageHistory,
            int compactedThroughSequence) {
        this(conversationId, userId, messageHistory, compactedThroughSequence,
                java.util.UUID.randomUUID().toString());
    }
}
