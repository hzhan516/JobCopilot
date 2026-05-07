package edu.asu.ser594.resumeassistant.domain.embedding.entity;

import edu.asu.ser594.resumeassistant.domain.embedding.valueobject.VectorStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 职位向量实体
 * Job vector domain entity
 */
@Getter
public class JobVector {

    private final String id;
    private final String jobId;
    private final float[] embedding;
    private final VectorStatus status;
    private final String errorMessage;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    // 扩展字段 / Extended fields
    private final String title;
    private final String description;
    private final List<String> requirements;
    private final String rawContent;
    private final String sourceFile;
    private final String modelVersion;

    public JobVector(String id, String jobId, float[] embedding, VectorStatus status, String errorMessage,
                     LocalDateTime createdAt, LocalDateTime updatedAt,
                     String title, String description, List<String> requirements,
                     String rawContent, String sourceFile, String modelVersion) {
        this.id = id;
        this.jobId = jobId;
        this.embedding = embedding;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.title = title;
        this.description = description;
        this.requirements = requirements;
        this.rawContent = rawContent;
        this.sourceFile = sourceFile;
        this.modelVersion = modelVersion;
    }

    /**
     * 静态工厂方法创建完成的职位向量（基础版本，兼容现有调用）
     * Static factory method to create completed job vector (basic version, backward compatible)
     */
    public static JobVector createCompleted(String id, String jobId, float[] embedding) {
        return createCompleted(id, jobId, embedding, null, null, null, null, null, null);
    }

    /**
     * 静态工厂方法创建完成的职位向量（完整版本，含扩展字段）
     * Static factory method to create completed job vector (full version with extended fields)
     */
    public static JobVector createCompleted(String id, String jobId, float[] embedding,
                                            String title, String description, List<String> requirements,
                                            String rawContent, String sourceFile, String modelVersion) {
        LocalDateTime now = LocalDateTime.now();
        return new JobVector(id, jobId, embedding, VectorStatus.COMPLETED, null, now, now,
                title, description, requirements, rawContent, sourceFile, modelVersion);
    }

    /**
     * 静态工厂方法创建失败的职位向量
     * Static factory method to create failed job vector
     */
    public static JobVector createFailed(String id, String jobId, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        return new JobVector(id, jobId, null, VectorStatus.FAILED, errorMessage, now, now,
                null, null, null, null, null, null);
    }
}
