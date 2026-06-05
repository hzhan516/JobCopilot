package io.jobcopilot.resumeassistant.domain.job.service;

import io.jobcopilot.resumeassistant.domain.embedding.entity.JobVector;
import io.jobcopilot.resumeassistant.domain.embedding.entity.ResumeVector;
import io.jobcopilot.resumeassistant.domain.embedding.repository.JobVectorRepository;
import io.jobcopilot.resumeassistant.domain.embedding.repository.ResumeVectorRepository;

/**
 * Computes cosine similarity between resume and job vector embeddings.
 * Result is normalized from [-1, 1] to [0, 1] for interpretability.
 * 计算简历与职位向量嵌入之间的余弦相似度。结果从 [-1, 1] 归一化到 [0, 1] 以便理解。
 */
public final class VectorSimilarityService {

    private final ResumeVectorRepository resumeVectorRepository;
    private final JobVectorRepository jobVectorRepository;

    public VectorSimilarityService(ResumeVectorRepository resumeVectorRepository,
                                   JobVectorRepository jobVectorRepository) {
        this.resumeVectorRepository = resumeVectorRepository;
        this.jobVectorRepository = jobVectorRepository;
    }

    /**
     * Calculates semantic match score between a resume version and a job.
     * Returns null if either embedding is missing.
     * 计算简历版本与职位之间的语义匹配分数。若任一嵌入缺失则返回 null。
     *
     * @param resumeVersionId Resume version UUID / 简历版本 UUID
     * @param jobId           Job ID / 职位 ID
     * @return Normalized cosine similarity [0, 1], or null / 归一化余弦相似度 [0, 1]，或 null
     */
    public Float calculate(String resumeVersionId, String jobId) {
        ResumeVector resumeVector = resumeVectorRepository.findByResumeVersionId(resumeVersionId).orElse(null);
        JobVector jobVector = jobVectorRepository.findByJobId(jobId).orElse(null);

        if (resumeVector == null || jobVector == null
                || resumeVector.getEmbedding() == null || jobVector.getEmbedding() == null) {
            return null;
        }

        float[] resumeEmbedding = resumeVector.getEmbedding();
        float[] jobEmbedding = jobVector.getEmbedding();
        int length = Math.min(resumeEmbedding.length, jobEmbedding.length);
        if (length == 0) {
            return null;
        }

        double dot = 0.0;
        double resumeNorm = 0.0;
        double jobNorm = 0.0;
        for (int i = 0; i < length; i++) {
            dot += resumeEmbedding[i] * jobEmbedding[i];
            resumeNorm += resumeEmbedding[i] * resumeEmbedding[i];
            jobNorm += jobEmbedding[i] * jobEmbedding[i];
        }

        if (resumeNorm == 0.0 || jobNorm == 0.0) {
            return null;
        }

        double cosineSimilarity = dot / (Math.sqrt(resumeNorm) * Math.sqrt(jobNorm));
        double normalized = Math.max(0.0, Math.min(1.0, (cosineSimilarity + 1.0) / 2.0));
        return (float) normalized;
    }
}
