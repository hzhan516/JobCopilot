package io.jobcopilot.resumeassistant.trigger.http.controller.admin;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 管理端监控 API / Admin monitoring API */
@RestController
@RequestMapping("/api/admin/v1/monitoring")
@RequiredArgsConstructor
public class AdminMonitoringController {

    private final MeterRegistry meterRegistry;

    @GetMapping("/metrics")
    public ApiResponse<Map<String, Object>> getMetrics() {
        var regCounter = meterRegistry.find("jobcopilot.users.registrations").counter();
        var aiTimer = meterRegistry.find("jobcopilot.ai.parsing.duration").timer();
        return ApiResponse.success(Map.of(
                "registrations", regCounter != null ? regCounter.count() : 0,
                "aiParsingCount", aiTimer != null ? aiTimer.count() : 0
        ));
    }

    @GetMapping("/errors")
    public ApiResponse<java.util.List<String>> getErrors() {
        return ApiResponse.success(java.util.List.of());
    }

    @GetMapping("/queues")
    public ApiResponse<Map<String, Object>> getQueues() {
        return ApiResponse.success(Map.of(
                "parseQueue", Map.of("depth", 0),
                "conversationQueue", Map.of("depth", 0)
        ));
    }
}
