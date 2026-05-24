package io.jobcopilot.resumeassistant.domain.embedding.port;

/**
 * Port for generating embedding vectors from text via an external AI service.
 * Implementations are provided in the infrastructure layer.
 * 通过外部 AI 服务将文本转换为嵌入向量的端口。实现由基础设施层提供。
 */
public interface VectorEmbeddingPort {

    /**
     * Generates an embedding vector for the given text.
     * 为给定文本生成嵌入向量。
     *
     * @param text Input text / 输入文本
     * @return Embedding vector / 嵌入向量
     */
    float[] generate(String text);
}
