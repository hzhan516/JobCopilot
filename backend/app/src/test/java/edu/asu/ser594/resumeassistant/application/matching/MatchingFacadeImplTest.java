package edu.asu.ser594.resumeassistant.application.matching;

import edu.asu.ser594.resumeassistant.api.job.dto.request.JobMatchRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobMatchResponse;
import edu.asu.ser594.resumeassistant.application.matching.service.MatchingApplicationService;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.matching.entity.JobMatchResult;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.RankedJob;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 匹配门面实现单元测试
 * Matching facade implementation unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Matching Facade Implementation Tests")
class MatchingFacadeImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String MATCH_ID = "match-001";

    @Mock
    private MatchingApplicationService matchingService;

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private MatchingFacadeImpl matchingFacade;

    @Test
    @DisplayName("Should start match and return processing response")
    void shouldStartMatchAndReturnProcessingResponse() {
        // 给定
        // Given
        JobMatchRequest request = new JobMatchRequest("resume-v1", "Java Developer", 5, null);
        when(matchingService.startJobMatch(any())).thenReturn(MATCH_ID);

        // 当
        // When
        JobMatchResponse response = matchingFacade.matchJobs(USER_ID, request);

        // 那么
        // Then
        assertThat(response.matchId()).isEqualTo(MATCH_ID);
        assertThat(response.status()).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("Should get match result when completed")
    void shouldGetMatchResultWhenCompleted() {
        // 给定
        // Given
        JobMatchResult result = JobMatchResult.createProcessing(MATCH_ID, USER_ID, "resume-v1", "query", "v1");
        result.complete(List.of(new RankedJob("job-1", "Title", "Company", 0.9, "Desc")), 100L);
        when(matchingService.getMatchResult(any())).thenReturn(Optional.of(result));


        // 当
        // When
        JobMatchResponse response = matchingFacade.getMatchResult(MATCH_ID);

        // 那么
        // Then
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.matches()).hasSize(1);
    }

    @Test
    @DisplayName("Should get match result when processing")
    void shouldGetMatchResultWhenProcessing() {
        // 给定
        // Given
        JobMatchResult result = JobMatchResult.createProcessing(MATCH_ID, USER_ID, "resume-v1", "query", "v1");
        when(matchingService.getMatchResult(any())).thenReturn(Optional.of(result));

        // 当
        // When
        JobMatchResponse response = matchingFacade.getMatchResult(MATCH_ID);

        // 那么
        // Then
        assertThat(response.status()).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("Should throw when match not found")
    void shouldThrowWhenMatchNotFound() {
        // 给定
        // Given
        when(matchingService.getMatchResult(any())).thenReturn(Optional.empty());

        // 当&那么
        // When&Then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> matchingFacade.getMatchResult(MATCH_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should list match history")
    void shouldListMatchHistory() {
        // 给定
        // Given
        List<JobMatchResult> results = List.of(
                JobMatchResult.createProcessing("m1", USER_ID, "rv1", "q1", "v1")
        );
        when(matchingService.listMatchHistory(any())).thenReturn(results);

        // 当
        // When
        var history = matchingFacade.getMatchHistory(USER_ID);

        // 那么
        // Then
        assertThat(history).hasSize(1);
    }
}
