package io.jobcopilot.resumeassistant.api.matching.facade;

import io.jobcopilot.resumeassistant.api.job.dto.request.JobMatchRequest;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobMatchResponse;
import io.jobcopilot.resumeassistant.api.job.dto.response.MatchItem;
import io.jobcopilot.resumeassistant.api.matching.dto.response.JobMatchHistoryResponse;

import java.util.List;
import java.util.UUID;

/**
 * 职位匹配门面接口
 * Job matching facade interface
 * <p>
 * 负责职位召回与精排的入站门面，将匹配领域与职位领域解耦。
 * Inbound port for job recall and ranking, decoupling the matching domain from the job domain.
 */
public interface MatchingFacade {

    /**
     * 启动异步职位匹配
     * Start async job matching
     *
     * @param userId  用户 ID / User ID
     * @param request 匹配请求参数 / Match request parameters
     * @return 异步匹配初始响应（状态为 PROCESSING） / Async match initial response with PROCESSING status
     * @since 1.0.0
     */
    JobMatchResponse matchJobs(UUID userId, JobMatchRequest request);

    /**
     * 查询匹配结果
     * Get match result by match ID
     *
     * @param matchId 匹配任务ID / Match task ID
     * @return 匹配结果响应 / Match result response
     * @since 1.0.0
     */
    JobMatchResponse getMatchResult(String matchId);

    /**
     * 查询用户的匹配历史
     * Get user's match history
     *
     * @param userId 用户 ID / User ID
     * @return 匹配历史列表 / List of match history
     * @since 1.0.0
     */
    List<JobMatchHistoryResponse> getMatchHistory(UUID userId);

    /**
     * 保存职位精排结果
     * Save job rank result
     *
     * @param matchId       匹配任务ID / Match task ID
     * @param rankedResults 精排结果列表 / Ranked result list
     * @param rankTimeMs    精排耗时(毫秒) / Ranking time in ms
     * @since 1.0.0
     */
    void saveJobRankResult(String matchId, List<MatchItem> rankedResults, Long rankTimeMs);
}
