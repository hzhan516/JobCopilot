package edu.asu.ser594.resumeassistant.trigger.listener.ai;

import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.JobVector;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.ResumeVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.JobVectorRepository;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiResultMessageListenerTest {

    @Mock
    private JobFacade jobFacade;

    @Mock
    private ResumeFacade resumeFacade;

    @Mock
    private ResumeVectorRepository resumeVectorRepository;

    @Mock
    private JobVectorRepository jobVectorRepository;

    @InjectMocks
    private AiResultMessageListener listener;

    @Test
    void onJobParseResult_ShouldCallJobFacade() {
        AiResultEvent event = new AiResultEvent("job-1", "JOB_PARSE", "COMPLETED", null, null, null);
        listener.onJobParseResult(event);
        verify(jobFacade).handleJobProcessResult(event);
    }

    @Test
    void onResumeParseResult_ShouldCallResumeFacade() {
        AiResultEvent event = new AiResultEvent("resume-1", "RESUME_PARSE", "COMPLETED", null, null, null);
        listener.onResumeParseResult(event);
        verify(resumeFacade).handleParseResult(event);
    }

    @Test
    void onVectorGenResult_ShouldCallEmbeddingRepositoryForJob() {
        AiResultEvent event = new AiResultEvent(
                "job-123",
                "VECTOR_GEN",
                "COMPLETED",
                Map.of("embedding", List.of(0.1, 0.2)),
                null,
                "JOB"
        );

        listener.onVectorGenResult(event);

        verify(jobVectorRepository).save(any(JobVector.class));
    }

    @Test
    void onVectorGenResult_ShouldCallEmbeddingRepositoryForResume() {
        AiResultEvent event = new AiResultEvent(
                "resume-456",
                "VECTOR_GEN",
                "COMPLETED",
                Map.of("embedding", List.of(0.3, 0.4), "entityType", "RESUME"),
                null,
                "RESUME"
        );

        listener.onVectorGenResult(event);

        verify(resumeVectorRepository).save(any(ResumeVector.class));
    }
}
