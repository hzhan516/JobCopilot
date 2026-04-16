package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.conversation;

import edu.asu.ser594.resumeassistant.domain.conversation.entity.Conversation;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Message;
import edu.asu.ser594.resumeassistant.domain.conversation.repository.ConversationRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.conversation.ConversationJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.conversation.MessageJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 对话仓储实现
 * Conversation repository implementation
 */
@Repository
@RequiredArgsConstructor
public class ConversationRepositoryImpl implements ConversationRepository {

    private final JpaConversationRepository jpaRepository;

    @Override
    public Conversation save(Conversation conversation) {
        ConversationJpaEntity entity = mapToJpaEntity(conversation);
        ConversationJpaEntity saved = jpaRepository.save(entity);
        return mapToDomainEntity(saved);
    }

    @Override
    public Optional<Conversation> findById(String id) {
        return jpaRepository.findById(id).map(this::mapToDomainEntity);
    }

    @Override
    public List<Conversation> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId).stream()
            .map(this::mapToDomainEntity)
            .toList();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    /**
     * 将领域实体映射为 JPA 实体
     * Map domain entity to JPA entity
     */
    private ConversationJpaEntity mapToJpaEntity(Conversation conversation) {
        ConversationJpaEntity entity = ConversationJpaEntity.builder()
            .id(conversation.getId())
            .userId(conversation.getUserId())
            .title(conversation.getTitle())
            .status(conversation.getStatus())
            .resumeVersionId(conversation.getResumeVersionId())
            .createdAt(conversation.getCreatedAt())
            .updatedAt(conversation.getUpdatedAt())
            .build();

        List<MessageJpaEntity> messageEntities = conversation.getMessages().stream()
            .map(msg -> mapMessageToJpaEntity(msg, entity))
            .toList();

        entity.setMessages(new java.util.ArrayList<>(messageEntities));
        return entity;
    }

    /**
     * 将消息领域实体映射为 JPA 实体
     * Map message domain entity to JPA entity
     */
    private MessageJpaEntity mapMessageToJpaEntity(Message message, ConversationJpaEntity conversation) {
        return MessageJpaEntity.builder()
            .id(message.getId())
            .conversation(conversation)
            .role(message.getRole())
            .content(message.getContent())
            .sequence(message.getSequence())
            .createdAt(message.getCreatedAt())
            .build();
    }

    /**
     * 将 JPA 实体映射为领域实体
     * Map JPA entity to domain entity
     */
    private Conversation mapToDomainEntity(ConversationJpaEntity entity) {
        List<Message> messages = entity.getMessages().stream()
            .sorted(Comparator.comparingInt(MessageJpaEntity::getSequence))
            .map(this::mapMessageToDomainEntity)
            .toList();

        return Conversation.reconstruct(
            entity.getId(),
            entity.getUserId(),
            entity.getTitle(),
            entity.getStatus(),
            entity.getResumeVersionId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            messages
        );
    }

    /**
     * 将消息 JPA 实体映射为领域实体
     * Map message JPA entity to domain entity
     */
    private Message mapMessageToDomainEntity(MessageJpaEntity entity) {
        // Since constructor is protected, we can reflect or we can simply add a reconstruct method or static method.
        // Or if the layers are separated and we can't call constructor, we should use reflection or expose a reconstruct method in Message.
        // Let's modify Message.java to add reconstruct method, but here we can just use the builder if we had one.
        // Wait, Message constructor is protected, and they are in different packages!
        // This is a common DDD issue. We need a way to reconstruct Message.
        return Message.reconstruct(
            entity.getId(),
            entity.getConversation().getId(),
            entity.getRole(),
            entity.getContent(),
            entity.getSequence(),
            entity.getCreatedAt()
        );
    }
}
