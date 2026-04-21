package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.embedding;

import edu.asu.ser594.resumeassistant.domain.embedding.entity.ResumeVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.embedding.ResumeVectorJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ResumeVectorRepositoryImpl implements ResumeVectorRepository {

    private final ResumeVectorJpaRepository jpaRepository;

    @Override
    public void save(ResumeVector vector) {
        Optional<ResumeVectorJpaEntity> existing = jpaRepository.findByResumeVersionId(vector.getResumeVersionId());
        ResumeVectorJpaEntity entity = toEntity(vector);
        existing.ifPresent(e -> entity.setId(e.getId()));  // 复用已有 id，JPA 就会执行 UPDATE
        jpaRepository.save(entity);
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