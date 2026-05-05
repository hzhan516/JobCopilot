package edu.asu.ser594.resumeassistant.api.embedding.dto.request;

import java.util.List;

/**
 * 批量职位向量 Upsert 请求
 * Batch job vector upsert request
 * <p>
 * 供 AI 层数据集迁移调用，将生成好的 embedding 批量写入后端。
 * Used by the AI layer for data migration to bulk-write pre-computed embeddings.
 *
 * @param items 职位向量条目列表 / List of job vector items
 */
public record BatchJobVectorUpsertRequest(
        List<JobVectorItem> items
) {

    /**
     * 单个职位向量条目
     * Single job vector item
     *
     * @param jobId        职位ID / Job ID
     * @param embedding    嵌入向量 / Embedding vector
     * @param title        职位标题 / Job title
     * @param description  职位描述 / Job description
     * @param requirements 职位要求列表 / Job requirements
     * @param rawContent   原始内容 / Raw content
     * @param sourceFile   来源文件 / Source file
     * @param modelVersion 模型版本 / Model version
     */
    public record JobVectorItem(
            String jobId,
            List<Float> embedding,
            String title,
            String description,
            List<String> requirements,
            String rawContent,
            String sourceFile,
            String modelVersion
    ) {
    }
}
