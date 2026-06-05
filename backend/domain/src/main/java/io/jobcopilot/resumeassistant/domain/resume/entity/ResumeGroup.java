package io.jobcopilot.resumeassistant.domain.resume.entity;

import io.jobcopilot.resumeassistant.domain.shared.entity.AggregateRoot;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root that groups all versions of a single resume under one user.
 * 将单一简历的所有版本聚合在同一用户下的聚合根。
 * <p>
 * Invariants:
 * 1. Each group is owned by exactly one user.
 * 2. Only one ACTIVE version is allowed per version type (ORIGINAL / CONVERTED / AI).
 * 不变式：
 * 1. 每个组必须属于一个用户
 * 2. 每种版本类型（ORIGINAL/CONVERTED/AI）只能有一个 ACTIVE 版本
 */
public final class ResumeGroup extends AggregateRoot<UUID> {

    private final UUID id;
    @Getter
    private final UUID userId;
    @Getter
    private final LocalDateTime createdAt;
    private final List<ResumeVersion> versions;
    @Getter
    private String title;
    @Getter
    private boolean isDefault;
    @Getter
    private LocalDateTime updatedAt;
    @Getter
    private long version;

    private ResumeGroup(UUID id, UUID userId, String title, boolean isDefault,
                        LocalDateTime createdAt, LocalDateTime updatedAt,
                        List<ResumeVersion> versions, long version) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.versions = versions != null ? new ArrayList<>(versions) : new ArrayList<>();
        this.version = version;
    }

    public static ResumeGroup create(UUID userId, String title) {
        return new ResumeGroup(
                UUID.randomUUID(),
                userId,
                title != null ? title : "Untitled Resume",
                false,
                LocalDateTime.now(),
                LocalDateTime.now(),
                new ArrayList<>(),
                0L
        );
    }

    /**
     * Reconstructs the aggregate from persisted state without re-evaluating business rules.
     * 从持久化状态重建聚合，不重新评估业务规则。
     */
    public static ResumeGroup reconstruct(UUID id, UUID userId, String title,
                                          boolean isDefault, LocalDateTime createdAt,
                                          LocalDateTime updatedAt, List<ResumeVersion> versions,
                                          long version) {
        return new ResumeGroup(id, userId, title, isDefault, createdAt, updatedAt, versions, version);
    }

    // Domain behaviors | 领域行为

    /**
     * Stores the original file and immediately provisions a blank converted version for editing.
     * 存储原始文件并立即创建一个空白转换版本供用户编辑。
     */
    public void uploadOriginalVersion(String fileName, String fileType, long fileSize, String storagePath) {
        ResumeVersion original = ResumeVersion.createOriginal(
                this.id, fileName, fileType, fileSize, storagePath);
        addVersion(original);

        ResumeVersion converted = ResumeVersion.createConverted(this.id);
        addVersion(converted);
    }

    /**
     * Adds a version to the group while enforcing the single-ACTIVE-per-type invariant.
     * 将版本加入组，同时强制每种类型只能有一个 ACTIVE 版本的不变式。
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

    public ResumeVersion getActiveVersionByType(ResumeVersion.VersionType type) {
        return versions.stream()
                .filter(v -> v.getVersionType() == type)
                .filter(v -> v.getStatus() == ResumeVersion.Status.ACTIVE)
                .findFirst()
                .orElse(null);
    }

    public List<ResumeVersion> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    public void setAsDefault() {
        this.isDefault = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeTitle(String newTitle) {
        this.title = newTitle != null ? newTitle : this.title;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reactivates a previously archived version and archives the currently active one of the same type.
     * 重新激活已归档版本，并将同类型当前 ACTIVE 版本归档。
     *
     * @throws IllegalArgumentException if version not found in group
     * @throws IllegalStateException    if version is ORIGINAL or already ACTIVE
     */
    public void activateVersion(UUID versionId) {
        ResumeVersion target = versions.stream()
                .filter(v -> v.getId().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Version not found in group"));

        if (target.getVersionType() == ResumeVersion.VersionType.ORIGINAL) {
            throw new IllegalStateException("Original version cannot be activated");
        }
        if (target.getStatus() == ResumeVersion.Status.ACTIVE) {
            throw new IllegalStateException("Version is already active");
        }

        // Archive the current ACTIVE version of the same type to maintain the invariant | 归档同类型当前 ACTIVE 版本以保持不变式
        versions.stream()
                .filter(v -> v.getVersionType() == target.getVersionType())
                .filter(v -> v.getStatus() == ResumeVersion.Status.ACTIVE)
                .findFirst()
                .ifPresent(ResumeVersion::archive);

        target.activate();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(UUID userId) {
        return this.userId.equals(userId);
    }

    @Override
    public UUID getId() {
        return id;
    }

}
