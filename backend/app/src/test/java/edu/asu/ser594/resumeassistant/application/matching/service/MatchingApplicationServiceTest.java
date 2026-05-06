package edu.asu.ser594.resumeassistant.application.matching.service;

import edu.asu.ser594.resumeassistant.api.embedding.facade.VectorFacade;
import edu.asu.ser594.resumeassistant.api.job.dto.response.MatchItem;
import edu.asu.ser594.resumeassistant.application.matching.command.SaveMatchResultCommand;
import edu.asu.ser594.resumeassistant.application.matching.command.StartJobMatchCommand;
import edu.asu.ser594.resumeassistant.application.matching.query.GetMatchResultQuery;
import edu.asu.ser594.resumeassistant.application.matching.query.ListMatchHistoryQuery;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.ResumeVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.JobStatus;
import edu.asu.ser594.resumeassistant.domain.matching.entity.JobMatchResult;
import edu.asu.ser594.resumeassistant.domain.matching.entity.MatchingModel;
import edu.asu.ser594.resumeassistant.domain.matching.exception.ResumeVectorNotReadyException;
import edu.asu.ser594.resumeassistant.domain.matching.port.VectorSearchPort;
import edu.asu.ser594.resumeassistant.domain.matching.repository.JobMatchResultRepository;
import edu.asu.ser594.resumeassistant.domain.matching.repository.MatchingModelRepository;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.ModelType;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.RecallResult;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.resume.valueobject.ParseStatus;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobRankCommand;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 职位匹配应用服务单元测试
 * Job matching application service unit tests
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
    private JobRepository jobRepository;

    @Mock
    private JobMatchResultRepository jobMatchResultRepository;

    @Mock
    private MatchingModelRepository matchingModelRepository;

    @Mock
    private AiMessagePublisherPort aiMessagePublisherPort;

    @Mock
    private VectorFacade vectorFacade;

    @Mock
    private VectorSearchPort vectorSearchPort;

    @Mock
    private ResumeVersionRepository resumeVersionRepository;

    @InjectMocks
    private MatchingApplicationService matchingService;

    @Test
    @DisplayName("Should start job match successfully")
    void shouldStartJobMatchSuccessfully() {
        // 给定
        // Given
        StartJobMatchCommand command = StartJobMatchCommand.builder()
                .userId(USER_ID)
                .resumeVersionId(RESUME_VERSION_ID)
                .query("Java Developer")
                .topK(5)
                .build();

        ResumeVector vector = ResumeVector.createCompleted("vec-1", RESUME_VERSION_ID, new float[]{0.1f, 0.2f});
        MatchingModel model = MatchingModel.builder().id(1L).version("v1.0").type(ModelType.RECALL).build();
        List<RecallResult> recallResults = List.of(new RecallResult("job-1", 0.5));
        Job job = new Job("job-1", USER_ID, "http://example.com", false, JobStatus.COMPLETED, null, null);

        ResumeVersion resumeVersion = ResumeVersion.reconstruct(
                UUID.fromString(RESUME_VERSION_ID), UUID.randomUUID(), ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                "resume content", null, ParseStatus.COMPLETED, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        when(matchingModelRepository.findActiveByType(ModelType.RECALL)).thenReturn(Optional.of(model));
        when(resumeVectorRepository.findByResumeVersionId(RESUME_VERSION_ID)).thenReturn(Optional.of(vector));
        when(vectorSearchPort.findSimilarJobs(any(), eq(5), eq("v1.0"))).thenReturn(recallResults);
        when(jobRepository.findById("job-1")).thenReturn(Optional.of(job));
        when(resumeVersionRepository.findById(UUID.fromString(RESUME_VERSION_ID))).thenReturn(Optional.of(resumeVersion));
        when(jobMatchResultRepository.save(any(JobMatchResult.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当
        // When
        String result = matchingService.startJobMatch(command);

        // 那么
        // Then
        assertThat(result).isNotNull();
        verify(jobMatchResultRepository, times(2)).save(any(JobMatchResult.class));
        verify(aiMessagePublisherPort).sendJobForRanking(any(JobRankCommand.class));
    }

    @Test
    @DisplayName("Should use default model when no active model")
    void shouldUseDefaultModelWhenNoActiveModel() {
        // 给定
        // Given
        StartJobMatchCommand command = StartJobMatchCommand.builder()
                .userId(USER_ID)
                .resumeVersionId(RESUME_VERSION_ID)
                .query("Java")
                .topK(5)
                .build();

        ResumeVector vector = ResumeVector.createCompleted("vec-1", RESUME_VERSION_ID, new float[]{0.1f});

        ResumeVersion resumeVersion = ResumeVersion.reconstruct(
                UUID.fromString(RESUME_VERSION_ID), UUID.randomUUID(), ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                "resume content", null, ParseStatus.COMPLETED, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        when(matchingModelRepository.findActiveByType(ModelType.RECALL)).thenReturn(Optional.empty());
        when(resumeVectorRepository.findByResumeVersionId(RESUME_VERSION_ID)).thenReturn(Optional.of(vector));
        when(vectorSearchPort.findSimilarJobs(any(), anyInt(), eq("default"))).thenReturn(Collections.emptyList());
        when(resumeVersionRepository.findById(UUID.fromString(RESUME_VERSION_ID))).thenReturn(Optional.of(resumeVersion));
        when(jobMatchResultRepository.save(any(JobMatchResult.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当
        // When
        matchingService.startJobMatch(command);

        // 那么
        // Then
        verify(vectorSearchPort).findSimilarJobs(any(), anyInt(), eq("default"));
    }

    @Test
    @DisplayName("Should throw ResumeVectorNotReadyException when resume vector not found and trigger re-gen")
    void shouldThrowResumeVectorNotReadyExceptionWhenVectorNotFound() {
        // 给定
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
        when(jobMatchResultRepository.save(any(JobMatchResult.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumeVersionRepository.findById(UUID.fromString(RESUME_VERSION_ID))).thenReturn(Optional.of(resumeVersion));
        doNothing().when(vectorFacade).generateAndSaveVector(anyString(), anyString(), anyString());

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> matchingService.startJobMatch(command))
                .isInstanceOf(ResumeVectorNotReadyException.class)
                .hasMessageContaining("matching.resume.vector.not.ready");

        verify(vectorFacade).generateAndSaveVector(eq(RESUME_VERSION_ID), eq("RESUME"), eq("resume content"));
    }

    @Test
    @DisplayName("Should throw ResumeVectorNotReadyException when embedding is null and trigger re-gen")
    void shouldThrowResumeVectorNotReadyExceptionWhenEmbeddingIsNull() {
        // 给定
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
        when(jobMatchResultRepository.save(any(JobMatchResult.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumeVersionRepository.findById(UUID.fromString(RESUME_VERSION_ID))).thenReturn(Optional.of(resumeVersion));
        doNothing().when(vectorFacade).generateAndSaveVector(anyString(), anyString(), anyString());

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> matchingService.startJobMatch(command))
                .isInstanceOf(ResumeVectorNotReadyException.class)
                .hasMessageContaining("matching.resume.vector.not.ready");

        verify(vectorFacade).generateAndSaveVector(eq(RESUME_VERSION_ID), eq("RESUME"), anyString());
    }

    @Test
    @DisplayName("Should use default topK when null")
    void shouldUseDefaultTopKWhenNull() {
        // 给定
        // Given
        StartJobMatchCommand command = StartJobMatchCommand.builder()
                .userId(USER_ID)
                .resumeVersionId(RESUME_VERSION_ID)
                .query("Java")
                .topK(null)
                .build();

        ResumeVector vector = ResumeVector.createCompleted("vec-1", RESUME_VERSION_ID, new float[]{0.1f});

        ResumeVersion resumeVersion = ResumeVersion.reconstruct(
                UUID.fromString(RESUME_VERSION_ID), UUID.randomUUID(), ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                "resume content", null, ParseStatus.COMPLETED, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        when(matchingModelRepository.findActiveByType(ModelType.RECALL)).thenReturn(Optional.empty());
        when(resumeVectorRepository.findByResumeVersionId(RESUME_VERSION_ID)).thenReturn(Optional.of(vector));
        when(vectorSearchPort.findSimilarJobs(any(), eq(10), any())).thenReturn(Collections.emptyList());
        when(resumeVersionRepository.findById(UUID.fromString(RESUME_VERSION_ID))).thenReturn(Optional.of(resumeVersion));
        when(jobMatchResultRepository.save(any(JobMatchResult.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当
        // When
        matchingService.startJobMatch(command);

        // 那么
        // Then
        verify(vectorSearchPort).findSimilarJobs(any(), eq(10), any());
    }

    @Test
    @DisplayName("Should save match result successfully")
    void shouldSaveMatchResultSuccessfully() {
        // 给定
        // Given
        SaveMatchResultCommand command = SaveMatchResultCommand.builder()
                .matchId(MATCH_ID)
                .rankedResults(List.of(new MatchItem("job-1", "Title", "Company", 0.9, null, "Desc", "Reason")))
                .rankTimeMs(100L)
                .build();

        JobMatchResult result = JobMatchResult.createProcessing(MATCH_ID, USER_ID, RESUME_VERSION_ID, "query", "v1");
        when(jobMatchResultRepository.findById(MATCH_ID)).thenReturn(Optional.of(result));

        // 当
        // When
        matchingService.saveMatchResult(command);

        // 那么
        // Then
        verify(jobMatchResultRepository).save(result);
    }

    @Test
    @DisplayName("Should throw when match result not found for save")
    void shouldThrowWhenMatchResultNotFoundForSave() {
        // 给定
        // Given
        SaveMatchResultCommand command = SaveMatchResultCommand.builder()
                .matchId(MATCH_ID)
                .rankedResults(Collections.emptyList())
                .rankTimeMs(0L)
                .build();
        when(jobMatchResultRepository.findById(MATCH_ID)).thenReturn(Optional.empty());

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> matchingService.saveMatchResult(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Match result not found");
    }

    @Test
    @DisplayName("Should get match result")
    void shouldGetMatchResult() {
        // 给定
        // Given
        JobMatchResult result = JobMatchResult.createProcessing(MATCH_ID, USER_ID, RESUME_VERSION_ID, "query", "v1");
        when(jobMatchResultRepository.findById(MATCH_ID)).thenReturn(Optional.of(result));

        // 当
        // When
        Optional<JobMatchResult> found = matchingService.getMatchResult(new GetMatchResultQuery(MATCH_ID));

        // 那么
        // Then
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("Should list match history")
    void shouldListMatchHistory() {
        // 给定
        // Given
        List<JobMatchResult> results = List.of(
                JobMatchResult.createProcessing("m1", USER_ID, RESUME_VERSION_ID, "q1", "v1")
        );
        when(jobMatchResultRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(results);

        // 当
        // When
        List<JobMatchResult> history = matchingService.listMatchHistory(new ListMatchHistoryQuery(USER_ID));

        // 那么
        // Then
        assertThat(history).hasSize(1);
    }
}
