package edu.asu.ser594.resumeassistant.api.embedding.config;

/**
 * Abstraction over embedding infrastructure so the application layer remains agnostic to model providers.
 * 嵌入向量配置抽象，使应用层无需关心具体模型提供商的实现细节
 */
public interface EmbeddingConfig {

    int getDimension();

    String getDefaultModelVersion();
}
