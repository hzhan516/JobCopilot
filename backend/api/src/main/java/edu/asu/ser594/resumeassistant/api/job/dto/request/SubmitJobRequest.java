package edu.asu.ser594.resumeassistant.api.job.dto.request;

/**
 * Request to submit a new job parsing task.
 * 
 * @param url The URL of the job posting.
 * @param imageCheckEnabled True to enable visual verification of the job posting layout.
 */
public record SubmitJobRequest(
    String url,
    boolean imageCheckEnabled
) {
}
