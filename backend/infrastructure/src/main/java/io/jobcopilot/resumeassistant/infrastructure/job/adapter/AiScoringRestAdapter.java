package io.jobcopilot.resumeassistant.infrastructure.job.adapter;

import io.jobcopilot.resumeassistant.domain.job.port.AiScoringPort;
import io.jobcopilot.resumeassistant.domain.shared.exception.AiServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * REST adapter for the AI scoring service.
 * Sends resume-job pairs to the external AI suitability endpoint.
 * AI 评分服务的 REST 适配器。将简历-职位对发送至外部 AI 适配度端点。
 */
@Slf4j
@Component
public class AiScoringRestAdapter implements AiScoringPort {

    private final RestTemplate restTemplate;
    private final String aiServiceBaseUrl;

    public AiScoringRestAdapter(RestTemplate restTemplate,
                                @Value("${ai.service.base-url:http://localhost:8000}") String aiServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.aiServiceBaseUrl = aiServiceBaseUrl;
    }

    @Override
    public Map<String, Object> score(String jobId, String resumeVersionId,
                                     Map<String, Object> resume, Map<String, Object> job,
                                     Float semanticMatch) {
        String url = aiServiceBaseUrl + "/api/v1/suitability";

        Map<String, Object> request = new HashMap<>();
        request.put("resume", resume);
        request.put("job", job);
        if (semanticMatch != null) {
            request.put("semanticMatch", semanticMatch);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        log.info("Calling AI service suitability endpoint for jobId={}, resumeVersionId={}",
                jobId, resumeVersionId);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response == null) {
                throw new RuntimeException("AI service returned empty response");
            }
            return response;
        } catch (ResourceAccessException e) {
            log.warn("AI service unavailable at {} for jobId={}: {}", url, jobId, e.getMessage());
            throw new AiServiceUnavailableException();
        }
    }
}
