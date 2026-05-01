package edu.asu.ser594.resumeassistant.application.tracking.query;

import java.util.UUID;

/**
 * 获取跟踪记录查询
 * Get tracking query
 *
 * @param userId     用户ID / User ID
 * @param trackingId 跟踪ID / Tracking ID
 */
public record GetTrackingQuery(
        UUID userId,
        String trackingId
) {
}
