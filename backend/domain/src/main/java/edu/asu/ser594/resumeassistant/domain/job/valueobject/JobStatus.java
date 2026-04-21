package edu.asu.ser594.resumeassistant.domain.job.valueobject;

/**
 * Represents the current status of a Job processing workflow.
 */
public enum JobStatus {
    /**
     * Job has been submitted but processing has not yet started.
     */
    PENDING,

    /**
     * The system is currently scraping the job posting URL.
     */
    SCRAPING,

    /**
     * The system is currently parsing the scraped content into structured data.
     */
    PARSING,

    /**
     * The job processing has completed successfully.
     */
    COMPLETED,

    /**
     * The job processing has failed.
     */
    FAILED
}
