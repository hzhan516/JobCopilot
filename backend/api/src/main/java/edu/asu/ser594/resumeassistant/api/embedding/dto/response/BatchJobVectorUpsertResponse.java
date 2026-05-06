package edu.asu.ser594.resumeassistant.api.embedding.dto.response;

import java.util.List;

/**
 * 批量职位向量 Upsert 响应
 * Batch job vector upsert response
 *
 * @param total        接收总数 / Total items received
 * @param success      成功数（插入或更新）/ Successfully processed count (inserted or updated)
 * @param failed       失败数 / Failed count
 * @param skipped      跳过数（内容完全相同，无需写入）/ Skipped count (content identical, no write needed)
 * @param failedJobIds 失败的职位ID列表 / List of failed job IDs
 */
public record BatchJobVectorUpsertResponse(
        int total,
        int success,
        int failed,
        int skipped,
        List<String> failedJobIds
) {
}
