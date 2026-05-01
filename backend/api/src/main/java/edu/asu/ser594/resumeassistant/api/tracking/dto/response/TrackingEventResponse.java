package edu.asu.ser594.resumeassistant.api.tracking.dto.response;

import java.time.LocalDateTime;

/**
 * 跟踪事件响应
 * Tracking event response
 *
 * @param timestamp  发生时间 / Timestamp
 * @param fromStatus 原状态 / From status
 * @param toStatus   新状态 / To status
 * @param note       备注 / Note
 */
public record TrackingEventResponse(
        LocalDateTime timestamp,
        String fromStatus,
        String toStatus,
        String note
) {
}
