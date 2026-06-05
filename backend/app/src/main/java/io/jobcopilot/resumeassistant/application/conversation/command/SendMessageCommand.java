package io.jobcopilot.resumeassistant.application.conversation.command;

import io.jobcopilot.resumeassistant.domain.conversation.valueobject.MessageRole;
import lombok.Builder;

import java.util.List;
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
        String content,
        List<String> fileUrls
) {
}
