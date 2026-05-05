package edu.asu.ser594.resumeassistant.trigger.http.controller.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.UpdateJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.api.matching.facade.MatchingFacade;
import edu.asu.ser594.resumeassistant.api.shared.service.ExceptionMessageResolver;
import edu.asu.ser594.resumeassistant.domain.shared.service.MessageProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JobController 契约测试 / Job controller contract test
 * <p>
 * 验证 Request DTO 的反序列化和 Response DTO 的序列化格式，不测试真实业务逻辑。
 * Validates Request DTO deserialization and Response DTO serialization formats
 * without testing real business logic.
 */
@WebMvcTest(JobController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JobControllerContractTest.TestConfig.class)
@DisplayName("Job Controller Contract Tests")
class JobControllerContractTest {

    private static final UUID TEST_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String TEST_JOB_ID = "job-test-123";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private JobFacade jobFacade;
    @MockitoBean
    private MatchingFacade matchingFacade;

    @Test
    @DisplayName("Should deserialize multipart submit request and serialize JobResponse correctly")
    void shouldDeserializeMultipartSubmitRequestAndSerializeJobResponse() throws Exception {
        // Given
        JobResponse response = new JobResponse(
                TEST_JOB_ID,
                TEST_USER_ID.toString(),
                "https://example.com/job",
                "PARSING",
                null,
                false,
                null
        );
        when(jobFacade.submitJob(any(), any(SubmitJobRequest.class))).thenReturn(response);

        MockMultipartFile screenshot = new MockMultipartFile(
                "screenshot", "screenshot.png", "image/png", "fake-image".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/v1/jobs")
                        .file(screenshot)
                        .param("url", "https://example.com/job")
                        .requestAttr("userId", TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(TEST_JOB_ID))
                .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.data.originalUrl").value("https://example.com/job"))
                .andExpect(jsonPath("$.data.status").value("PARSING"));
    }

    @Test
    @DisplayName("Should serialize JobResponse with nested parsed content correctly")
    void shouldSerializeJobResponseWithNestedParsedContent() throws Exception {
        // Given
        JobResponse.ParsedJobContentResponse parsedContent = new JobResponse.ParsedJobContentResponse(
                "Senior Java Developer",
                "Example Corp",
                "$120k - $150k",
                "Remote",
                "Build scalable backend services.",
                List.of("Java", "Spring Boot", "Kubernetes")
        );
        JobResponse response = new JobResponse(
                TEST_JOB_ID,
                TEST_USER_ID.toString(),
                "https://example.com/job",
                "COMPLETED",
                parsedContent,
                false,
                null
        );
        when(jobFacade.getJob(eq(TEST_JOB_ID), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/v1/jobs/{jobId}", TEST_JOB_ID)
                        .requestAttr("userId", TEST_USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_JOB_ID))
                .andExpect(jsonPath("$.data.parsedContent.title").value("Senior Java Developer"))
                .andExpect(jsonPath("$.data.parsedContent.company").value("Example Corp"))
                .andExpect(jsonPath("$.data.parsedContent.salary").value("$120k - $150k"))
                .andExpect(jsonPath("$.data.parsedContent.location").value("Remote"))
                .andExpect(jsonPath("$.data.parsedContent.description").value("Build scalable backend services."))
                .andExpect(jsonPath("$.data.parsedContent.requirements[0]").value("Java"))
                .andExpect(jsonPath("$.data.parsedContent.requirements[1]").value("Spring Boot"))
                .andExpect(jsonPath("$.data.parsedContent.requirements[2]").value("Kubernetes"));
    }

    @Test
    @DisplayName("Should deserialize UpdateJobRequest JSON correctly")
    void shouldDeserializeUpdateJobRequestJsonCorrectly() throws Exception {
        // Given
        UpdateJobRequest request = new UpdateJobRequest(
                "Updated Title",
                "Updated Corp",
                "$100k",
                "New York",
                "Updated description.",
                List.of("Python", "AWS")
        );
        JobResponse response = new JobResponse(
                TEST_JOB_ID,
                TEST_USER_ID.toString(),
                "https://example.com/job",
                "COMPLETED",
                new JobResponse.ParsedJobContentResponse(
                        request.title(),
                        request.company(),
                        request.salary(),
                        request.location(),
                        request.description(),
                        request.requirements()
                ),
                false,
                null
        );
        when(jobFacade.updateJob(eq(TEST_JOB_ID), any(), any(UpdateJobRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/v1/jobs/{jobId}", TEST_JOB_ID)
                        .requestAttr("userId", TEST_USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_JOB_ID))
                .andExpect(jsonPath("$.data.parsedContent.title").value("Updated Title"))
                .andExpect(jsonPath("$.data.parsedContent.company").value("Updated Corp"))
                .andExpect(jsonPath("$.data.parsedContent.requirements[0]").value("Python"))
                .andExpect(jsonPath("$.data.parsedContent.requirements[1]").value("AWS"));
    }

    /**
     * 测试专用的最小化 Spring Boot 配置
     * Minimal Spring Boot configuration for testing.
     * <p>
     * 启用当前 controller 包的组件扫描，确保 @WebMvcTest 能加载 JobController；
     * 同时提供 @ControllerAdvice 所需的领域层接口 Stub 实现。
     * Enables component scanning for the current controller package so @WebMvcTest
     * can load JobController; provides stub implementations for domain-layer interfaces
     * required by @ControllerAdvice.
     */
    @SpringBootConfiguration
    @ComponentScan(basePackages = "edu.asu.ser594.resumeassistant.trigger.http.controller.job")
    static class TestConfig {

        @Bean
        public MessageProvider messageProvider() {
            return new MessageProvider() {
                @Override
                public String getMessage(String key) {
                    return key;
                }

                @Override
                public String getMessage(String key, Object... args) {
                    return key;
                }
            };
        }

        @Bean
        public ExceptionMessageResolver exceptionMessageResolver() {
            return errorType -> errorType != null ? errorType.name() : "UNKNOWN";
        }
    }
}
