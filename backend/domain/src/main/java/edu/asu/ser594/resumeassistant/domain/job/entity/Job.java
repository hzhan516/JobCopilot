package edu.asu.ser594.resumeassistant.domain.job.entity;

import edu.asu.ser594.resumeassistant.domain.job.valueobject.JobStatus;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import edu.asu.ser594.resumeassistant.domain.shared.entity.AggregateRoot;
import lombok.Getter;

import java.util.UUID;

/**
 * The Job aggregate root. Manages the lifecycle of processing a job posting url.
 */
public class Job extends AggregateRoot<String> {

    private final String id;
    @Getter
    private final String userId;
    @Getter
    private final String originalUrl;
    @Getter
    private final boolean imageCheckEnabled;
    @Getter
    private JobStatus status;
    @Getter
    private ParsedJobContent parsedContent;
    @Getter
    private String errorMessage;

    public Job(String id, String userId, String originalUrl, boolean imageCheckEnabled, JobStatus status, ParsedJobContent parsedContent, String errorMessage) {
        this.id = id;
        this.userId = userId;
        this.originalUrl = originalUrl;
        this.imageCheckEnabled = imageCheckEnabled;
        this.status = status;
        this.parsedContent = parsedContent;
        this.errorMessage = errorMessage;
    }

    private Job(String id, String userId, String originalUrl, boolean imageCheckEnabled, JobStatus status) {
        this.id = id;
        this.userId = userId;
        this.originalUrl = originalUrl;
        this.imageCheckEnabled = imageCheckEnabled;
        this.status = status;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Creates a new Job for processing.
     * 
     * @param userId The ID of the user requesting the job parse.
     * @param url The URL of the job posting.
     * @param imageCheckEnabled Whether visual verification is required.
     * @return A newly initialized Job aggregate root.
     */
    public static Job create(String userId, String url, boolean imageCheckEnabled) {
        return new Job(UUID.randomUUID().toString(), userId, url, imageCheckEnabled, JobStatus.PENDING);
    }

    /**
     * Transitions the job state to indicate scraping has started.
     */
    public void markScraping() {
        if (this.status != JobStatus.PENDING) {
            throw new IllegalStateException("Job must be PENDING to start scraping.");
        }
        this.status = JobStatus.SCRAPING;
    }

    /**
     * Transitions the job state to indicate parsing has started.
     */
    public void markParsing() {
        if (this.status != JobStatus.SCRAPING) {
            throw new IllegalStateException("Job must be SCRAPING to start parsing.");
        }
        this.status = JobStatus.PARSING;
    }

    /**
     * Marks the job as successfully completed with the parsed content.
     * 
     * @param parsedContent The structured data extracted from the job posting.
     */
    public void markCompleted(ParsedJobContent parsedContent) {
        if (this.status != JobStatus.PARSING) {
            throw new IllegalStateException("Job must be PARSING to complete.");
        }
        this.status = JobStatus.COMPLETED;
        this.parsedContent = parsedContent;
    }

    /**
     * Marks the job as failed and records the error reason.
     * 
     * @param error A description of why the job processing failed.
     */
    public void markFailed(String error) {
        this.status = JobStatus.FAILED;
        this.errorMessage = error;
    }

}
