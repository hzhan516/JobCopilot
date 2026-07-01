package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.resume;

import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.resume.ResumeGroupJpaEntity;
import io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.resume.ResumeGroupPersistenceMapper;
import io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.resume.ResumeVersionPersistenceMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Infrastructure implementation of ResumeGroupRepository with careful flush ordering
 * to respect the partial unique index on active resume versions.
 * ResumeGroupRepository 的基础设施实现，通过精细控制 flush 顺序避免违反活跃简历版本的部分唯一索引
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
        ResumeGroupJpaEntity groupEntity = groupMapper.toJpaEntity(group);
        if (!jpaGroupRepo.existsById(group.getId())) {
            groupEntity.setVersion(null);
        }
        jpaGroupRepo.save(groupEntity);

        // Separate existing and new versions to ensure UPDATEs run before INSERTs,
        // preventing violation of the partial unique index (group_id, version_type) WHERE status = 'ACTIVE'.
        // 分离已存在版本与新增版本，确保 UPDATE 先于 INSERT 执行，避免违反部分唯一索引
        List<ResumeVersion> existing = new ArrayList<>();
        List<ResumeVersion> newVersions = new ArrayList<>();

        for (ResumeVersion version : group.getVersions()) {
            if (jpaVersionRepo.existsById(version.getId())) {
                existing.add(version);
            } else {
                newVersions.add(version);
            }
        }

        // Split ARCHIVED versions to flush first so they release the partial unique slot
        // before ACTIVE versions claim it in the same transaction.
        // 将 ARCHIVED 版本挑出来先 flush，使其在 ACTIVE 版本占用唯一索引槽位前释放
        List<ResumeVersion> archived = new ArrayList<>();
        List<ResumeVersion> rest = new ArrayList<>();
        for (ResumeVersion version : existing) {
            if (version.getStatus() == ResumeVersion.Status.ARCHIVED) {
                archived.add(version);
            } else {
                rest.add(version);
            }
        }

        for (ResumeVersion version : archived) {
            jpaVersionRepo.saveAndFlush(versionMapper.toJpaEntity(version));
        }

        for (ResumeVersion version : rest) {
            jpaVersionRepo.save(versionMapper.toJpaEntity(version));
        }
        if (!rest.isEmpty()) {
            entityManager.flush();
        }

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
    public long count() {
        return jpaGroupRepo.count();
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
