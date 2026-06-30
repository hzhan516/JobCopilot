package io.jobcopilot.resumeassistant.api.admin.dto;

import lombok.Builder;

@Builder
public record SystemStatsResponse(
    long userCount, long resumeCount, long jobCount,
    long conversationCount, long aiCallCount, long applicationCount
) {}
