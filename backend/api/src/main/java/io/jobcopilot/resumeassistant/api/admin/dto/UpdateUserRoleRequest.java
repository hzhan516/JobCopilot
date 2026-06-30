package io.jobcopilot.resumeassistant.api.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull String role) {}
