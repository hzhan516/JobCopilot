package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.embedding;

import edu.asu.ser594.resumeassistant.domain.embedding.entity.JobVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.JobVectorRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.embedding.JobVectorJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 职位向量仓库实现 / Job vector repository implementation */
@Repository
@RequiredArgsConstructor
public class JobVectorRepositoryImpl implements JobVectorRepository {

    private final JobVectorJpaRepository jpaRepository;

    /** 保存向量（存在则更新） / Save vector (update if exists) */
    @Override
    public void save(JobVector vector) {
        Optional<JobVectorJpaEntity> existing = jpaRepository.findByJobId(vector.getJobId());
        JobVectorJpaEntity entity = toEntity(vector);
        existing.ifPresent(e -> entity.setId(e.getId())); // 复用已有 id，JPA 就会执行 UPDATE / Reuse existing id for JPA UPDATE
        jpaRepository.save(entity);
    }

    /** 根据职位 ID 查询向量 / Find vector by job ID */
    @Override
    public Optional<JobVector> findByJobId(String jobId) {
        return jpaRepository.findByJobId(jobId)
                .map(this::toDomain);
    }

    // 领域对象转 JPA 实体 / Domain object to JPA entity
    private JobVectorJpaEntity toEntity(JobVector domain) {
        return JobVectorJpaEntity.builder()
                .id(domain.getId())
                .jobId(domain.getJobId())
                .embedding(domain.getEmbedding())
                .status(domain.getStatus())
                .errorMessage(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    // JPA 实体转领域对象 / JPA entity to domain object
    private JobVector toDomain(JobVectorJpaEntity entity) {
        return new JobVector(
                entity.getId(),
                entity.getJobId(),
                entity.getEmbedding(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
