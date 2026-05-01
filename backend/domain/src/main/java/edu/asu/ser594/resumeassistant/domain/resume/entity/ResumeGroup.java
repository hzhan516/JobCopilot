package edu.asu.ser594.resumeassistant.domain.resume.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.AggregateRoot;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 简历组聚合根
 * Resume Group Aggregate Root
 * <p>
 * 不变式（Invariants）：
 * 1. 每个组必须属于一个用户
 * 2. 每种版本类型（ORIGINAL/CONVERTED/AI）只能有一个ACTIVE版本
 */
public final class ResumeGroup extends AggregateRoot<UUID> {

    private final UUID id;
    @Getter
    private final UUID userId;
    @Getter
    private String title;
    @Getter
    private boolean isDefault;
    @Getter
    private final LocalDateTime createdAt;
    @Getter
    private LocalDateTime updatedAt;
    private final List<ResumeVersion> versions;

    private ResumeGroup(UUID id, UUID userId, String title, boolean isDefault,
                        LocalDateTime createdAt, LocalDateTime updatedAt,
                        List<ResumeVersion> versions) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.versions = versions != null ? new ArrayList<>(versions) : new ArrayList<>();
    }

    /**
     * 工厂方法：创建新的简历组
     * Factory method: create new resume group
     */
    public static ResumeGroup create(UUID userId, String title) {
        return new ResumeGroup(
                UUID.randomUUID(),
                userId,
                title != null ? title : "Untitled Resume",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                new ArrayList<>()
        );
    }

    /**
     * 重建聚合（从仓储恢复）
     * Reconstruct aggregate from persistence
     */
    public static ResumeGroup reconstruct(UUID id, UUID userId, String title,
                                          boolean isDefault, LocalDateTime createdAt,
                                          LocalDateTime updatedAt, List<ResumeVersion> versions) {
        return new ResumeGroup(id, userId, title, isDefault, createdAt, updatedAt, versions);
    }

    // ==================== 领域行为 ====================
    // Domain behaviors

    /**
     * 上传并添加原版简历
     * Upload and add original resume
     * 业务规则：自动创建对应的转换版（空白）
     * Business rule: automatically create corresponding converted version (blank)
     */
    public void uploadOriginalVersion(String fileName, String fileType, long fileSize, String storagePath) {
        ResumeVersion original = ResumeVersion.createOriginal(
                this.id, fileName, fileType, fileSize, storagePath);
        addVersion(original);

        ResumeVersion converted = ResumeVersion.createConverted(this.id);
        addVersion(converted);
    }

    /**
     * 添加版本到组
     * Add version to group
     * 业务规则：同类型ACTIVE版本自动归档
     * Business rule: auto-archive ACTIVE version of same type
     */
    public ResumeVersion addVersion(ResumeVersion newVersion) {
        versions.stream()
                .filter(v -> v.getVersionType() == newVersion.getVersionType())
                .filter(v -> v.getStatus() == ResumeVersion.Status.ACTIVE)
                .findFirst()
                .ifPresent(ResumeVersion::archive);

        versions.add(newVersion);
        this.updatedAt = LocalDateTime.now();
        return newVersion;
    }

    /**
     * 获取指定类型的ACTIVE版本
     * Get ACTIVE version of specified type
     */
    public ResumeVersion getActiveVersionByType(ResumeVersion.VersionType type) {
        return versions.stream()
                .filter(v -> v.getVersionType() == type)
                .filter(v -> v.getStatus() == ResumeVersion.Status.ACTIVE)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有版本（只读）
     * Get all versions (read-only)
     */
    public List<ResumeVersion> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    /**
     * 设为默认简历
     * Set as default resume
     */
    public void setAsDefault() {
        this.isDefault = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新标题
     * Update title
     */
    public void changeTitle(String newTitle) {
        this.title = newTitle != null ? newTitle : this.title;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查用户所有权
     * Check user ownership
     */
    public boolean isOwnedBy(UUID userId) {
        return this.userId.equals(userId);
    }

    @Override
    public UUID getId() {
        return id;
    }

}