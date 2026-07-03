package io.jobcopilot.resumeassistant.domain.resume.repository;

import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 简历组仓储接口
 * Resume Group Repository Interface
 */
public interface ResumeGroupRepository {

    /**
     * 保存聚合（新增或更新）
     * Save aggregate (create or update)
     */
    void save(ResumeGroup group);

    /**
     * 根据ID查找
     * Find by ID
     */
    Optional<ResumeGroup> findById(UUID groupId);

    /**
     * 根据ID和用户ID查找（权限检查）
     * Find by ID and user ID (permission check)
     */
    Optional<ResumeGroup> findByIdAndUserId(UUID groupId, UUID userId);

    /**
     * 查找用户的所有简历组
     * Find all resume groups for user
     */
    List<ResumeGroup> findAllByUserId(UUID userId);

    /**
     * 统计用户的简历组数量
     * Count resume groups for user
     */
    long countByUserId(UUID userId);

    /**
     * 统计所有简历组数量
     * Count all resume groups
     */
    long count();

    /**
     * 查找用户的默认简历组
     * Find default resume group for user
     */
    Optional<ResumeGroup> findDefaultByUserId(UUID userId);

    /**
     * 删除简历组
     * Delete resume group
     */
    void delete(UUID groupId);

    /**
     * 清除用户的默认标记
     * Clear user's default flag
     */
    void clearDefaultForUser(UUID userId);
}