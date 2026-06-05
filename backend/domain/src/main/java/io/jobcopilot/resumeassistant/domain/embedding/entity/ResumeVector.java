package io.jobcopilot.resumeassistant.domain.embedding.entity;

import io.jobcopilot.resumeassistant.domain.embedding.valueobject.VectorStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 简历向量实体
 * Resume vector domain entity
 */
@Getter
public class ResumeVector {

    private final String id;
    private final String resumeVersionId;
    private final float[] embedding;
    private final VectorStatus status;
    private final String errorMessage;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ResumeVector(String id, String resumeVersionId, float[] embedding, VectorStatus status, String errorMessage, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.resumeVersionId = resumeVersionId;
        this.embedding = embedding;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 静态工厂方法创建完成的简历向量
     * Static factory method to create completed resume vector
     */
    public static ResumeVector createCompleted(String id, String resumeVersionId, float[] embedding) {
        LocalDateTime now = LocalDateTime.now();
        return new ResumeVector(id, resumeVersionId, embedding, VectorStatus.COMPLETED, null, now, now);
    }

    /**
     * 静态工厂方法创建失败的简历向量
     * Static factory method to create failed resume vector
     */
    public static ResumeVector createFailed(String id, String resumeVersionId, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        return new ResumeVector(id, resumeVersionId, null, VectorStatus.FAILED, errorMessage, now, now);
    }
}
