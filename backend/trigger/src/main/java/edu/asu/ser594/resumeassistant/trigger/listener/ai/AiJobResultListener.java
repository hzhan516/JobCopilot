package edu.asu.ser594.resumeassistant.trigger.listener.ai;

import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.domain.job.event.JobProcessResultEvent;
import edu.asu.ser594.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiJobResultListener {

    private final JobFacade jobFacade;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_JOB_PROCESS_RES)
    public void onJobProcessResult(JobProcessResultEvent event) {
        log.info("Received JobProcessResultEvent for job: {}, success: {}", event.jobId(), event.success());
        try {
            jobFacade.handleJobProcessResult(event);
        } catch (Exception e) {
            log.error("Error processing JobProcessResultEvent for job: {}", event.jobId(), e);
        }
    }
}

