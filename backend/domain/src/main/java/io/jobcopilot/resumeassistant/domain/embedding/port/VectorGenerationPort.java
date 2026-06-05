package io.jobcopilot.resumeassistant.domain.embedding.port;

/**
 * Port for generating and persisting vector embeddings for domain entities.
 * Implementations are provided in the infrastructure layer.
 * 为领域实体生成并持久化向量嵌入的端口。实现由基础设施层提供。
 */
public interface VectorGenerationPort {

    /**
     * Generates an embedding vector for the given entity and persists it.
     * 为指定实体生成嵌入向量并持久化。
     *
     * @param referenceId Entity ID / 实体 ID
     * @param entityType  Entity type label (e.g. "JOB", "RESUME") / 实体类型标签
     * @param text        Text to embed / 待嵌入文本
     */
    void generateAndSaveVector(String referenceId, String entityType, String text);
}
