package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.conversation;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.conversation.ConversationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.AiReplyStatus;

/**
 * Spring Data JPA 仓储接口
 * Spring Data JPA repository interface
 */
public interface JpaConversationRepository extends JpaRepository<ConversationJpaEntity, String> {

    /**
     * 查找某用户的所有对话
     * Find all conversations by user ID
     */
    List<ConversationJpaEntity> findAllByUserId(String userId);

    @Query(value = """
            SELECT c.id AS conversationId,
                   c.user_id AS userId,
                   c.title AS title,
                   c.status AS status,
                   c.resume_version_id AS resumeVersionId,
                   c.job_id AS jobId,
                   c.created_at AS createdAt,
                   c.updated_at AS updatedAt,
                   c.ai_reply_request_id AS aiReplyRequestId,
                   c.ai_reply_status AS aiReplyStatus,
                   c.ai_reply_error_code AS aiReplyErrorCode,
                   c.ai_reply_started_at AS aiReplyStartedAt,
                   c.ai_reply_completed_at AS aiReplyCompletedAt,
                   c.ai_reply_user_message_sequence AS aiReplyUserMessageSequence,
                   c.ai_reply_assistant_message_sequence AS aiReplyAssistantMessageSequence,
                   (SELECT LEFT(m.content, 240)
                      FROM messages m
                     WHERE m.conversation_id = c.id
                     ORDER BY m.sequence DESC
                     LIMIT 1) AS lastMessagePreview
              FROM conversations c
             WHERE c.user_id = :userId
             ORDER BY c.updated_at DESC
            """, nativeQuery = true)
    List<ConversationSummaryProjection> findSummariesByUserId(@Param("userId") String userId);

    @Query(value = """
            SELECT c.id AS conversationId,
                   c.user_id AS userId,
                   c.title AS title,
                   c.status AS status,
                   c.resume_version_id AS resumeVersionId,
                   c.job_id AS jobId,
                   c.created_at AS createdAt,
                   c.updated_at AS updatedAt,
                   c.ai_reply_request_id AS aiReplyRequestId,
                   c.ai_reply_status AS aiReplyStatus,
                   c.ai_reply_error_code AS aiReplyErrorCode,
                   c.ai_reply_started_at AS aiReplyStartedAt,
                   c.ai_reply_completed_at AS aiReplyCompletedAt,
                   c.ai_reply_user_message_sequence AS aiReplyUserMessageSequence,
                   c.ai_reply_assistant_message_sequence AS aiReplyAssistantMessageSequence,
                   c.context_tokens AS contextTokens,
                   c.compaction_request_id AS compactionRequestId,
                   NULL AS lastMessagePreview
              FROM conversations c
             WHERE c.id = :conversationId
            """, nativeQuery = true)
    java.util.Optional<ConversationSummaryProjection> findSummaryByConversationId(
            @Param("conversationId") String conversationId);

    /**
     * 统计某用户的对话数量
     * Count conversations by user ID
     */
    long countByUserId(String userId);

    List<ConversationJpaEntity> findByAiReplyStatusAndAiReplyStartedAtBefore(
            AiReplyStatus status, LocalDateTime cutoff);
}
