package io.jobcopilot.resumeassistant.application.tracking.service;

import io.jobcopilot.resumeassistant.domain.tracking.entity.ApplicationTracking;
import io.jobcopilot.resumeassistant.domain.tracking.repository.ApplicationTrackingRepository;
import io.jobcopilot.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 求职申请统计服务单元测试
 * Tracking statistics service unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tracking Stats Service Tests")
class TrackingStatsServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ApplicationTrackingRepository trackingRepository;

    @InjectMocks
    private TrackingStatsService statsService;

    @Test
    @DisplayName("Should return stats with multiple statuses")
    void shouldReturnStatsWithMultipleStatuses() {
        // 给定
        // Given
        List<ApplicationTracking> trackings = List.of(
                createTracking(ApplicationStatus.PENDING),
                createTracking(ApplicationStatus.PENDING),
                createTracking(ApplicationStatus.APPLIED),
                createTracking(ApplicationStatus.APPLIED),
                createTracking(ApplicationStatus.APPLIED),
                createTracking(ApplicationStatus.INTERVIEWING),
                createTracking(ApplicationStatus.OFFER)
        );
        when(trackingRepository.findAllByUserId(USER_ID)).thenReturn(trackings);

        // 当
        // When
        var result = statsService.calculateStats(USER_ID);

        // 那么
        // Then
        assertThat(result.total()).isEqualTo(7);
        assertThat(result.pending()).isEqualTo(2);
        assertThat(result.applied()).isEqualTo(4); // APPLIED + SCREENING + INTERVIEWING
        assertThat(result.interviewing()).isEqualTo(1);
        assertThat(result.offer()).isEqualTo(1); // OFFER + ACCEPTED
        assertThat(result.rejected()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return empty stats when no trackings")
    void shouldReturnEmptyStatsWhenNoTrackings() {
        // 给定
        // Given
        when(trackingRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        // 当
        // When
        var result = statsService.calculateStats(USER_ID);

        // 那么
        // Then
        assertThat(result.total()).isZero();
        assertThat(result.pending()).isZero();
        assertThat(result.applied()).isZero();
        assertThat(result.successRate()).isZero();
    }

    private ApplicationTracking createTracking(ApplicationStatus status) {
        ApplicationTracking tracking = ApplicationTracking.create(
                USER_ID, null, "Company", "Job", null, null
        );
        // Directly set status for testing since create() always creates PENDING
        // In reality we would transition through valid states, but for stats counting we just need the end state
        try {
            java.lang.reflect.Field field = ApplicationTracking.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(tracking, status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return tracking;
    }
}
