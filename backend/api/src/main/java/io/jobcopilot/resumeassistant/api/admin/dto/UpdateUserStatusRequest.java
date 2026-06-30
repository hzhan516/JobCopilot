package io.jobcopilot.resumeassistant.api.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull String status) {}
