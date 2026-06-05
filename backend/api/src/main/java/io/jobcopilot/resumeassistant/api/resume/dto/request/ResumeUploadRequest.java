package io.jobcopilot.resumeassistant.api.resume.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历上传请求
 * Resume upload request
 */
@Getter
@Builder
public class ResumeUploadRequest {

    @NotNull(message = "{validation.file.required}")
    private MultipartFile file;

    private String title;
}