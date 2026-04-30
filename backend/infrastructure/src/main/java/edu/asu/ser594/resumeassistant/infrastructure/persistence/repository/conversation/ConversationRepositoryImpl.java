package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.conversation;

import edu.asu.ser594.resumeassistant.domain.conversation.entity.Conversation;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Message;
import edu.asu.ser594.resumeassistant.domain.conversation.repository.ConversationRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.conversation.ConversationJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.conversation.MessageJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<Conversation> findById(UUID id) {
        return jpaRepository.findById(id.toString()).map(this::mapToDomainEntity);
    }

    @Override
    public List<Conversation> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserId(userId.toString()).stream()
            .map(this::mapToDomainEntity)
            .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id.toString());
    }

    /**
     * 将领域实体映射为 JPA 实体
     * Map domain entity to JPA entity
     */
    private ConversationJpaEntity mapToJpaEntity(Conversation conversation) {
        ConversationJpaEntity entity = ConversationJpaEntity.builder()
            .id(conversation.getId().toString())
            .userId(conversation.getUserId().toString())
            .title(conversation.getTitle())
            .status(conversation.getStatus())
            .resumeVersionId(conversation.getResumeVersionId() != null ? conversation.getResumeVersionId().toString() : null)
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
            .id(message.getId().toString())
            .conversation(conversation)
            .role(message.getRole())
            .content(message.getContent())
            .sequence(message.getSequence())
            .createdAt(message.getCreatedAt())
            .fileUrl(message.getFileUrl())
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
            java.util.UUID.fromString(entity.getId()),
            java.util.UUID.fromString(entity.getUserId()),
            entity.getTitle(),
            entity.getStatus(),
            entity.getResumeVersionId() != null ? java.util.UUID.fromString(entity.getResumeVersionId()) : null,
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            new ArrayList<>(messages)
        );
    }

    /**
     * 将消息 JPA 实体映射为领域实体
     * Map message JPA entity to domain entity
     */
    private Message mapMessageToDomainEntity(MessageJpaEntity entity) {
        return Message.reconstruct(
            java.util.UUID.fromString(entity.getId()),
            java.util.UUID.fromString(entity.getConversation().getId()),
            entity.getRole(),
            entity.getContent(),
            entity.getSequence(),
            entity.getCreatedAt(),
            entity.getFileUrl()
        );
    }
}
