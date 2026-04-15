package edu.asu.ser594.resumeassistant.domain.job.event;

import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;

/**
 * Domain event representing the result of an AI job processing request.
 */
public record JobProcessResultEvent(
    String jobId,
    boolean success,
    ParsedJobContent parsedContent,
    String errorMessage
) {}
