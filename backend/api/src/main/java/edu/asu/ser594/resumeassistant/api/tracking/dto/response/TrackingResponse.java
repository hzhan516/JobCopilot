package edu.asu.ser594.resumeassistant.api.tracking.dto.response;

import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 求职跟踪响应
 * Tracking response
 *
 * @param trackingId  跟踪ID / Tracking ID
 * @param userId      用户ID / User ID
 * @param job         职位信息(可选) / Job info (optional)
 * @param companyName 公司名称 / Company name
 * @param jobTitle    职位标题 / Job title
 * @param status      状态 / Status
 * @param appliedAt   投递日期 / Applied date
 * @param updatedAt   更新时间 / Updated at
 * @param notes       备注 / Notes
 * @param events      事件历史 / Event history
 */
public record TrackingResponse(
        String trackingId,
        String userId,
        JobResponse job,
        String companyName,
        String jobTitle,
        String status,
        LocalDate appliedAt,
        LocalDateTime updatedAt,
        String notes,
        List<TrackingEventResponse> events
) {
}
