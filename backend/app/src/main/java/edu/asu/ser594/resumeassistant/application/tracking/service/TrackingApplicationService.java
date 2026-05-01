package edu.asu.ser594.resumeassistant.application.tracking.service;

import edu.asu.ser594.resumeassistant.application.tracking.command.ChangeTrackingStatusCommand;
import edu.asu.ser594.resumeassistant.application.tracking.command.CreateTrackingCommand;
import edu.asu.ser594.resumeassistant.application.tracking.command.DeleteTrackingCommand;
import edu.asu.ser594.resumeassistant.application.tracking.command.UpdateTrackingCommand;
import edu.asu.ser594.resumeassistant.domain.tracking.entity.ApplicationTracking;
import edu.asu.ser594.resumeassistant.domain.tracking.repository.ApplicationTrackingRepository;
import edu.asu.ser594.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        final ApplicationTracking tracking = ApplicationTracking.create(
                command.userId(),
                command.jobId(),
                command.companyName(),
                command.jobTitle(),
                command.appliedAt(),
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
        }

        if (statusCmd != null && statusCmd.status() != null) {
            final ApplicationStatus newStatus = ApplicationStatus.valueOf(statusCmd.status());
            tracking.changeStatus(newStatus, statusCmd.note());
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
}
