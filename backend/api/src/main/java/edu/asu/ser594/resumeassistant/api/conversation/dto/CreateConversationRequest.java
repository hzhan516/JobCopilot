package edu.asu.ser594.resumeassistant.api.conversation.dto;

/**
 * 创建对话请求 DTO
 * Create conversation request DTO
 *
 * @param title           对话标题 / Conversation title
 * @param resumeVersionId 简历版本 ID / Resume version ID
 * @param jobId           职位 ID / Job ID
 */
public record CreateConversationRequest(
        String title,
        String resumeVersionId,
        String jobId
) {
}
