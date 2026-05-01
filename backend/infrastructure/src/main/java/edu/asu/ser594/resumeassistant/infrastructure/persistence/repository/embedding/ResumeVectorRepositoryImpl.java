package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.embedding;

import edu.asu.ser594.resumeassistant.domain.embedding.entity.ResumeVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.embedding.ResumeVectorJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 简历向量仓库实现 / Resume vector repository implementation */
@Repository
@RequiredArgsConstructor
public class ResumeVectorRepositoryImpl implements ResumeVectorRepository {

    private final ResumeVectorJpaRepository jpaRepository;

    /** 保存向量（存在则更新） / Save vector (update if exists) */
    @Override
    public void save(ResumeVector vector) {
        Optional<ResumeVectorJpaEntity> existing = jpaRepository.findByResumeVersionId(vector.getResumeVersionId());
        ResumeVectorJpaEntity entity = toEntity(vector);
        existing.ifPresent(e -> entity.setId(e.getId()));  // 复用已有 id，JPA 就会执行 UPDATE / Reuse existing id for JPA UPDATE
        jpaRepository.save(entity);
    }

    /** 根据简历版本 ID 查询向量 / Find vector by resume version ID */
    @Override
    public Optional<ResumeVector> findByResumeVersionId(String resumeVersionId) {
        return jpaRepository.findByResumeVersionId(resumeVersionId)
                .map(this::toDomain);
    }

    // 领域对象转 JPA 实体 / Domain object to JPA entity
    private ResumeVectorJpaEntity toEntity(ResumeVector domain) {
        return ResumeVectorJpaEntity.builder()
                .id(domain.getId())
                .resumeVersionId(domain.getResumeVersionId())
                .embedding(domain.getEmbedding())
                .status(domain.getStatus())
                .errorMessage(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    // JPA 实体转领域对象 / JPA entity to domain object
    private ResumeVector toDomain(ResumeVectorJpaEntity entity) {
        return new ResumeVector(
                entity.getId(),
                entity.getResumeVersionId(),
                entity.getEmbedding(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
