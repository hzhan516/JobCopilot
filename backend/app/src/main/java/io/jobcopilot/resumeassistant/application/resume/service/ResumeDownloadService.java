package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.application.resume.dto.ResumeDownloadResult;
import io.jobcopilot.resumeassistant.application.resume.query.ResumeDownloadQuery;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.shared.exception.StorageException;
import io.jobcopilot.resumeassistant.domain.shared.service.DocumentFormatConverter;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import io.jobcopilot.resumeassistant.domain.shared.valueobject.DocumentFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handles resume file download and on-the-fly format conversion.
 * 处理简历文件下载及实时格式转换。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeDownloadService {

    private final FileStorageService fileStorageService;
    private final DocumentFormatConverter documentFormatConverter;

    public ResumeDownloadResult download(ResumeVersion version, String targetFormat) {
        InputStream sourceStream;
        DocumentFormat sourceFormat;

        if (version.getVersionType() == ResumeVersion.VersionType.ORIGINAL) {
            sourceStream = fileStorageService.download(version.getStoragePath())
                    .orElseThrow(() -> new StorageException("file.not.found"));
            sourceFormat = DocumentFormat.fromMimeType(version.getFileType());
        } else {
            sourceStream = new ByteArrayInputStream(
                    version.getContent().getBytes(StandardCharsets.UTF_8));
            sourceFormat = DocumentFormat.fromFormatString("md");
        }

        DocumentFormat requestedFormat = DocumentFormat.fromFormatString(targetFormat);
        if (!"original".equalsIgnoreCase(targetFormat) && !sourceFormat.equals(requestedFormat)) {
            try {
                sourceStream = documentFormatConverter.convert(
                        sourceStream, sourceFormat.getFormat(), requestedFormat.getFormat());
                sourceFormat = requestedFormat;
            } catch (IOException e) {
                log.error("Conversion failed: {} -> {}", sourceFormat.getFormat(), requestedFormat.getFormat(), e);
                throw new StorageException("conversion.failed", e);
            }
        }

        return ResumeDownloadResult.builder()
                .inputStream(sourceStream)
                .fileName(sourceFormat.generateOutputFileName(version.getOriginalFileName()))
                .contentType(sourceFormat.getMimeType())
                .build();
    }
}
