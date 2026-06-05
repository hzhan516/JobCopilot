package io.jobcopilot.resumeassistant.trigger.http.controller.embedding;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchJobVectorUpsertRequest;
import io.jobcopilot.resumeassistant.api.embedding.dto.response.BatchJobVectorUpsertResponse;
import io.jobcopilot.resumeassistant.api.embedding.facade.JobVectorBatchFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 职位向量控制器
 * Job vector controller
 * <p>
 * 提供职位向量的批量写入接口，供 AI 层数据集迁移调用。
 * Provides batch job vector write endpoints for AI layer data migration.
 */
@Slf4j
@RestController
@RequestMapping("/v1/job-vectors")
@RequiredArgsConstructor
public class JobVectorController {

    private final JobVectorBatchFacade jobVectorBatchFacade;

    /**
     * 批量 Upsert 职位向量（供 AI 层数据集迁移调用）
     * Batch upsert job vectors (for AI layer data migration)
     *
     * @param request 批量写入请求 / Batch write request
     * @return 批量操作结果 / Batch operation result
     */
    @PostMapping("/batch")
    public ApiResponse<BatchJobVectorUpsertResponse> batchUpsert(
            @Validated @RequestBody BatchJobVectorUpsertRequest request) {
        log.info("Received batch upsert request, item count: {}",
                request.items() != null ? request.items().size() : 0);
        BatchJobVectorUpsertResponse response = jobVectorBatchFacade.batchUpsert(request);
        return ApiResponse.success(response);
    }
}
