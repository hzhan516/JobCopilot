package io.jobcopilot.resumeassistant.api.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminUserListRequest(
    String role, String status, String email,
    @Min(0) int page, @Min(1) @Max(100) int size
) {}
