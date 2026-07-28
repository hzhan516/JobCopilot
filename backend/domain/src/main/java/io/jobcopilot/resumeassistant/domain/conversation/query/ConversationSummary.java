package io.jobcopilot.resumeassistant.domain.conversation.query;

import io.jobcopilot.resumeassistant.domain.conversation.valueobject.AiReplyState;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.ConversationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Lightweight query-side view that never hydrates the conversation message collection. */
public record ConversationSummary(
        UUID conversationId,
        UUID userId,
        String title,
        ConversationStatus status,
        UUID resumeVersionId,
        UUID jobId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        AiReplyState aiReplyState,
        String lastMessagePreview
) {
}
