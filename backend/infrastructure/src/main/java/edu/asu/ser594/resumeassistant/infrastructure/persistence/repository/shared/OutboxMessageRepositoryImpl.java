package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.shared;

import edu.asu.ser594.resumeassistant.domain.shared.entity.OutboxMessage;
import edu.asu.ser594.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.shared.OutboxMessagePersistenceMapper;
import edu.asu.ser594.resumeassistant.types.enums.OutboxStatus;
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
