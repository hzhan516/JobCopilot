package io.jobcopilot.resumeassistant.api.tracking.dto.response;

/**
 * 求职跟踪统计响应
 * Tracking stats response
 *
 * @param totalApplications 总申请数 / Total applications
 * @param pendingCount      待投递数 / Pending count
 * @param appliedCount      已投递数 / Applied count
 * @param interviewingCount 面试中数 / Interviewing count
 * @param offerCount        已收到offer数 / Offer count
 * @param rejectedCount     被拒绝数 / Rejected count
 * @param withdrawnCount    已撤回数 / Withdrawn count
 * @param successRate       成功率(%) / Success rate (%)
 */
public record TrackingStatsResponse(
        Long totalApplications,
        Long pendingCount,
        Long appliedCount,
        Long interviewingCount,
        Long offerCount,
        Long rejectedCount,
        Long withdrawnCount,
        Double successRate
) {
}
