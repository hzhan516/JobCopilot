package edu.asu.ser594.resumeassistant.domain.embedding.entity;

import edu.asu.ser594.resumeassistant.domain.embedding.valueobject.VectorStatus;
import lombok.Getter;

import java.time.LocalDateTime;

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

    public JobVector(String id, String jobId, float[] embedding, VectorStatus status, String errorMessage, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.jobId = jobId;
        this.embedding = embedding;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 静态工厂方法创建完成的职位向量
     * Static factory method to create completed job vector
     */
    public static JobVector createCompleted(String id, String jobId, float[] embedding) {
        LocalDateTime now = LocalDateTime.now();
        return new JobVector(id, jobId, embedding, VectorStatus.COMPLETED, null, now, now);
    }

    /**
     * 静态工厂方法创建失败的职位向量
     * Static factory method to create failed job vector
     */
    public static JobVector createFailed(String id, String jobId, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        return new JobVector(id, jobId, null, VectorStatus.FAILED, errorMessage, now, now);
    }
}
