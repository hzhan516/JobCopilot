package io.jobcopilot.resumeassistant.infrastructure.persistence.entity.shared;

import io.jobcopilot.resumeassistant.types.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Outbox 消息 JPA 实体
 * Outbox message JPA entity
 */
@Entity
@Table(name = "outbox_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class OutboxMessageJpaEntity {

    @Id
    @Column(updatable = false, nullable = false, length = 36)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @Column(name = "exchange", nullable = false, length = 255)
    private String exchange;

    @Column(name = "routing_key", nullable = false, length = 255)
    private String routingKey;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "worker_id", length = 128)
    private String workerId;
}
