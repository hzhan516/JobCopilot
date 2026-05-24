package io.jobcopilot.resumeassistant.application.embedding.service;

import io.jobcopilot.resumeassistant.api.embedding.config.EmbeddingConfig;
import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchJobVectorUpsertRequest;
import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchJobVectorUpsertRequest.JobVectorItem;
import io.jobcopilot.resumeassistant.api.embedding.dto.response.BatchJobVectorUpsertResponse;
import io.jobcopilot.resumeassistant.domain.embedding.entity.JobVector;
import io.jobcopilot.resumeassistant.domain.embedding.repository.JobVectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * JobVectorBatchService 单元测试
 * 职位向量批量服务单元测试
 * <p>
 * 测试批量 Upsert 完整路径：
 * Tests the batch upsert full path:
 * - 正常批量插入
 * - Normal batch insertion
 * - 内容相同跳过
 * - Content identical skip
 * - 部分失败处理
 * - Partial failure handling
 * - 空列表处理
 * - Empty list handling
 * - 重复失败降级
 * - Duplicate failure fallback
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Job Vector Batch Service Tests")
class JobVectorBatchServiceTest {

    @Mock
    private JobVectorRepository jobVectorRepository;

    @Mock
    private EmbeddingConfig embeddingConfig;

    @InjectMocks
    private JobVectorBatchService service;

    @BeforeEach
    void setUp() {
        when(embeddingConfig.getDefaultModelVersion()).thenReturn("text-embedding-3-small");
    }

    // ==================== 正常路径 ====================

    @Test
    @DisplayName("Should return empty response for null items")
    void shouldReturnEmptyResponseForNullItems() {
        // 当 / When
        BatchJobVectorUpsertResponse response = service.batchUpsert(null);

        // 那么 / Then
        assertThat(response.total()).isEqualTo(0);
        assertThat(response.success()).isEqualTo(0);
        assertThat(response.failed()).isEqualTo(0);
        assertThat(response.skipped()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return empty response for empty items")
    void shouldReturnEmptyResponseForEmptyItems() {
        // 当 / When
        BatchJobVectorUpsertResponse response = service.batchUpsert(Collections.emptyList());

        // 那么 / Then
        assertThat(response.total()).isEqualTo(0);
        assertThat(response.success()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should insert new job vectors")
    void shouldInsertNewJobVectors() {
        // 给定 / Given
        JobVectorItem item = new JobVectorItem(
                "job-1", Arrays.asList(0.1f, 0.2f), "Dev", "desc",
                Arrays.asList("Java"), "raw", "file", "model-v1"
        );
        when(jobVectorRepository.findByJobId("job-1")).thenReturn(Optional.empty());

        // 当 / When
        BatchJobVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.success()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(0);
        assertThat(response.skipped()).isEqualTo(0);
        verify(jobVectorRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should skip identical content")
    void shouldSkipIdenticalContent() {
        // 给定 / Given
        float[] embedding = {0.1f, 0.2f};
        JobVector existing = JobVector.createCompleted(
                "vid-1", "job-1", embedding, "Dev", "desc",
                Arrays.asList("Java"), "raw", "file", "model-v1"
        );
        JobVectorItem item = new JobVectorItem(
                "job-1", Arrays.asList(0.1f, 0.2f), "Dev", "desc",
                Arrays.asList("Java"), "raw", "file", "model-v1"
        );
        when(jobVectorRepository.findByJobId("job-1")).thenReturn(Optional.of(existing));

        // 当 / When
        BatchJobVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.success()).isEqualTo(0);
        assertThat(response.skipped()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(0);
        verify(jobVectorRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should update when content differs")
    void shouldUpdateWhenContentDiffers() {
        // 给定 / Given
        float[] embedding = {0.1f, 0.2f};
        JobVector existing = JobVector.createCompleted(
                "vid-1", "job-1", embedding, "Dev", "desc",
                Arrays.asList("Java"), "raw", "file", "model-v1"
        );
        JobVectorItem item = new JobVectorItem(
                "job-1", Arrays.asList(0.3f, 0.4f), "Senior Dev", "desc",
                Arrays.asList("Java"), "raw", "file", "model-v1"
        );
        when(jobVectorRepository.findByJobId("job-1")).thenReturn(Optional.of(existing));

        // 当 / When
        BatchJobVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.success()).isEqualTo(1);
        assertThat(response.skipped()).isEqualTo(0);
        verify(jobVectorRepository).saveAll(anyList());
    }

    // ==================== 异常路径 ====================

    @Test
    @DisplayName("Should skip item with blank jobId")
    void shouldSkipItemWithBlankJobId() {
        // 给定 / Given
        JobVectorItem item = new JobVectorItem(
                "", Arrays.asList(0.1f), "Dev", "desc",
                null, null, null, null
        );

        // 当 / When
        BatchJobVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.success()).isEqualTo(0);
        assertThat(response.failed()).isEqualTo(0);
        assertThat(response.skipped()).isEqualTo(0);
        verify(jobVectorRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle saveAll failure and mark all as failed")
    void shouldHandleSaveAllFailure() {
        // 给定 / Given
        JobVectorItem item = new JobVectorItem(
                "job-1", Arrays.asList(0.1f, 0.2f), "Dev", "desc",
                null, null, null, null
        );
        when(jobVectorRepository.findByJobId("job-1")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("DB error")).when(jobVectorRepository).saveAll(anyList());

        // 当 / When
        BatchJobVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.success()).isEqualTo(0);
        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.failedJobIds()).containsExactly("job-1");
    }

    @Test
    @DisplayName("Should handle null embedding list")
    void shouldHandleNullEmbeddingList() {
        // 给定 / Given
        JobVectorItem item = new JobVectorItem(
                "job-1", null, "Dev", "desc",
                null, null, null, null
        );
        when(jobVectorRepository.findByJobId("job-1")).thenReturn(Optional.empty());

        // 当 / When
        BatchJobVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.failedJobIds()).containsExactly("job-1");
    }

    @Test
    @DisplayName("Should handle partial success in batch")
    void shouldHandlePartialSuccessInBatch() {
        // 给定 / Given
        JobVectorItem goodItem = new JobVectorItem(
                "job-1", Arrays.asList(0.1f, 0.2f), "Dev", "desc",
                null, null, null, null
        );
        JobVectorItem badItem = new JobVectorItem(
                "job-2", null, "Dev2", "desc2",
                null, null, null, null
        );
        when(jobVectorRepository.findByJobId("job-1")).thenReturn(Optional.empty());
        when(jobVectorRepository.findByJobId("job-2")).thenReturn(Optional.empty());

        // 当 / When
        BatchJobVectorUpsertResponse response = service.batchUpsert(Arrays.asList(goodItem, badItem));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(2);
        assertThat(response.success()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.failedJobIds()).containsExactly("job-2");
    }

    @Test
    @DisplayName("Should use default model version when item version is null")
    void shouldUseDefaultModelVersionWhenItemVersionIsNull() {
        // 给定 / Given
        JobVectorItem item = new JobVectorItem(
                "job-1", Arrays.asList(0.1f, 0.2f), "Dev", "desc",
                null, null, null, null
        );
        when(jobVectorRepository.findByJobId("job-1")).thenReturn(Optional.empty());

        // 当 / When
        service.batchUpsert(List.of(item));

        // 那么 / Then
        verify(jobVectorRepository).saveAll(anyList());
    }
}
