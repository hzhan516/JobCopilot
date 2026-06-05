package io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.job;

import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.job.JobJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobPersistenceMapper {

    Job toDomain(JobJpaEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobJpaEntity toEntity(Job domain);
}
