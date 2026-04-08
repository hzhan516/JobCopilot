package edu.asu.ser594.resumeassistant.domain.resume.repository;

import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;

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
     */
    void save(ResumeGroup group);

    /**
     * 根据ID查找
     */
    Optional<ResumeGroup> findById(UUID groupId);

    /**
     * 根据ID和用户ID查找（权限检查）
     */
    Optional<ResumeGroup> findByIdAndUserId(UUID groupId, UUID userId);

    /**
     * 查找用户的所有简历组
     */
    List<ResumeGroup> findAllByUserId(UUID userId);

    /**
     * 查找用户的默认简历组
     */
    Optional<ResumeGroup> findDefaultByUserId(UUID userId);

    /**
     * 删除简历组
     */
    void delete(UUID groupId);

    /**
     * 清除用户的默认标记
     */
    void clearDefaultForUser(UUID userId);
}