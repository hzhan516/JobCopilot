package edu.asu.ser594.resumeassistant.api.conversation.dto;

import java.time.OffsetDateTime;

/**
 * 消息响应 DTO
 * Message response DTO
 *
 * @param messageId 消息 ID / Message ID
 * @param role      角色 / Role
 * @param content   内容 / Content
 * @param sequence  序列号 / Sequence
 * @param fileUrl   关联文件 URL / Associated file URL
 * @param createdAt 创建时间 / Created at
 */
public record MessageResponse(
        String messageId,
        String role,
        String content,
        int sequence,
        String fileUrl,
        OffsetDateTime createdAt
) {
}
