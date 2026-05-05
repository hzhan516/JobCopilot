package edu.asu.ser594.resumeassistant.infrastructure.persistence.config;

import edu.asu.ser594.resumeassistant.api.embedding.config.EmbeddingConfig;
import edu.asu.ser594.resumeassistant.infrastructure.config.EmbeddingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 嵌入向量配置实现
 * Embedding configuration implementation.
 * <p>
 * 将 Infrastructure 层的 EmbeddingProperties 适配为 API 层接口，
 * 消除 App 层对 Infrastructure 配置类的直接依赖。
 * Adapts infrastructure-layer EmbeddingProperties to the API-layer interface,
 * removing direct app-layer dependency on infrastructure configuration classes.
 */
@Component
@RequiredArgsConstructor
public class EmbeddingConfigImpl implements EmbeddingConfig {

    private final EmbeddingProperties embeddingProperties;

    @Override
    public int getDimension() {
        return embeddingProperties.getDimension();
    }
}
