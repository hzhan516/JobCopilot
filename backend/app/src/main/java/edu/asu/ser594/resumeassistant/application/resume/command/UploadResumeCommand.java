package edu.asu.ser594.resumeassistant.application.resume.command;

import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

/**
 * 上传简历命令
 * Upload resume command
 */
@Getter
@Builder
public class UploadResumeCommand {

    private final String fileName;
    private final String contentType;
    private final long fileSize;
    private final InputStream inputStream;
    private final String title;
}
