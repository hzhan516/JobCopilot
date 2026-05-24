package io.jobcopilot.resumeassistant.domain.resume.service;

import io.jobcopilot.resumeassistant.domain.shared.service.DocumentFormatConverter;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import io.jobcopilot.resumeassistant.domain.shared.valueobject.DocumentFormat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Converts uploaded resume files to Markdown for immediate editing.
 * Non-blocking: failures return null without throwing.
 * 将上传的简历文件转换为 Markdown 以便立即编辑。非阻塞：失败时返回 null 而不抛异常。
 */
public final class ResumeConverterService {

    private final FileStorageService fileStorageService;
    private final DocumentFormatConverter documentFormatConverter;

    public ResumeConverterService(FileStorageService fileStorageService,
                                  DocumentFormatConverter documentFormatConverter) {
        this.fileStorageService = fileStorageService;
        this.documentFormatConverter = documentFormatConverter;
    }

    /**
     * Downloads the uploaded file and converts it to Markdown.
     * If the source is already Markdown, returns raw content.
     * 下载上传的文件并将其转换为 Markdown。若源文件已是 Markdown 则返回原始内容。
     *
     * @param storagePath Storage key / 存储键
     * @param contentType MIME type / MIME 类型
     * @return Markdown string, or null on failure / Markdown 字符串，失败时为 null
     */
    public String convertToMarkdown(String storagePath, String contentType) {
        try (InputStream rawStream = fileStorageService.download(storagePath).orElse(null)) {
            if (rawStream == null) {
                return null;
            }

            DocumentFormat sourceFormat = DocumentFormat.fromMimeType(contentType);
            if (!"md".equals(sourceFormat.getFormat())) {
                try (InputStream mdStream = documentFormatConverter.convert(
                        rawStream, sourceFormat.getFormat(), "md")) {
                    return new String(mdStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            } else {
                return new String(rawStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
