package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.shared;

import io.jobcopilot.resumeassistant.domain.shared.entity.OutboxMessage;
import io.jobcopilot.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.shared.OutboxMessagePersistenceMapper;
import io.jobcopilot.resumeassistant.types.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Outbox 消息仓储实现
 * Outbox message repository implementation
 */
@Repository
@RequiredArgsConstructor
public class OutboxMessageRepositoryImpl implements OutboxMessageRepository {

    private final JpaOutboxMessageRepository jpaRepository;
    private final OutboxMessagePersistenceMapper mapper;

    @Override
    public OutboxMessage save(OutboxMessage message) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(message)));
    }

    @Override
    public List<OutboxMessage> findByStatus(OutboxStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByStatusAndSentAtBefore(OutboxStatus status, LocalDateTime cutoff) {
        jpaRepository.deleteByStatusAndSentAtBefore(status, cutoff);
    }
}
