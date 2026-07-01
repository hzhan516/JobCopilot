package io.jobcopilot.resumeassistant.trigger.http.controller.admin;

import io.micrometer.core.instrument.MeterRegistry;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端监控控制器单元测试
 * Admin monitoring controller unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Monitoring Controller Tests")
class AdminMonitoringControllerTest {

    private static final String AI_BASE_URL = "http://localhost:8000";
    private static final String INTERNAL_KEY = "secret-monitoring-key";

    private MockMvc mockMvc;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AdminMonitoringController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "aiServiceBaseUrl", AI_BASE_URL);
        ReflectionTestUtils.setField(controller, "internalApiKey", INTERNAL_KEY);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should proxy GET /queues to AI service queue-stats")
    void shouldProxyQueueStats() throws Exception {
        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/queue-stats"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"queues\":{\"q1\":{\"depth\":3}}}"));

        mockMvc.perform(get("/api/admin/v1/monitoring/queues"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"queues\":{\"q1\":{\"depth\":3}}}"));
    }

    @Test
    @DisplayName("Should proxy POST /queues/{name}/purge to AI service")
    void shouldProxyPurgeQueue() throws Exception {
        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/queue/purge/ai.queue.feedback"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"status\":\"purged\"}"));

        mockMvc.perform(post("/api/admin/v1/monitoring/queues/ai.queue.feedback/purge"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"status\":\"purged\"}"));
    }

    @Test
    @DisplayName("Should proxy POST /queues/{name}/retry-dlq to AI service")
    void shouldProxyRetryDlq() throws Exception {
        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/queue/retry-dlq/ai.queue.feedback"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"status\":\"completed\"}"));

        mockMvc.perform(post("/api/admin/v1/monitoring/queues/ai.queue.feedback/retry-dlq"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"status\":\"completed\"}"));
    }

    @Test
    @DisplayName("Should forward internal API key header")
    void shouldForwardInternalApiKey() throws Exception {
        when(restTemplate.exchange(
                eq(AI_BASE_URL + "/admin/queue-stats"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenAnswer(invocation -> {
            HttpEntity<?> entity = invocation.getArgument(2);
            String header = entity.getHeaders().getFirst("X-Internal-API-Key");
            return ResponseEntity.status(HttpStatus.OK).body(header);
        });

        mockMvc.perform(get("/api/admin/v1/monitoring/queues"))
                .andExpect(status().isOk())
                .andExpect(content().string(INTERNAL_KEY));
    }
}
