package edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.tracking;

import edu.asu.ser594.resumeassistant.domain.tracking.entity.ApplicationTracking;
import edu.asu.ser594.resumeassistant.domain.tracking.entity.TrackingEvent;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.tracking.ApplicationTrackingJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.tracking.TrackingEventJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

/**
 * 求职申请跟踪持久化映射器
 * Application tracking persistence mapper
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = UUID.class)
public interface TrackingPersistenceMapper {

    @Mapping(target = "userId", expression = "java(UUID.fromString(entity.getUserId()))")
    ApplicationTracking toDomain(ApplicationTrackingJpaEntity entity);

    @Mapping(target = "userId", expression = "java(domain.getUserId().toString())")
    ApplicationTrackingJpaEntity toEntity(ApplicationTracking domain);

    TrackingEvent toDomainEvent(TrackingEventJpaEntity entity);

    TrackingEventJpaEntity toEntityEvent(TrackingEvent domain);

    List<TrackingEvent> toDomainEvents(List<TrackingEventJpaEntity> entities);

    List<TrackingEventJpaEntity> toEntityEvents(List<TrackingEvent> domains);
}
