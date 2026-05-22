package io.jobcopilot.resumeassistant.domain.embedding.repository;

import io.jobcopilot.resumeassistant.domain.embedding.entity.JobVector;
import io.jobcopilot.resumeassistant.domain.embedding.valueobject.JobVectorSearchResult;

import java.util.List;
import java.util.Optional;

/**
 * 职位向量仓储接口
 * Job vector repository interface
 */
public interface JobVectorRepository {

    /**
     * 保存职位向量
     * Save job vector
     */
    void save(JobVector vector);

    /**
     * 批量保存职位向量
     * Save job vectors in batch
     */
    void saveAll(List<JobVector> vectors);

    /**
     * 根据职位ID查找向量
     * Find vector by job ID
     */
    Optional<JobVector> findByJobId(String jobId);

    /**
     * 向量近似最近邻搜索
     * Vector approximate nearest neighbor search
     *
     * @param vectorStr pgvector 字符串格式（如 [0.1,0.2,...]）/ pgvector literal string (e.g. [0.1,0.2,...])
     * @param limit     返回最大数量 / Maximum results to return
     * @return 搜索结果列表 / List of search results
     */
    List<JobVectorSearchResult> findNearestNeighbors(String vectorStr, int limit);
}