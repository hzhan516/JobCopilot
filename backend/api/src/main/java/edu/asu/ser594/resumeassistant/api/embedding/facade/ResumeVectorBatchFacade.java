package edu.asu.ser594.resumeassistant.api.embedding.facade;

import edu.asu.ser594.resumeassistant.api.embedding.dto.request.BatchResumeVectorUpsertRequest;
import edu.asu.ser594.resumeassistant.api.embedding.dto.response.BatchResumeVectorUpsertResponse;

/**
 * 简历向量批量写入门面接口
 * Resume vector batch upsert facade interface
 */
public interface ResumeVectorBatchFacade {

    /**
     * 批量 Upsert 简历向量
     * Batch upsert resume vectors
     *
     * @param request 批量写入请求 / Batch write request
     * @return 批量操作结果 / Batch operation result
     */
    BatchResumeVectorUpsertResponse batchUpsert(BatchResumeVectorUpsertRequest request);
}
