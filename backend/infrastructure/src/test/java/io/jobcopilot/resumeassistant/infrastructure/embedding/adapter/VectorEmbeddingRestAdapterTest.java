package io.jobcopilot.resumeassistant.infrastructure.embedding.adapter;

import io.jobcopilot.resumeassistant.domain.shared.exception.AiServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * VectorEmbeddingRestAdapter 单元测试
 * 向量嵌入 REST 适配器单元测试
 * <p>
 * 测试外部 AI Embedding 服务调用的完整路径：
 * Tests the full path of external AI Embedding service invocation:
 * - 正常响应解析
 * - Normal response parsing
 * - 空/异常响应处理
 * - Empty / exception response handling
 * - 网络不可用降级
 * - Network unavailable fallback
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Vector Embedding REST Adapter Tests")
class VectorEmbeddingRestAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private VectorEmbeddingRestAdapter adapter;

    private static final String BASE_URL = "http://ai-service:8000";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "aiServiceBaseUrl", BASE_URL);
    }

    // ==================== 正常路径 ====================
    // ==================== Happy Path ====================

    @Test
    @DisplayName("Should return embedding array on valid response")
    void shouldReturnEmbeddingArrayOnValidResponse() {
        // 给定 / Given
        Map<String, Object> response = Map.of(
                "embeddings", List.of(List.of(0.1f, 0.2f, 0.3f)),
                "modelUsed", "text-embedding-3-small",
                "count", 1
        );
        when(restTemplate.postForObject(
                eq(BASE_URL + "/api/v1/ai/embeddings"),
                any(),
                eq(Map.class)
        )).thenReturn(response);

        // 当 / When
        float[] result = adapter.generate("Software Engineer with 5 years experience");

        // 那么 / Then
        assertThat(result).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    @DisplayName("Should handle large dimension embedding")
    void shouldHandleLargeDimensionEmbedding() {
        // 给定 / Given
        List<Float> largeEmbedding = new java.util.ArrayList<>();
        for (int i = 0; i < 1536; i++) {
            largeEmbedding.add(i / 1536.0f);
        }
        Map<String, Object> response = Map.of(
                "embeddings", List.of(largeEmbedding),
                "modelUsed", "text-embedding-3-large"
        );
        when(restTemplate.postForObject(any(), any(), eq(Map.class))).thenReturn(response);

        // 当 / When
        float[] result = adapter.generate("Long text for embedding generation test");

        // 那么 / Then
        assertThat(result).hasSize(1536);
        assertThat(result[0]).isEqualTo(0.0f);
        assertThat(result[1535]).isEqualTo(1535 / 1536.0f);
    }

    // ==================== 参数校验 ====================
    // ==================== Parameter Validation ====================

    @Test
    @DisplayName("Should reject blank text")
    void shouldRejectBlankText() {
        // 当&那么 / When & Then
        assertThatThrownBy(() -> adapter.generate(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("Should reject null text")
    void shouldRejectNullText() {
        // 当&那么 / When & Then
        assertThatThrownBy(() -> adapter.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("Should reject whitespace-only text")
    void shouldRejectWhitespaceOnlyText() {
        // 当&那么 / When & Then
        assertThatThrownBy(() -> adapter.generate("   \t\n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    // ==================== 异常响应 ====================
    // ==================== Exception Response ====================

    @Test
    @DisplayName("Should throw on null response body")
    void shouldThrowOnNullResponseBody() {
        // 给定 / Given
        when(restTemplate.postForObject(any(), any(), eq(Map.class))).thenReturn(null);

        // 当&那么 / When & Then
        assertThatThrownBy(() -> adapter.generate("Some text"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    @DisplayName("Should throw when embeddings key missing")
    void shouldThrowWhenEmbeddingsKeyMissing() {
        // 给定 / Given
        Map<String, Object> response = Map.of("modelUsed", "model");
        when(restTemplate.postForObject(any(), any(), eq(Map.class))).thenReturn(response);

        // 当&那么 / When & Then
        assertThatThrownBy(() -> adapter.generate("Some text"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("empty embeddings");
    }

    @Test
    @DisplayName("Should throw when embeddings list empty")
    void shouldThrowWhenEmbeddingsListEmpty() {
        // 给定 / Given
        Map<String, Object> response = Map.of("embeddings", List.of());
        when(restTemplate.postForObject(any(), any(), eq(Map.class))).thenReturn(response);

        // 当&那么 / When & Then
        assertThatThrownBy(() -> adapter.generate("Some text"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("empty embeddings");
    }

    @Test
    @DisplayName("Should throw when first embedding is null")
    void shouldThrowWhenFirstEmbeddingIsNull() {
        // 给定 / Given
        Map<String, Object> response = Map.of("embeddings", List.of((Object) null));
        when(restTemplate.postForObject(any(), any(), eq(Map.class))).thenReturn(response);

        // 当&那么 / When & Then
        assertThatThrownBy(() -> adapter.generate("Some text"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("empty embedding at index 0");
    }

    // ==================== 网络异常 ====================
    // ==================== Network Exception ====================

    @Test
    @DisplayName("Should throw AiServiceUnavailableException on ResourceAccessException")
    void shouldThrowAiServiceUnavailableExceptionOnResourceAccessException() {
        // 给定 / Given
        when(restTemplate.postForObject(any(), any(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> adapter.generate("Some text"))
                .isInstanceOf(AiServiceUnavailableException.class);
    }

    @Test
    @DisplayName("Should throw RuntimeException on RestClientException")
    void shouldThrowRuntimeExceptionOnRestClientException() {
        // 给定 / Given
        when(restTemplate.postForObject(any(), any(), eq(Map.class)))
                .thenThrow(new RestClientException("400 Bad Request"));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> adapter.generate("Some text"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate embedding");
    }
}
