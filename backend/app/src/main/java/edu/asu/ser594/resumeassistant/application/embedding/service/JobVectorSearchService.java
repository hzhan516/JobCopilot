package edu.asu.ser594.resumeassistant.application.embedding.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.ser594.resumeassistant.api.job.dto.request.VectorSearchRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.VectorSearchResponse;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.JobVectorRepository;
import edu.asu.ser594.resumeassistant.domain.embedding.valueobject.JobVectorSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 职位向量搜索服务
 * Job vector search service
 * <p>
 * 提供基于向量的近似最近邻搜索，支持外部传入向量或由服务内部生成。
 * Provides vector-based approximate nearest neighbor search, supporting externally provided vectors
 * or internally generated ones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobVectorSearchService {

    private final EmbeddingService embeddingService;
    private final JobVectorRepository jobVectorRepository;
    private final ObjectMapper objectMapper;

    /**
     * 执行向量搜索
     * Execute vector search
     *
     * @param request 搜索请求 / Search request
     * @return 搜索结果列表 / List of search results
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
            embedding = embeddingService.generate(request.queryText());
            log.debug("Generated embedding from query text, dimension: {}", embedding.length);
        }

        String vectorStr = buildPgVectorLiteral(embedding);
        int limit = request.limit();

        log.info("Executing vector search with limit: {}", limit);
        List<JobVectorSearchResult> results = jobVectorRepository.findNearestNeighbors(vectorStr, limit);
        log.info("Vector search returned {} results", results.size());

        return results.stream()
                .map(this::mapToResponse)
                .toList();
    }

    // 将 List<Float> 转为 float[] / Convert List<Float> to float[]
    private float[] convertListToArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    // 构建 pgvector 字符串格式 / Build pgvector literal string
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

    // 将搜索结果映射为响应 DTO / Map search result to response DTO
    private VectorSearchResponse mapToResponse(JobVectorSearchResult result) {
        List<String> requirements = parseRequirements(result.requirementsJson());

        // matchFactors 目前仅包含相似度，预留扩展 / matchFactors currently only contains similarity, reserved for extension
        Map<String, Object> matchFactors = Map.of("similarity", result.similarity());

        // company 信息目前从 raw_content 或 description 中无法直接提取，留空 / company info not directly available, left empty
        return new VectorSearchResponse(
                result.jobId(),
                result.title(),
                "", // company 字段暂无直接来源 / company field not directly available
                result.description(),
                requirements,
                result.similarity().floatValue(),
                matchFactors
        );
    }

    // 解析 requirements JSON 字符串 / Parse requirements JSON string
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
