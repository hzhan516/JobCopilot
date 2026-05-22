package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.embedding;

import io.jobcopilot.resumeassistant.domain.embedding.entity.ResumeVector;
import io.jobcopilot.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.embedding.ResumeVectorJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Infrastructure implementation of ResumeVectorRepository bridging domain operations
 * to pgvector-backed JPA queries.
 * ResumeVectorRepository 的基础设施实现，桥接领域操作与 pgvector 原生查询
 */
@Repository
@RequiredArgsConstructor
public class ResumeVectorRepositoryImpl implements ResumeVectorRepository {

    private final ResumeVectorJpaRepository jpaRepository;

    @Override
    public void save(ResumeVector vector) {
        Optional<ResumeVectorJpaEntity> existing = jpaRepository.findByResumeVersionId(vector.getResumeVersionId());
        ResumeVectorJpaEntity entity = toEntity(vector);
        existing.ifPresent(e -> entity.setId(e.getId())); // Reuse ID to trigger JPA UPDATE | 复用已有 ID 使 JPA 执行更新
        jpaRepository.save(entity);
    }

    @Override
    public void saveAll(List<ResumeVector> vectors) {
        List<ResumeVectorJpaEntity> entities = new ArrayList<>(vectors.size());
        for (ResumeVector vector : vectors) {
            Optional<ResumeVectorJpaEntity> existing = jpaRepository.findByResumeVersionId(vector.getResumeVersionId());
            ResumeVectorJpaEntity entity = toEntity(vector);
            existing.ifPresent(e -> entity.setId(e.getId()));
            entities.add(entity);
        }
        jpaRepository.saveAll(entities);
    }

    @Override
    public Optional<ResumeVector> findByResumeVersionId(String resumeVersionId) {
        return jpaRepository.findByResumeVersionId(resumeVersionId)
                .map(this::toDomain);
    }

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
