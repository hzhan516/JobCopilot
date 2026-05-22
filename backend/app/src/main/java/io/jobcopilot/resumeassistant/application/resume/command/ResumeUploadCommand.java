package io.jobcopilot.resumeassistant.application.resume.command;

import lombok.Builder;

import java.io.InputStream;

/**
 * 上传简历命令
 * Upload resume command
 */
@Builder
public record ResumeUploadCommand(
        String fileName,
        String contentType,
        long fileSize,
        InputStream inputStream,
        String title
) {
}
