package io.jobcopilot.resumeassistant.api.admin.facade;

import io.jobcopilot.resumeassistant.api.admin.dto.AdminUserListRequest;
import io.jobcopilot.resumeassistant.api.admin.dto.AdminUserResponse;
import io.jobcopilot.resumeassistant.types.common.PageResult;

import java.util.UUID;

public interface AdminUserFacade {
    PageResult<AdminUserResponse> listUsers(AdminUserListRequest request);
    AdminUserResponse getUserDetail(UUID id);
    AdminUserResponse updateUserRole(UUID id, String role, UUID adminUserId);
    AdminUserResponse updateUserStatus(UUID id, String status, UUID adminUserId);
    void deleteUser(UUID id, UUID adminUserId);
}
