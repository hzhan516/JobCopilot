package edu.asu.ser594.resumeassistant.domain.resume.entity;

import edu.asu.ser594.resumeassistant.domain.resume.valueobject.ParseStatus;
import edu.asu.ser594.resumeassistant.domain.shared.entity.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single version of a resume within a ResumeGroup, capturing either the uploaded file or editable content.
 * 简历组中的单个版本，承载上传的原始文件或可编辑内容。
 * <p>
 * Invariants:
 * 1. ORIGINAL versions are immutable.
 * 2. Only ACTIVE versions are mutable.
 * 不变式：
 * 1. ORIGINAL 版本不可编辑
 * 2. 只有 ACTIVE 版本可编辑
 */
public final class ResumeVersion implements Entity<UUID> {

    private final UUID id;
    private final UUID groupId;
    private final VersionType versionType;
    // File metadata persisted for the original upload | 原始上传文件的元数据
    private final String originalFileName;
    private final String storedFileName;
    private final String fileType;
    private final long fileSize;
    private final String storagePath;
    private final String storageProvider;
    private final LocalDateTime createdAt;
    // Editable content and parsing state for converted / AI versions | 转换版/AI 版本的可编辑内容与解析状态
    private String content;
    private String parsedContent;
    private ParseStatus parseStatus;
    private String parseErrorMessage;
    private Status status;
    private LocalDateTime updatedAt;

    private ResumeVersion(UUID id, UUID groupId, VersionType versionType,
                          String originalFileName, String storedFileName,
                          String fileType, long fileSize, String storagePath,
                          String storageProvider, String content, String parsedContent,
                          ParseStatus parseStatus, String parseErrorMessage,
                          Status status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.groupId = groupId;
        this.versionType = versionType;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
        this.storageProvider = storageProvider;
        this.content = content;
        this.parsedContent = parsedContent;
        this.parseStatus = parseStatus;
        this.parseErrorMessage = parseErrorMessage;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ResumeVersion createOriginal(UUID groupId, String originalFileName,
                                               String fileType, long fileSize,
                                               String storagePath) {
        return new ResumeVersion(
                UUID.randomUUID(),
                groupId,
                VersionType.ORIGINAL,
                originalFileName,
                UUID.randomUUID().toString(),
                fileType,
                fileSize,
                storagePath,
                "minio",
                null,
                null,
                ParseStatus.PENDING,
                null,
                Status.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public static ResumeVersion createConverted(UUID groupId) {
        return new ResumeVersion(
                UUID.randomUUID(),
                groupId,
                VersionType.CONVERTED,
                null,
                null,
                "text/markdown",
                0L,
                null,
                null,
                "",  // Empty placeholder to be filled by parser | 空占位符，等待解析器填充
                null,
                ParseStatus.PENDING,
                null,
                Status.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // Factory methods | 工厂方法

    public static ResumeVersion reconstruct(UUID id, UUID groupId, VersionType versionType,
                                            String originalFileName, String storedFileName,
                                            String fileType, long fileSize, String storagePath,
                                            String storageProvider, String content,
                                            String parsedContent, ParseStatus parseStatus, String parseErrorMessage,
                                            Status status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ResumeVersion(id, groupId, versionType, originalFileName, storedFileName,
                fileType, fileSize, storagePath, storageProvider, content, parsedContent,
                parseStatus, parseErrorMessage, status, createdAt, updatedAt);
    }

    public static ResumeVersion createAiOptimized(UUID groupId, String content) {
        return new ResumeVersion(
                UUID.randomUUID(),
                groupId,
                VersionType.AI_OPTIMIZED,
                null,
                null,
                "text/markdown",
                0L,
                null,
                null,
                content,
                null,
                ParseStatus.PENDING,
                null,
                Status.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public void editContent(String newContent) {
        if (versionType == VersionType.ORIGINAL) {
            throw new IllegalStateException("Original version cannot be edited");
        }
        if (status != Status.ACTIVE) {
            throw new IllegalStateException("Only active version can be edited");
        }
        this.content = newContent;
        this.updatedAt = LocalDateTime.now();
    }

    public void markParsing() {
        this.parseStatus = ParseStatus.PARSING;
        this.updatedAt = LocalDateTime.now();
    }

    // Domain behaviors | 领域行为

    public void markParseCompleted(String parsedContent) {
        this.parseStatus = ParseStatus.COMPLETED;
        this.parsedContent = parsedContent;
        this.updatedAt = LocalDateTime.now();
    }

    public void markParseFailed(String errorMessage) {
        this.parseStatus = ParseStatus.FAILED;
        this.parseErrorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public void applyParsedContent(String parsedJson) {
        this.parsedContent = parsedJson;
        this.updatedAt = LocalDateTime.now();
    }

    public void archive() {
        this.status = Status.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = Status.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEditable() {
        return versionType != VersionType.ORIGINAL && status == Status.ACTIVE;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    // Property accessors | 属性访问器

    public VersionType getVersionType() {
        return versionType;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public String getContent() {
        return content;
    }

    public String getParsedContent() {
        return parsedContent;
    }

    public ParseStatus getParseStatus() {
        return parseStatus;
    }

    public String getParseErrorMessage() {
        return parseErrorMessage;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public enum VersionType {
        ORIGINAL,      // Immutable uploaded file | 不可变的上传文件
        CONVERTED,     // Editable Markdown derived from parsing | 从解析得到的可编辑 Markdown
        AI_OPTIMIZED   // Editable Markdown generated by AI copilot | AI 助手生成的可编辑 Markdown
    }

    public enum Status {
        ACTIVE,    // Currently in use and editable | 当前使用中且可编辑
        ARCHIVED   // Retained for history but immutable | 保留历史但不可编辑
    }
}
