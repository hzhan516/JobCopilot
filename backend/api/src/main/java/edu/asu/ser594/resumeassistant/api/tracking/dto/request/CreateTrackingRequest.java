package edu.asu.ser594.resumeassistant.api.tracking.dto.request;

import java.time.LocalDate;

/**
 * 创建跟踪记录请求
 * Create tracking request
 *
 * @param jobId       职位ID(可选) / Job ID (optional)
 * @param companyName 公司名称(必填) / Company name (required)
 * @param jobTitle    职位标题(必填) / Job title (required)
 * @param appliedAt   投递日期(可选) / Applied date (optional)
 * @param notes       备注(可选) / Notes (optional)
 */
public record CreateTrackingRequest(
        String jobId,
        String companyName,
        String jobTitle,
        LocalDate appliedAt,
        String notes
) {
}
