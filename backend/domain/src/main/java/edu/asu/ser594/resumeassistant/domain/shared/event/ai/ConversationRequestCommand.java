package edu.asu.ser594.resumeassistant.domain.shared.event.ai;

import java.util.List;
import java.util.Map;

/**
 * 对话 AI 请求命令
 * Conversation AI request command
 *
 * @param conversationId   对话 ID / Conversation ID
 * @param userId           用户 ID / User ID
 * @param messageHistory   历史消息列表 / Message history list
 * @param currentMessage   当前用户消息 / Current user message
 * @param fileUrls         关联文件 URL 列表 / Associated file URL list
 * @param resumeVersionId  关联简历版本 ID / Associated resume version ID
 */
public record ConversationRequestCommand(
    String conversationId,
    String userId,
    List<Map<String, Object>> messageHistory,
    String currentMessage,
    List<String> fileUrls,
    String resumeVersionId
) {
}
