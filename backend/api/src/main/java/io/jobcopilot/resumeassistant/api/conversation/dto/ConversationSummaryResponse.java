package io.jobcopilot.resumeassistant.api.conversation.dto;

import java.time.OffsetDateTime;

/** Lightweight conversation list item; full messages are available only from the detail endpoint. */
public record ConversationSummaryResponse(
        String conversationId,
        String userId,
        String title,
        String status,
        String resumeVersionId,
        String jobId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        AiReplyResponse aiReply,
        String lastMessagePreview
) {
}
