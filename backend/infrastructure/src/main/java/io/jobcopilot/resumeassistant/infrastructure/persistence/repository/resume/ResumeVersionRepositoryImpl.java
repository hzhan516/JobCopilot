package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.resume;

import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.resume.ResumeVersionPersistenceMapper;
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
    private final ResumeVersionPersistenceMapper mapper;

    @Override
    public void save(ResumeVersion version) {
        jpaRepo.save(mapper.toJpaEntity(version));
    }

    @Override
    public void saveAll(List<ResumeVersion> versions) {
        jpaRepo.saveAll(versions.stream().map(mapper::toJpaEntity).collect(Collectors.toList()));
    }

    @Override
    public Optional<ResumeVersion> findById(UUID versionId) {
        return jpaRepo.findById(versionId).map(mapper::toDomain);
    }

    @Override
    public List<ResumeVersion> findAllByGroupId(UUID groupId) {
        return jpaRepo.findAllByGroupId(groupId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ResumeVersion> findActiveByGroupIdAndType(UUID groupId,
                                                              ResumeVersion.VersionType type) {
        return jpaRepo.findActiveByGroupIdAndType(groupId, type.name())
                .map(mapper::toDomain);
    }

    @Override
    public void delete(UUID versionId) {
        jpaRepo.deleteById(versionId);
    }

    @Override
    public void deleteAllByGroupId(UUID groupId) {
        jpaRepo.deleteAllByGroupId(groupId);
    }

    @Override
    public List<ResumeVersion> findAllByGroupIdAndType(UUID groupId, ResumeVersion.VersionType type) {
        return jpaRepo.findAllByGroupIdAndVersionTypeOrderByCreatedAtAsc(groupId, type.name())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
