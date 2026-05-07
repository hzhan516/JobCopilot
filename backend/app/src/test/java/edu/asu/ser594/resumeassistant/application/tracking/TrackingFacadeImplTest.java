package edu.asu.ser594.resumeassistant.application.tracking;

import edu.asu.ser594.resumeassistant.api.tracking.dto.request.CreateTrackingRequest;
import edu.asu.ser594.resumeassistant.api.tracking.dto.response.TrackingResponse;
import edu.asu.ser594.resumeassistant.application.tracking.service.TrackingApplicationService;
import edu.asu.ser594.resumeassistant.application.tracking.service.TrackingStatsService;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.tracking.entity.ApplicationTracking;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 跟踪门面实现单元测试
 * Tracking facade implementation unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tracking Facade Implementation Tests")
class TrackingFacadeImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private TrackingApplicationService trackingService;

    @Mock
    private TrackingStatsService statsService;

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private TrackingFacadeImpl trackingFacade;

    @Test
    @DisplayName("Should create tracking and return response")
    void shouldCreateTrackingAndReturnResponse() {
        // 给定
        // Given
        CreateTrackingRequest request = new CreateTrackingRequest("job-001", "Tech Corp", "Developer", null, null, null);
        ApplicationTracking tracking = ApplicationTracking.create(
                USER_ID, "job-001", "Tech Corp", "Developer", null, null
        );
        when(trackingService.createTracking(any())).thenReturn(tracking);

        // 当
        // When
        TrackingResponse response = trackingFacade.createTracking(USER_ID, request);

        // 那么
        // Then
        assertThat(response.companyName()).isEqualTo("Tech Corp");
        assertThat(response.jobTitle()).isEqualTo("Developer");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should list trackings")
    void shouldListTrackings() {
        // 给定
        // Given
        ApplicationTracking t1 = ApplicationTracking.create(USER_ID, null, "C1", "J1", null, null);
        when(trackingService.listTrackings(USER_ID, "PENDING")).thenReturn(List.of(t1));

        // 当
        // When
        List<TrackingResponse> result = trackingFacade.listTrackings(USER_ID, "PENDING");

        // 那么
        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should get tracking stats")
    void shouldGetTrackingStats() {
        // 给定
        // Given
        when(statsService.calculateStats(USER_ID)).thenReturn(
                new TrackingStatsService.TrackingStats(5, 1, 2, 1, 1, 0, 0, 20.0)
        );

        // 当
        // When
        var stats = trackingFacade.getStats(USER_ID);

        // 那么
        // Then
        assertThat(stats.totalApplications()).isEqualTo(5);
        assertThat(stats.appliedCount()).isEqualTo(2);
    }
}
