package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.resume;

import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeGroupJpaEntity;
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

        // 将 existing 中 status 为 ARCHIVED 的挑出来先 saveAndFlush，
        // 避免与后续变为 ACTIVE 的版本在 partial unique index 上冲突。
        // Separate ARCHIVED existing versions to saveAndFlush first,
        // preventing partial unique index violation with later ACTIVE versions.
        List<ResumeVersion> archived = new ArrayList<>();
        List<ResumeVersion> rest = new ArrayList<>();
        for (ResumeVersion version : existing) {
            if (version.getStatus() == ResumeVersion.Status.ARCHIVED) {
                archived.add(version);
            } else {
                rest.add(version);
            }
        }

        // 先保存并刷盘 ARCHIVED 版本
        // Save and flush ARCHIVED versions first
        for (ResumeVersion version : archived) {
            jpaVersionRepo.saveAndFlush(versionMapper.toJpaEntity(version));
        }

        // 再批量保存剩下的已存在版本（产生 UPDATE）
        // Then batch save remaining existing versions (produces UPDATE)
        for (ResumeVersion version : rest) {
            jpaVersionRepo.save(versionMapper.toJpaEntity(version));
        }
        if (!rest.isEmpty()) {
            entityManager.flush();
        }

        // 最后保存新增版本（产生 INSERT）
        // Finally save new versions (produces INSERT)
        for (ResumeVersion version : newVersions) {
            jpaVersionRepo.save(versionMapper.toJpaEntity(version));
        }
    }

    @Override
    public Optional<ResumeGroup> findById(UUID groupId) {
        return jpaGroupRepo.findById(groupId)
                .map(this::mapToDomainWithVersions);
    }

    @Override
    public Optional<ResumeGroup> findByIdAndUserId(UUID groupId, UUID userId) {
        return jpaGroupRepo.findByIdAndUserId(groupId, userId)
                .map(this::mapToDomainWithVersions);
    }

    @Override
    public List<ResumeGroup> findAllByUserId(UUID userId) {
        return jpaGroupRepo.findAllByUserId(userId).stream()
                .map(this::mapToDomainWithVersions)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ResumeGroup> findDefaultByUserId(UUID userId) {
        return jpaGroupRepo.findByUserIdAndIsDefaultTrue(userId)
                .map(this::mapToDomainWithVersions);
    }

    @Override
    public void delete(UUID groupId) {
        jpaGroupRepo.deleteById(groupId);
    }

    @Override
    public void clearDefaultForUser(UUID userId) {
        jpaGroupRepo.clearDefaultForUser(userId);
    }

    private ResumeGroup mapToDomainWithVersions(ResumeGroupJpaEntity entity) {
        List<ResumeVersion> versions = jpaVersionRepo.findAllByGroupId(entity.getId()).stream()
                .map(versionMapper::toDomain)
                .collect(Collectors.toList());
        return groupMapper.toDomain(entity, versions);
    }
}
