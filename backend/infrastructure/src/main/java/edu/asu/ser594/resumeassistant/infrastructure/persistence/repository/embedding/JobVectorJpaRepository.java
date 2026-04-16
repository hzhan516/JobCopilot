package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.embedding;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.embedding.JobVectorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobVectorJpaRepository extends JpaRepository<JobVectorJpaEntity, String> {

    Optional<JobVectorJpaEntity> findByJobId(String jobId);
}