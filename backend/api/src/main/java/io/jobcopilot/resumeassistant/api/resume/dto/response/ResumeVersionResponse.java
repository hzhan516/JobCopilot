package io.jobcopilot.resumeassistant.api.resume.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 简历版本响应
 * Resume version response
 */
@Getter
@Builder
public class ResumeVersionResponse {

    private final UUID versionId;
    private final UUID groupId;
    private final String versionType;
    private final String status;
    private final String originalFileName;
    private final String fileType;
    private final long fileSize;
    private final String content;
    private final String parseStatus;
    private final boolean editable;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
}
