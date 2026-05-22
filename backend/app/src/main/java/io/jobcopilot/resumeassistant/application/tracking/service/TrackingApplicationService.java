package io.jobcopilot.resumeassistant.application.tracking.service;

import io.jobcopilot.resumeassistant.application.tracking.command.ChangeTrackingStatusCommand;
import io.jobcopilot.resumeassistant.application.tracking.command.CreateTrackingCommand;
import io.jobcopilot.resumeassistant.application.tracking.command.DeleteTrackingCommand;
import io.jobcopilot.resumeassistant.application.tracking.command.UpdateTrackingCommand;
import io.jobcopilot.resumeassistant.domain.tracking.entity.ApplicationTracking;
import io.jobcopilot.resumeassistant.domain.tracking.repository.ApplicationTrackingRepository;
import io.jobcopilot.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 求职申请跟踪应用服务
 * Application tracking application service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingApplicationService {

    private final ApplicationTrackingRepository trackingRepository;

    @Transactional
    public ApplicationTracking createTracking(final CreateTrackingCommand command) {
        log.info("Creating tracking for user: {}, company: {}", command.userId(), command.companyName());
        final ApplicationStatus initialStatus = command.status() != null
                ? ApplicationStatus.valueOf(command.status())
                : null;
        final LocalDate appliedAt = normalizeAppliedAt(initialStatus, command.appliedAt());
        final ApplicationTracking tracking = ApplicationTracking.create(
                command.userId(),
                command.jobId(),
                command.companyName(),
                command.jobTitle(),
                initialStatus,
                appliedAt,
                command.notes()
        );
        return trackingRepository.save(tracking);
    }

    @Transactional
    public ApplicationTracking updateTracking(final String trackingId,
                                              final UUID userId,
                                              final UpdateTrackingCommand updateCmd,
                                              final ChangeTrackingStatusCommand statusCmd) {
        log.info("Updating tracking: {} for user: {}", trackingId, userId);
        final ApplicationTracking tracking = trackingRepository.findByIdAndUserId(trackingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking not found: " + trackingId));

        if (updateCmd != null) {
            tracking.updateInfo(updateCmd.companyName(), updateCmd.jobTitle(), updateCmd.notes());
            if (updateCmd.appliedAt() != null) {
                validateAppliedAt(updateCmd.appliedAt());
                tracking.setAppliedAt(updateCmd.appliedAt());
            }
        }

        if (statusCmd != null && statusCmd.status() != null) {
            final ApplicationStatus newStatus = ApplicationStatus.valueOf(statusCmd.status());
            if (newStatus != tracking.getStatus()) {
                tracking.changeStatus(newStatus, statusCmd.note());
            }
            if (newStatus == ApplicationStatus.APPLIED && tracking.getAppliedAt() == null) {
                tracking.setAppliedAt(LocalDate.now());
            }
        }

        return trackingRepository.save(tracking);
    }

    @Transactional(readOnly = true)
    public ApplicationTracking getTracking(final String trackingId, final UUID userId) {
        return trackingRepository.findByIdAndUserId(trackingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tracking not found: " + trackingId));
    }

    @Transactional(readOnly = true)
    public List<ApplicationTracking> listTrackings(final UUID userId, final String status) {
        if (status != null && !status.isBlank()) {
            final ApplicationStatus applicationStatus = ApplicationStatus.valueOf(status);
            return trackingRepository.findAllByUserIdAndStatus(userId, applicationStatus);
        }
        return trackingRepository.findAllByUserId(userId);
    }

    @Transactional
    public void deleteTracking(final DeleteTrackingCommand command) {
        log.info("Deleting tracking: {} for user: {}", command.trackingId(), command.userId());
        final ApplicationTracking tracking = trackingRepository.findByIdAndUserId(command.trackingId(), command.userId())
                .orElseThrow(() -> new IllegalArgumentException("Tracking not found: " + command.trackingId()));
        trackingRepository.deleteById(tracking.getId());
    }

    private LocalDate normalizeAppliedAt(final ApplicationStatus status, final LocalDate appliedAt) {
        if (appliedAt != null) {
            validateAppliedAt(appliedAt);
            return appliedAt;
        }
        return status == ApplicationStatus.APPLIED ? LocalDate.now() : null;
    }

    private void validateAppliedAt(final LocalDate appliedAt) {
        if (appliedAt.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Applied date cannot be in the future");
        }
    }
}
