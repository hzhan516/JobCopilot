package edu.asu.ser594.resumeassistant.application.job.service;

import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.VectorGenCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

class JobApplicationServiceTest {

    @Mock
    private JobRepository jobRepository;
    
    @Mock
    private AiMessagePublisherPort aiMessagePublisherPort;

    @InjectMocks
    private JobApplicationService jobApplicationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void submitJob_Success() {
        UUID userId = UUID.randomUUID();
        String url = "http://example.com/job";
        SubmitJobRequest request = new SubmitJobRequest(url, false);

        Job job = Job.create(userId, url, false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobApplicationService.submitJob(userId, request);

        assertNotNull(response);
        assertEquals(userId.toString(), response.userId());
        assertEquals("SCRAPING", response.status());

        verify(jobRepository, times(1)).save(any(Job.class));
        verify(aiMessagePublisherPort, times(1)).sendJobForParsing(any(JobParseCommand.class));
    }

    @Test
    void handleJobProcessResult_Success_TriggersVectorGen() {
        String jobId = "job123";
        Job job = Job.create(UUID.randomUUID(), "http://example.com/job", false);
        job.markScraping();
        job.markParsing();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        
        Map<String, Object> mockParsedData = Map.of(
            "title", "Software Engineer",
            "company", "Tech Corp",
            "description", "A great job",
            "requirements", List.of("Java", "Spring")
        );

        AiResultEvent event = new AiResultEvent(jobId, "JOB_PARSE", "COMPLETED", mockParsedData, null, "JOB");

        jobApplicationService.handleJobProcessResult(event);

        assertEquals("COMPLETED", job.getStatus().name());
        assertNotNull(job.getParsedContent());
        assertEquals("Software Engineer", job.getParsedContent().title());

        verify(jobRepository, times(1)).save(job);
        verify(aiMessagePublisherPort, times(1)).sendTextForVectorGeneration(any(VectorGenCommand.class));
    }

    @Test
    void getJob_Success() {
        String jobId = "job123";
        UUID userId = UUID.randomUUID();
        Job job = Job.create(userId, "http://example.com", false);
        
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        JobResponse response = jobApplicationService.getJob(jobId, userId);

        assertNotNull(response);
        assertEquals(userId.toString(), response.userId());
    }

    @Test
    void getJob_NotFound_ThrowsException() {
        when(jobRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            jobApplicationService.getJob("invalid-id", UUID.randomUUID());
        });
    }

    @Test
    void getJob_WrongUser_ThrowsException() {
        Job job = Job.create(UUID.randomUUID(), "http://example.com", false);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(job));

        assertThrows(IllegalArgumentException.class, () -> {
            jobApplicationService.getJob("job123", UUID.randomUUID());
        });
    }
}
