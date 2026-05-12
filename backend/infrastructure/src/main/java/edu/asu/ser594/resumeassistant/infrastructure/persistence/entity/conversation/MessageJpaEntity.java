package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.conversation;

import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.MessageRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 消息持久化实体
 * Message JPA entity
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageJpaEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationJpaEntity conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int sequence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "file_url")
    private String fileUrl;
}
