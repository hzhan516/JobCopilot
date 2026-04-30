package edu.asu.ser594.resumeassistant.application.conversation.command;

import lombok.Builder;

import java.util.UUID;

/**
 * 创建对话命令
 * Create conversation command
 */
@Builder
public record CreateConversationCommand(
    UUID userId,
    String title,
    UUID resumeVersionId,
    UUID jobId
) {
}
