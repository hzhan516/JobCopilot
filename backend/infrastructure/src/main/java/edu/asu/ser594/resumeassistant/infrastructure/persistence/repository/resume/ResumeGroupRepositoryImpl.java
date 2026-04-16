package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.resume;

import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeGroupJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeVersionJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.resume.ResumeGroupPersistenceMapper;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.resume.ResumeVersionPersistenceMapper;
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
    private final ResumeGroupPersistenceMapper groupMapper;
    private final ResumeVersionPersistenceMapper versionMapper;

    @Override
    public void save(ResumeGroup group) {
        jpaGroupRepo.save(groupMapper.toJpaEntity(group));

        // 级联保存版本
        for (ResumeVersion version : group.getVersions()) {
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
