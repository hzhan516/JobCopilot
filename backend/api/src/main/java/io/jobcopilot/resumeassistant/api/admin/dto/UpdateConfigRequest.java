package io.jobcopilot.resumeassistant.api.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateConfigRequest(@NotNull String value) {}
