package io.jobcopilot.resumeassistant.application.matching.service;

import io.jobcopilot.resumeassistant.api.job.dto.response.MatchItem;
import io.jobcopilot.resumeassistant.application.matching.command.SaveMatchResultCommand;
import io.jobcopilot.resumeassistant.application.matching.command.StartJobMatchCommand;
import io.jobcopilot.resumeassistant.application.matching.query.GetMatchResultQuery;
import io.jobcopilot.resumeassistant.application.matching.query.ListMatchHistoryQuery;
import io.jobcopilot.resumeassistant.domain.embedding.entity.ResumeVector;
import io.jobcopilot.resumeassistant.domain.embedding.port.VectorGenerationPort;
import io.jobcopilot.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import io.jobcopilot.resumeassistant.domain.matching.entity.JobMatchResult;
import io.jobcopilot.resumeassistant.domain.matching.entity.MatchingModel;
import io.jobcopilot.resumeassistant.domain.matching.exception.ResumeVectorNotReadyException;
import io.jobcopilot.resumeassistant.domain.matching.repository.JobMatchResultRepository;
import io.jobcopilot.resumeassistant.domain.matching.repository.MatchingModelRepository;
import io.jobcopilot.resumeassistant.domain.matching.valueobject.ModelType;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 职位匹配应用服务单元测试（Orchestrator 层）
 * Tests MatchingApplicationService as the orchestrator; the transactional core
 * (recall + persist + outbox-write) is covered by MatchTransactionServiceTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Matching Application Service Tests")
class MatchingApplicationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String RESUME_VERSION_ID = UUID.randomUUID().toString();
    private static final String MATCH_ID = "match-001";

    @Mock
    private ResumeVectorRepository resumeVectorRepository;

    @Mock
    private JobMatchResultRepository jobMatchResultRepository;

    @Mock
    private MatchingModelRepository matchingModelRepository;

    @Mock
    private VectorGenerationPort vectorGenerationPort;

    @Mock
    private ResumeVersionRepository resumeVersionRepository;

    @Mock
    private MatchTransactionService matchTransactionService;

    @InjectMocks
    private MatchingApplicationService matchingService;

    @Test
    @DisplayName("Should delegate to MatchTransactionService when vector is ready")
    void shouldDelegateToMatchTransactionServiceWhenVectorIsReady() {
        // Given
        StartJobMatchCommand command = StartJobMatchCommand.builder()
                .userId(USER_ID)
                .resumeVersionId(RESUME_VERSION_ID)
                .query("Java Developer")
                .topK(5)
                .build();

        ResumeVector vector = ResumeVector.createCompleted("vec-1", RESUME_VERSION_ID, new float[]{0.1f, 0.2f});
        MatchingModel model = MatchingModel.builder().id(1L).version("v1.0").type(ModelType.RECALL).build();

        when(matchingModelRepository.findActiveByType(ModelType.RECALL)).thenReturn(Optional.of(model));
        when(resumeVectorRepository.findByResumeVersionId(RESUME_VERSION_ID)).thenReturn(Optional.of(vector));
        when(matchTransactionService.execute(any(), anyString(), eq("v1.0"))).thenReturn(MATCH_ID);

        // When
        String result = matchingService.startJobMatch(command);

        // Then
        assertThat(result).isEqualTo(MATCH_ID);
        verify(matchTransactionService).execute(eq(command), anyString(), eq("v1.0"));
        // Note: result is a UUID generated inside startJobMatch, so we capture it dynamically
    }

    @Test
    @DisplayName("Should use default model version when no active model")
    void shouldUseDefaultModelVersionWhenNoActiveModel() {
        // Given
        StartJobMatchCommand command = StartJobMatchCommand.builder()
                .userId(USER_ID)
                .resumeVersionId(RESUME_VERSION_ID)
                .query("Java")
                .build();

        ResumeVector vector = ResumeVector.createCompleted("vec-1", RESUME_VERSION_ID, new float[]{0.1f});

        when(matchingModelRepository.findActiveByType(ModelType.RECALL)).thenReturn(Optional.empty());
        when(resumeVectorRepository.findByResumeVersionId(RESUME_VERSION_ID)).thenReturn(Optional.of(vector));
        when(matchTransactionService.execute(any(), anyString(), eq("default"))).thenReturn(MATCH_ID);

        // When
        matchingService.startJobMatch(command);

        // Then
        verify(matchTransactionService).execute(any(), anyString(), eq("default"));
    }

    @Test
    @DisplayName("Should throw ResumeVectorNotReadyException when resume vector not found and trigger re-gen")
    void shouldThrowResumeVectorNotReadyExceptionWhenVectorNotFound() {
        // Given
        StartJobMatchCommand command = StartJobMatchCommand.builder()
                .userId(USER_ID)
                .resumeVersionId(RESUME_VERSION_ID)
                .query("Java")
                .build();

        ResumeVersion resumeVersion = ResumeVersion.reconstruct(
                UUID.fromString(RESUME_VERSION_ID), UUID.randomUUID(), ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                "resume content", null, ParseStatus.COMPLETED, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        when(matchingModelRepository.findActiveByType(ModelType.RECALL)).thenReturn(Optional.empty());
        when(resumeVectorRepository.findByResumeVersionId(RESUME_VERSION_ID)).thenReturn(Optional.empty());
        when(resumeVersionRepository.findById(UUID.fromString(RESUME_VERSION_ID))).thenReturn(Optional.of(resumeVersion));
        doNothing().when(vectorGenerationPort).generateAndSaveVector(anyString(), anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> matchingService.startJobMatch(command))
                .isInstanceOf(ResumeVectorNotReadyException.class)
                .hasMessageContaining("matching.resume.vector.not.ready");

        verify(vectorGenerationPort).generateAndSaveVector(eq(RESUME_VERSION_ID), eq("RESUME"), eq("resume content"));
        verifyNoInteractions(matchTransactionService);
    }

    @Test
    @DisplayName("Should throw ResumeVectorNotReadyException when embedding is null and trigger re-gen")
    void shouldThrowResumeVectorNotReadyExceptionWhenEmbeddingIsNull() {
        // Given
        StartJobMatchCommand command = StartJobMatchCommand.builder()
                .userId(USER_ID)
                .resumeVersionId(RESUME_VERSION_ID)
                .query("Java")
                .build();

        ResumeVector vector = ResumeVector.createFailed("vec-1", RESUME_VERSION_ID, "parse error");

        ResumeVersion resumeVersion = ResumeVersion.reconstruct(
                UUID.fromString(RESUME_VERSION_ID), UUID.randomUUID(), ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                "resume content", null, ParseStatus.COMPLETED, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        when(matchingModelRepository.findActiveByType(ModelType.RECALL)).thenReturn(Optional.empty());
        when(resumeVectorRepository.findByResumeVersionId(RESUME_VERSION_ID)).thenReturn(Optional.of(vector));
        when(resumeVersionRepository.findById(UUID.fromString(RESUME_VERSION_ID))).thenReturn(Optional.of(resumeVersion));
        doNothing().when(vectorGenerationPort).generateAndSaveVector(anyString(), anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> matchingService.startJobMatch(command))
                .isInstanceOf(ResumeVectorNotReadyException.class)
                .hasMessageContaining("matching.resume.vector.not.ready");

        verify(vectorGenerationPort).generateAndSaveVector(eq(RESUME_VERSION_ID), eq("RESUME"), anyString());
        verifyNoInteractions(matchTransactionService);
    }

    @Test
    @DisplayName("Should save match result successfully")
    void shouldSaveMatchResultSuccessfully() {
        // Given
        SaveMatchResultCommand command = SaveMatchResultCommand.builder()
                .matchId(MATCH_ID)
                .rankedResults(List.of(new MatchItem("job-1", "Title", "Company", 0.9, null, "Desc", "Reason")))
                .rankTimeMs(100L)
                .build();

        JobMatchResult result = JobMatchResult.createProcessing(MATCH_ID, USER_ID, RESUME_VERSION_ID, "query", "v1");
        when(jobMatchResultRepository.findById(MATCH_ID)).thenReturn(Optional.of(result));

        // When
        matchingService.saveMatchResult(command);

        // Then
        verify(jobMatchResultRepository).save(result);
    }

    @Test
    @DisplayName("Should throw when match result not found for save")
    void shouldThrowWhenMatchResultNotFoundForSave() {
        // Given
        SaveMatchResultCommand command = SaveMatchResultCommand.builder()
                .matchId(MATCH_ID)
                .rankedResults(Collections.emptyList())
                .rankTimeMs(0L)
                .build();
        when(jobMatchResultRepository.findById(MATCH_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> matchingService.saveMatchResult(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Match result not found");
    }

    @Test
    @DisplayName("Should get match result")
    void shouldGetMatchResult() {
        // Given
        JobMatchResult result = JobMatchResult.createProcessing(MATCH_ID, USER_ID, RESUME_VERSION_ID, "query", "v1");
        when(jobMatchResultRepository.findById(MATCH_ID)).thenReturn(Optional.of(result));

        // When
        Optional<JobMatchResult> found = matchingService.getMatchResult(new GetMatchResultQuery(MATCH_ID));

        // Then
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("Should list match history")
    void shouldListMatchHistory() {
        // Given
        List<JobMatchResult> results = List.of(
                JobMatchResult.createProcessing("m1", USER_ID, RESUME_VERSION_ID, "q1", "v1")
        );
        when(jobMatchResultRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(results);

        // When
        List<JobMatchResult> history = matchingService.listMatchHistory(new ListMatchHistoryQuery(USER_ID));

        // Then
        assertThat(history).hasSize(1);
    }
}
