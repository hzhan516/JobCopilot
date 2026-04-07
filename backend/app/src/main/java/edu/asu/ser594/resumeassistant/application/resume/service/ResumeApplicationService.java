package edu.asu.ser594.resumeassistant.application.resume.service;

import edu.asu.ser594.resumeassistant.application.resume.command.UploadResumeCommand;
import edu.asu.ser594.resumeassistant.domain.resume.entity.Resume;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeRepository;
import edu.asu.ser594.resumeassistant.domain.shared.exception.StorageException;
import edu.asu.ser594.resumeassistant.domain.shared.service.DocumentFormatConverter;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 简历应用服务
 * Resume application service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeApplicationService {

    private final ResumeRepository resumeRepository;
    private final FileStorageService fileStorageService;

    @Value("${minio.bucket-name:resumes}")
    private String bucketName;

    private static final String STORAGE_PROVIDER = "minio";
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofHours(1);

    private final DocumentFormatConverter documentFormatConverter;

    /**
     * 上传简历
     */
    @Transactional
    public Resume uploadResume(UUID userId, UploadResumeCommand command) {
        // 第一步：先保存简历记录获取ID（storagePath暂时用占位符）
        Resume initialResume = Resume.create(
                userId,
                command.getFileName(),
                command.getContentType(),
                command.getFileSize(),
                "pending", // 临时路径，稍后更新
                STORAGE_PROVIDER
        );

        // 保存获取生成的ID
        Resume savedResume = resumeRepository.save(initialResume);
        log.info("Resume record created with ID: {}", savedResume.getId());

        // 第二步：使用生成的ID构建存储路径
        String storagePath = String.format("resumes/%s/%s/%s",
                userId, savedResume.getId(), savedResume.getStoredFileName());

        // 第三步：上传文件到 MinIO
        boolean uploadSuccess = false;
        try {
            fileStorageService.upload(
                    storagePath,
                    command.getInputStream(),
                    command.getFileSize(),
                    command.getContentType()
            );
            uploadSuccess = true;
            log.info("File uploaded to storage: {}", storagePath);
        } catch (StorageException e) {
            log.error("Failed to upload resume file: {}", command.getFileName(), e);
            throw e;
        }

        // 第四步：更新简历记录的最终存储路径
        // 如果后续保存失败，需要清理已上传的文件
        try {
            Resume updatedResume = savedResume.toBuilder()
                    .storagePath(storagePath)
                    .build();
            Resume result = resumeRepository.save(updatedResume);
            log.info("Resume upload completed successfully: {}", result.getId());
            return result;
        } catch (Exception e) {
            // 如果保存失败，尝试删除已上传的文件（防止孤立文件）
            if (uploadSuccess) {
                try {
                    fileStorageService.delete(storagePath);
                    log.warn("Deleted orphaned file due to save failure: {}", storagePath);
                } catch (Exception deleteEx) {
                    log.error("Failed to delete orphaned file: {}", storagePath, deleteEx);
                }
            }
            throw e;
        }
    }

    /**
     * 根据ID和用户ID查找简历
     */
    public Optional<Resume> findResumeByIdAndUserId(UUID resumeId, UUID userId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId);
    }

    /**
     * 生成预签名下载URL
     */
    public Optional<String> generateDownloadUrl(UUID resumeId, UUID userId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .map(resume -> fileStorageService.generatePresignedUrl(
                        resume.getStoragePath(), PRESIGNED_URL_EXPIRATION));
    }

    /**
     * 删除简历
     */
    @Transactional
    public void deleteResume(UUID resumeId, UUID userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new StorageException("resume.not.found"));

        // 删除文件
        fileStorageService.delete(resume.getStoragePath());

        // 删除记录
        resumeRepository.delete(resume);
    }

    /**
     * Download resume with format conversion
     * 下载简历（支持格式转换）
     *
     * @param resumeId     Resume ID
     * @param userId       User ID
     * @param targetFormat Target format (pdf, docx, md, txt, html)
     * @return InputStream of converted document
     */
    public InputStream downloadResumeWithFormat(UUID resumeId, UUID userId, String targetFormat) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new StorageException("resume.not.found"));

        InputStream sourceStream = fileStorageService.download(resume.getStoragePath())
                .orElseThrow(() -> new StorageException("resume.file.not.found"));

        String sourceFormat = normalizeFormat(resume.getFileType());
        String tf = normalizeFormat(targetFormat);

        // Same format - return as-is
        if (sourceFormat.equals(tf) || tf.equals("original")) {
            return sourceStream;
        }

        // Perform conversion (with chain support for MD -> PDF)
        try {
            InputStream result = performConversion(sourceStream, sourceFormat, tf);
            if (result == null) {
                throw new IOException("No converter found for: " + sourceFormat + " -> " + tf);
            }
            return result;
        } catch (IOException e) {
            log.error("Format conversion failed: {} -> {}", sourceFormat, tf, e);
            throw new StorageException("resume.conversion.failed", e);
        }
    }

    /**
     * Perform format conversion with chain support
     * 执行格式转换（支持链式转换）
     */
    private InputStream performConversion(InputStream source, String sourceFormat, String targetFormat) throws IOException {
        // Check if direct conversion is supported
        if (documentFormatConverter.supports(sourceFormat, targetFormat)) {
            return documentFormatConverter.convert(source, sourceFormat, targetFormat);
        }
        
        return null;
    }

    /**
     * Get resume metadata for download
     */
    public Resume getResumeForDownload(UUID resumeId, UUID userId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new StorageException("resume.not.found"));
    }

    private String normalizeFormat(String format) {
        if (format == null) return "";
        String f = format.toLowerCase();
        
        // Handle MIME types
        if (f.equals("text/markdown") || f.equals("text/x-markdown")) return "md";
        if (f.equals("application/pdf")) return "pdf";
        if (f.equals("text/plain")) return "txt";
        if (f.equals("text/html")) return "html";
        if (f.equals("application/msword") || f.contains("wordprocessingml")) return "docx";

        // Handle cases where format might be a filename/path
        if (f.contains(".")) {
            f = f.substring(f.lastIndexOf('.') + 1);
        }

        return switch (f) {
            case "markdown" -> "md";
            case "word" -> "docx";
            case "text" -> "txt";
            default -> f;
        };
    }

}
