package edu.asu.ser594.resumeassistant.application.tracking.command;

import java.util.UUID;

/**
 * 删除跟踪记录命令
 * Delete tracking command
 *
 * @param userId     用户ID / User ID
 * @param trackingId 跟踪ID / Tracking ID
 */
public record DeleteTrackingCommand(
        UUID userId,
        String trackingId
) {
}
