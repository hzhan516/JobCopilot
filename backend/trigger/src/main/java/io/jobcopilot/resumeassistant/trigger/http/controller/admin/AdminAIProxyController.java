package io.jobcopilot.resumeassistant.trigger.http.controller.admin;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * AI 服务管理端点代理 / Proxy for AI service admin endpoints.
 * <p>
 * 前端统一访问后端 /api/admin/v1/ai/*，由后端转发到 AI 服务 /admin/*，
 * 并附加内部 API Key 进行服务间认证。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/v1/ai")
@RequiredArgsConstructor
public class AdminAIProxyController {

    private final RestTemplate restTemplate;

    @Value("${ai.service.base-url}")
    private String aiServiceBaseUrl;

    @Value("${app.internal-api-key}")
    private String internalApiKey;

    @GetMapping("/status")
    public ResponseEntity<String> aiStatus() {
        return proxy(HttpMethod.GET, "/admin/status", null);
    }

    @GetMapping("/model/info")
    public ResponseEntity<String> modelInfo() {
        return proxy(HttpMethod.GET, "/admin/model/info", null);
    }

    @GetMapping("/model/history")
    public ResponseEntity<String> modelHistory() {
        return proxy(HttpMethod.GET, "/admin/model/history", null);
    }

    @PostMapping("/model/retrain")
    public ResponseEntity<String> retrain() {
        return proxy(HttpMethod.POST, "/admin/model/retrain", null);
    }

    @PostMapping("/model/rollback")
    public ResponseEntity<String> rollback(@RequestBody String body) {
        return proxy(HttpMethod.POST, "/admin/model/rollback", body);
    }

    @PostMapping("/cache/flush")
    public ResponseEntity<String> flushCache() {
        return proxy(HttpMethod.POST, "/admin/cache/flush", null);
    }

    @GetMapping("/queues")
    public ResponseEntity<String> queueStats() {
        return proxy(HttpMethod.GET, "/admin/queue-stats", null);
    }

    @PostMapping("/queues/{name}/purge")
    public ResponseEntity<String> purgeQueue(@PathVariable String name) {
        return proxy(HttpMethod.POST, "/admin/queue/purge/" + name, null);
    }

    @PostMapping("/queues/{name}/retry-dlq")
    public ResponseEntity<String> retryDlq(@PathVariable String name) {
        return proxy(HttpMethod.POST, "/admin/queue/retry-dlq/" + name, null);
    }

    private ResponseEntity<String> proxy(HttpMethod method, String path, String body) {
        String url = aiServiceBaseUrl.replaceAll("/$", "") + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-API-Key", internalApiKey);
        }
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        log.debug("Proxying AI admin request: {} {}", method, url);
        return restTemplate.exchange(url, method, entity, String.class);
    }
}
