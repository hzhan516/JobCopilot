package edu.asu.ser594.resumeassistant.api.conversation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话响应 DTO
 * Conversation response DTO
 *
 * @param conversationId  对话 ID / Conversation ID
 * @param userId          用户 ID / User ID
 * @param title           标题 / Title
 * @param status          状态 / Status
 * @param resumeVersionId 简历版本 ID / Resume version ID
 * @param messages        消息列表 / Messages list
 * @param createdAt       创建时间 / Created at
 * @param updatedAt       更新时间 / Updated at
 */
public record ConversationResponse(
    String conversationId,
    String userId,
    String title,
    String status,
    String resumeVersionId,
    List<MessageResponse> messages,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
