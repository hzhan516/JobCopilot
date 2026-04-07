package edu.asu.ser594.resumeassistant.api.resume.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 简历上传响应
 * Resume upload response
 */
@Getter
@Builder
public class ResumeUploadResponse {

    private final UUID resumeId;
    private final String fileName;
    private final long fileSize;
    private final String status;
    private final LocalDateTime uploadedAt;
}
