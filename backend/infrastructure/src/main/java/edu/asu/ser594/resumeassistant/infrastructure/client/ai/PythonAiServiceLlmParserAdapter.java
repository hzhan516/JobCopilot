package edu.asu.ser594.resumeassistant.infrastructure.client.ai;

import edu.asu.ser594.resumeassistant.domain.job.service.LlmParserPort;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class PythonAiServiceLlmParserAdapter implements LlmParserPort {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;
    private final ObjectMapper objectMapper;

    public PythonAiServiceLlmParserAdapter(
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.aiServiceUrl = aiServiceUrl;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ParsedJobContent parse(String markdownText) {
        log.info("Delegating job parsing to Python AI service");
        String endpoint = aiServiceUrl + "/api/v1/ai/parse-job";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = Map.of(
                "markdownText", markdownText
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                return objectMapper.convertValue(data, ParsedJobContent.class);
            }
            throw new RuntimeException("Unexpected response from AI service");
        } catch (Exception e) {
            log.error("Failed to parse job content via AI service: {}", e.getMessage());
            throw new RuntimeException("AI Parsing failed: " + e.getMessage(), e);
        }
    }
}
