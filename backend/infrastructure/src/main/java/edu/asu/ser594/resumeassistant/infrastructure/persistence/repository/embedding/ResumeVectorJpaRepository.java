package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.embedding;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.embedding.ResumeVectorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeVectorJpaRepository extends JpaRepository<ResumeVectorJpaEntity, String> {

    Optional<ResumeVectorJpaEntity> findByResumeVersionId(String resumeVersionId);
}