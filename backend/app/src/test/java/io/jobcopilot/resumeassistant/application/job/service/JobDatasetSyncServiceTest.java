package io.jobcopilot.resumeassistant.application.job.service;

import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.valueobject.ParsedJobContent;
import io.jobcopilot.resumeassistant.domain.matching.entity.JobDataset;
import io.jobcopilot.resumeassistant.domain.matching.repository.JobDatasetRepository;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JobDatasetSyncService 单元测试
 * Job Dataset Sync Service Unit Tests
 *
 * 测试职位同步到 ML 训练数据集的逻辑：
 * Tests job synchronization to ML training dataset logic:
 * - 正常同步 / Normal sync
 * - 空需求处理 / Empty requirements handling
 * - 异常容错 / Exception tolerance
 * - 文本构建 / Vector text building
 * - 内容映射 / Content mapping
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Job Dataset Sync Service Tests")
class JobDatasetSyncServiceTest {

    @Mock
    private JobDatasetRepository jobDatasetRepository;

    @InjectMocks
    private JobDatasetSyncService syncService;

    private Job job;
    private AiResultEvent event;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        job = Job.create(userId, "http://example.com/job", false);
        job.markScraping();
        job.markParsing();

        ParsedJobContent pc = new ParsedJobContent(
                "Software Engineer",
                "Tech Corp",
                "100K-150K",
                "Remote",
                "Build scalable systems",
                List.of("Java", "Spring", "AWS")
        );
        job.updateParsedContent(pc);

        Map<String, Object> data = Map.of(
                "title", "Software Engineer",
                "company", "Tech Corp",
                "salary", "100K-150K",
                "location", "Remote",
                "description", "Build scalable systems",
                "requirements", List.of("Java", "Spring", "AWS")
        );
        event = new AiResultEvent("job123", "JOB_PARSE", "COMPLETED", data, null, "JOB");
    }

    // ==================== 正常同步 ====================

    @Test
    @DisplayName("Should sync job to dataset successfully")
    void shouldSyncJobToDatasetSuccessfully() {
        // 给定 / Given
        doNothing().when(jobDatasetRepository).save(any(JobDataset.class));

        // 当 / When
        assertThatNoException().isThrownBy(() -> syncService.sync(job, event));

        // 那么 / Then
        ArgumentCaptor<JobDataset> captor = ArgumentCaptor.forClass(JobDataset.class);
        verify(jobDatasetRepository).save(captor.capture());

        JobDataset saved = captor.getValue();
        assertThat(saved.getExternalId()).isEqualTo(job.getId());
        assertThat(saved.getTitle()).isEqualTo("Software Engineer");
        assertThat(saved.getCompany()).isEqualTo("Tech Corp");
        assertThat(saved.getSource()).isEqualTo("USER_SUBMITTED");
    }

    @Test
    @DisplayName("Should set requirements array correctly")
    void shouldSetRequirementsArrayCorrectly() {
        // 给定 / Given
        doNothing().when(jobDatasetRepository).save(any(JobDataset.class));

        // 当 / When
        syncService.sync(job, event);

        // 那么 / Then
        ArgumentCaptor<JobDataset> captor = ArgumentCaptor.forClass(JobDataset.class);
        verify(jobDatasetRepository).save(captor.capture());

        JobDataset saved = captor.getValue();
        assertThat(saved.getRequirements()).containsExactly("Java", "Spring", "AWS");
    }

    // ==================== 边界与异常 ====================

    @Test
    @DisplayName("Should handle null requirements gracefully")
    void shouldHandleNullRequirementsGracefully() {
        // 给定 / Given
        ParsedJobContent pc = new ParsedJobContent(
                "Title", "Company", null, null, "Desc", null
        );
        job.updateParsedContent(pc);

        Map<String, Object> data = Map.of(
                "title", "Title",
                "company", "Company",
                "description", "Desc"
        );
        AiResultEvent nullReqEvent = new AiResultEvent("job456", "JOB_PARSE", "COMPLETED", data, null, "JOB");

        doNothing().when(jobDatasetRepository).save(any(JobDataset.class));

        // 当 / When
        assertThatNoException().isThrownBy(() -> syncService.sync(job, nullReqEvent));

        // 那么 / Then
        ArgumentCaptor<JobDataset> captor = ArgumentCaptor.forClass(JobDataset.class);
        verify(jobDatasetRepository).save(captor.capture());
        assertThat(captor.getValue().getRequirements()).isEmpty();
    }

    @Test
    @DisplayName("Should not throw on repository exception")
    void shouldNotThrowOnRepositoryException() {
        // 给定 / Given
        doThrow(new RuntimeException("DB error"))
                .when(jobDatasetRepository).save(any(JobDataset.class));

        // 当 & 那么 / When & Then
        assertThatNoException().isThrownBy(() -> syncService.sync(job, event));
    }

    // ==================== 内容映射 ====================

    @Test
    @DisplayName("Should map event data to ParsedJobContent")
    void shouldMapEventDataToParsedJobContent() {
        // 给定 / Given
        Map<String, Object> data = Map.of(
                "title", "Senior Dev",
                "company", "BigTech",
                "salary", "200K",
                "location", "SF",
                "description", "Lead projects",
                "requirements", List.of("10+ years", "K8s")
        );

        // 当 / When
        ParsedJobContent result = syncService.mapToParsedContent(data);

        // 那么 / Then
        assertThat(result.title()).isEqualTo("Senior Dev");
        assertThat(result.company()).isEqualTo("BigTech");
        assertThat(result.requirements()).containsExactly("10+ years", "K8s");
    }

    // ==================== 向量文本构建 ====================

    @Test
    @DisplayName("Should build vector text from parsed content")
    void shouldBuildVectorTextFromParsedContent() {
        // 给定 / Given
        ParsedJobContent pc = new ParsedJobContent(
                "Java Developer",
                "Acme Corp",
                "80K-120K",
                "NYC",
                "Build APIs",
                List.of("Java", "Spring Boot")
        );

        // 当 / When
        String vectorText = syncService.buildVectorText(pc);

        // 那么 / Then
        assertThat(vectorText).contains("Java Developer");
        assertThat(vectorText).contains("Acme Corp");
        assertThat(vectorText).contains("Build APIs");
        assertThat(vectorText).contains("Java");
        assertThat(vectorText).contains("Spring Boot");
    }

    @Test
    @DisplayName("Should handle empty requirements in vector text")
    void shouldHandleEmptyRequirementsInVectorText() {
        // 给定 / Given
        ParsedJobContent pc = new ParsedJobContent(
                "Title", "Company", null, null, "Desc", List.of()
        );

        // 当 / When
        String vectorText = syncService.buildVectorText(pc);

        // 那么 / Then
        assertThat(vectorText).contains("Title");
        assertThat(vectorText).contains("Company");
        assertThat(vectorText).contains("Desc");
    }
}
