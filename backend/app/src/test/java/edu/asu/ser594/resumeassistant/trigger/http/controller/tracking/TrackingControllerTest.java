package edu.asu.ser594.resumeassistant.trigger.http.controller.tracking;

import edu.asu.ser594.resumeassistant.api.tracking.dto.request.CreateTrackingRequest;
import edu.asu.ser594.resumeassistant.api.tracking.dto.response.TrackingResponse;
import edu.asu.ser594.resumeassistant.api.tracking.dto.response.TrackingStatsResponse;
import edu.asu.ser594.resumeassistant.api.tracking.facade.TrackingFacade;
import edu.asu.ser594.resumeassistant.trigger.http.security.CurrentUserArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 求职申请跟踪控制器单元测试 (Standalone MockMvc)
 * Tracking controller unit tests using standalone MockMvc
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tracking Controller Tests")
class TrackingControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TRACKING_ID = "tracking-001";

    private MockMvc mockMvc;

    @Mock
    private TrackingFacade trackingFacade;

    @InjectMocks
    private TrackingController trackingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(trackingController)
                .setCustomArgumentResolvers(new TestCurrentUserArgumentResolver(USER_ID))
                .build();
    }

    @Test
    @DisplayName("Should create tracking")
    void shouldCreateTracking() throws Exception {
        // 给定
        // Given
        when(trackingFacade.createTracking(eq(USER_ID), any(CreateTrackingRequest.class)))
                .thenReturn(new TrackingResponse(TRACKING_ID, USER_ID.toString(), null, "Company", "Title", "PENDING", null, null, null, null));

        // 当&那么
        // When&Then
        mockMvc.perform(post("/v1/trackings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\":\"job-1\",\"companyName\":\"Company\",\"jobTitle\":\"Title\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackingId").value(TRACKING_ID));
    }

    @Test
    @DisplayName("Should list trackings")
    void shouldListTrackings() throws Exception {
        // 给定
        // Given
        when(trackingFacade.listTrackings(USER_ID, null))
                .thenReturn(List.of(
                        new TrackingResponse(TRACKING_ID, USER_ID.toString(), null, "C1", "J1", "PENDING", null, null, null, null)
                ));

        // 当&那么
        // When&Then
        mockMvc.perform(get("/v1/trackings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("Should get tracking stats")
    void shouldGetTrackingStats() throws Exception {
        // 给定
        // Given
        when(trackingFacade.getStats(USER_ID))
                .thenReturn(new TrackingStatsResponse(5L, 1L, 2L, 1L, 1L, 0L, 0L, 20.0));

        // 当&那么
        // When&Then
        mockMvc.perform(get("/v1/trackings/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalApplications").value(5));
    }

    /**
     * 测试用的当前用户参数解析器
     * Test current user argument resolver
     */
    private static class TestCurrentUserArgumentResolver extends CurrentUserArgumentResolver {
        private final UUID userId;

        TestCurrentUserArgumentResolver(UUID userId) {
            this.userId = userId;
        }

        @Override
        public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                      org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                      org.springframework.web.context.request.NativeWebRequest webRequest,
                                      org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return userId;
        }
    }
}
