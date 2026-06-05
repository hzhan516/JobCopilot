package io.jobcopilot.resumeassistant.infrastructure.embedding.adapter;

import io.jobcopilot.resumeassistant.domain.embedding.port.ModelRetrainingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Infrastructure adapter that calls the AI Service model-retraining endpoint via REST.
 * Implements ModelRetrainingPort so the domain layer stays free of HTTP and Spring.
 * 通过 REST 调用 AI 服务模型重训练端点的基础设施适配器。实现 ModelRetrainingPort，使领域层与 HTTP 和 Spring 解耦。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRetrainingRestAdapter implements ModelRetrainingPort {

    private final RestTemplate restTemplate;

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    /**
     * {@inheritDoc}
     *
     * <p>Endpoint: POST /api/v1/admin/recompute-model</p>
     */
    @Override
    public void triggerRetraining() {
        String url = aiServiceBaseUrl + "/api/v1/admin/recompute-model";
        log.info("Triggering incremental model retraining at URL: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            ).getBody();

            log.info("Incremental model retraining triggered successfully: {}", response);
        } catch (Exception e) {
            log.error("Failed to trigger incremental model retraining: {}", e.getMessage());
            // Non-blocking: scheduler failure must not affect main business / 非阻塞：定时调度失败不应影响主业务
        }
    }
}
