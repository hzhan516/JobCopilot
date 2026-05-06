package edu.asu.ser594.resumeassistant.trigger.http.controller.embedding;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.embedding.dto.request.BatchResumeVectorUpsertRequest;
import edu.asu.ser594.resumeassistant.api.embedding.dto.response.BatchResumeVectorUpsertResponse;
import edu.asu.ser594.resumeassistant.api.embedding.facade.ResumeVectorBatchFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简历向量控制器
 * Resume vector controller
 * <p>
 * 提供简历向量的批量写入接口，供 AI 层数据集迁移调用。
 * Provides batch resume vector write endpoints for AI layer data migration.
 */
@Slf4j
@RestController
@RequestMapping("/v1/resume-vectors")
@RequiredArgsConstructor
public class ResumeVectorController {

    private final ResumeVectorBatchFacade resumeVectorBatchFacade;

    /**
     * 批量 Upsert 简历向量（供 AI 层数据集迁移调用）
     * Batch upsert resume vectors (for AI layer data migration)
     *
     * @param request 批量写入请求 / Batch write request
     * @return 批量操作结果 / Batch operation result
     */
    @PostMapping("/batch")
    public ApiResponse<BatchResumeVectorUpsertResponse> batchUpsert(
            @Validated @RequestBody BatchResumeVectorUpsertRequest request) {
        log.info("Received batch upsert request, item count: {}",
                request.items() != null ? request.items().size() : 0);
        BatchResumeVectorUpsertResponse response = resumeVectorBatchFacade.batchUpsert(request);
        return ApiResponse.success(response);
    }
}
