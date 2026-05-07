package edu.asu.ser594.resumeassistant.application.embedding;

import edu.asu.ser594.resumeassistant.api.embedding.dto.request.BatchJobVectorUpsertRequest;
import edu.asu.ser594.resumeassistant.api.embedding.dto.response.BatchJobVectorUpsertResponse;
import edu.asu.ser594.resumeassistant.api.embedding.facade.JobVectorBatchFacade;
import edu.asu.ser594.resumeassistant.application.embedding.service.JobVectorBatchService;
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
