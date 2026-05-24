package io.jobcopilot.resumeassistant.trigger.http.controller.embedding;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchJobVectorUpsertRequest;
import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchJobVectorUpsertRequest.JobVectorItem;
import io.jobcopilot.resumeassistant.api.embedding.dto.response.BatchJobVectorUpsertResponse;
import io.jobcopilot.resumeassistant.api.embedding.facade.JobVectorBatchFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * JobVectorController 单元测试
 * 职位向量控制器单元测试
 * <p>
 * 测试批量 Upsert API 入口：
 * Tests the batch upsert API entrypoint:
 * - 正常批量写入
 * - Normal batch write
 * - 空列表处理
 * - Empty list handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Job Vector Controller Tests")
class JobVectorControllerTest {

    @Mock
    private JobVectorBatchFacade jobVectorBatchFacade;

    @InjectMocks
    private JobVectorController controller;

    @Test
    @DisplayName("Should return success response for batch upsert")
    void shouldReturnSuccessResponseForBatchUpsert() {
        // 给定 / Given
        JobVectorItem item = new JobVectorItem(
                "job-1", Arrays.asList(0.1f, 0.2f), "Dev", "desc",
                null, null, null, null
        );
        BatchJobVectorUpsertRequest request = new BatchJobVectorUpsertRequest(List.of(item));
        BatchJobVectorUpsertResponse facadeResponse = new BatchJobVectorUpsertResponse(1, 1, 0, 0, List.of());
        when(jobVectorBatchFacade.batchUpsert(any())).thenReturn(facadeResponse);

        // 当 / When
        ApiResponse<BatchJobVectorUpsertResponse> response = controller.batchUpsert(request);

        // 那么 / Then
        assertThat(response.getData().total()).isEqualTo(1);
        assertThat(response.getData().success()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle null items in request")
    void shouldHandleNullItemsInRequest() {
        // 给定 / Given
        BatchJobVectorUpsertRequest request = new BatchJobVectorUpsertRequest(null);
        BatchJobVectorUpsertResponse facadeResponse = new BatchJobVectorUpsertResponse(0, 0, 0, 0, List.of());
        when(jobVectorBatchFacade.batchUpsert(any())).thenReturn(facadeResponse);

        // 当 / When
        ApiResponse<BatchJobVectorUpsertResponse> response = controller.batchUpsert(request);

        // 那么 / Then
        assertThat(response.getData().total()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle empty items in request")
    void shouldHandleEmptyItemsInRequest() {
        // 给定 / Given
        BatchJobVectorUpsertRequest request = new BatchJobVectorUpsertRequest(List.of());
        BatchJobVectorUpsertResponse facadeResponse = new BatchJobVectorUpsertResponse(0, 0, 0, 0, List.of());
        when(jobVectorBatchFacade.batchUpsert(any())).thenReturn(facadeResponse);

        // 当 / When
        ApiResponse<BatchJobVectorUpsertResponse> response = controller.batchUpsert(request);

        // 那么 / Then
        assertThat(response.getData().total()).isEqualTo(0);
    }
}
