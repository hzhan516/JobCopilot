package edu.asu.ser594.resumeassistant.application.conversation.command;

import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.MessageRole;
import lombok.Builder;

import java.util.UUID;

/**
 * 发送消息命令
 * Send message command
 */
@Builder
public record SendMessageCommand(
    UUID conversationId,
    UUID userId,
    MessageRole role,
    String content
) {
}
