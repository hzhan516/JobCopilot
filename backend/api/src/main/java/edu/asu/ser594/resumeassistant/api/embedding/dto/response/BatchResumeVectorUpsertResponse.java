package edu.asu.ser594.resumeassistant.api.embedding.dto.response;

import java.util.List;

/**
 * 批量简历向量 Upsert 响应
 * Batch resume vector upsert response
 *
 * @param total                 接收总数 / Total items received
 * @param success               成功数 / Successfully processed count
 * @param failed                失败数 / Failed count
 * @param skipped               跳过数 / Skipped count
 * @param failedResumeVersionIds 失败的简历版本ID列表 / List of failed resume version IDs
 */
public record BatchResumeVectorUpsertResponse(
        int total,
        int success,
        int failed,
        int skipped,
        List<String> failedResumeVersionIds
) {
}
