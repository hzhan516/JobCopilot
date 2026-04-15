package edu.asu.ser594.resumeassistant.domain.job.port;

import edu.asu.ser594.resumeassistant.domain.job.event.JobProcessRequestEvent;

/**
 * Outbound port for publishing job events to the Message Queue.
 */
public interface JobEventPublisherPort {

    /**
     * Publishes a request to process a job asynchronously.
     * @param event The event details containing jobId and URL.
     */
    void publishJobProcessRequest(JobProcessRequestEvent event);
}
