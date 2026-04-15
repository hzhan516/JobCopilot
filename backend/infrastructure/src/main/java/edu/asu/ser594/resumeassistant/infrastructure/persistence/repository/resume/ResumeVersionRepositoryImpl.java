package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.resume;

import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeVersionJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.repository.resume.JpaResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 简历版本仓储实现
 * Resume Version Repository Implementation
 */
@Repository
@RequiredArgsConstructor
public class ResumeVersionRepositoryImpl implements ResumeVersionRepository {

    private final JpaResumeVersionRepository jpaRepo;

    @Override
    public void save(ResumeVersion version) {
        jpaRepo.save(toEntity(version));
    }

    @Override
    public void saveAll(List<ResumeVersion> versions) {
        jpaRepo.saveAll(versions.stream().map(this::toEntity).collect(Collectors.toList()));
    }

    @Override
    public Optional<ResumeVersion> findById(UUID versionId) {
        return jpaRepo.findById(versionId).map(this::toDomain);
    }

    @Override
    public List<ResumeVersion> findAllByGroupId(UUID groupId) {
        return jpaRepo.findAllByGroupId(groupId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ResumeVersion> findActiveByGroupIdAndType(UUID groupId,
                                                              ResumeVersion.VersionType type) {
        return jpaRepo.findActiveByGroupIdAndType(groupId, type.name())
                .map(this::toDomain);
    }

    @Override
    public void delete(UUID versionId) {
        jpaRepo.deleteById(versionId);
    }

    @Override
    public void deleteAllByGroupId(UUID groupId) {
        jpaRepo.deleteAllByGroupId(groupId);
    }

    private ResumeVersionJpaEntity toEntity(ResumeVersion domain) {
        ResumeVersionJpaEntity e = new ResumeVersionJpaEntity();
        e.setId(domain.getId());
        e.setGroupId(domain.getGroupId());
        e.setVersionType(domain.getVersionType().name());
        e.setOriginalFileName(domain.getOriginalFileName());
        e.setStoredFileName(domain.getStoredFileName());
        e.setFileType(domain.getFileType());
        e.setFileSize(domain.getFileSize());
        e.setStoragePath(domain.getStoragePath());
        e.setStorageProvider(domain.getStorageProvider());
        e.setContent(domain.getContent());
        e.setParsedContent(domain.getParsedContent());
        e.setParseStatus(domain.getParseStatus());
        e.setParseErrorMessage(domain.getParseErrorMessage());
        e.setStatus(domain.getStatus().name());
        e.setCreatedAt(domain.getCreatedAt());
        e.setUpdatedAt(domain.getUpdatedAt());
        return e;
    }

    private ResumeVersion toDomain(ResumeVersionJpaEntity e) {
        return ResumeVersion.reconstruct(
                e.getId(), e.getGroupId(),
                ResumeVersion.VersionType.valueOf(e.getVersionType()),
                e.getOriginalFileName(), e.getStoredFileName(),
                e.getFileType(),
                e.getFileSize() != null ? e.getFileSize() : 0L,
                e.getStoragePath(), e.getStorageProvider(),
                e.getContent(), e.getParsedContent(),
                e.getParseStatus(), e.getParseErrorMessage(),
                ResumeVersion.Status.valueOf(e.getStatus()),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
