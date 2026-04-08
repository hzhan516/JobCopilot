package edu.asu.ser594.resumeassistant.application.resume.query;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 下载简历查询
 * Download Resume Query
 */
@Getter
@Builder
public final class ResumeDownloadQuery {
    private final UUID versionId;
    private final UUID userId;
    private final String targetFormat;
}
