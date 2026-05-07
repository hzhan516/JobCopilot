package edu.asu.ser594.resumeassistant.api.embedding.facade;

/**
 * 向量生成门面接口
 * Vector generation facade interface.
 * <p>
 * 将 Trigger 层的向量生成需求与向量存储领域解耦，确保事务边界由 App 层控制。
 * Decouples trigger-layer vector generation needs from vector storage domain,
 * ensuring transaction boundaries are controlled by the app layer.
 */
public interface VectorFacade {

    /**
     * 同步生成向量并保存
     * Synchronously generate embedding and persist.
     *
     * @param referenceId 实体 ID / Entity ID
     * @param entityType  实体类型 ("JOB" or "RESUME") / Entity type
     * @param text        待嵌入文本 / Text to embed
     */
    void generateAndSaveVector(String referenceId, String entityType, String text);
}
