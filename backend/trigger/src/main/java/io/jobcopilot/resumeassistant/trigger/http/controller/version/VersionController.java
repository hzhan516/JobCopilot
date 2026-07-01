package io.jobcopilot.resumeassistant.trigger.http.controller.version;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.version.dto.VersionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 公开版本查询端点 / Public version endpoint */
@RestController
@RequestMapping("/v1/version")
public class VersionController {

    @Value("${app.version:dev}")
    private String version;

    @GetMapping
    public ApiResponse<VersionResponse> getVersion() {
        return ApiResponse.success(new VersionResponse(version, "backend"));
    }
}
