package edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.resume;

import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeVersionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * 简历版本持久化映射器
 * Resume Version Persistence Mapper
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResumeVersionPersistenceMapper {

    default ResumeVersion toDomain(ResumeVersionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ResumeVersion.reconstruct(
                entity.getId(),
                entity.getGroupId(),
                ResumeVersion.VersionType.valueOf(entity.getVersionType()),
                entity.getOriginalFileName(),
                entity.getStoredFileName(),
                entity.getFileType(),
                entity.getFileSize() != null ? entity.getFileSize() : 0L,
                entity.getStoragePath(),
                entity.getStorageProvider(),
                entity.getContent(),
                entity.getParsedContent(),
                entity.getParseStatus(),
                entity.getParseErrorMessage(),
                ResumeVersion.Status.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    default ResumeVersionJpaEntity toJpaEntity(ResumeVersion domain) {
        if (domain == null) {
            return null;
        }
        ResumeVersionJpaEntity entity = new ResumeVersionJpaEntity();
        entity.setId(domain.getId());
        entity.setGroupId(domain.getGroupId());
        entity.setVersionType(domain.getVersionType().name());
        entity.setOriginalFileName(domain.getOriginalFileName());
        entity.setStoredFileName(domain.getStoredFileName());
        entity.setFileType(domain.getFileType());
        entity.setFileSize(domain.getFileSize());
        entity.setStoragePath(domain.getStoragePath());
        entity.setStorageProvider(domain.getStorageProvider());
        entity.setContent(domain.getContent());
        entity.setParsedContent(domain.getParsedContent());
        entity.setParseStatus(domain.getParseStatus());
        entity.setParseErrorMessage(domain.getParseErrorMessage());
        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
