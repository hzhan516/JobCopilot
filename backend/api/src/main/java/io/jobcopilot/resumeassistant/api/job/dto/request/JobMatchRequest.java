package io.jobcopilot.resumeassistant.api.job.dto.request;

import java.util.Map;

/**
 * 职位匹配请求参数
 * Job match request parameters
 *
 * @param resumeVersionId 简历版本 ID / Resume version ID
 * @param query           用户查询词或意图 / User query or intent
 * @param topK            期望返回的最大数量 / Expected maximum number of results to return
 * @param filters         过滤条件字典 / Filter conditions dictionary
 */
public record JobMatchRequest(
        String resumeVersionId,
        String query,
        Integer topK,
        Map<String, String> filters
) {
    /**
     * 紧凑构造函数，处理默认值
     * Compact constructor to handle default values
     */
    public JobMatchRequest {
        if (topK == null || topK <= 0) {
            topK = 10;
        }
    }
}
