package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.shared;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.shared.OutboxMessageJpaEntity;
import io.jobcopilot.resumeassistant.types.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 消息 JPA 仓储
 * Outbox message JPA repository
 */
@Repository
public interface JpaOutboxMessageRepository extends JpaRepository<OutboxMessageJpaEntity, String> {

    List<OutboxMessageJpaEntity> findByStatus(OutboxStatus status);

    void deleteByStatusAndSentAtBefore(OutboxStatus status, LocalDateTime cutoff);
}
