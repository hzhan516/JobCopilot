package io.jobcopilot.resumeassistant.domain.resume.service;

import io.jobcopilot.resumeassistant.domain.embedding.port.VectorGenerationPort;

import java.util.UUID;

/**
 * Generates and persists vector embeddings for resume and job content.
 * Non-blocking: failures are logged but never propagated to the caller.
 * 为简历和职位内容生成并持久化向量嵌入。非阻塞：失败仅记录日志，不向上传播。
 */
public final class VectorGenerationService {

    private final VectorGenerationPort vectorGenerationPort;

    public VectorGenerationService(VectorGenerationPort vectorGenerationPort) {
        this.vectorGenerationPort = vectorGenerationPort;
    }

    /**
     * Generates a vector for resume content.
     * 为简历内容生成向量。
     *
     * @param versionId Resume version UUID / 简历版本 UUID
     * @param content   Text content / 文本内容
     */
    public void generateForResume(UUID versionId, String content) {
        try {
            vectorGenerationPort.generateAndSaveVector(versionId.toString(), "RESUME", content);
        } catch (Exception e) {
            // Non-blocking: vector generation failure should not break the main flow
            // 非阻塞：向量生成失败不应阻断主流程
        }
    }

    /**
     * Generates a vector for job content.
     * 为职位内容生成向量。
     *
     * @param jobId   Job ID / 职位 ID
     * @param content Text content / 文本内容
     */
    public void generateForJob(UUID jobId, String content) {
        try {
            vectorGenerationPort.generateAndSaveVector(jobId.toString(), "JOB", content);
        } catch (Exception e) {
            // Non-blocking
        }
    }
}
