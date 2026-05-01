package edu.asu.ser594.resumeassistant.domain.matching.port;

import edu.asu.ser594.resumeassistant.domain.matching.valueobject.RecallResult;

import java.util.List;

/**
 * 向量搜索端口
 * Vector search port
 * <p>
 * 定义应用层对向量相似度搜索的抽象需求，由基础设施层提供具体实现（如 PGVector）。
 * Defines the application layer's abstract requirement for vector similarity search,
 * implemented by the infrastructure layer (e.g., PGVector).
 */
public interface VectorSearchPort {

    /**
     * 根据简历向量搜索相似的职位
     * Find similar jobs based on resume vector
     *
     * @param resumeVector 简历向量 / Resume vector
     * @param topK         返回最大数量 / Maximum results to return
     * @param modelVersion 模型版本（当前保留用于扩展） / Model version (reserved for extension)
     * @return 召回结果列表 / List of recall results
     */
    List<RecallResult> findSimilarJobs(float[] resumeVector, int topK, String modelVersion);
}
