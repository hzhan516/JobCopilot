package io.jobcopilot.resumeassistant.infrastructure.search;

import io.jobcopilot.resumeassistant.domain.matching.port.VectorSearchPort;
import io.jobcopilot.resumeassistant.domain.matching.valueobject.RecallResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * PGVector 向量搜索服务
 * PGVector search service
 * <p>
 * 使用 JPA + 原生 SQL 进行向量相似度搜索
 * Uses JPA + native SQL for vector similarity search
 */
@Slf4j
@Service
public class PgVectorSearchService implements VectorSearchPort {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 根据简历向量搜索相似的职位
     * Find similar jobs based on resume vector
     *
     * @param resumeVector 简历向量 / Resume vector
     * @param topK         返回最大数量 / Maximum results to return
     * @param modelVersion 模型版本（当前保留用于扩展） / Model version (reserved for extension)
     * @return 召回结果列表 / List of recall results
     */
    public List<RecallResult> findSimilarJobs(final float[] resumeVector, final int topK, final String modelVersion) {
        log.debug("Searching similar jobs with topK: {}, modelVersion: {}", topK, modelVersion);

        final String vectorLiteral = buildPgVectorLiteral(resumeVector);
        final String sql = """
                SELECT job_id, embedding <=> CAST(:queryVector AS vector) AS distance
                FROM job_vectors
                WHERE status = 'COMPLETED'
                ORDER BY embedding <=> CAST(:queryVector AS vector)
                LIMIT :topK
                """;

        final Query query = entityManager.createNativeQuery(sql);
        query.setParameter("queryVector", vectorLiteral);
        query.setParameter("topK", topK);

        @SuppressWarnings("unchecked") final List<Object[]> results = query.getResultList();

        final List<RecallResult> recallResults = new ArrayList<>(results.size());
        for (Object[] row : results) {
            final String jobId = (String) row[0];
            final double distance = ((Number) row[1]).doubleValue();
            recallResults.add(new RecallResult(jobId, distance));
        }

        log.info("PGVector recall found {} jobs for modelVersion: {}", recallResults.size(), modelVersion);
        return recallResults;
    }

    private String buildPgVectorLiteral(final float[] vector) {
        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
