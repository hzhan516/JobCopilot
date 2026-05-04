package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.resume;

import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.resume.ResumeGroupPersistenceMapper;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.resume.ResumeVersionPersistenceMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
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
    private final ResumeGroupPersistenceMapper groupMapper;
    private final ResumeVersionPersistenceMapper versionMapper;
    private final EntityManager entityManager;

    @Override
    public void save(ResumeGroup group) {
        jpaGroupRepo.save(groupMapper.toJpaEntity(group));

        // 分离已存在版本与新增版本，确保 UPDATE 先于 INSERT 执行，
        // 避免违反 partial unique index (group_id, version_type) WHERE status = 'ACTIVE'。
        // Separate existing and new versions to ensure UPDATEs run before INSERTs,
        // preventing violation of the partial unique index.
        List<ResumeVersion> existing = new ArrayList<>();
        List<ResumeVersion> newVersions = new ArrayList<>();

        for (ResumeVersion version : group.getVersions()) {
            if (jpaVersionRepo.existsById(version.getId())) {
                existing.add(version);
            } else {
                newVersions.add(version);
            }
        }

        // 先保存已存在版本（产生 UPDATE）
        // Save existing versions first (produces UPDATE)
        for (ResumeVersion version : existing) {
            jpaVersionRepo.save(versionMapper.toJpaEntity(version));
        }

        // 强制刷盘，确保 UPDATE 先于 INSERT 落库
        // Force flush so UPDATEs are persisted before INSERTs
        if (!existing.isEmpty()) {
            entityManager.flush();
        }

        // 再保存新增版本（产生 INSERT）
        // Then save new versions (produces INSERT)
        for (ResumeVersion version : newVersions) {
            jpaVersionRepo.save(versionMapper.toJpaEntity(version));
        }
    }

    @Override
    public Optional<ResumeGroup> findById(UUID groupId) {
        return jpaGroupRepo.findById(groupId)
                .map(e -> {
                    ResumeGroup group = groupMapper.toDomain(e);
                    loadVersionsIntoGroup(group);
                    return group;
                });
    }

    @Override
    public Optional<ResumeGroup> findByIdAndUserId(UUID groupId, UUID userId) {
        return jpaGroupRepo.findByIdAndUserId(groupId, userId)
                .map(e -> {
                    ResumeGroup group = groupMapper.toDomain(e);
                    loadVersionsIntoGroup(group);
                    return group;
                });
    }

    @Override
    public List<ResumeGroup> findAllByUserId(UUID userId) {
        return jpaGroupRepo.findAllByUserId(userId).stream()
                .map(e -> {
                    ResumeGroup group = groupMapper.toDomain(e);
                    loadVersionsIntoGroup(group);
                    return group;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ResumeGroup> findDefaultByUserId(UUID userId) {
        return jpaGroupRepo.findByUserIdAndIsDefaultTrue(userId)
                .map(e -> {
                    ResumeGroup group = groupMapper.toDomain(e);
                    loadVersionsIntoGroup(group);
                    return group;
                });
    }

    @Override
    public void delete(UUID groupId) {
        jpaGroupRepo.deleteById(groupId);
    }

    @Override
    public void clearDefaultForUser(UUID userId) {
        jpaGroupRepo.clearDefaultForUser(userId);
    }

    private void loadVersionsIntoGroup(ResumeGroup group) {
        List<ResumeVersion> versions = jpaVersionRepo.findAllByGroupId(group.getId()).stream()
                .map(versionMapper::toDomain)
                .collect(Collectors.toList());
        for (ResumeVersion version : versions) {
            group.addVersion(version);
        }
    }
}
