package io.jobcopilot.resumeassistant.domain.embedding.entity;

import io.jobcopilot.resumeassistant.domain.embedding.valueobject.VectorStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain entity representing a job posting's embedding vector and its generation lifecycle.
 * 表示职位嵌入向量及其生成生命周期的领域实体。
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

    // Extended fields captured at generation time for traceability | 生成时捕获的扩展字段，用于可追溯性
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

    public static JobVector createCompleted(String id, String jobId, float[] embedding) {
        return createCompleted(id, jobId, embedding, null, null, null, null, null, null);
    }

    public static JobVector createCompleted(String id, String jobId, float[] embedding,
                                            String title, String description, List<String> requirements,
                                            String rawContent, String sourceFile, String modelVersion) {
        LocalDateTime now = LocalDateTime.now();
        return new JobVector(id, jobId, embedding, VectorStatus.COMPLETED, null, now, now,
                title, description, requirements, rawContent, sourceFile, modelVersion);
    }

    public static JobVector createFailed(String id, String jobId, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        return new JobVector(id, jobId, null, VectorStatus.FAILED, errorMessage, now, now,
                null, null, null, null, null, null);
    }
}
