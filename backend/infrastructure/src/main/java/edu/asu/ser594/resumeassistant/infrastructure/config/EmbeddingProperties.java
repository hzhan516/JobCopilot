package edu.asu.ser594.resumeassistant.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 嵌入向量配置属性
 * Embedding configuration properties
 */
@Component
@ConfigurationProperties(prefix = "app.embedding")
@Getter
@Setter
public class EmbeddingProperties {

    /**
     * 嵌入向量维度，必须与所选模型输出维度一致
     * Embedding vector dimension, must match the selected model output dimension
     */
    private int dimension = 1536;
}
