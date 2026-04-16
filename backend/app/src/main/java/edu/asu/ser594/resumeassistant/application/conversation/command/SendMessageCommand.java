package edu.asu.ser594.resumeassistant.application.conversation.command;

import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.MessageRole;
import lombok.Builder;

/**
 * 发送消息命令
 * Send message command
 */
@Builder
public record SendMessageCommand(
    String conversationId,
    String userId,
    MessageRole role,
    String content
) {
}
