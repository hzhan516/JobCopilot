package io.jobcopilot.resumeassistant.infrastructure.persistence.entity.conversation;

import io.jobcopilot.resumeassistant.domain.conversation.valueobject.ConversationStatus;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.AiReplyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话持久化实体
 * Conversation JPA entity
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationJpaEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationStatus status;

    @Column(name = "resume_version_id")
    private String resumeVersionId;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "ai_optimized_version_id")
    private String aiOptimizedVersionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "context_tokens", nullable = false)
    @Builder.Default
    private Integer contextTokens = 0;

    @Column(name = "total_tokens_used", nullable = false)
    @Builder.Default
    private Long totalTokensUsed = 0L;

    @Column(name = "context_summary", columnDefinition = "TEXT")
    private String contextSummary;

    @Column(name = "compacted_through_sequence", nullable = false)
    @Builder.Default
    private Integer compactedThroughSequence = 0;

    @Column(name = "compaction_request_id", length = 36)
    private String compactionRequestId;

    @Column(name = "ai_reply_request_id", length = 36)
    private String aiReplyRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_reply_status", nullable = false, length = 20)
    @Builder.Default
    private AiReplyStatus aiReplyStatus = AiReplyStatus.IDLE;

    @Column(name = "ai_reply_error_code", length = 64)
    private String aiReplyErrorCode;

    @Column(name = "ai_reply_started_at")
    private LocalDateTime aiReplyStartedAt;

    @Column(name = "ai_reply_completed_at")
    private LocalDateTime aiReplyCompletedAt;

    @Column(name = "ai_reply_user_message_sequence")
    private Integer aiReplyUserMessageSequence;

    @Column(name = "ai_reply_assistant_message_sequence")
    private Integer aiReplyAssistantMessageSequence;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MessageJpaEntity> messages = new ArrayList<>();
}
