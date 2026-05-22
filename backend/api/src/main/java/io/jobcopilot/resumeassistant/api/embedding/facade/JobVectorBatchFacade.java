package io.jobcopilot.resumeassistant.api.embedding.facade;

import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchJobVectorUpsertRequest;
import io.jobcopilot.resumeassistant.api.embedding.dto.response.BatchJobVectorUpsertResponse;

/**
 * 职位向量批量写入门面接口
 * Job vector batch upsert facade interface
 */
public interface JobVectorBatchFacade {

    /**
     * 批量 Upsert 职位向量
     * Batch upsert job vectors
     *
     * @param request 批量写入请求 / Batch write request
     * @return 批量操作结果 / Batch operation result
     */
    BatchJobVectorUpsertResponse batchUpsert(BatchJobVectorUpsertRequest request);
}
