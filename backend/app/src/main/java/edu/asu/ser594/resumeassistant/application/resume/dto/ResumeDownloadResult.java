package edu.asu.ser594.resumeassistant.application.resume.dto;

import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

/**
 * 简历下载结果
 * Resume Download Result
 */
@Getter
@Builder
public final class ResumeDownloadResult {
    private final InputStream inputStream;
    private final String fileName;
    private final String contentType;
}
