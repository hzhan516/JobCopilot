package edu.asu.ser594.resumeassistant.api.resume.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
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
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
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
        private final String parseStatus;
        private final OffsetDateTime createdAt;
        private final boolean exists;
    }
}
