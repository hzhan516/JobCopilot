package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.embedding;

import edu.asu.ser594.resumeassistant.domain.embedding.entity.JobVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.JobVectorRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.embedding.JobVectorJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JobVectorRepositoryImpl implements JobVectorRepository {

    private final JobVectorJpaRepository jpaRepository;

    @Override
    public void save(JobVector vector) {
        JobVectorJpaEntity entity = toEntity(vector);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<JobVector> findByJobId(String jobId) {
        return jpaRepository.findByJobId(jobId)
                .map(this::toDomain);
    }

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