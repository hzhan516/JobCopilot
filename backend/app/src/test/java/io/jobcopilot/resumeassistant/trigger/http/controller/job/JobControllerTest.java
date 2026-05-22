package io.jobcopilot.resumeassistant.trigger.http.controller.job;

import io.jobcopilot.resumeassistant.api.job.dto.request.JobMatchRequest;
import io.jobcopilot.resumeassistant.api.job.dto.request.SubmitJobRequest;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobMatchResponse;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobResponse;
import io.jobcopilot.resumeassistant.api.job.facade.JobFacade;
import io.jobcopilot.resumeassistant.api.matching.facade.MatchingFacade;
import io.jobcopilot.resumeassistant.trigger.http.security.CurrentUserArgumentResolver;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 职位控制器单元测试 (Standalone MockMvc)
 * Job controller unit tests using standalone MockMvc
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Job Controller Tests")
class JobControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String MATCH_ID = "match-001";

    private MockMvc mockMvc;

    @Mock
    private JobFacade jobFacade;

    @Mock
    private MatchingFacade matchingFacade;

    @InjectMocks
    private JobController jobController;

    @BeforeEach
    void setUp() {
        CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();
        mockMvc = MockMvcBuilders.standaloneSetup(jobController)
                .setCustomArgumentResolvers(new TestCurrentUserArgumentResolver(USER_ID))
                .build();
    }

    @Test
    @DisplayName("Should submit job")
    void shouldSubmitJob() throws Exception {
        // 给定
        // Given
        when(jobFacade.submitJob(eq(USER_ID), any(SubmitJobRequest.class)))
                .thenReturn(new JobResponse("job-1", USER_ID.toString(), "http://example.com", "COMPLETED", null, false, null));

        MockMultipartFile screenshot = new MockMultipartFile(
                "screenshot", "job.png", MediaType.IMAGE_PNG_VALUE, "fake-image-data".getBytes()
        );

        // 当&那么
        // When&Then
        mockMvc.perform(multipart("/v1/jobs")
                        .file(screenshot)
                        .param("url", "http://example.com/job"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("job-1"));
    }

    @Test
    @DisplayName("Should list jobs")
    void shouldListJobs() throws Exception {
        // 给定
        // Given
        when(jobFacade.listJobs(USER_ID)).thenReturn(List.of(
                new JobResponse("job-1", USER_ID.toString(), "http://example.com", "COMPLETED", null, false, null)
        ));

        // 当&那么
        // When&Then
        mockMvc.perform(get("/v1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("Should delete job")
    void shouldDeleteJob() throws Exception {
        // 当&那么
        // When&Then
        mockMvc.perform(delete("/v1/jobs/job-1"))
                .andExpect(status().isOk());

        verify(jobFacade).deleteJob("job-1", USER_ID);
    }

    @Test
    @DisplayName("Should start job match")
    void shouldStartJobMatch() throws Exception {
        // 给定
        // Given
        when(matchingFacade.matchJobs(eq(USER_ID), any(JobMatchRequest.class)))
                .thenReturn(JobMatchResponse.processing(MATCH_ID));

        // 当&那么
        // When&Then
        mockMvc.perform(post("/v1/jobs/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeVersionId\":\"rv1\",\"query\":\"Java\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchId").value(MATCH_ID));
    }

    @Test
    @DisplayName("Should get match result")
    void shouldGetMatchResult() throws Exception {
        // 给定
        // Given
        when(matchingFacade.getMatchResult(MATCH_ID))
                .thenReturn(JobMatchResponse.processing(MATCH_ID));

        // 当&那么
        // When&Then
        mockMvc.perform(get("/v1/jobs/match/" + MATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchId").value(MATCH_ID));
    }

    @Test
    @DisplayName("Should get match history")
    void shouldGetMatchHistory() throws Exception {
        // 给定
        // Given
        when(matchingFacade.getMatchHistory(USER_ID)).thenReturn(List.of());

        // 当&那么
        // When&Then
        mockMvc.perform(get("/v1/jobs/match/history"))
                .andExpect(status().isOk());
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
        public Object resolveArgument(org.springframework.core.@NonNull MethodParameter parameter,
                                      org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                      org.springframework.web.context.request.NativeWebRequest webRequest,
                                      org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return userId;
        }
    }
}
