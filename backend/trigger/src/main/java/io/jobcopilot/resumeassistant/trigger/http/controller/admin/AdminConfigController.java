package io.jobcopilot.resumeassistant.trigger.http.controller.admin;

import io.jobcopilot.resumeassistant.api.admin.dto.ConfigItemResponse;
import io.jobcopilot.resumeassistant.api.admin.dto.UpdateConfigRequest;
import io.jobcopilot.resumeassistant.api.admin.facade.AdminConfigFacade;
import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.trigger.http.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/v1/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final AdminConfigFacade configFacade;

    @GetMapping
    public ApiResponse<List<ConfigItemResponse>> getAllConfigs() {
        return ApiResponse.success(configFacade.getAllConfigs());
    }

    @GetMapping("/{key}")
    public ApiResponse<ConfigItemResponse> getConfig(@PathVariable String key) {
        return ApiResponse.success(configFacade.getConfig(key));
    }

    @PutMapping("/{key}")
    public ApiResponse<ConfigItemResponse> updateConfig(
            @PathVariable String key, @Valid @RequestBody UpdateConfigRequest req,
            @CurrentUser UUID adminUserId) {
        return ApiResponse.success(configFacade.updateConfig(key, req.value(), adminUserId));
    }

    @PostMapping("/{key}/reset")
    public ApiResponse<ConfigItemResponse> resetConfig(
            @PathVariable String key, @CurrentUser UUID adminUserId) {
        return ApiResponse.success(configFacade.resetConfig(key, adminUserId));
    }
}
