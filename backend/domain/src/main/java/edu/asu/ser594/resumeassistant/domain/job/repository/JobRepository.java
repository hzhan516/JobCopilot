package edu.asu.ser594.resumeassistant.domain.job.repository;

import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import java.util.Optional;

/**
 * Repository interface for Job aggregates.
 */
public interface JobRepository {

    /**
     * Saves a job to the repository.
     * 
     * @param job The job to save.
     * @return The saved job.
     */
    Job save(Job job);

    /**
     * Finds a job by its unique identifier.
     * 
     * @param id The ID of the job.
     * @return An Optional containing the job if found, or empty otherwise.
     */
    Optional<Job> findById(String id);
}
