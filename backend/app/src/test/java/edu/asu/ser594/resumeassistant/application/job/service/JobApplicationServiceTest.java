package edu.asu.ser594.resumeassistant.application.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.ser594.resumeassistant.api.embedding.facade.VectorFacade;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.JobVectorRepository;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.exception.JobException;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobScoreRepository;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import edu.asu.ser594.resumeassistant.domain.matching.repository.JobDatasetRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 职位应用服务单元测试 / Job application service unit tests
 */
class JobApplicationServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobScoreRepository jobScoreRepository;

    @Mock
    private ResumeVersionRepository resumeVersionRepository;

    @Mock
    private ResumeGroupRepository resumeGroupRepository;

    @Mock
    private JobDatasetRepository jobDatasetRepository;

    @Mock
    private ResumeVectorRepository resumeVectorRepository;

    @Mock
    private JobVectorRepository jobVectorRepository;

    @Mock
    private AiMessagePublisherPort aiMessagePublisherPort;

    @Mock
    private VectorFacade vectorFacade;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JobApplicationService jobApplicationService;

    // 准备 / Given
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void submitJob_Success() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        String url = "http://example.com/job";
        SubmitJobRequest request = new SubmitJobRequest(url, false, null);

        Job job = Job.create(userId, url, false);
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 执行 / When
        JobResponse response = jobApplicationService.submitJob(userId, request);

        // 验证 / Then
        assertNotNull(response);
        assertEquals(userId.toString(), response.userId());
        assertEquals("PARSING", response.status());

        verify(jobRepository, times(2)).save(any(Job.class));
        verify(aiMessagePublisherPort, times(1)).sendJobForParsing(any(JobParseCommand.class));
    }

    @Test
    void handleJobProcessResult_Success_TriggersVectorGen() {
        // 准备 / Given
        String jobId = "job123";
        Job job = Job.create(UUID.randomUUID(), "http://example.com/job", false);
        job.markScraping();
        job.markParsing();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        Map<String, Object> mockParsedData = Map.of(
                "title", "Software Engineer",
                "company", "Tech Corp",
                "salary", "100K-150K",
                "location", "Remote",
                "description", "A great job",
                "requirements", List.of("Java", "Spring")
        );

        AiResultEvent event = new AiResultEvent(jobId, "JOB_PARSE", "COMPLETED", mockParsedData, null, "JOB");
        when(objectMapper.convertValue(eq(mockParsedData), eq(ParsedJobContent.class)))
                .thenReturn(new ParsedJobContent("Software Engineer", "Tech Corp", "100K-150K", "Remote", "A great job", List.of("Java", "Spring")));

        // 执行 / When
        jobApplicationService.handleJobProcessResult(event);

        // 验证 / Then
        assertEquals("COMPLETED", job.getStatus().name());
        assertNotNull(job.getParsedContent());
        assertEquals("Software Engineer", job.getParsedContent().title());

        verify(jobRepository, times(1)).save(job);
        verify(vectorFacade, times(1)).generateAndSaveVector(anyString(), eq("JOB"), anyString());
    }

    @Test
    void getJob_Success() {
        // 准备 / Given
        String jobId = "job123";
        UUID userId = UUID.randomUUID();
        Job job = Job.create(userId, "http://example.com", false);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        // 执行 / When
        JobResponse response = jobApplicationService.getJob(jobId, userId);

        // 验证 / Then
        assertNotNull(response);
        assertEquals(userId.toString(), response.userId());
    }

    @Test
    void getJob_NotFound_ThrowsException() {
        // 准备 / Given
        when(jobRepository.findById(anyString())).thenReturn(Optional.empty());

        // 执行与验证 / When & Then
        JobException exception = assertThrows(JobException.class, () -> {
            jobApplicationService.getJob("invalid-id", UUID.randomUUID());
        });
        assertEquals("job.not.found", exception.getMessageKey());
    }

    @Test
    void getJob_WrongUser_ThrowsException() {
        // 准备 / Given
        Job job = Job.create(UUID.randomUUID(), "http://example.com", false);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(job));

        // 执行与验证 / When & Then
        JobException exception = assertThrows(JobException.class, () -> {
            jobApplicationService.getJob("job123", UUID.randomUUID());
        });
        assertEquals("access.denied", exception.getMessageKey());
    }

    @Test
    void deleteJob_Success_HidesJob() {
        // 准备 / Given
        String jobId = "job123";
        UUID userId = UUID.randomUUID();
        Job job = Job.create(userId, "http://example.com", false);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 执行 / When
        jobApplicationService.deleteJob(jobId, userId);

        // 验证 / Then
        assertTrue(job.isHidden());
        assertNotNull(job.getHiddenAt());
        verify(jobRepository).save(job);
    }

    @Test
    void deleteJob_WrongUser_ThrowsException() {
        // 准备 / Given
        String jobId = "job123";
        Job job = Job.create(UUID.randomUUID(), "http://example.com", false);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        // 执行与验证 / When & Then
        JobException exception = assertThrows(JobException.class, () -> {
            jobApplicationService.deleteJob(jobId, UUID.randomUUID());
        });
        assertEquals("access.denied", exception.getMessageKey());
        verify(jobRepository, never()).save(any(Job.class));
    }
}
