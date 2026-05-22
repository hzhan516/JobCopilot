package io.jobcopilot.resumeassistant.application.embedding;

import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchResumeVectorUpsertRequest;
import io.jobcopilot.resumeassistant.api.embedding.dto.response.BatchResumeVectorUpsertResponse;
import io.jobcopilot.resumeassistant.api.embedding.facade.ResumeVectorBatchFacade;
import io.jobcopilot.resumeassistant.application.embedding.service.ResumeVectorBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 简历向量批量写入门面实现
 * Resume vector batch upsert facade implementation
 */
@Component
@RequiredArgsConstructor
public class ResumeVectorBatchFacadeImpl implements ResumeVectorBatchFacade {

    private final ResumeVectorBatchService resumeVectorBatchService;

    @Override
    public BatchResumeVectorUpsertResponse batchUpsert(BatchResumeVectorUpsertRequest request) {
        return resumeVectorBatchService.batchUpsert(request.items());
    }
}
