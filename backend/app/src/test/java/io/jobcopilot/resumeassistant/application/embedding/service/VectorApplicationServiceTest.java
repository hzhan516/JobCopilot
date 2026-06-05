package io.jobcopilot.resumeassistant.application.embedding.service;

import io.jobcopilot.resumeassistant.api.embedding.config.EmbeddingConfig;
import io.jobcopilot.resumeassistant.domain.embedding.entity.JobVector;
import io.jobcopilot.resumeassistant.domain.embedding.entity.ResumeVector;
import io.jobcopilot.resumeassistant.domain.embedding.port.VectorEmbeddingPort;
import io.jobcopilot.resumeassistant.domain.embedding.repository.JobVectorRepository;
import io.jobcopilot.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import io.jobcopilot.resumeassistant.domain.embedding.valueobject.VectorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VectorApplicationService 单元测试
 * 向量应用服务单元测试
 * <p>
 * 测试向量生成与持久化的完整路径：
 * Tests the full vector generation and persistence path:
 * - 正常生成与保存
 * - Normal generation and save
 * - 维度不匹配降级
 * - Dimension mismatch fallback
 * - 异常降级到失败记录
 * - Exception fallback to failure record
 * - RESUME/JOB 实体分支
 * - RESUME/JOB entity branching
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Vector Application Service Tests")
class VectorApplicationServiceTest {

    @Mock
    private ResumeVectorRepository resumeVectorRepository;

    @Mock
    private JobVectorRepository jobVectorRepository;

    @Mock
    private EmbeddingConfig embeddingConfig;

    @Mock
    private VectorEmbeddingPort vectorEmbeddingPort;

    @Mock
    private FailedVectorPersistenceService failedVectorPersistenceService;

    @InjectMocks
    private VectorApplicationService service;

    private static final int EMBEDDING_DIMENSION = 1536;

    @BeforeEach
    void setUp() {
        when(embeddingConfig.getDimension()).thenReturn(EMBEDDING_DIMENSION);
        when(embeddingConfig.getDefaultModelVersion()).thenReturn("text-embedding-3-small");
    }

    // ==================== 正常路径 ====================
    // ==================== Happy Path ====================

    @Test
    @DisplayName("Should generate and save resume vector")
    void shouldGenerateAndSaveResumeVector() {
        // 给定 / Given
        String referenceId = "resume-version-123";
        String text = "Software Engineer with 5 years experience";
        float[] embedding = new float[EMBEDDING_DIMENSION];
        when(vectorEmbeddingPort.generate(text)).thenReturn(embedding);

        // 当 / When
        service.generateAndSaveVector(referenceId, "RESUME", text);

        // 那么 / Then
        ArgumentCaptor<ResumeVector> captor = ArgumentCaptor.forClass(ResumeVector.class);
        verify(resumeVectorRepository).save(captor.capture());
        assertThat(captor.getValue().getResumeVersionId()).isEqualTo(referenceId);
        assertThat(captor.getValue().getEmbedding()).isEqualTo(embedding);
        assertThat(captor.getValue().getStatus()).isEqualTo(VectorStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should generate and save job vector")
    void shouldGenerateAndSaveJobVector() {
        // 给定 / Given
        String referenceId = "job-456";
        String text = "Senior Java Developer position";
        float[] embedding = new float[EMBEDDING_DIMENSION];
        when(vectorEmbeddingPort.generate(text)).thenReturn(embedding);

        // 当 / When
        service.generateAndSaveVector(referenceId, "JOB", text);

        // 那么 / Then
        ArgumentCaptor<JobVector> captor = ArgumentCaptor.forClass(JobVector.class);
        verify(jobVectorRepository).save(captor.capture());
        assertThat(captor.getValue().getJobId()).isEqualTo(referenceId);
        assertThat(captor.getValue().getStatus()).isEqualTo(VectorStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should handle RESUME_VECTOR entity type as resume")
    void shouldHandleResumeVectorEntityTypeAsResume() {
        // 给定 / Given
        float[] embedding = new float[EMBEDDING_DIMENSION];
        when(vectorEmbeddingPort.generate(anyString())).thenReturn(embedding);

        // 当 / When
        service.generateAndSaveVector("id", "RESUME_VECTOR", "text");

        // 那么 / Then — should save to resume repository
        verify(resumeVectorRepository).save(any(ResumeVector.class));
        verify(jobVectorRepository, never()).save(any(JobVector.class));
    }

    // ==================== 异常路径 ====================
    // ==================== Exception Path ====================

    @Test
    @DisplayName("Should save failed vector on dimension mismatch")
    void shouldSaveFailedVectorOnDimensionMismatch() {
        // 给定 / Given
        float[] shortEmbedding = new float[768]; // wrong dimension
        when(vectorEmbeddingPort.generate(anyString())).thenReturn(shortEmbedding);

        // 当 / When
        service.generateAndSaveVector("id", "RESUME", "text");

        // 那么 / Then — should NOT save to normal repository, should call failed persistence
        verify(resumeVectorRepository, never()).save(any());
        verify(failedVectorPersistenceService).saveFailedVector(
                eq("id"), eq("RESUME"), contains("dimension mismatch"));
    }

    @Test
    @DisplayName("Should save failed vector on embedding generation exception")
    void shouldSaveFailedVectorOnEmbeddingGenerationException() {
        // 给定 / Given
        RuntimeException exception = new RuntimeException("AI service timeout");
        when(vectorEmbeddingPort.generate(anyString())).thenThrow(exception);

        // 当 / When
        service.generateAndSaveVector("id", "JOB", "text");

        // 那么 / Then
        verify(jobVectorRepository, never()).save(any());
        verify(failedVectorPersistenceService).saveFailedVector(
                eq("id"), eq("JOB"), eq("AI service timeout"));
    }

    @Test
    @DisplayName("Should handle null exception message")
    void shouldHandleNullExceptionMessage() {
        // 给定 / Given
        when(vectorEmbeddingPort.generate(anyString()))
                .thenThrow(new RuntimeException((String) null));

        // 当 / When
        service.generateAndSaveVector("id", "RESUME", "text");

        // 那么 / Then
        verify(failedVectorPersistenceService).saveFailedVector(
                eq("id"), eq("RESUME"), isNull());
    }
}
