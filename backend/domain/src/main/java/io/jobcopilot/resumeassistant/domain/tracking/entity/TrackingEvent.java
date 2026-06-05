package io.jobcopilot.resumeassistant.domain.tracking.entity;

import io.jobcopilot.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 跟踪事件
 * Tracking event
 * <p>
 * 记录状态变更历史 / Records status change history
 */
@Getter
public class TrackingEvent {

    private final LocalDateTime timestamp;
    private final ApplicationStatus fromStatus;
    private final ApplicationStatus toStatus;
    private final String note;

    @Builder
    public TrackingEvent(final LocalDateTime timestamp,
                         final ApplicationStatus fromStatus,
                         final ApplicationStatus toStatus,
                         final String note) {
        this.timestamp = timestamp;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
    }

    /**
     * 创建状态变更事件
     * Create a status change event
     *
     * @param fromStatus 原状态 / From status
     * @param toStatus   新状态 / To status
     * @param note       备注 / Note
     * @return 跟踪事件 / Tracking event
     */
    public static TrackingEvent create(final ApplicationStatus fromStatus,
                                       final ApplicationStatus toStatus,
                                       final String note) {
        return TrackingEvent.builder()
                .timestamp(LocalDateTime.now())
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .note(note)
                .build();
    }
}
