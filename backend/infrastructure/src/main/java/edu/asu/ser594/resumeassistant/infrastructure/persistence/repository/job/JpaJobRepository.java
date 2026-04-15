package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.job;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.job.JobJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaJobRepository extends JpaRepository<JobJpaEntity, String> {
}
