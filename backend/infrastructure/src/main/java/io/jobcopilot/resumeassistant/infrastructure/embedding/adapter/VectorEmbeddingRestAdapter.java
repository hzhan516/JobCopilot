package io.jobcopilot.resumeassistant.infrastructure.embedding.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.domain.embedding.port.VectorEmbeddingPort;
import io.jobcopilot.resumeassistant.domain.shared.exception.AiServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Infrastructure adapter that calls the AI Service embedding endpoint via REST.
 * Implements VectorEmbeddingPort so the domain layer stays free of HTTP and Spring.
 * 通过 REST 调用 AI 服务嵌入端点的基础设施适配器。实现 VectorEmbeddingPort，使领域层与 HTTP 和 Spring 解耦。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorEmbeddingRestAdapter implements VectorEmbeddingPort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    /**
     * {@inheritDoc}
     *
     * <p>Integration contract (implemented by the AI side) / 对接契约（由 AI 端实现）：
     * <ul>
     *   <li>Endpoint: POST /api/v1/ai/embeddings</li>
     *   <li>Request body: {"texts": ["..."], "model": "..."}</li>
     *   <li>Response body: {"embeddings": [[0.1, 0.2, ...]], "modelUsed": "...", "count": 1}</li>
     * </ul>
     */
    @Override
    public float[] generate(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text for embedding generation must not be blank");
        }

        String url = aiServiceBaseUrl + "/api/v1/ai/embeddings";
        log.debug("Calling AI service embedding endpoint: {}", url);

        try {
            Map<String, Object> requestBody = Map.of("texts", List.of(text));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = restTemplate.postForObject(url, entity, Map.class);

            if (responseBody == null) {
                throw new RuntimeException("AI service returned empty response for embedding request");
            }

            @SuppressWarnings("unchecked")
            List<List<Number>> embeddingsList = (List<List<Number>>) responseBody.get("embeddings");
            if (embeddingsList == null || embeddingsList.isEmpty()) {
                throw new RuntimeException("AI service returned empty embeddings");
            }
            List<Number> embeddingList = embeddingsList.get(0);
            if (embeddingList == null || embeddingList.isEmpty()) {
                throw new RuntimeException("AI service returned empty embedding at index 0");
            }

            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = embeddingList.get(i).floatValue();
            }

            log.info("Successfully generated embedding, dimension: {}", embedding.length);
            return embedding;

        } catch (ResourceAccessException e) {
            log.warn("AI service unavailable at {} for embedding generation: {}", url, e.getMessage());
            throw new AiServiceUnavailableException();
        } catch (RestClientException e) {
            log.error("AI service client error during embedding generation: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }
}
