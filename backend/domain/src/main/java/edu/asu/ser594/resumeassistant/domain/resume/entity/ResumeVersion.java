package edu.asu.ser594.resumeassistant.domain.resume.entity;

import edu.asu.ser594.resumeassistant.domain.resume.valueobject.ParseStatus;
import edu.asu.ser594.resumeassistant.domain.shared.entity.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 简历版本实体
 * Resume Version Entity
 * <p>
 * 不变式：
 * 1. ORIGINAL版本不可编辑
 * 2. 只有ACTIVE版本可编辑
 */
public final class ResumeVersion implements Entity<UUID> {

    private final UUID id;
    private final UUID groupId;
    private final VersionType versionType;
    // 文件信息（原版特有） / File info (original version only)
    private final String originalFileName;
    private final String storedFileName;
    private final String fileType;
    private final long fileSize;
    private final String storagePath;
    private final String storageProvider;
    private final LocalDateTime createdAt;
    // 内容（转换版/AI版特有） / Content (converted/AI version only)
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
                "",  // 空内容待填充 / Empty content to be filled
                null,
                ParseStatus.NOT_APPLICABLE,
                null,
                Status.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ==================== 工厂方法 ====================
    // Factory methods

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

    /**
     * 创建 AI 优化版本
     * Create AI optimized version
     */
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
                ParseStatus.NOT_APPLICABLE,
                null,
                Status.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    /**
     * 编辑内容
     * Edit content
     *
     * @throws IllegalStateException if not editable
     */
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

    // ==================== 领域行为 ====================
    // Domain behaviors

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

    /**
     * 应用解析后的结构化内容
     * Apply parsed structured content
     */
    public void applyParsedContent(String parsedJson) {
        this.parsedContent = parsedJson;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 归档此版本
     * Archive this version
     */
    public void archive() {
        this.status = Status.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 激活此版本
     * Activate this version
     */
    public void activate() {
        this.status = Status.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否可编辑
     * Check if editable
     */
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

    // 属性访问器
    // ==================== Getters ====================

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
        ORIGINAL,      // 原版 - 只读 / Original version - read-only
        CONVERTED,     // 转换版 - 可编辑Markdown / Converted version - editable Markdown
        AI_OPTIMIZED   // AI版 - 可编辑Markdown / AI version - editable Markdown
    }

    public enum Status {
        ACTIVE,    // 活跃 / Active
        ARCHIVED   // 已归档 / Archived
    }
}