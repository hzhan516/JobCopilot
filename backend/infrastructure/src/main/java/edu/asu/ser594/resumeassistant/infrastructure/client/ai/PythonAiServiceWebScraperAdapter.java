package edu.asu.ser594.resumeassistant.infrastructure.client.ai;

import edu.asu.ser594.resumeassistant.domain.job.service.WebScraperPort;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ScrapeResult;
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
public class PythonAiServiceWebScraperAdapter implements WebScraperPort {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public PythonAiServiceWebScraperAdapter(
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.aiServiceUrl = aiServiceUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ScrapeResult scrape(String url, boolean captureScreenshot) {
        log.info("Delegating web scraping to Python AI service: {} (screenshot={})", url, captureScreenshot);
        String endpoint = aiServiceUrl + "/api/v1/ai/scrape";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = Map.of(
                "url", url,
                "captureScreenshot", captureScreenshot
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                String markdown = (String) data.get("markdownText");
                String screenshotUrl = (String) data.get("screenshotUrl");
                return new ScrapeResult(markdown != null ? markdown : "", screenshotUrl);
            }
            throw new RuntimeException("Unexpected response format from AI service");
        } catch (Exception e) {
            log.error("Failed to scrape URL via AI service: {}", url, e);
            throw new RuntimeException("AI Scraping failed: " + e.getMessage(), e);
        }
    }
}
