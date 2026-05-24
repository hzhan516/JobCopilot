package io.jobcopilot.resumeassistant.application.embedding.service;

import io.jobcopilot.resumeassistant.domain.embedding.entity.JobVector;
import io.jobcopilot.resumeassistant.domain.embedding.entity.ResumeVector;
import io.jobcopilot.resumeassistant.domain.embedding.repository.JobVectorRepository;
import io.jobcopilot.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FailedVectorPersistenceService 单元测试
 * 失败向量持久化服务单元测试
 * <p>
 * 测试独立事务中保存失败记录：
 * Tests saving failure records in an independent transaction:
 * - RESUME 失败记录保存
 * - RESUME failure record persistence
 * - JOB 失败记录保存
 * - JOB failure record persistence
 * - REQUIRES_NEW 事务传播
 * - REQUIRES_NEW transaction propagation (verified by annotation)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Failed Vector Persistence Service Tests")
class FailedVectorPersistenceServiceTest {

    @Mock
    private ResumeVectorRepository resumeVectorRepository;

    @Mock
    private JobVectorRepository jobVectorRepository;

    @InjectMocks
    private FailedVectorPersistenceService service;

    // ==================== RESUME 分支 ====================

    @Test
    @DisplayName("Should save failed resume vector")
    void shouldSaveFailedResumeVector() {
        // 当 / When
        service.saveFailedVector("resume-v1", "RESUME", "AI service timeout");

        // 那么 / Then
        ArgumentCaptor<ResumeVector> captor = ArgumentCaptor.forClass(ResumeVector.class);
        verify(resumeVectorRepository).save(captor.capture());
        ResumeVector saved = captor.getValue();
        assertThat(saved.getResumeVersionId()).isEqualTo("resume-v1");
        assertThat(saved.getStatus()).isEqualTo(ResumeVector.Status.FAILED);
        assertThat(saved.getErrorMessage()).isEqualTo("AI service timeout");
    }

    @Test
    @DisplayName("Should save failed resume vector with RESUME_VECTOR type")
    void shouldSaveFailedResumeVectorWithResumeVectorType() {
        // 当 / When
        service.saveFailedVector("resume-v2", "RESUME_VECTOR", "Dimension mismatch");

        // 那么 / Then
        verify(resumeVectorRepository).save(any(ResumeVector.class));
        verify(jobVectorRepository, never()).save(any());
    }

    // ==================== JOB 分支 ====================

    @Test
    @DisplayName("Should save failed job vector")
    void shouldSaveFailedJobVector() {
        // 当 / When
        service.saveFailedVector("job-1", "JOB", "AI service timeout");

        // 那么 / Then
        ArgumentCaptor<JobVector> captor = ArgumentCaptor.forClass(JobVector.class);
        verify(jobVectorRepository).save(captor.capture());
        JobVector saved = captor.getValue();
        assertThat(saved.getJobId()).isEqualTo("job-1");
        assertThat(saved.getStatus()).isEqualTo(JobVector.Status.FAILED);
        assertThat(saved.getErrorMessage()).isEqualTo("AI service timeout");
    }

    @Test
    @DisplayName("Should save failed job vector with lowercase type")
    void shouldSaveFailedJobVectorWithLowercaseType() {
        // 当 / When
        service.saveFailedVector("job-2", "job", "Network error");

        // 那么 / Then
        verify(jobVectorRepository).save(any(JobVector.class));
        verify(resumeVectorRepository, never()).save(any());
    }

    // ==================== 边界条件 ====================

    @Test
    @DisplayName("Should handle null error message")
    void shouldHandleNullErrorMessage() {
        // 当 / When
        service.saveFailedVector("id", "RESUME", null);

        // 那么 / Then
        ArgumentCaptor<ResumeVector> captor = ArgumentCaptor.forClass(ResumeVector.class);
        verify(resumeVectorRepository).save(captor.capture());
        assertThat(captor.getValue().getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should handle empty error message")
    void shouldHandleEmptyErrorMessage() {
        // 当 / When
        service.saveFailedVector("id", "JOB", "");

        // 那么 / Then
        ArgumentCaptor<JobVector> captor = ArgumentCaptor.forClass(JobVector.class);
        verify(jobVectorRepository).save(captor.capture());
        assertThat(captor.getValue().getErrorMessage()).isEmpty();
    }
}
