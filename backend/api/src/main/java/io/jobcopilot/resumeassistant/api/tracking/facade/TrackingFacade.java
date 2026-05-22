package io.jobcopilot.resumeassistant.api.tracking.facade;

import io.jobcopilot.resumeassistant.api.tracking.dto.request.CreateTrackingRequest;
import io.jobcopilot.resumeassistant.api.tracking.dto.request.UpdateTrackingRequest;
import io.jobcopilot.resumeassistant.api.tracking.dto.response.TrackingResponse;
import io.jobcopilot.resumeassistant.api.tracking.dto.response.TrackingStatsResponse;

import java.util.List;
import java.util.UUID;

/**
 * 求职申请跟踪门面接口
 * Application tracking facade interface
 */
public interface TrackingFacade {

    /**
     * 创建跟踪记录
     * Create tracking record
     *
     * @param userId  用户ID / User ID
     * @param request 创建请求 / Create request
     * @return 跟踪响应 / Tracking response
     */
    TrackingResponse createTracking(UUID userId, CreateTrackingRequest request);

    /**
     * 更新跟踪记录
     * Update tracking record
     *
     * @param userId     用户ID / User ID
     * @param trackingId 跟踪ID / Tracking ID
     * @param request    更新请求 / Update request
     * @return 跟踪响应 / Tracking response
     */
    TrackingResponse updateTracking(UUID userId, String trackingId, UpdateTrackingRequest request);

    /**
     * 获取跟踪详情
     * Get tracking detail
     *
     * @param userId     用户ID / User ID
     * @param trackingId 跟踪ID / Tracking ID
     * @return 跟踪响应 / Tracking response
     */
    TrackingResponse getTracking(UUID userId, String trackingId);

    /**
     * 获取跟踪列表
     * Get tracking list
     *
     * @param userId 用户ID / User ID
     * @param status 状态过滤(可选) / Status filter (optional)
     * @return 跟踪响应列表 / List of tracking responses
     */
    List<TrackingResponse> listTrackings(UUID userId, String status);

    /**
     * 删除跟踪记录
     * Delete tracking record
     *
     * @param userId     用户ID / User ID
     * @param trackingId 跟踪ID / Tracking ID
     */
    void deleteTracking(UUID userId, String trackingId);

    /**
     * 获取统计信息
     * Get statistics
     *
     * @param userId 用户ID / User ID
     * @return 统计响应 / Stats response
     */
    TrackingStatsResponse getStats(UUID userId);
}
