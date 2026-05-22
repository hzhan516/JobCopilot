package io.jobcopilot.resumeassistant.api.job.facade;

import io.jobcopilot.resumeassistant.api.job.dto.request.JobScoreRequest;
import io.jobcopilot.resumeassistant.api.job.dto.request.SubmitJobRequest;
import io.jobcopilot.resumeassistant.api.job.dto.request.UpdateJobRequest;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobResponse;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobScoreHistoryResponse;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobScoreResponse;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;

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
     * @param userId  提交用户的 ID / The ID of the user submitting the job.
     * @param request 职位提交请求 / The job submission details containing URL and options.
     * @return 初步响应结果 / The initial state of the job as a response.
     */
    JobResponse submitJob(UUID userId, SubmitJobRequest request);

    /**
     * 获取指定职位的处理状态与详情
     * Retrieves the current processing status and details of a job.
     *
     * @param jobId  职位的唯一标识 / The unique ID of the job.
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
     * 更新职位的解析内容
     * Updates the parsed content of a job.
     *
     * @param jobId   职位 ID / The job ID.
     * @param userId  用户 ID / The user ID.
     * @param request 更新请求 / The update request.
     * @return 更新后的职位 / The updated job.
     */
    JobResponse updateJob(String jobId, UUID userId, UpdateJobRequest request);

    /**
     * 隐藏职位，使其不再出现在用户列表中。
     * Hide a job from user-facing lists.
     *
     * @param jobId  职位 ID / The job ID.
     * @param userId 用户 ID / The user ID.
     */
    void deleteJob(String jobId, UUID userId);

    /**
     * 对单个职位进行简历评分
     * Scores a single job against a resume.
     *
     * @param jobId   职位 ID / The job ID.
     * @param userId  用户 ID / The user ID.
     * @param request 评分请求 / The score request.
     * @return 评分结果 / The score result.
     */
    JobScoreResponse scoreJob(String jobId, UUID userId, JobScoreRequest request);

    /**
     * 追踪真实用户反馈行为，并发送至 AI 队列以支持增量学习。
     * Track user action (CLICK, APPLY, REJECT) for AI incremental learning.
     *
     * @param jobId      职位 ID / The job ID.
     * @param userId     用户 ID / The user ID.
     * @param actionType      操作类型 / Action type (CLICK, APPLY, REJECT)
     * @param resumeVersionId 关联的简历 ID（可选） / Associated resume version ID (optional)
     */
    void trackJobAction(String jobId, UUID userId, String actionType, String resumeVersionId);

    /**
     * 获取用户的评分历史记录
     * Gets the score history for a user.
     *
     * @param userId 用户 ID / The user ID.
     * @return 评分历史列表 / List of score history records.
     */
    List<JobScoreHistoryResponse> getScoreHistory(UUID userId);

    /**
     * 处理异步的职位 AI 解析结果
     * Handles the asynchronous result of an AI job processing request.
     *
     * @param event AI 结果事件 / The result event containing parsed content or error details.
     */
    void handleJobProcessResult(AiResultEvent event);
}
