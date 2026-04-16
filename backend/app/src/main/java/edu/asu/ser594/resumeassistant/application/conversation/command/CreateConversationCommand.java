package edu.asu.ser594.resumeassistant.application.conversation.command;

import lombok.Builder;

/**
 * 创建对话命令
 * Create conversation command
 */
@Builder
public record CreateConversationCommand(
    String userId,
    String title,
    String resumeVersionId
) {
}
