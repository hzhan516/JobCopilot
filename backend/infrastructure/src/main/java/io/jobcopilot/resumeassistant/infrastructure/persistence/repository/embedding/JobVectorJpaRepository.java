package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.embedding;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.embedding.JobVectorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobVectorJpaRepository extends JpaRepository<JobVectorJpaEntity, String> {

    Optional<JobVectorJpaEntity> findByJobId(String jobId);

    /**
     * 向量近似最近邻搜索
     * Vector approximate nearest neighbor search using pgvector <=> operator
     *
     * @param vectorStr pgvector 字符串格式 / pgvector literal string
     * @param limit     返回最大数量 / Maximum results to return
     * @return 原始查询结果列表 / List of raw query results
     */
    @Query(value = """
            SELECT
              job_id,
              title,
              description,
              requirements,
              raw_content,
              1 - (embedding <=> CAST(:vectorStr AS vector(#{@embeddingProperties.dimension}))) AS similarity
            FROM job_vectors
            WHERE status = 'COMPLETED'
            ORDER BY embedding <=> CAST(:vectorStr AS vector(#{@embeddingProperties.dimension}))
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findNearestNeighbors(@Param("vectorStr") String vectorStr, @Param("limit") int limit);
}