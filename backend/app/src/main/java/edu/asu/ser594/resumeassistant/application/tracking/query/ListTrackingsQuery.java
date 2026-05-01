package edu.asu.ser594.resumeassistant.application.tracking.query;

import java.util.UUID;

/**
 * 查询跟踪列表查询
 * List trackings query
 *
 * @param userId 用户ID / User ID
 * @param status 状态过滤(可选) / Status filter (optional)
 */
public record ListTrackingsQuery(
        UUID userId,
        String status
) {
}
