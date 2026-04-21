package edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.matching;

import edu.asu.ser594.resumeassistant.domain.matching.entity.JobMatchResult;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.matching.JobMatchResultJpaEntity;
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
