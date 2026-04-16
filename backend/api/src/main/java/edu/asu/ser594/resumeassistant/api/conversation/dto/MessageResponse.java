package edu.asu.ser594.resumeassistant.api.conversation.dto;

import java.time.LocalDateTime;

/**
 * 消息响应 DTO
 * Message response DTO
 *
 * @param messageId 消息 ID / Message ID
 * @param role      角色 / Role
 * @param content   内容 / Content
 * @param sequence  序列号 / Sequence
 * @param createdAt 创建时间 / Created at
 */
public record MessageResponse(
    String messageId,
    String role,
    String content,
    int sequence,
    LocalDateTime createdAt
) {
}
