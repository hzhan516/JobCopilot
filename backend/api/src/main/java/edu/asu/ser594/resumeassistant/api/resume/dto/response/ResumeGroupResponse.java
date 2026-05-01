package edu.asu.ser594.resumeassistant.api.resume.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 简历组响应
 * Resume group response
 */
@Getter
@Builder
public class ResumeGroupResponse {

    private final UUID groupId;
    private final String title;
    private final boolean isDefault;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final VersionSummary originalVersion;
    private final VersionSummary convertedVersion;
    private final VersionSummary aiOptimizedVersion;

    /**
     * 版本摘要信息
     * Version summary information
     */
    @Getter
    @Builder
    public static class VersionSummary {
        private final UUID versionId;
        private final String status;
        private final LocalDateTime createdAt;
        private final boolean exists;
    }
}
