package edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.shared;

import edu.asu.ser594.resumeassistant.domain.shared.entity.OutboxMessage;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.shared.OutboxMessageJpaEntity;
import org.mapstruct.Mapper;

/**
 * Outbox 消息持久化映射器
 * Outbox message persistence mapper
 */
@Mapper(componentModel = "spring")
public interface OutboxMessagePersistenceMapper {

    OutboxMessage toDomain(OutboxMessageJpaEntity entity);

    OutboxMessageJpaEntity toJpaEntity(OutboxMessage message);
}
