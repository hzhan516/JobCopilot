package io.jobcopilot.resumeassistant.api.job.dto.response;

import java.util.List;
import java.util.Map;

/**
 * 向量搜索响应项
 * Vector search response item
 *
 * @param jobId        职位ID / Job ID
 * @param title        职位标题 / Job title
 * @param company      公司名称 / Company name
 * @param description  职位描述 / Job description
 * @param requirements 职位要求列表 / List of job requirements
 * @param similarity   相似度得分（0-1，越大越相似）/ Similarity score (0-1, higher is more similar)
 * @param matchFactors 匹配因子详情（预留扩展）/ Match factor details (reserved for extension)
 */
public record VectorSearchResponse(
        String jobId,
        String title,
        String company,
        String description,
        List<String> requirements,
        Float similarity,
        Map<String, Object> matchFactors
) {
}
