package io.jobcopilot.resumeassistant.application.embedding.service;

import io.jobcopilot.resumeassistant.api.job.dto.request.VectorSearchRequest;
import io.jobcopilot.resumeassistant.api.job.dto.response.VectorSearchResponse;
import io.jobcopilot.resumeassistant.domain.embedding.port.VectorEmbeddingPort;
import io.jobcopilot.resumeassistant.domain.embedding.repository.JobVectorRepository;
import io.jobcopilot.resumeassistant.domain.embedding.valueobject.JobVectorSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JobVectorSearchService 单元测试
 * 职位向量搜索服务单元测试
 * <p>
 * 测试近似最近邻搜索完整路径：
 * Tests the approximate nearest neighbor search full path:
 * - 使用提供的 embedding 搜索
 * - Search with provided embedding
 * - 使用 queryText 生成 embedding 后搜索
 * - Search after generating embedding from query text
 * - 空结果处理
 * - Empty result handling
 * - 参数校验
 * - Parameter validation
 * - limit 上限截断
 * - Limit capping
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Job Vector Search Service Tests")
class JobVectorSearchServiceTest {

    @Mock
    private VectorEmbeddingPort vectorEmbeddingPort;

    @Mock
    private JobVectorRepository jobVectorRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JobVectorSearchService service;

    private static final int MAX_LIMIT = 100;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxSearchLimit", MAX_LIMIT);
    }

    // ==================== 正常路径 ====================

    @Test
    @DisplayName("Should search with provided embedding")
    void shouldSearchWithProvidedEmbedding() {
        // 给定 / Given
        List<Float> embedding = List.of(0.1f, 0.2f, 0.3f);
        VectorSearchRequest request = new VectorSearchRequest(null, embedding, 5, null);

        JobVectorSearchResult result = new JobVectorSearchResult(
                "job-1", "Java Dev", "desc", "[\"Java\"]",
                BigDecimal.valueOf(0.95), "content", "file", "model");
        when(jobVectorRepository.findNearestNeighbors(contains("[0.1,"), eq(5)))
                .thenReturn(List.of(result));

        // 当 / When
        List<VectorSearchResponse> responses = service.search(request);

        // 那么 / Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).jobId()).isEqualTo("job-1");
        assertThat(responses.get(0).similarity()).isEqualTo(0.95f);
    }

    @Test
    @DisplayName("Should generate embedding from query text when no embedding provided")
    void shouldGenerateEmbeddingFromQueryText() {
        // 给定 / Given
        VectorSearchRequest request = new VectorSearchRequest("Java developer", null, 10, null);
        float[] generatedEmbedding = {0.4f, 0.5f, 0.6f};
        when(vectorEmbeddingPort.generate("Java developer")).thenReturn(generatedEmbedding);
        when(jobVectorRepository.findNearestNeighbors(anyString(), eq(10)))
                .thenReturn(Collections.emptyList());

        // 当 / When
        List<VectorSearchResponse> responses = service.search(request);

        // 那么 / Then
        assertThat(responses).isEmpty();
        verify(vectorEmbeddingPort).generate("Java developer");
    }

    @Test
    @DisplayName("Should cap limit at maxSearchLimit")
    void shouldCapLimitAtMaxSearchLimit() {
        // 给定 / Given
        List<Float> embedding = List.of(0.1f);
        VectorSearchRequest request = new VectorSearchRequest(null, embedding, 200, null);
        when(jobVectorRepository.findNearestNeighbors(anyString(), eq(MAX_LIMIT)))
                .thenReturn(Collections.emptyList());

        // 当 / When
        service.search(request);

        // 那么 / Then
        verify(jobVectorRepository).findNearestNeighbors(anyString(), eq(MAX_LIMIT));
    }

    @Test
    @DisplayName("Should use default limit when null")
    void shouldUseDefaultLimitWhenNull() {
        // 给定 / Given — compact constructor sets limit to 10 when null
        List<Float> embedding = List.of(0.1f);
        VectorSearchRequest request = new VectorSearchRequest(null, embedding, null, null);
        when(jobVectorRepository.findNearestNeighbors(anyString(), eq(10)))
                .thenReturn(Collections.emptyList());

        // 当 / When
        service.search(request);

        // 那么 / Then
        verify(jobVectorRepository).findNearestNeighbors(anyString(), eq(10));
    }

    @Test
    @DisplayName("Should handle empty results")
    void shouldHandleEmptyResults() {
        // 给定 / Given
        List<Float> embedding = List.of(0.1f);
        VectorSearchRequest request = new VectorSearchRequest(null, embedding, 5, null);
        when(jobVectorRepository.findNearestNeighbors(anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        // 当 / When
        List<VectorSearchResponse> responses = service.search(request);

        // 那么 / Then
        assertThat(responses).isEmpty();
    }

    // ==================== 参数校验 ====================

    @Test
    @DisplayName("Should throw when both queryText and embedding are null")
    void shouldThrowWhenBothQueryTextAndEmbeddingAreNull() {
        // 给定 / Given
        VectorSearchRequest request = new VectorSearchRequest(null, null, 5, null);

        // 当&那么 / When & Then
        assertThatThrownBy(() -> service.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("queryText or queryEmbedding");
    }

    @Test
    @DisplayName("Should throw when queryText is blank and embedding empty")
    void shouldThrowWhenQueryTextIsBlankAndEmbeddingEmpty() {
        // 给定 / Given
        VectorSearchRequest request = new VectorSearchRequest("   ", Collections.emptyList(), 5, null);

        // 当&那么 / When & Then
        assertThatThrownBy(() -> service.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("queryText or queryEmbedding");
    }

    @Test
    @DisplayName("Should throw when queryText is blank and embedding null")
    void shouldThrowWhenQueryTextIsBlankAndEmbeddingNull() {
        // 给定 / Given
        VectorSearchRequest request = new VectorSearchRequest("", null, 5, null);

        // 当&那么 / When & Then
        assertThatThrownBy(() -> service.search(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("queryText or queryEmbedding");
    }

    // ==================== 响应映射 ====================

    @Test
    @DisplayName("Should parse requirements JSON")
    void shouldParseRequirementsJson() throws Exception {
        // 给定 / Given
        List<Float> embedding = List.of(0.1f);
        VectorSearchRequest request = new VectorSearchRequest(null, embedding, 5, null);

        JobVectorSearchResult result = new JobVectorSearchResult(
                "job-1", "Dev", "desc", "[\"Java\", \"Spring\"]",
                BigDecimal.valueOf(0.88), "content", "file", "model");
        when(jobVectorRepository.findNearestNeighbors(anyString(), anyInt()))
                .thenReturn(List.of(result));
        when(objectMapper.readValue(eq("[\"Java\", \"Spring\"]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of("Java", "Spring"));

        // 当 / When
        List<VectorSearchResponse> responses = service.search(request);

        // 那么 / Then
        assertThat(responses.get(0).requirements()).containsExactly("Java", "Spring");
    }

    @Test
    @DisplayName("Should return empty requirements on null JSON")
    void shouldReturnEmptyRequirementsOnNullJson() {
        // 给定 / Given
        List<Float> embedding = List.of(0.1f);
        VectorSearchRequest request = new VectorSearchRequest(null, embedding, 5, null);

        JobVectorSearchResult result = new JobVectorSearchResult(
                "job-1", "Dev", "desc", null,
                BigDecimal.valueOf(0.88), "content", "file", "model");
        when(jobVectorRepository.findNearestNeighbors(anyString(), anyInt()))
                .thenReturn(List.of(result));

        // 当 / When
        List<VectorSearchResponse> responses = service.search(request);

        // 那么 / Then
        assertThat(responses.get(0).requirements()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty requirements on parse failure")
    void shouldReturnEmptyRequirementsOnParseFailure() throws Exception {
        // 给定 / Given
        List<Float> embedding = List.of(0.1f);
        VectorSearchRequest request = new VectorSearchRequest(null, embedding, 5, null);

        JobVectorSearchResult result = new JobVectorSearchResult(
                "job-1", "Dev", "desc", "invalid json",
                BigDecimal.valueOf(0.88), "content", "file", "model");
        when(jobVectorRepository.findNearestNeighbors(anyString(), anyInt()))
                .thenReturn(List.of(result));
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonParseException("invalid"));

        // 当 / When
        List<VectorSearchResponse> responses = service.search(request);

        // 那么 / Then
        assertThat(responses.get(0).requirements()).isEmpty();
    }
}
