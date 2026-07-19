package io.jobcopilot.resumeassistant.domain.conversation.valueobject;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable snapshot of the current AI reply request owned by a Conversation aggregate.
 */
public record AiReplyState(
        UUID requestId,
        AiReplyStatus status,
        String errorCode,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Integer userMessageSequence,
        Integer assistantMessageSequence
) {
    public static AiReplyState idle() {
        return new AiReplyState(null, AiReplyStatus.IDLE, null, null, null, null, null);
    }

    public static AiReplyState pending(UUID requestId, int userMessageSequence, LocalDateTime startedAt) {
        return new AiReplyState(requestId, AiReplyStatus.PENDING, null, startedAt, null,
                userMessageSequence, null);
    }

    public boolean isPending() {
        return status == AiReplyStatus.PENDING;
    }

    public boolean matches(UUID candidateRequestId) {
        return requestId != null && requestId.equals(candidateRequestId);
    }
}
