package io.jobcopilot.resumeassistant.application.tracking.service;

import io.jobcopilot.resumeassistant.application.tracking.command.ChangeTrackingStatusCommand;
import io.jobcopilot.resumeassistant.application.tracking.command.CreateTrackingCommand;
import io.jobcopilot.resumeassistant.application.tracking.command.DeleteTrackingCommand;
import io.jobcopilot.resumeassistant.application.tracking.command.UpdateTrackingCommand;
import io.jobcopilot.resumeassistant.domain.tracking.entity.ApplicationTracking;
import io.jobcopilot.resumeassistant.domain.tracking.repository.ApplicationTrackingRepository;
import io.jobcopilot.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 求职申请跟踪应用服务单元测试
 * Application tracking application service unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tracking Application Service Tests")
class TrackingApplicationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TRACKING_ID = "tracking-001";

    @Mock
    private ApplicationTrackingRepository trackingRepository;

    @InjectMocks
    private TrackingApplicationService trackingService;

    @Test
    @DisplayName("Should create tracking successfully")
    void shouldCreateTrackingSuccessfully() {
        // 给定
        // Given
        CreateTrackingCommand command = CreateTrackingCommand.builder()
                .userId(USER_ID)
                .jobId("job-001")
                .companyName("Tech Corp")
                .jobTitle("Java Developer")
                .appliedAt(LocalDate.now())
                .notes("Applied via LinkedIn")
                .build();
        when(trackingRepository.save(any(ApplicationTracking.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 当
        // When
        ApplicationTracking result = trackingService.createTracking(command);

        // 那么
        // Then
        assertThat(result.getCompanyName()).isEqualTo("Tech Corp");
        assertThat(result.getJobTitle()).isEqualTo("Java Developer");
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(trackingRepository).save(any(ApplicationTracking.class));
    }

    @Test
    @DisplayName("Should set today as applied date when creating applied tracking without date")
    void shouldSetTodayAsAppliedDateWhenCreatingAppliedTrackingWithoutDate() {
        CreateTrackingCommand command = CreateTrackingCommand.builder()
                .userId(USER_ID)
                .companyName("Tech Corp")
                .jobTitle("Java Developer")
                .status("APPLIED")
                .build();
        when(trackingRepository.save(any(ApplicationTracking.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ApplicationTracking result = trackingService.createTracking(command);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(result.getAppliedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Should reject future applied date when creating tracking")
    void shouldRejectFutureAppliedDateWhenCreatingTracking() {
        CreateTrackingCommand command = CreateTrackingCommand.builder()
                .userId(USER_ID)
                .companyName("Tech Corp")
                .jobTitle("Java Developer")
                .appliedAt(LocalDate.now().plusDays(1))
                .build();

        assertThatThrownBy(() -> trackingService.createTracking(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Applied date cannot be in the future");
    }

    @Test
    @DisplayName("Should update tracking with both info and status")
    void shouldUpdateTrackingWithBothInfoAndStatus() {
        // 给定
        // Given
        ApplicationTracking tracking = createTestTracking();
        UpdateTrackingCommand updateCmd = UpdateTrackingCommand.builder()
                .companyName("Updated Company")
                .jobTitle("Updated Job")
                .appliedAt(LocalDate.of(2026, 5, 7))
                .notes("Updated notes")
                .build();
        ChangeTrackingStatusCommand statusCmd = ChangeTrackingStatusCommand.builder()
                .status("APPLIED")
                .build();
        when(trackingRepository.findByIdAndUserId(TRACKING_ID, USER_ID))
                .thenReturn(Optional.of(tracking));
        when(trackingRepository.save(any(ApplicationTracking.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 当
        // When
        ApplicationTracking result = trackingService.updateTracking(TRACKING_ID, USER_ID, updateCmd, statusCmd);

        // 那么
        // Then
        assertThat(result.getCompanyName()).isEqualTo("Updated Company");
        assertThat(result.getJobTitle()).isEqualTo("Updated Job");
        assertThat(result.getAppliedAt()).isEqualTo(LocalDate.of(2026, 5, 7));
        assertThat(result.getNotes()).isEqualTo("Updated notes");
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        verify(trackingRepository).save(tracking);
    }

    @Test
    @DisplayName("Should update tracking with only info")
    void shouldUpdateTrackingWithOnlyInfo() {
        // 给定
        // Given
        ApplicationTracking tracking = createTestTracking();
        UpdateTrackingCommand updateCmd = UpdateTrackingCommand.builder()
                .companyName("Info Company")
                .jobTitle("Info Job")
                .notes("Only notes updated")
                .build();
        when(trackingRepository.findByIdAndUserId(TRACKING_ID, USER_ID))
                .thenReturn(Optional.of(tracking));
        when(trackingRepository.save(any(ApplicationTracking.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 当
        // When
        ApplicationTracking result = trackingService.updateTracking(TRACKING_ID, USER_ID, updateCmd, null);

        // 那么
        // Then
        assertThat(result.getCompanyName()).isEqualTo("Info Company");
        assertThat(result.getJobTitle()).isEqualTo("Info Job");
        assertThat(result.getNotes()).isEqualTo("Only notes updated");
    }

    @Test
    @DisplayName("Should update tracking with only status")
    void shouldUpdateTrackingWithOnlyStatus() {
        // 给定
        // Given
        ApplicationTracking tracking = createTestTracking();
        ChangeTrackingStatusCommand statusCmd = ChangeTrackingStatusCommand.builder()
                .status("APPLIED")
                .build();
        when(trackingRepository.findByIdAndUserId(TRACKING_ID, USER_ID))
                .thenReturn(Optional.of(tracking));
        when(trackingRepository.save(any(ApplicationTracking.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 当
        // When
        ApplicationTracking result = trackingService.updateTracking(TRACKING_ID, USER_ID, null, statusCmd);

        // 那么
        // Then
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(result.getAppliedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Should reject future applied date when updating tracking")
    void shouldRejectFutureAppliedDateWhenUpdatingTracking() {
        ApplicationTracking tracking = createTestTracking();
        UpdateTrackingCommand updateCmd = UpdateTrackingCommand.builder()
                .appliedAt(LocalDate.now().plusDays(1))
                .build();
        when(trackingRepository.findByIdAndUserId(TRACKING_ID, USER_ID))
                .thenReturn(Optional.of(tracking));

        assertThatThrownBy(() -> trackingService.updateTracking(TRACKING_ID, USER_ID, updateCmd, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Applied date cannot be in the future");
    }

    @Test
    @DisplayName("Should throw when tracking not found for update")
    void shouldThrowWhenTrackingNotFoundForUpdate() {
        // 给定
        // Given
        when(trackingRepository.findByIdAndUserId(TRACKING_ID, USER_ID))
                .thenReturn(Optional.empty());

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> trackingService.updateTracking(TRACKING_ID, USER_ID, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tracking not found");
    }

    @Test
    @DisplayName("Should throw for invalid status value")
    void shouldThrowForInvalidStatusValue() {
        // 给定
        // Given
        ApplicationTracking tracking = createTestTracking();
        ChangeTrackingStatusCommand statusCmd = ChangeTrackingStatusCommand.builder()
                .status("INVALID_STATUS")
                .build();
        when(trackingRepository.findByIdAndUserId(TRACKING_ID, USER_ID))
                .thenReturn(Optional.of(tracking));

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> trackingService.updateTracking(TRACKING_ID, USER_ID, null, statusCmd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should get tracking successfully")
    void shouldGetTrackingSuccessfully() {
        // 给定
        // Given
        ApplicationTracking tracking = createTestTracking();
        when(trackingRepository.findByIdAndUserId(TRACKING_ID, USER_ID))
                .thenReturn(Optional.of(tracking));

        // 当
        // When
        ApplicationTracking result = trackingService.getTracking(TRACKING_ID, USER_ID);

        // 那么
        // Then
        assertThat(result.getId()).isNotNull();
    }

    @Test
    @DisplayName("Should throw when tracking not found for get")
    void shouldThrowWhenTrackingNotFoundForGet() {
        // 给定
        // Given
        when(trackingRepository.findByIdAndUserId(TRACKING_ID, USER_ID))
                .thenReturn(Optional.empty());

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> trackingService.getTracking(TRACKING_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tracking not found");
    }

    @Test
    @DisplayName("Should list trackings with status filter")
    void shouldListTrackingsWithStatusFilter() {
        // 给定
        // Given
        List<ApplicationTracking> trackings = List.of(createTestTracking());
        when(trackingRepository.findAllByUserIdAndStatus(USER_ID, ApplicationStatus.PENDING))
                .thenReturn(trackings);

        // 当
        // When
        List<ApplicationTracking> result = trackingService.listTrackings(USER_ID, "PENDING");

        // 那么
        // Then
        assertThat(result).hasSize(1);
        verify(trackingRepository).findAllByUserIdAndStatus(USER_ID, ApplicationStatus.PENDING);
    }

    @Test
    @DisplayName("Should list trackings without status filter")
    void shouldListTrackingsWithoutStatusFilter() {
        // 给定
        // Given
        List<ApplicationTracking> trackings = List.of(createTestTracking());
        when(trackingRepository.findAllByUserId(USER_ID)).thenReturn(trackings);

        // 当
        // When
        List<ApplicationTracking> result = trackingService.listTrackings(USER_ID, null);

        // 那么
        // Then
        assertThat(result).hasSize(1);
        verify(trackingRepository).findAllByUserId(USER_ID);
    }

    @Test
    @DisplayName("Should delete tracking successfully")
    void shouldDeleteTrackingSuccessfully() {
        // 给定
        // Given
        ApplicationTracking tracking = createTestTracking();
        when(trackingRepository.findByIdAndUserId(any(), eq(USER_ID)))
                .thenReturn(Optional.of(tracking));

        // 当
        // When
        trackingService.deleteTracking(new DeleteTrackingCommand(USER_ID, TRACKING_ID));

        // 那么
        // Then
        verify(trackingRepository).deleteById(tracking.getId());
    }

    @Test
    @DisplayName("Should throw when tracking not found for delete")
    void shouldThrowWhenTrackingNotFoundForDelete() {
        // 给定
        // Given
        when(trackingRepository.findByIdAndUserId(TRACKING_ID, USER_ID))
                .thenReturn(Optional.empty());

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> trackingService.deleteTracking(new DeleteTrackingCommand(USER_ID, TRACKING_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tracking not found");
    }

    private ApplicationTracking createTestTracking() {
        return ApplicationTracking.create(USER_ID, null, "Test Company", "Test Job", null, null);
    }
}
