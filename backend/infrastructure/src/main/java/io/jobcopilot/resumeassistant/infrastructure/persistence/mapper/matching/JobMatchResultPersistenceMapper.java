package io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.matching;

import io.jobcopilot.resumeassistant.domain.matching.entity.JobMatchResult;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.matching.JobMatchResultJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 职位匹配结果持久化映射器
 * Job match result persistence mapper
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobMatchResultPersistenceMapper {

    @Mapping(target = "userId", expression = "java(java.util.UUID.fromString(entity.getUserId()))")
    JobMatchResult toDomain(JobMatchResultJpaEntity entity);

    @Mapping(target = "userId", expression = "java(domain.getUserId().toString())")
    @Mapping(target = "createdAt", ignore = true)
    JobMatchResultJpaEntity toEntity(JobMatchResult domain);
}
