package io.jobcopilot.resumeassistant.trigger.http.controller.admin;

import io.jobcopilot.resumeassistant.api.admin.dto.AdminUserListRequest;
import io.jobcopilot.resumeassistant.api.admin.dto.AdminUserResponse;
import io.jobcopilot.resumeassistant.api.admin.dto.UpdateUserRoleRequest;
import io.jobcopilot.resumeassistant.api.admin.dto.UpdateUserStatusRequest;
import io.jobcopilot.resumeassistant.api.admin.facade.AdminUserFacade;
import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.trigger.http.security.CurrentUser;
import io.jobcopilot.resumeassistant.types.common.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/v1/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserFacade adminUserFacade;

    @GetMapping
    public ApiResponse<PageResult<AdminUserResponse>> listUsers(@Valid AdminUserListRequest request) {
        return ApiResponse.success(adminUserFacade.listUsers(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> getUserDetail(@PathVariable UUID id) {
        return ApiResponse.success(adminUserFacade.getUserDetail(id));
    }

    @PutMapping("/{id}/role")
    public ApiResponse<AdminUserResponse> updateUserRole(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserRoleRequest request,
            @CurrentUser UUID adminUserId) {
        return ApiResponse.success(adminUserFacade.updateUserRole(id, request.role(), adminUserId));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AdminUserResponse> updateUserStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserStatusRequest request,
            @CurrentUser UUID adminUserId) {
        return ApiResponse.success(adminUserFacade.updateUserStatus(id, request.status(), adminUserId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable UUID id, @CurrentUser UUID adminUserId) {
        adminUserFacade.deleteUser(id, adminUserId);
        return ApiResponse.success("User deleted", null);
    }
}
