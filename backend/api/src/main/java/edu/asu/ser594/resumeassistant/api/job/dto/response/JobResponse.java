package edu.asu.ser594.resumeassistant.api.job.dto.response;

import java.util.List;

/**
 * Output representation of a Job.
 * 
 * @param id The unique job identifier.
 * @param userId The ID of the user who submitted the job.
 * @param originalUrl The URL of the job posting.
 * @param status The current processing status of the job.
 * @param parsedContent The parsed structured data, if completed.
 * @param imageCheckEnabled Whether visual verification was enabled.
 * @param errorMessage Any error message if the job failed.
 */
public record JobResponse(
    String id,
    String userId,
    String originalUrl,
    String status,
    ParsedJobContentResponse parsedContent,
    boolean imageCheckEnabled,
    String errorMessage
) {
    /**
     * DTO for parsed job content to avoid exposing domain value objects directly.
     * 
     * @param title The job title.
     * @param company The hiring company.
     * @param description The full job description.
     * @param requirements A list of requirements or qualifications.
     */
    public record ParsedJobContentResponse(
        String title,
        String company,
        String description,
        List<String> requirements
    ) {}
}
