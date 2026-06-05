package io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.matching;

import io.jobcopilot.resumeassistant.domain.matching.entity.JobDataset;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.matching.JobDatasetJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 职位数据集持久化映射器
 * Job dataset persistence mapper
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobDatasetPersistenceMapper {

    JobDataset toDomain(JobDatasetJpaEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    JobDatasetJpaEntity toEntity(JobDataset domain);
}
