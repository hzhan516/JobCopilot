package io.jobcopilot.resumeassistant.api.conversation.dto;

import java.time.OffsetDateTime;

/** Durable, request-correlated AI reply state exposed to clients. */
public record AiReplyResponse(
        String requestId,
        String status,
        String errorCode,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Integer userMessageSequence,
        Integer assistantMessageSequence
) {
}
