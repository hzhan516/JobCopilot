package edu.asu.ser594.resumeassistant.api.embedding.facade;

import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;

/**
 * 向量生成结果处理的入站门面接口
 * Inbound port facade for handling vector generation results.
 * <p>
 * 将 Trigger 层的 MQ 监听与向量存储领域解耦，确保事务边界由 App 层控制。
 * Decouples trigger-layer MQ listeners from vector storage domain,
 * ensuring transaction boundaries are controlled by the app layer.
 */
public interface VectorFacade {

    /**
     * 处理异步向量生成结果
     * Handles the asynchronous result of a vector generation request.
     *
     * @param event AI 结果事件 / The result event containing embedding vector or error details.
     */
    void handleVectorGenResult(AiResultEvent event);
}
