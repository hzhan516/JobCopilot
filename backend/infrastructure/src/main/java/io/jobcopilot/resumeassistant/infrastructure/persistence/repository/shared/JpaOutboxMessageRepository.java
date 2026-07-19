package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.shared;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.shared.OutboxMessageJpaEntity;
import io.jobcopilot.resumeassistant.types.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

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

    @Query("""
            select o from OutboxMessageJpaEntity o
            where o.status in (io.jobcopilot.resumeassistant.types.enums.OutboxStatus.PENDING,
                               io.jobcopilot.resumeassistant.types.enums.OutboxStatus.FAILED)
              and (o.nextAttemptAt is null or o.nextAttemptAt <= :now)
            order by o.createdAt asc
            """)
    List<OutboxMessageJpaEntity> findDueForDelivery(LocalDateTime now, Pageable pageable);

    @Query("""
            select o from OutboxMessageJpaEntity o
            where o.status = io.jobcopilot.resumeassistant.types.enums.OutboxStatus.PROCESSING
              and o.lockedAt < :cutoff
            order by o.lockedAt asc
            """)
    List<OutboxMessageJpaEntity> findStaleProcessing(LocalDateTime cutoff, Pageable pageable);
}
