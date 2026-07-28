package io.jobcopilot.resumeassistant.domain.conversation.query;

import io.jobcopilot.resumeassistant.domain.conversation.entity.Message;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.AiReplyState;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.ConversationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Read-only paged conversation detail; it is not a write aggregate. */
public record ConversationDetail(
        UUID conversationId,
        UUID userId,
        String title,
        ConversationStatus status,
        UUID resumeVersionId,
        UUID jobId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int contextTokens,
        UUID compactionRequestId,
        AiReplyState aiReplyState,
        List<Message> messages
) {
}
