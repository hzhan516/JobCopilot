package edu.asu.ser594.resumeassistant.application.resume.query;

import lombok.Builder;

import java.util.UUID;

/**
 * 下载简历查询
 * Download Resume Query
 */
@Builder
public record ResumeDownloadQuery(
        UUID versionId,
        UUID userId,
        String targetFormat
) {
}
