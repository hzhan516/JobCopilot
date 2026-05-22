package io.jobcopilot.resumeassistant.domain.matching.repository;

import io.jobcopilot.resumeassistant.domain.matching.entity.JobMatchResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 职位匹配结果仓储接口
 * Repository interface for job match results
 */
public interface JobMatchResultRepository {

    /**
     * 保存匹配结果
     * Save a match result
     *
     * @param result 匹配结果实体 / Match result entity
     * @return 保存后的实体 / Saved entity
     */
    JobMatchResult save(JobMatchResult result);

    /**
     * 根据ID查询匹配结果
     * Find match result by ID
     *
     * @param matchId 匹配ID / Match ID
     * @return 匹配结果(可选) / Optional match result
     */
    Optional<JobMatchResult> findById(String matchId);

    /**
     * 查询用户的匹配历史
     * Find match history by user ID
     *
     * @param userId 用户ID / User ID
     * @return 匹配结果列表 / List of match results
     */
    List<JobMatchResult> findAllByUserId(UUID userId);

    /**
     * 查询用户的匹配历史（按创建时间倒序）
     * Find match history by user ID ordered by created time desc
     *
     * @param userId 用户ID / User ID
     * @return 匹配结果列表 / List of match results
     */
    List<JobMatchResult> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
