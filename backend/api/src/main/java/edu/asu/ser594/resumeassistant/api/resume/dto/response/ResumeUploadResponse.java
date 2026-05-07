package edu.asu.ser594.resumeassistant.api.resume.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 简历上传响应
 * Resume upload response
 */
@Getter
@Builder
public class ResumeUploadResponse {

    private final UUID groupId;
    private final UUID originalVersionId;
    private final String title;
    private final OffsetDateTime createdAt;
}
