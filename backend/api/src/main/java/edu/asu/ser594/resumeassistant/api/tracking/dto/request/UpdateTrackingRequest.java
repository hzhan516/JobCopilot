package edu.asu.ser594.resumeassistant.api.tracking.dto.request;

import java.time.LocalDate;

/**
 * 更新跟踪记录请求
 * Update tracking request
 *
 * @param companyName 公司名称(可选) / Company name (optional)
 * @param jobTitle    职位标题(可选) / Job title (optional)
 * @param status      状态(可选) / Status (optional)
 * @param appliedAt   投递日期(可选) / Applied date (optional)
 * @param notes       备注(可选) / Notes (optional)
 */
public record UpdateTrackingRequest(
        String companyName,
        String jobTitle,
        String status,
        LocalDate appliedAt,
        String notes
) {
}
