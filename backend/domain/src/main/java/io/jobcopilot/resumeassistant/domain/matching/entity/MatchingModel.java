package io.jobcopilot.resumeassistant.domain.matching.entity;

import io.jobcopilot.resumeassistant.domain.matching.valueobject.ModelType;
import io.jobcopilot.resumeassistant.domain.shared.entity.AggregateRoot;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 匹配模型实体
 * Matching model entity
 * <p>
 * 对应 job_matching_models 表 / Corresponds to job_matching_models table
 */
@Getter
public class MatchingModel extends AggregateRoot<Long> {

    private final Long id;
    private final LocalDateTime createdAt;
    private String modelName;
    private String version;
    private ModelType type;
    private String storagePath;
    private Map<String, Object> evaluationMetrics;
    private boolean isActive;
    private LocalDateTime updatedAt;

    @Builder
    public MatchingModel(final Long id,
                         final String modelName,
                         final String version,
                         final ModelType type,
                         final String storagePath,
                         final Map<String, Object> evaluationMetrics,
                         final boolean isActive,
                         final LocalDateTime createdAt,
                         final LocalDateTime updatedAt) {
        this.id = id;
        this.modelName = modelName;
        this.version = version;
        this.type = type;
        this.storagePath = storagePath;
        this.evaluationMetrics = evaluationMetrics;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 激活当前模型版本
     * Activate current model version
     */
    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 停用当前模型版本
     * Deactivate current model version
     */
    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新评估指标
     * Update evaluation metrics
     *
     * @param metrics 新的评估指标 / New evaluation metrics
     */
    public void updateEvaluationMetrics(final Map<String, Object> metrics) {
        this.evaluationMetrics = metrics;
        this.updatedAt = LocalDateTime.now();
    }
}
