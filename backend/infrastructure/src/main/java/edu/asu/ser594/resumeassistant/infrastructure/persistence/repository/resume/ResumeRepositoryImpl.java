package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.resume;

import edu.asu.ser594.resumeassistant.domain.resume.entity.Resume;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.ResumePersistenceMapper;
import edu.asu.ser594.resumeassistant.infrastructure.repository.resume.JpaResumeRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 简历仓储实现
 * Resume repository implementation
 */
@Repository
@RequiredArgsConstructor
public class ResumeRepositoryImpl implements ResumeRepository {

    private final JpaResumeRepository jpaRepository;
    private final ResumePersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public Resume save(Resume resume) {
        // 如果 ID 已存在，先从数据库加载实体，再更新字段
        if (resume.getId() != null && jpaRepository.existsById(resume.getId())) {
            ResumeJpaEntity existingEntity = jpaRepository.findById(resume.getId()).orElseThrow();
            mapper.updateEntityFromDomain(resume, existingEntity);
            return mapper.toDomain(jpaRepository.save(existingEntity));
        }
        // 新实体使用 persist() 而非 save()，避免 merge() 对 pre-set ID 的问题
        ResumeJpaEntity newEntity = mapper.toJpaEntity(resume);
        entityManager.persist(newEntity);
        return mapper.toDomain(newEntity);
    }

    @Override
    public Resume updateStoragePath(UUID resumeId, String storagePath) {
        ResumeJpaEntity entity = jpaRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found: " + resumeId));
        entity.setStoragePath(storagePath);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Resume> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Resume> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public Page<Resume> findByUserId(UUID userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable).map(mapper::toDomain);
    }

    @Override
    public void delete(Resume resume) {
        jpaRepository.deleteById(resume.getId());
    }

    @Override
    public boolean existsByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.existsByIdAndUserId(id, userId);
    }
}
