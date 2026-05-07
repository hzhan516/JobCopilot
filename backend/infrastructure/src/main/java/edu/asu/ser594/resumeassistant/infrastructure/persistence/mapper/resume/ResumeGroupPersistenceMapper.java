package edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.resume;

import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeGroupJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历组持久化映射器
 * Resume Group Persistence Mapper
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResumeGroupPersistenceMapper {

    default ResumeGroup toDomain(ResumeGroupJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ResumeGroup.reconstruct(
                entity.getId(),
                entity.getUserId(),
                entity.getTitle(),
                Boolean.TRUE.equals(entity.getIsDefault()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                new ArrayList<>()
        );
    }

    default ResumeGroup toDomain(ResumeGroupJpaEntity entity, List<ResumeVersion> versions) {
        if (entity == null) {
            return null;
        }
        return ResumeGroup.reconstruct(
                entity.getId(),
                entity.getUserId(),
                entity.getTitle(),
                Boolean.TRUE.equals(entity.getIsDefault()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                versions
        );
    }

    default ResumeGroupJpaEntity toJpaEntity(ResumeGroup domain) {
        if (domain == null) {
            return null;
        }
        ResumeGroupJpaEntity entity = new ResumeGroupJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setTitle(domain.getTitle());
        entity.setIsDefault(domain.isDefault());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
