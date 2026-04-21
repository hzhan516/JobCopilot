package edu.asu.ser594.resumeassistant.domain.tracking.repository;

import edu.asu.ser594.resumeassistant.domain.tracking.entity.ApplicationTracking;
import edu.asu.ser594.resumeassistant.domain.tracking.valueobject.ApplicationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 求职申请跟踪仓储接口
 * Application tracking repository interface
 */
public interface ApplicationTrackingRepository {

    /**
     * 保存跟踪记录
     * Save tracking record
     *
     * @param tracking 跟踪实体 / Tracking entity
     * @return 保存后的实体 / Saved entity
     */
    ApplicationTracking save(ApplicationTracking tracking);

    /**
     * 根据ID查询
     * Find by ID
     *
     * @param id 跟踪ID / Tracking ID
     * @return 跟踪实体(可选) / Optional tracking entity
     */
    Optional<ApplicationTracking> findById(String id);

    /**
     * 根据ID和用户ID查询
     * Find by ID and user ID
     *
     * @param id 跟踪ID / Tracking ID
     * @param userId 用户ID / User ID
     * @return 跟踪实体(可选) / Optional tracking entity
     */
    Optional<ApplicationTracking> findByIdAndUserId(String id, UUID userId);

    /**
     * 查询用户的所有跟踪记录
     * Find all tracking records by user ID
     *
     * @param userId 用户ID / User ID
     * @return 跟踪记录列表 / List of tracking records
     */
    List<ApplicationTracking> findAllByUserId(UUID userId);

    /**
     * 根据用户ID和状态查询
     * Find by user ID and status
     *
     * @param userId 用户ID / User ID
     * @param status 状态 / Status
     * @return 跟踪记录列表 / List of tracking records
     */
    List<ApplicationTracking> findAllByUserIdAndStatus(UUID userId, ApplicationStatus status);

    /**
     * 删除跟踪记录
     * Delete tracking record
     *
     * @param id 跟踪ID / Tracking ID
     */
    void deleteById(String id);
}
