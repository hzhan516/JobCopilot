package edu.asu.ser594.resumeassistant.application.tracking;

import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.tracking.dto.request.CreateTrackingRequest;
import edu.asu.ser594.resumeassistant.api.tracking.dto.request.UpdateTrackingRequest;
import edu.asu.ser594.resumeassistant.api.tracking.dto.response.TrackingEventResponse;
import edu.asu.ser594.resumeassistant.api.tracking.dto.response.TrackingResponse;
import edu.asu.ser594.resumeassistant.api.tracking.dto.response.TrackingStatsResponse;
import edu.asu.ser594.resumeassistant.api.tracking.facade.TrackingFacade;
import edu.asu.ser594.resumeassistant.application.tracking.command.ChangeTrackingStatusCommand;
import edu.asu.ser594.resumeassistant.application.tracking.command.CreateTrackingCommand;
import edu.asu.ser594.resumeassistant.application.tracking.command.DeleteTrackingCommand;
import edu.asu.ser594.resumeassistant.application.tracking.command.UpdateTrackingCommand;
import edu.asu.ser594.resumeassistant.application.tracking.service.TrackingApplicationService;
import edu.asu.ser594.resumeassistant.application.tracking.service.TrackingStatsService;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.tracking.entity.ApplicationTracking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 求职申请跟踪门面实现
 * Application tracking facade implementation
 */
@Component
@RequiredArgsConstructor
public class TrackingFacadeImpl implements TrackingFacade {

    private final TrackingApplicationService trackingApplicationService;
    private final TrackingStatsService trackingStatsService;
    private final JobRepository jobRepository;

    @Override
    public TrackingResponse createTracking(final UUID userId, final CreateTrackingRequest request) {
        final CreateTrackingCommand command = CreateTrackingCommand.builder()
                .userId(userId)
                .jobId(request.jobId())
                .companyName(request.companyName())
                .jobTitle(request.jobTitle())
                .status(request.status())
                .appliedAt(request.appliedAt())
                .notes(request.notes())
                .build();
        final ApplicationTracking tracking = trackingApplicationService.createTracking(command);
        return mapToResponse(tracking);
    }

    @Override
    public TrackingResponse updateTracking(final UUID userId, final String trackingId, final UpdateTrackingRequest request) {
        final UpdateTrackingCommand updateCmd = UpdateTrackingCommand.builder()
                .notes(request.notes())
                .build();
        final ChangeTrackingStatusCommand statusCmd = request.status() != null
                ? ChangeTrackingStatusCommand.builder().status(request.status()).build()
                : null;
        final ApplicationTracking tracking = trackingApplicationService.updateTracking(trackingId, userId, updateCmd, statusCmd);
        return mapToResponse(tracking);
    }

    @Override
    public TrackingResponse getTracking(final UUID userId, final String trackingId) {
        final ApplicationTracking tracking = trackingApplicationService.getTracking(trackingId, userId);
        return mapToResponse(tracking);
    }

    @Override
    public List<TrackingResponse> listTrackings(final UUID userId, final String status) {
        return trackingApplicationService.listTrackings(userId, status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteTracking(final UUID userId, final String trackingId) {
        trackingApplicationService.deleteTracking(new DeleteTrackingCommand(userId, trackingId));
    }

    @Override
    public TrackingStatsResponse getStats(final UUID userId) {
        final TrackingStatsService.TrackingStats stats = trackingStatsService.calculateStats(userId);
        return new TrackingStatsResponse(
                stats.total(),
                stats.pending(),
                stats.applied(),
                stats.interviewing(),
                stats.offer(),
                stats.rejected(),
                stats.withdrawn(),
                stats.successRate()
        );
    }

    private TrackingResponse mapToResponse(final ApplicationTracking tracking) {
        final JobResponse job = tracking.getJobId() != null
                ? jobRepository.findById(tracking.getJobId()).map(this::mapJobToResponse).orElse(null)
                : null;

        final List<TrackingEventResponse> events = tracking.getEvents().stream()
                .map(e -> new TrackingEventResponse(
                        e.getTimestamp(),
                        e.getFromStatus().name(),
                        e.getToStatus().name(),
                        e.getNote()
                ))
                .collect(Collectors.toList());

        return new TrackingResponse(
                tracking.getId(),
                tracking.getUserId().toString(),
                job,
                tracking.getCompanyName(),
                tracking.getJobTitle(),
                tracking.getStatus().name(),
                tracking.getAppliedAt(),
                tracking.getUpdatedAt(),
                tracking.getNotes(),
                events
        );
    }

    private JobResponse mapJobToResponse(final edu.asu.ser594.resumeassistant.domain.job.entity.Job job) {
        JobResponse.ParsedJobContentResponse parsed = null;
        if (job.getParsedContent() != null) {
            parsed = new JobResponse.ParsedJobContentResponse(
                    job.getParsedContent().title(),
                    job.getParsedContent().company(),
                    job.getParsedContent().salary(),
                    job.getParsedContent().location(),
                    job.getParsedContent().description(),
                    job.getParsedContent().requirements()
            );
        }
        return new JobResponse(
                job.getId(),
                job.getUserId().toString(),
                job.getOriginalUrl(),
                job.getStatus().name(),
                parsed,
                job.isImageCheckEnabled(),
                job.getErrorMessage()
        );
    }
}
