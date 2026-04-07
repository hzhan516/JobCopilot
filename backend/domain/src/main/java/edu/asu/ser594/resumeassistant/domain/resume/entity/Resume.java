package edu.asu.ser594.resumeassistant.domain.resume.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.AggregateRoot;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 简历聚合根
 * Resume aggregate root
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Resume extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID userId;
    private String title;
    private String originalFileName;
    private String storedFileName;
    private String fileType;
    private long fileSize;
    private String storagePath;
    private String storageProvider;
    private ProcessingStatus processingStatus;
    private String parsedContent;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public static Resume create(UUID userId, String originalFileName,
                                String fileType, long fileSize,
                                String storagePath, String storageProvider) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String storedFileName = generateStoredFileName(originalFileName);

        return Resume.builder()
                .id(id)
                .userId(userId)
                .title(originalFileName)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .fileType(fileType)
                .fileSize(fileSize)
                .storagePath(storagePath)
                .storageProvider(storageProvider)
                .processingStatus(ProcessingStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static String generateStoredFileName(String originalFileName) {
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }

    public void markProcessing() {
        this.processingStatus = ProcessingStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted(String parsedContent) {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.parsedContent = parsedContent;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.processingStatus = ProcessingStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public UUID getId() {
        return id;
    }
}
