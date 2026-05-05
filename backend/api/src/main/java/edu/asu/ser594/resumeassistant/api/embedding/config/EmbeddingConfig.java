package edu.asu.ser594.resumeassistant.api.embedding.config;

/**
 * 嵌入向量配置接口
 * Embedding configuration interface.
 * <p>
 * 由 Infrastructure 层提供实现，供 App 层查询向量维度等配置。
 * Implemented by the infrastructure layer and used by the app layer
 * to query embedding dimensions and related settings.
 */
public interface EmbeddingConfig {

    /**
     * 获取嵌入向量维度
     * Get embedding vector dimension.
     *
     * @return 维度值 / Dimension value
     */
    int getDimension();
}
