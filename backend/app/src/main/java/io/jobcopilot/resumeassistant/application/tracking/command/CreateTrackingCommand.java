package io.jobcopilot.resumeassistant.application.tracking.command;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 创建跟踪记录命令
 * Create tracking command
 *
 * @param userId      用户ID / User ID
 * @param jobId       职位ID(可选) / Job ID (optional)
 * @param companyName 公司名称 / Company name
 * @param jobTitle    职位标题 / Job title
 * @param status      初始状态(可选) / Initial status (optional)
 * @param appliedAt   投递日期(可选) / Applied date (optional)
 * @param notes       备注(可选) / Notes (optional)
 */
@Builder
public record CreateTrackingCommand(
        UUID userId,
        String jobId,
        String companyName,
        String jobTitle,
        String status,
        LocalDate appliedAt,
        String notes
) {
}
