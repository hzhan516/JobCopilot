package edu.asu.ser594.resumeassistant.application.resume.dto;

import lombok.Builder;

import java.io.InputStream;

/**
 * 简历下载结果
 * Resume Download Result
 */
@Builder
public record ResumeDownloadResult(
    InputStream inputStream,
    String fileName,
    String contentType
) {}
