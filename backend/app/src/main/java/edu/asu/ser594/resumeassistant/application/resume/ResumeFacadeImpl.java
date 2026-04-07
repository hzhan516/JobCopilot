package edu.asu.ser594.resumeassistant.application.resume;

import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeUploadRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeUploadResponse;
import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import edu.asu.ser594.resumeassistant.application.resume.command.UploadResumeCommand;
import edu.asu.ser594.resumeassistant.application.resume.service.ResumeApplicationService;
import edu.asu.ser594.resumeassistant.domain.resume.entity.Resume;
import edu.asu.ser594.resumeassistant.domain.shared.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j

/**
 * 简历门面实现
 * Resume facade implementation
 */
@Component
@RequiredArgsConstructor
public class ResumeFacadeImpl implements ResumeFacade {

    private final ResumeApplicationService resumeService;

    // 允许的文件类型
    private static final String[] ALLOWED_CONTENT_TYPES = {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/markdown",
            "text/plain"
    };

    // 最大文件大小: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Override
    public ResumeUploadResponse uploadResume(ResumeUploadRequest request, UUID userId) {
        MultipartFile file = request.getFile();

        // 验证文件
        validateFile(file);

        try {
            UploadResumeCommand command = UploadResumeCommand.builder()
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .inputStream(file.getInputStream())
                    .title(request.getTitle() != null ? request.getTitle() : file.getOriginalFilename())
                    .build();

            Resume resume = resumeService.uploadResume(userId, command);

            return ResumeUploadResponse.builder()
                    .resumeId(resume.getId())
                    .fileName(resume.getOriginalFileName())
                    .fileSize(resume.getFileSize())
                    .status(resume.getProcessingStatus().name())
                    .uploadedAt(resume.getCreatedAt())
                    .build();

        } catch (IOException e) {
            throw new StorageException("resume.upload.io.error", e);
        }
    }

    @Override
    public ResponseEntity<InputStreamResource> downloadResume(UUID resumeId, UUID userId, String exportFormat) {
        // Get resume metadata
        Resume resume = resumeService.getResumeForDownload(resumeId, userId);

        // Get file stream (with conversion if needed)
        InputStream inputStream = resumeService.downloadResumeWithFormat(resumeId, userId, exportFormat);

        // Determine output filename and Content-Type
        String outputFileName = determineOutputFileName(resume.getOriginalFileName(), exportFormat);
        String contentType = determineContentType(exportFormat, resume.getFileType());

        // Encode filename for Content-Disposition
        String encodedFileName = URLEncoder.encode(outputFileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String asciiFileName = outputFileName.replaceAll("[^\\x20-\\x7E]", "_");
        String contentDisposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                asciiFileName,
                encodedFileName
        );

        log.info("Downloading resume: {}, format: {} -> {}, filename: {}",
                resumeId, resume.getFileType(), exportFormat, outputFileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(inputStream));
    }

    /**
     * 根据导出格式确定输出文件名
     */
    private String determineOutputFileName(String originalFileName, String exportFormat) {
        if (exportFormat == null || exportFormat.isEmpty() || "original".equalsIgnoreCase(exportFormat)) {
            return originalFileName;
        }
        
        // 获取原始文件名（不含扩展名）
        String baseName = originalFileName;
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalFileName.substring(0, dotIndex);
        }
        
        // 根据导出格式添加对应扩展名
        return baseName + "." + exportFormat.toLowerCase();
    }

    /**
     * 根据导出格式确定Content-Type
     */
    private String determineContentType(String exportFormat, String originalContentType) {
        if (exportFormat == null || exportFormat.isEmpty() || "original".equalsIgnoreCase(exportFormat)) {
            return originalContentType;
        }
        
        return switch (exportFormat.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            case "html", "htm" -> "text/html";
            case "md", "markdown" -> "text/markdown";
            case "txt" -> "text/plain";
            default -> originalContentType;
        };
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("validation.file.required");
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new StorageException("resume.upload.size.exceeded");
        }

        // 检查文件类型
        boolean allowed = false;
        String contentType = file.getContentType();
        for (String allowedType : ALLOWED_CONTENT_TYPES) {
            if (allowedType.equals(contentType)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new StorageException("resume.upload.type.invalid");
        }
    }
}
