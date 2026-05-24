package io.jobcopilot.resumeassistant.application.embedding.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.api.job.dto.request.VectorSearchRequest;
import io.jobcopilot.resumeassistant.api.job.dto.response.VectorSearchResponse;
import io.jobcopilot.resumeassistant.domain.embedding.port.VectorEmbeddingPort;
import io.jobcopilot.resumeassistant.domain.embedding.repository.JobVectorRepository;
import io.jobcopilot.resumeassistant.domain.embedding.valueobject.JobVectorSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Executes approximate nearest neighbor search against pgvector using either a client-provided
 * embedding or a text query that is internally embedded via the AI service.
 * 使用客户端提供的嵌入向量或经由 AI 服务内部生成的文本查询，对 pgvector 执行近似最近邻搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobVectorSearchService {

    private final VectorEmbeddingPort vectorEmbeddingPort;
    private final JobVectorRepository jobVectorRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.search.max-limit:100}")
    private int maxSearchLimit;

    /**
     * Performs vector similarity search with an upper bound to prevent unbounded result sets
     * from degrading database performance.
     * 执行向量相似度搜索并设置上限，防止无限制的结果集降低数据库性能
     *
     * @param request Search request / 搜索请求
     * @return List of search results / 搜索结果列表
     */
    public List<VectorSearchResponse> search(VectorSearchRequest request) {
        float[] embedding;
        if (request.queryEmbedding() != null && !request.queryEmbedding().isEmpty()) {
            embedding = convertListToArray(request.queryEmbedding());
            log.debug("Using provided query embedding, dimension: {}", embedding.length);
        } else {
            if (request.queryText() == null || request.queryText().isBlank()) {
                throw new IllegalArgumentException("Either queryText or queryEmbedding must be provided");
            }
            embedding = vectorEmbeddingPort.generate(request.queryText());
            log.debug("Generated embedding from query text, dimension: {}", embedding.length);
        }

        String vectorStr = buildPgVectorLiteral(embedding);
        int limit = Math.min(request.limit(), maxSearchLimit);

        log.info("Executing vector search with limit: {} (capped at {})", limit, maxSearchLimit);
        List<JobVectorSearchResult> results = jobVectorRepository.findNearestNeighbors(vectorStr, limit);
        log.info("Vector search returned {} results", results.size());

        return results.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private float[] convertListToArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    // pgvector requires the literal format [a,b,c] for native queries
    // pgvector 原生查询要求字面量格式 [a,b,c]
    private String buildPgVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder();
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

    private VectorSearchResponse mapToResponse(JobVectorSearchResult result) {
        List<String> requirements = parseRequirements(result.requirementsJson());

        // matchFactors is intentionally minimal now; additional signals (e.g., keyword overlap) can be appended later
        // matchFactors 当前有意保持最小化，后续可追加额外信号（如关键词重叠度）
        Map<String, Object> matchFactors = Map.of("similarity", result.similarity());

        // Company name is not indexed in the vector table; it must be resolved from the job domain if needed
        // 公司名称未在向量表中索引，如需使用需从职位领域解析
        return new VectorSearchResponse(
                result.jobId(),
                result.title(),
                "",
                result.description(),
                requirements,
                result.similarity().floatValue(),
                matchFactors
        );
    }

    private List<String> parseRequirements(String requirementsJson) {
        if (requirementsJson == null || requirementsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(requirementsJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse requirements JSON: {}", requirementsJson, e);
            return Collections.emptyList();
        }
    }
}
