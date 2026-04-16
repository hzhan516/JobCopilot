package edu.asu.ser594.resumeassistant.api.job.facade;

import edu.asu.ser594.resumeassistant.api.job.dto.request.JobMatchRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobMatchResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
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
     * 匹配职位 (调用 AI 引擎)
     * Match jobs (Call AI engine)
     * 
     * @param userId 用户 ID / User ID
     * @param request 匹配请求参数 / Match request parameters
     * @return 职位匹配整体响应 / Job match response
     */
    JobMatchResponse matchJobs(UUID userId, JobMatchRequest request);

    /**
     * 处理异步的职位 AI 解析结果
     * Handles the asynchronous result of an AI job processing request.
     * 
     * @param event AI 结果事件 / The result event containing parsed content or error details.
     */
    void handleJobProcessResult(AiResultEvent event);
}

