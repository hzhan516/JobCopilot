package io.jobcopilot.resumeassistant.application.admin;

import io.jobcopilot.resumeassistant.api.admin.dto.AdminUserListRequest;
import io.jobcopilot.resumeassistant.api.admin.dto.AdminUserResponse;
import io.jobcopilot.resumeassistant.api.admin.facade.AdminUserFacade;
import io.jobcopilot.resumeassistant.domain.admin.entity.AuditLog;
import io.jobcopilot.resumeassistant.domain.admin.repository.AuditLogRepository;
import io.jobcopilot.resumeassistant.domain.user.entity.User;
import io.jobcopilot.resumeassistant.domain.user.repository.UserRepository;
import io.jobcopilot.resumeassistant.types.common.PageResult;
import io.jobcopilot.resumeassistant.types.enums.UserRole;
import io.jobcopilot.resumeassistant.types.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class AdminUserFacadeImpl implements AdminUserFacade {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminUserResponse> listUsers(AdminUserListRequest req) {
        var result = userRepository.findAll(req.page(), req.size());
        return PageResult.of(
                result.content().stream().map(this::toResponse).toList(),
                req.page(), req.size(), result.totalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserDetail(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toResponse(user);
    }

    @Override
    public AdminUserResponse updateUserRole(UUID id, String role, UUID adminUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.updateRole(UserRole.valueOf(role));
        var saved = userRepository.save(user);
        audit(adminUserId, "UPDATE_ROLE", "user", id.toString(), "role=" + role);
        return toResponse(saved);
    }

    @Override
    public AdminUserResponse updateUserStatus(UUID id, String status, UUID adminUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.updateStatus(UserStatus.valueOf(status));
        var saved = userRepository.save(user);
        audit(adminUserId, "UPDATE_STATUS", "user", id.toString(), "status=" + status);
        return toResponse(saved);
    }

    @Override
    public void deleteUser(UUID id, UUID adminUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.updateStatus(UserStatus.DELETED);
        userRepository.save(user);
        audit(adminUserId, "DELETE_USER", "user", id.toString(), null);
    }

    private AdminUserResponse toResponse(User u) {
        return AdminUserResponse.builder()
                .id(u.getId()).email(u.getEmail())
                .role(u.getRole().name()).status(u.getStatus().name())
                .authProvider(u.getAuthProvider().name())
                .emailVerified(u.isEmailVerified())
                .resumeCount(0).jobCount(0).conversationCount(0) // ponytail: counts deferred to Phase 3 monitoring
                .createdAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : null)
                .updatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : null)
                .build();
    }

    private void audit(UUID adminUserId, String action, String targetType, String targetId, String details) {
        var log = AuditLog.builder()
                .id(UUID.randomUUID()).adminUserId(adminUserId)
                .action(action).targetType(targetType).targetId(targetId)
                .details(details).createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }
}
