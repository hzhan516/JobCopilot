package io.jobcopilot.resumeassistant.domain.conversation.valueobject;

/**
 * Durable lifecycle of one user-visible AI reply request.
 */
public enum AiReplyStatus {
    IDLE,
    PENDING,
    COMPLETED,
    FAILED,
    TIMED_OUT
}
