package io.jobcopilot.resumeassistant.trigger.http.controller.admin;

import io.jobcopilot.resumeassistant.api.admin.dto.AuditLogResponse;
import io.jobcopilot.resumeassistant.api.admin.facade.AdminAuditFacade;
import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/v1/audit-logs")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditFacade adminAuditFacade;

    @GetMapping
    public ApiResponse<PageResult<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) UUID adminUserId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
                adminAuditFacade.listAuditLogs(adminUserId, action, page, size));
    }
}
