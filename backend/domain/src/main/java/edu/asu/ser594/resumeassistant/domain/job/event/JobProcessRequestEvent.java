package edu.asu.ser594.resumeassistant.domain.job.event;

/**
 * Domain event representing a request to process a job via AI service (scraping + parsing + vision).
 */
public record JobProcessRequestEvent(
    String jobId,
    String url,
    boolean imageCheckEnabled
) {}
