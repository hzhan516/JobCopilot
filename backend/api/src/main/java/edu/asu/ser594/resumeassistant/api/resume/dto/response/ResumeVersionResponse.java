package edu.asu.ser594.resumeassistant.api.resume.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
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
    private final boolean editable;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
