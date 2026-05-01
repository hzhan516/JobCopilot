package edu.asu.ser594.resumeassistant.application.tracking.command;

import lombok.Builder;

/**
 * 更新跟踪记录命令
 * Update tracking command
 *
 * @param companyName 公司名称(可选) / Company name (optional)
 * @param jobTitle    职位标题(可选) / Job title (optional)
 * @param notes       备注(可选) / Notes (optional)
 */
@Builder
public record UpdateTrackingCommand(
        String companyName,
        String jobTitle,
        String notes
) {
}
