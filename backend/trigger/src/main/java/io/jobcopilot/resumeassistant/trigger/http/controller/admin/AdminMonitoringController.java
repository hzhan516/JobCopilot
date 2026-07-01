package io.jobcopilot.resumeassistant.trigger.http.controller.admin;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** 管理端监控 API / Admin monitoring API */
@Slf4j
@RestController
@RequestMapping("/api/admin/v1/monitoring")
@RequiredArgsConstructor
public class AdminMonitoringController {

    private final MeterRegistry meterRegistry;
    private final RestTemplate restTemplate;

    @Value("${ai.service.base-url}")
    private String aiServiceBaseUrl;

    @Value("${app.internal-api-key}")
    private String internalApiKey;

    @GetMapping("/metrics")
    public ApiResponse<Map<String, Object>> getMetrics() {
        var regCounter = meterRegistry.find("jobcopilot.users.registrations").counter();
        var aiTimer = meterRegistry.find("jobcopilot.ai.parsing.duration").timer();
        return ApiResponse.success(Map.of(
                "registrations", regCounter != null ? regCounter.count() : 0,
                "aiParsingCount", aiTimer != null ? aiTimer.count() : 0
        ));
    }

    @GetMapping("/errors")
    public ApiResponse<java.util.List<String>> getErrors() {
        return ApiResponse.success(java.util.List.of());
    }

    @GetMapping("/queues")
    public ResponseEntity<String> getQueues() {
        return proxy(HttpMethod.GET, "/admin/queue-stats");
    }

    @PostMapping("/queues/{name}/purge")
    public ResponseEntity<String> purgeQueue(@PathVariable String name) {
        return proxy(HttpMethod.POST, "/admin/queue/purge/" + name);
    }

    @PostMapping("/queues/{name}/retry-dlq")
    public ResponseEntity<String> retryDlq(@PathVariable String name) {
        return proxy(HttpMethod.POST, "/admin/queue/retry-dlq/" + name);
    }

    private ResponseEntity<String> proxy(HttpMethod method, String path) {
        String url = aiServiceBaseUrl.replaceAll("/$", "") + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-API-Key", internalApiKey);
        }
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        log.debug("Proxying monitoring request: {} {}", method, url);
        return restTemplate.exchange(url, method, entity, String.class);
    }
}
