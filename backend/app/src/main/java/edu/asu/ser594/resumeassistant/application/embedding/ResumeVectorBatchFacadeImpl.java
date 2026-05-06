package edu.asu.ser594.resumeassistant.application.embedding;

import edu.asu.ser594.resumeassistant.api.embedding.dto.request.BatchResumeVectorUpsertRequest;
import edu.asu.ser594.resumeassistant.api.embedding.dto.response.BatchResumeVectorUpsertResponse;
import edu.asu.ser594.resumeassistant.api.embedding.facade.ResumeVectorBatchFacade;
import edu.asu.ser594.resumeassistant.application.embedding.service.ResumeVectorBatchService;
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
