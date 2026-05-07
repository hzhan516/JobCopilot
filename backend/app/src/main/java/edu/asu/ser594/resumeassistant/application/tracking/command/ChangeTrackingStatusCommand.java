package edu.asu.ser594.resumeassistant.application.tracking.command;

import lombok.Builder;

/**
 * 变更跟踪状态命令
 * Change tracking status command
 *
 * @param status 新状态 / New status
 * @param note   备注(可选) / Note (optional)
 */
@Builder
public record ChangeTrackingStatusCommand(
        String status,
        String note
) {
}
