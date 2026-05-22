package io.jobcopilot.resumeassistant.domain.matching.valueobject;

/**
 * 匹配算法类型枚举
 * Match algorithm type enumeration
 */
public enum MatchAlgorithm {
    /**
     * 基于向量相似度的余弦召回
     * Cosine similarity based vector recall
     */
    VECTOR_COSINE,

    /**
     * 基于关键词的BM25匹配
     * Keyword based BM25 matching
     */
    BM25,

    /**
     * 混合召回（向量 + 关键词）
     * Hybrid recall (vector + keyword)
     */
    HYBRID
}
