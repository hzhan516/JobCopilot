package edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.matching;

import edu.asu.ser594.resumeassistant.domain.matching.entity.MatchingModel;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.matching.MatchingModelJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 匹配模型持久化映射器
 * Matching model persistence mapper
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MatchingModelPersistenceMapper {

    MatchingModel toDomain(MatchingModelJpaEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MatchingModelJpaEntity toEntity(MatchingModel domain);
}
