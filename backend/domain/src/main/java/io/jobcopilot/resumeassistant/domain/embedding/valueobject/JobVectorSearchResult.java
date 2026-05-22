package io.jobcopilot.resumeassistant.domain.embedding.valueobject;

/**
 * 职位向量搜索结果投影
 * Job vector search result projection
 * <p>
 * 用于向量近似搜索的只读查询结果，非完整领域实体。
 * Read-only query result for vector approximate search, not a full domain entity.
 *
 * @param jobId            职位ID / Job ID
 * @param title            职位标题 / Job title
 * @param description      职位描述 / Job description
 * @param requirementsJson 要求列表的JSON字符串（由上层解析）/ Requirements as JSON string (parsed by upper layer)
 * @param rawContent       原始内容 / Raw content
 * @param similarity       相似度得分 / Similarity score
 */
public record JobVectorSearchResult(
        String jobId,
        String title,
        String description,
        String requirementsJson,
        String rawContent,
        Double similarity
) {
}
