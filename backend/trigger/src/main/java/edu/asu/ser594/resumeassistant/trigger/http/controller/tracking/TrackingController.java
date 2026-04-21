package edu.asu.ser594.resumeassistant.trigger.http.controller.tracking;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.tracking.dto.request.CreateTrackingRequest;
import edu.asu.ser594.resumeassistant.api.tracking.dto.request.UpdateTrackingRequest;
import edu.asu.ser594.resumeassistant.api.tracking.dto.response.TrackingResponse;
import edu.asu.ser594.resumeassistant.api.tracking.dto.response.TrackingStatsResponse;
import edu.asu.ser594.resumeassistant.api.tracking.facade.TrackingFacade;
import edu.asu.ser594.resumeassistant.trigger.http.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 求职申请跟踪控制器
 * Application tracking controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/trackings")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingFacade trackingFacade;

    /**
     * 创建跟踪记录
     * Create tracking record
     */
    @PostMapping
    public ApiResponse<TrackingResponse> createTracking(
            @CurrentUser UUID userId,
            @RequestBody CreateTrackingRequest request) {
        log.info("User {} creating tracking for company: {}", userId, request.companyName());
        TrackingResponse response = trackingFacade.createTracking(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 获取跟踪列表
     * Get tracking list
     */
    @GetMapping
    public ApiResponse<List<TrackingResponse>> listTrackings(
            @CurrentUser UUID userId,
            @RequestParam(name = "status", required = false) String status) {
        log.info("User {} listing trackings with status filter: {}", userId, status);
        List<TrackingResponse> response = trackingFacade.listTrackings(userId, status);
        return ApiResponse.success(response);
    }

    /**
     * 获取跟踪详情
     * Get tracking detail
     */
    @GetMapping("/{id}")
    public ApiResponse<TrackingResponse> getTracking(
            @CurrentUser UUID userId,
            @PathVariable("id") String id) {
        log.info("User {} fetching tracking: {}", userId, id);
        TrackingResponse response = trackingFacade.getTracking(userId, id);
        return ApiResponse.success(response);
    }

    /**
     * 更新跟踪记录（含状态流转）
     * Update tracking record (including status transition)
     */
    @PutMapping("/{id}")
    public ApiResponse<TrackingResponse> updateTracking(
            @CurrentUser UUID userId,
            @PathVariable("id") String id,
            @RequestBody UpdateTrackingRequest request) {
        log.info("User {} updating tracking: {}, status: {}", userId, id, request.status());
        TrackingResponse response = trackingFacade.updateTracking(userId, id, request);
        return ApiResponse.success(response);
    }

    /**
     * 删除跟踪记录
     * Delete tracking record
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTracking(
            @CurrentUser UUID userId,
            @PathVariable("id") String id) {
        log.info("User {} deleting tracking: {}", userId, id);
        trackingFacade.deleteTracking(userId, id);
        return ApiResponse.success(null);
    }

    /**
     * 获取统计信息
     * Get statistics
     */
    @GetMapping("/stats")
    public ApiResponse<TrackingStatsResponse> getStats(
            @CurrentUser UUID userId) {
        log.info("User {} fetching tracking stats", userId);
        TrackingStatsResponse response = trackingFacade.getStats(userId);
        return ApiResponse.success(response);
    }
}
