package io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.shared;

import io.jobcopilot.resumeassistant.domain.shared.entity.OutboxMessage;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.shared.OutboxMessageJpaEntity;
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
