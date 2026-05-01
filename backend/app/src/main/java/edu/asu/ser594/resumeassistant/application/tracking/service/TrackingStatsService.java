package edu.asu.ser594.resumeassistant.application.tracking.service;

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
 * 求职申请跟踪统计服务
 * Application tracking stats service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingStatsService {

    private final ApplicationTrackingRepository trackingRepository;

    @Transactional(readOnly = true)
    public TrackingStats calculateStats(final UUID userId) {
        final List<ApplicationTracking> trackings = trackingRepository.findAllByUserId(userId);

        final long total = trackings.size();
        final long pending = countByStatus(trackings, ApplicationStatus.PENDING);
        final long applied = countByStatus(trackings, ApplicationStatus.APPLIED,
                ApplicationStatus.SCREENING, ApplicationStatus.INTERVIEWING);
        final long interviewing = countByStatus(trackings, ApplicationStatus.INTERVIEWING);
        final long offer = countByStatus(trackings, ApplicationStatus.OFFER, ApplicationStatus.ACCEPTED);
        final long rejected = countByStatus(trackings, ApplicationStatus.REJECTED);
        final long withdrawn = countByStatus(trackings, ApplicationStatus.WITHDRAWN);

        final double successRate = total > 0
                ? Math.round((double) offer / total * 10000) / 100.0
                : 0.0;

        log.debug("Calculated stats for user: {} -> total: {}, successRate: {}", userId, total, successRate);

        return new TrackingStats(total, pending, applied, interviewing, offer, rejected, withdrawn, successRate);
    }

    private long countByStatus(final List<ApplicationTracking> trackings, final ApplicationStatus... statuses) {
        return trackings.stream()
                .filter(t -> {
                    for (ApplicationStatus s : statuses) {
                        if (t.getStatus() == s) {
                            return true;
                        }
                    }
                    return false;
                })
                .count();
    }

    /**
     * 统计结果值对象
     * Stats result value object
     */
    public record TrackingStats(
            long total,
            long pending,
            long applied,
            long interviewing,
            long offer,
            long rejected,
            long withdrawn,
            double successRate
    ) {
    }
}
