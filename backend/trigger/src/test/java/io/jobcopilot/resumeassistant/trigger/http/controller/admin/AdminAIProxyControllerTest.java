package io.jobcopilot.resumeassistant.trigger.http.controller.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 服务管理代理控制器单元测试
 * Admin AI proxy controller unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Admin AI Proxy Controller Tests")
class AdminAIProxyControllerTest {

    private static final String AI_BASE_URL = "http://localhost:8000";
    private static final String INTERNAL_KEY = "secret-ai-key";

    private MockMvc mockMvc;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AdminAIProxyController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "aiServiceBaseUrl", AI_BASE_URL);
        ReflectionTestUtils.setField(controller, "internalApiKey", INTERNAL_KEY);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should proxy GET /status to AI service")
    void shouldProxyStatus() throws Exception {
        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/status"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"version\":\"0.2.0\"}"));

        mockMvc.perform(get("/api/admin/v1/ai/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"version\":\"0.2.0\"}"));
    }

    @Test
    @DisplayName("Should proxy GET /model/info to AI service")
    void shouldProxyModelInfo() throws Exception {
        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/model/info"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"loaded\":true}"));

        mockMvc.perform(get("/api/admin/v1/ai/model/info"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"loaded\":true}"));
    }

    @Test
    @DisplayName("Should proxy POST /model/retrain to AI service")
    void shouldProxyRetrain() throws Exception {
        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/model/retrain"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"status\":\"completed\"}"));

        mockMvc.perform(post("/api/admin/v1/ai/model/retrain"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"status\":\"completed\"}"));
    }

    @Test
    @DisplayName("Should proxy POST /model/rollback with request body")
    void shouldProxyRollback() throws Exception {
        String requestBody = "{\"version\":\"v42\"}";
        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/model/rollback"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"status\":\"rolled_back\"}"));

        mockMvc.perform(post("/api/admin/v1/ai/model/rollback")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"status\":\"rolled_back\"}"));
    }

    @Test
    @DisplayName("Should proxy POST /cache/flush to AI service")
    void shouldProxyCacheFlush() throws Exception {
        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/cache/flush"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"status\":\"flushed\"}"));

        mockMvc.perform(post("/api/admin/v1/ai/cache/flush"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"status\":\"flushed\"}"));
    }

    @Test
    @DisplayName("Should proxy queue stats and queue management endpoints")
    void shouldProxyQueueEndpoints() throws Exception {
        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/queue-stats"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"queues\":{}}"));

        mockMvc.perform(get("/api/admin/v1/ai/queues"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"queues\":{}}"));

        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/queue/purge/ai.queue.job.parse"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"status\":\"purged\"}"));

        mockMvc.perform(post("/api/admin/v1/ai/queues/ai.queue.job.parse/purge"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"status\":\"purged\"}"));

        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/queue/retry-dlq/ai.queue.job.parse"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"status\":\"completed\"}"));

        mockMvc.perform(post("/api/admin/v1/ai/queues/ai.queue.job.parse/retry-dlq"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"status\":\"completed\"}"));
    }

    @Test
    @DisplayName("Should forward internal API key header")
    void shouldForwardInternalApiKey() throws Exception {
        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/status"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenAnswer(invocation -> {
            HttpEntity<?> entity = invocation.getArgument(2);
            String header = entity.getHeaders().getFirst("X-Internal-API-Key");
            return ResponseEntity.status(HttpStatus.OK).body(header);
        });

        mockMvc.perform(get("/api/admin/v1/ai/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(INTERNAL_KEY));
    }
}
