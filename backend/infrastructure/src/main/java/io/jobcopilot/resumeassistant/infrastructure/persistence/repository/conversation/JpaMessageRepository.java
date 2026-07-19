package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.conversation;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.conversation.MessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaMessageRepository extends JpaRepository<MessageJpaEntity, String> {
    List<MessageJpaEntity> findByConversationIdOrderBySequenceDesc(String conversationId, Pageable pageable);
}
