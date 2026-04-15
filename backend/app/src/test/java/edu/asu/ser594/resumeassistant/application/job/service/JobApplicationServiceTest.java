package edu.asu.ser594.resumeassistant.application.job.service;

import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.job.service.LlmParserPort;
import edu.asu.ser594.resumeassistant.domain.job.service.VisionVerificationPort;
import edu.asu.ser594.resumeassistant.domain.job.service.WebScraperPort;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ScrapeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JobApplicationServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private WebScraperPort webScraperPort;
    @Mock
    private LlmParserPort llmParserPort;
    @Mock
    private VisionVerificationPort visionVerificationPort;

    @InjectMocks
    private JobApplicationService jobApplicationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void submitJob_Success_WithoutImageCheck() {
        String userId = "user123";
        String url = "http://example.com/job";
        SubmitJobRequest request = new SubmitJobRequest(url, false);

        Job job = Job.create(userId, url, false);
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        ScrapeResult scrapeResult = new ScrapeResult("markdown content", null);
        when(webScraperPort.scrape(anyString(), anyBoolean())).thenReturn(scrapeResult);

        ParsedJobContent parsedContent = new ParsedJobContent("Software Engineer", "Tech Corp", "Description", List.of("Java"));
        when(llmParserPort.parse(anyString())).thenReturn(parsedContent);

        JobResponse response = jobApplicationService.submitJob(userId, request);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals("COMPLETED", response.status());
        assertNotNull(response.parsedContent());
        assertEquals("Software Engineer", response.parsedContent().title());
        assertFalse(response.imageCheckEnabled());

        verify(jobRepository, times(4)).save(any(Job.class));
        verify(webScraperPort, times(1)).scrape(url, false);
        verify(llmParserPort, times(1)).parse("markdown content");
        verify(visionVerificationPort, never()).verifyAndFix(any(), any());
    }

    @Test
    void submitJob_Success_WithImageCheck() {
        String userId = "user123";
        String url = "http://example.com/job";
        SubmitJobRequest request = new SubmitJobRequest(url, true);

        Job job = Job.create(userId, url, true);
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        ScrapeResult scrapeResult = new ScrapeResult("markdown content", "http://example.com/screenshot.png");
        when(webScraperPort.scrape(anyString(), anyBoolean())).thenReturn(scrapeResult);

        ParsedJobContent initialParsedContent = new ParsedJobContent("Software Eng", "Tech", "Desc", List.of("Java"));
        when(llmParserPort.parse(anyString())).thenReturn(initialParsedContent);

        ParsedJobContent verifiedContent = new ParsedJobContent("Software Engineer", "Tech Corp", "Description", List.of("Java", "Spring"));
        when(visionVerificationPort.verifyAndFix(any(), anyString())).thenReturn(verifiedContent);

        JobResponse response = jobApplicationService.submitJob(userId, request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.status());
        assertEquals("Software Engineer", response.parsedContent().title());
        assertTrue(response.imageCheckEnabled());

        verify(visionVerificationPort, times(1)).verifyAndFix(initialParsedContent, "http://example.com/screenshot.png");
    }

    @Test
    void getJob_Success() {
        String jobId = "job123";
        String userId = "user123";
        Job job = Job.create(userId, "http://example.com", false);
        
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        JobResponse response = jobApplicationService.getJob(jobId, userId);

        assertNotNull(response);
        assertEquals(userId, response.userId());
    }

    @Test
    void getJob_NotFound_ThrowsException() {
        when(jobRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            jobApplicationService.getJob("invalid-id", "user123");
        });
    }

    @Test
    void getJob_WrongUser_ThrowsException() {
        Job job = Job.create("otherUser", "http://example.com", false);
        when(jobRepository.findById("job123")).thenReturn(Optional.of(job));

        assertThrows(IllegalArgumentException.class, () -> {
            jobApplicationService.getJob("job123", "user123");
        });
    }
}
