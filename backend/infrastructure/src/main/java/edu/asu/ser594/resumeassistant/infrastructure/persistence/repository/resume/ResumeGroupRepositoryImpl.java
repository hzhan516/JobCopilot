package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.resume;

import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeGroupJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeVersionJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.repository.resume.JpaResumeGroupRepository;
import edu.asu.ser594.resumeassistant.infrastructure.repository.resume.JpaResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 简历组仓储实现
 * Resume Group Repository Implementation
 */
@Repository
@RequiredArgsConstructor
public class ResumeGroupRepositoryImpl implements ResumeGroupRepository {

    private final JpaResumeGroupRepository jpaGroupRepo;
    private final JpaResumeVersionRepository jpaVersionRepo;

    @Override
    public void save(ResumeGroup group) {
        jpaGroupRepo.save(toEntity(group));

        // 级联保存版本
        for (ResumeVersion version : group.getVersions()) {
            jpaVersionRepo.save(toVersionEntity(version));
        }
    }

    @Override
    public Optional<ResumeGroup> findById(UUID groupId) {
        return jpaGroupRepo.findById(groupId)
                .map(e -> toDomain(e, loadVersions(groupId)));
    }

    @Override
    public Optional<ResumeGroup> findByIdAndUserId(UUID groupId, UUID userId) {
        return jpaGroupRepo.findByIdAndUserId(groupId, userId)
                .map(e -> toDomain(e, loadVersions(groupId)));
    }

    @Override
    public List<ResumeGroup> findAllByUserId(UUID userId) {
        return jpaGroupRepo.findAllByUserId(userId).stream()
                .map(e -> toDomain(e, loadVersions(e.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ResumeGroup> findDefaultByUserId(UUID userId) {
        return jpaGroupRepo.findByUserIdAndIsDefaultTrue(userId)
                .map(e -> toDomain(e, loadVersions(e.getId())));
    }

    @Override
    public void delete(UUID groupId) {
        jpaGroupRepo.deleteById(groupId);
    }

    @Override
    public void clearDefaultForUser(UUID userId) {
        jpaGroupRepo.clearDefaultForUser(userId);
    }

    // ==================== 映射方法 ====================

    private List<ResumeVersion> loadVersions(UUID groupId) {
        return jpaVersionRepo.findAllByGroupId(groupId).stream()
                .map(this::toVersionDomain)
                .collect(Collectors.toList());
    }

    private ResumeGroupJpaEntity toEntity(ResumeGroup domain) {
        ResumeGroupJpaEntity e = new ResumeGroupJpaEntity();
        e.setId(domain.getId());
        e.setUserId(domain.getUserId());
        e.setTitle(domain.getTitle());
        e.setIsDefault(domain.isDefault());
        e.setCreatedAt(domain.getCreatedAt());
        e.setUpdatedAt(domain.getUpdatedAt());
        return e;
    }

    private ResumeGroup toDomain(ResumeGroupJpaEntity e, List<ResumeVersion> versions) {
        return ResumeGroup.reconstruct(
                e.getId(), e.getUserId(), e.getTitle(),
                Boolean.TRUE.equals(e.getIsDefault()),
                e.getCreatedAt(), e.getUpdatedAt(), versions
        );
    }

    private ResumeVersionJpaEntity toVersionEntity(ResumeVersion domain) {
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
        e.setStatus(domain.getStatus().name());
        e.setCreatedAt(domain.getCreatedAt());
        e.setUpdatedAt(domain.getUpdatedAt());
        return e;
    }

    private ResumeVersion toVersionDomain(ResumeVersionJpaEntity e) {
        return ResumeVersion.reconstruct(
                e.getId(), e.getGroupId(),
                ResumeVersion.VersionType.valueOf(e.getVersionType()),
                e.getOriginalFileName(), e.getStoredFileName(),
                e.getFileType(),
                e.getFileSize() != null ? e.getFileSize() : 0L,
                e.getStoragePath(), e.getStorageProvider(),
                e.getContent(), e.getParsedContent(),
                ResumeVersion.Status.valueOf(e.getStatus()),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
