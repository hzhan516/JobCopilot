package edu.asu.ser594.resumeassistant.trigger.mq.listener;

import edu.asu.ser594.resumeassistant.api.matching.facade.MatchingFacade;
import edu.asu.ser594.resumeassistant.trigger.listener.ai.JobRankResultListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * 职位排序结果监听器单元测试
 * Job rank result listener unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Job Rank Result Listener Tests")
class JobRankResultListenerTest {

    private static final String MATCH_ID = "match-001";

    @Mock
    private MatchingFacade matchingFacade;

    @InjectMocks
    private JobRankResultListener listener;

    @Test
    @DisplayName("Should handle rank result successfully")
    @SuppressWarnings("unchecked")
    void shouldHandleRankResultSuccessfully() {
        // 给定
        // Given
        Map<String, Object> payload = Map.of(
                "matchId", MATCH_ID,
                "status", "COMPLETED",
                "rankTimeMs", 150L,
                "rankedResults", List.of(
                        Map.of("jobId", "job-1", "title", "Title", "company", "Company",
                                "matchScore", 0.95, "description", "Description")
                )
        );

        // 当
        // When
        listener.onJobRankResult(payload);

        // 那么
        // Then
        verify(matchingFacade).saveJobRankResult(eq(MATCH_ID), any(List.class), eq(150L));
    }

    @Test
    @DisplayName("Should skip on failed status")
    void shouldSkipOnFailedStatus() {
        // 给定
        // Given
        Map<String, Object> payload = Map.of(
                "matchId", MATCH_ID,
                "status", "FAILED",
                "errorMessage", "AI service error"
        );

        // 当
        // When
        listener.onJobRankResult(payload);

        // 那么
        // Then
        // No interaction with matchingFacade expected
    }
}
