package edu.asu.ser594.resumeassistant.api.embedding.dto.request;

import java.util.List;

/**
 * 批量简历向量 Upsert 请求
 * Batch resume vector upsert request
 * <p>
 * 供 AI 层调用，将生成好的简历 embedding 批量写入后端。
 * Used by the AI layer to bulk-write pre-computed resume embeddings.
 *
 * @param items 简历向量条目列表 / List of resume vector items
 */
public record BatchResumeVectorUpsertRequest(
        List<ResumeVectorItem> items
) {

    /**
     * 单个简历向量条目
     * Single resume vector item
     *
     * @param resumeVersionId 简历版本ID / Resume version ID
     * @param embedding       嵌入向量 / Embedding vector
     */
    public record ResumeVectorItem(
            String resumeVersionId,
            List<Float> embedding
    ) {
    }
}
