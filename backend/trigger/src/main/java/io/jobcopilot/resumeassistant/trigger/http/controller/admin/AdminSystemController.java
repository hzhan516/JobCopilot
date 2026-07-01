package io.jobcopilot.resumeassistant.trigger.http.controller.admin;

import io.jobcopilot.resumeassistant.api.admin.dto.ComponentHealthResponse;
import io.jobcopilot.resumeassistant.api.admin.dto.SystemStatsResponse;
import io.jobcopilot.resumeassistant.api.admin.facade.AdminSystemFacade;
import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.version.dto.VersionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final AdminSystemFacade adminSystemFacade;

    @GetMapping("/stats")
    public ApiResponse<SystemStatsResponse> getStats() {
        return ApiResponse.success(adminSystemFacade.getStats());
    }

    @GetMapping("/health")
    public ApiResponse<ComponentHealthResponse> getHealth() {
        return ApiResponse.success(adminSystemFacade.getHealth());
    }

    @GetMapping("/version")
    public ApiResponse<VersionResponse> getVersion() {
        return ApiResponse.success(adminSystemFacade.getVersion());
    }
}
