package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.conversation;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.conversation.ConversationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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

    /**
     * 统计某用户的对话数量
     * Count conversations by user ID
     */
    long countByUserId(String userId);
}
