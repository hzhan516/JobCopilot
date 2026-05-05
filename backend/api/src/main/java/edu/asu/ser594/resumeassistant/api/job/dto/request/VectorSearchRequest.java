package edu.asu.ser594.resumeassistant.api.job.dto.request;

import java.util.List;
import java.util.Map;

/**
 * 向量搜索请求参数
 * Vector search request parameters
 *
 * @param queryText      查询文本（当 queryEmbedding 为空时用于生成向量）/ Query text used for embedding generation when queryEmbedding is absent
 * @param queryEmbedding 查询向量（优先使用）/ Query embedding vector (takes precedence)
 * @param limit          返回最大数量 / Maximum number of results to return
 * @param filters        过滤条件字典（预留扩展）/ Filter conditions dictionary (reserved for extension)
 */
public record VectorSearchRequest(
        String queryText,
        List<Float> queryEmbedding,
        Integer limit,
        Map<String, String> filters
) {
    /**
     * 紧凑构造函数，处理默认值
     * Compact constructor to handle default values
     */
    public VectorSearchRequest {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }
    }
}
