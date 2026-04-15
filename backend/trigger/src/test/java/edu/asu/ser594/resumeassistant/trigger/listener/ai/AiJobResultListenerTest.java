package edu.asu.ser594.resumeassistant.trigger.listener.ai;

import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.domain.job.event.JobProcessResultEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiJobResultListenerTest {

    @Mock
    private JobFacade jobFacade;

    @InjectMocks
    private AiJobResultListener listener;

    @Test
    void onJobProcessResult_ShouldCallFacade() {
        JobProcessResultEvent event = new JobProcessResultEvent("job-1", true, null, null);

        listener.onJobProcessResult(event);

        verify(jobFacade).handleJobProcessResult(event);
    }
}
