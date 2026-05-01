package edu.asu.ser594.resumeassistant.api.tracking.dto.request;

/**
 * 更新跟踪记录请求
 * Update tracking request
 *
 * @param status 状态(可选) / Status (optional)
 * @param notes  备注(可选) / Notes (optional)
 */
public record UpdateTrackingRequest(
        String status,
        String notes
) {
}
