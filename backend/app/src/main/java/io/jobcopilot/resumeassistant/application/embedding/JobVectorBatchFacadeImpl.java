package io.jobcopilot.resumeassistant.application.embedding;

import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchJobVectorUpsertRequest;
import io.jobcopilot.resumeassistant.api.embedding.dto.response.BatchJobVectorUpsertResponse;
import io.jobcopilot.resumeassistant.api.embedding.facade.JobVectorBatchFacade;
import io.jobcopilot.resumeassistant.application.embedding.service.JobVectorBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 职位向量批量写入门面实现
 * Job vector batch upsert facade implementation
 */
@Component
@RequiredArgsConstructor
public class JobVectorBatchFacadeImpl implements JobVectorBatchFacade {

    private final JobVectorBatchService jobVectorBatchService;

    @Override
    public BatchJobVectorUpsertResponse batchUpsert(BatchJobVectorUpsertRequest request) {
        return jobVectorBatchService.batchUpsert(request.items());
    }
}
