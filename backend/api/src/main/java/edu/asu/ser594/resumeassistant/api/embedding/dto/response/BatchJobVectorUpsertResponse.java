package edu.asu.ser594.resumeassistant.api.embedding.dto.response;

import java.util.List;

/**
 * 批量职位向量 Upsert 响应
 * Batch job vector upsert response
 *
 * @param total        接收总数 / Total items received
 * @param success      成功数 / Successfully processed count
 * @param failed       失败数 / Failed count
 * @param failedJobIds 失败的职位ID列表 / List of failed job IDs
 */
public record BatchJobVectorUpsertResponse(
        int total,
        int success,
        int failed,
        List<String> failedJobIds
) {
}
