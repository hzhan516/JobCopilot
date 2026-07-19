package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.conversation;

import java.time.LocalDateTime;

public interface ConversationSummaryProjection {
    String getConversationId();
    String getUserId();
    String getTitle();
    String getStatus();
    String getResumeVersionId();
    String getJobId();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    String getAiReplyRequestId();
    String getAiReplyStatus();
    String getAiReplyErrorCode();
    LocalDateTime getAiReplyStartedAt();
    LocalDateTime getAiReplyCompletedAt();
    Integer getAiReplyUserMessageSequence();
    Integer getAiReplyAssistantMessageSequence();
    Integer getContextTokens();
    String getCompactionRequestId();
    String getLastMessagePreview();
}
