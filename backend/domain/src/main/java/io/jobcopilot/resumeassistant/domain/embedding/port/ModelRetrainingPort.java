package io.jobcopilot.resumeassistant.domain.embedding.port;

/**
 * Port for triggering remote model retraining via an external AI service.
 * Implementations are provided in the infrastructure layer.
 * 通过外部 AI 服务触发远程模型重训练的端口。实现由基础设施层提供。
 */
public interface ModelRetrainingPort {

    /**
     * Triggers incremental model retraining / weight recomputation on the AI service.
     * 触发 AI 服务上的增量模型重训练 / 权重重算。
     */
    void triggerRetraining();
}
