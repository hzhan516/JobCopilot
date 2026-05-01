package edu.asu.ser594.resumeassistant.application.tracking.query;

import java.util.UUID;

/**
 * 获取跟踪统计查询
 * Get tracking stats query
 *
 * @param userId 用户ID / User ID
 */
public record GetTrackingStatsQuery(
        UUID userId
) {
}
