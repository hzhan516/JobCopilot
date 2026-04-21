package edu.asu.ser594.resumeassistant.api.job.facade;

import edu.asu.ser594.resumeassistant.api.job.dto.request.JobMatchRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.matching.dto.response.JobMatchHistoryResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobMatchResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.MatchItem;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;

import java.util.List;
import java.util.UUID;

/**
 * 职位相关操作的入站门面接口
 * Inbound port facade for Job related operations.
 */
public interface JobFacade {

    /**
     * 提交一个新的职位请求以供解析
     * Submits a new job posting to be processed and parsed.
     *
     * @param userId 提交用户的 ID / The ID of the user submitting the job.
     * @param request 职位提交请求 / The job submission details containing URL and options.
     * @return 初步响应结果 / The initial state of the job as a response.
     */
    JobResponse submitJob(UUID userId, SubmitJobRequest request);

    /**
     * 获取指定职位的处理状态与详情
     * Retrieves the current processing status and details of a job.
     *
     * @param jobId 职位的唯一标识 / The unique ID of the job.
     * @param userId 请求用户的 ID / The ID of the requesting user.
     * @return 职位状态及详情 / The current state and details of the job.
     */
    JobResponse getJob(String jobId, UUID userId);

    /**
     * 根据用户 ID 获取职位列表
     * Get job list by user ID
     *
     * @param userId 用户 ID / User ID
     * @return 职位列表响应 / List of job responses
     */
    List<JobResponse> listJobs(UUID userId);

    /**
     * 启动异步职位匹配
     * Start async job matching
     *
     * @param userId 用户 ID / User ID
     * @param request 匹配请求参数 / Match request parameters
     * @return 异步匹配初始响应（状态为 PROCESSING） / Async match initial response with PROCESSING status
     */
    JobMatchResponse matchJobs(UUID userId, JobMatchRequest request);

    /**
     * 查询匹配结果
     * Get match result by match ID
     *
     * @param matchId 匹配任务ID / Match task ID
     * @return 匹配结果响应 / Match result response
     */
    JobMatchResponse getMatchResult(String matchId);

    /**
     * 查询用户的匹配历史
     * Get user's match history
     *
     * @param userId 用户 ID / User ID
     * @return 匹配历史列表 / List of match history
     */
    List<JobMatchHistoryResponse> getMatchHistory(UUID userId);

    /**
     * 处理异步的职位 AI 解析结果
     * Handles the asynchronous result of an AI job processing request.
     *
     * @param event AI 结果事件 / The result event containing parsed content or error details.
     */
    void handleJobProcessResult(AiResultEvent event);

    /**
     * 保存职位精排结果
     * Save job rank result
     *
     * @param matchId 匹配任务ID / Match task ID
     * @param rankedResults 精排结果列表 / Ranked result list
     * @param rankTimeMs 精排耗时(毫秒) / Ranking time in ms
     */
    void saveJobRankResult(String matchId, List<MatchItem> rankedResults, Long rankTimeMs);
}
