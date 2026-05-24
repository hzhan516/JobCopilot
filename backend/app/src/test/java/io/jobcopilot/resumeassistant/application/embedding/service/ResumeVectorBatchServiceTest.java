package io.jobcopilot.resumeassistant.application.embedding.service;

import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchResumeVectorUpsertRequest;
import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchResumeVectorUpsertRequest.ResumeVectorItem;
import io.jobcopilot.resumeassistant.api.embedding.dto.response.BatchResumeVectorUpsertResponse;
import io.jobcopilot.resumeassistant.domain.embedding.entity.ResumeVector;
import io.jobcopilot.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * ResumeVectorBatchService 单元测试
 * 简历向量批量服务单元测试
 * <p>
 * 测试批量 Upsert 完整路径：
 * Tests the batch upsert full path:
 * - 正常批量插入
 * - Normal batch insertion
 * - 嵌入相同跳过
 * - Embedding identical skip
 * - 空列表处理
 * - Empty list handling
 * - 部分失败处理
 * - Partial failure handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resume Vector Batch Service Tests")
class ResumeVectorBatchServiceTest {

    @Mock
    private ResumeVectorRepository resumeVectorRepository;

    @InjectMocks
    private ResumeVectorBatchService service;

    // ==================== 正常路径 ====================

    @Test
    @DisplayName("Should return empty response for null items")
    void shouldReturnEmptyResponseForNullItems() {
        // 当 / When
        BatchResumeVectorUpsertResponse response = service.batchUpsert(null);

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
        BatchResumeVectorUpsertResponse response = service.batchUpsert(Collections.emptyList());

        // 那么 / Then
        assertThat(response.total()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should insert new resume vectors")
    void shouldInsertNewResumeVectors() {
        // 给定 / Given
        ResumeVectorItem item = new ResumeVectorItem("resume-v1", Arrays.asList(0.1f, 0.2f));
        when(resumeVectorRepository.findByResumeVersionId("resume-v1")).thenReturn(Optional.empty());

        // 当 / When
        BatchResumeVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.success()).isEqualTo(1);
        assertThat(response.skipped()).isEqualTo(0);
        assertThat(response.failed()).isEqualTo(0);
        verify(resumeVectorRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should skip identical embeddings")
    void shouldSkipIdenticalEmbeddings() {
        // 给定 / Given
        float[] embedding = {0.1f, 0.2f};
        ResumeVector existing = ResumeVector.createCompleted("vid-1", "resume-v1", embedding);
        ResumeVectorItem item = new ResumeVectorItem("resume-v1", Arrays.asList(0.1f, 0.2f));
        when(resumeVectorRepository.findByResumeVersionId("resume-v1")).thenReturn(Optional.of(existing));

        // 当 / When
        BatchResumeVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.success()).isEqualTo(0);
        assertThat(response.skipped()).isEqualTo(1);
        verify(resumeVectorRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should update when embedding differs")
    void shouldUpdateWhenEmbeddingDiffers() {
        // 给定 / Given
        float[] existingEmbedding = {0.1f, 0.2f};
        ResumeVector existing = ResumeVector.createCompleted("vid-1", "resume-v1", existingEmbedding);
        ResumeVectorItem item = new ResumeVectorItem("resume-v1", Arrays.asList(0.3f, 0.4f));
        when(resumeVectorRepository.findByResumeVersionId("resume-v1")).thenReturn(Optional.of(existing));

        // 当 / When
        BatchResumeVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.success()).isEqualTo(1);
        assertThat(response.skipped()).isEqualTo(0);
        verify(resumeVectorRepository).saveAll(anyList());
    }

    // ==================== 异常路径 ====================

    @Test
    @DisplayName("Should skip item with blank resumeVersionId")
    void shouldSkipItemWithBlankResumeVersionId() {
        // 给定 / Given
        ResumeVectorItem item = new ResumeVectorItem("", Arrays.asList(0.1f));

        // 当 / When
        BatchResumeVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.success()).isEqualTo(0);
        assertThat(response.failed()).isEqualTo(0);
        assertThat(response.skipped()).isEqualTo(0);
        verify(resumeVectorRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle saveAll failure and mark all as failed")
    void shouldHandleSaveAllFailure() {
        // 给定 / Given
        ResumeVectorItem item = new ResumeVectorItem("resume-v1", Arrays.asList(0.1f, 0.2f));
        when(resumeVectorRepository.findByResumeVersionId("resume-v1")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("DB error")).when(resumeVectorRepository).saveAll(anyList());

        // 当 / When
        BatchResumeVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.success()).isEqualTo(0);
        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.failedResumeVersionIds()).containsExactly("resume-v1");
    }

    @Test
    @DisplayName("Should handle null embedding list")
    void shouldHandleNullEmbeddingList() {
        // 给定 / Given
        ResumeVectorItem item = new ResumeVectorItem("resume-v1", null);
        when(resumeVectorRepository.findByResumeVersionId("resume-v1")).thenReturn(Optional.empty());

        // 当 / When
        BatchResumeVectorUpsertResponse response = service.batchUpsert(List.of(item));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.failedResumeVersionIds()).containsExactly("resume-v1");
    }

    @Test
    @DisplayName("Should handle partial success in batch")
    void shouldHandlePartialSuccessInBatch() {
        // 给定 / Given
        ResumeVectorItem goodItem = new ResumeVectorItem("resume-v1", Arrays.asList(0.1f, 0.2f));
        ResumeVectorItem badItem = new ResumeVectorItem("resume-v2", null);
        when(resumeVectorRepository.findByResumeVersionId("resume-v1")).thenReturn(Optional.empty());
        when(resumeVectorRepository.findByResumeVersionId("resume-v2")).thenReturn(Optional.empty());

        // 当 / When
        BatchResumeVectorUpsertResponse response = service.batchUpsert(Arrays.asList(goodItem, badItem));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(2);
        assertThat(response.success()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.failedResumeVersionIds()).containsExactly("resume-v2");
    }

    @Test
    @DisplayName("Should handle mixed batch with skip and success")
    void shouldHandleMixedBatchWithSkipAndSuccess() {
        // 给定 / Given
        float[] embedding = {0.1f, 0.2f};
        ResumeVector existing = ResumeVector.createCompleted("vid-1", "resume-v1", embedding);
        ResumeVectorItem skipItem = new ResumeVectorItem("resume-v1", Arrays.asList(0.1f, 0.2f));
        ResumeVectorItem newItem = new ResumeVectorItem("resume-v2", Arrays.asList(0.3f, 0.4f));

        when(resumeVectorRepository.findByResumeVersionId("resume-v1")).thenReturn(Optional.of(existing));
        when(resumeVectorRepository.findByResumeVersionId("resume-v2")).thenReturn(Optional.empty());

        // 当 / When
        BatchResumeVectorUpsertResponse response = service.batchUpsert(Arrays.asList(skipItem, newItem));

        // 那么 / Then
        assertThat(response.total()).isEqualTo(2);
        assertThat(response.success()).isEqualTo(1);
        assertThat(response.skipped()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(0);
    }
}
