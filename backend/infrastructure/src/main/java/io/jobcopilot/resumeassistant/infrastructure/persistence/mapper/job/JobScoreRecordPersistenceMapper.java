package io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.job;

import io.jobcopilot.resumeassistant.domain.job.entity.JobScoreRecord;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.job.JobScoreRecordJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobScoreRecordPersistenceMapper {

    JobScoreRecord toDomain(JobScoreRecordJpaEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    JobScoreRecordJpaEntity toEntity(JobScoreRecord domain);
}
