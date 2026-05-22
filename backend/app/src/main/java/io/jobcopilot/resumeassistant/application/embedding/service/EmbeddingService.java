package io.jobcopilot.resumeassistant.application.embedding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.domain.shared.exception.AiServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 嵌入向量生成服务
 * Embedding generation service
 * <p>
 * 负责调用 AI Service 将文本转换为向量。
 * Responsible for calling the AI Service to convert text into embedding vectors.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    /**
     * 调用 AI Service 生成单条文本的嵌入向量
     * Call AI Service to generate embedding vector for a single text
     *
     * <p>对接契约 / Integration contract（由 AI 端实现）：
     * <ul>
     *   <li>Endpoint: POST /api/v1/ai/embeddings</li>
     *   <li>Request body: {"texts": ["..."], "model": "..."}</li>
     *   <li>Response body: {"embeddings": [[0.1, 0.2, ...]], "modelUsed": "...", "count": 1}</li>
     * </ul>
     *
     * @param text 输入文本 / Input text
     * @return 嵌入向量 / Embedding vector
     * @throws AiServiceUnavailableException 当 AI 服务不可用时 / When AI service is unavailable
     */
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
