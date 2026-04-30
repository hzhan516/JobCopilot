package edu.asu.ser594.resumeassistant.trigger.http.controller.job;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.request.JobMatchRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.matching.dto.response.JobMatchHistoryResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobMatchResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.trigger.http.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobFacade jobFacade;

    @PostMapping
    public ApiResponse<JobResponse> submitJob(
            @CurrentUser UUID userId,
            @Validated @RequestBody SubmitJobRequest request) {
        log.info("User {} submitting job link: {}", userId, request.url());
        JobResponse response = jobFacade.submitJob(userId, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/{jobId}")
    public ApiResponse<JobResponse> getJob(
            @CurrentUser UUID userId,
            @PathVariable("jobId") String jobId) {
        log.info("User {} fetching job details: {}", userId, jobId);
        JobResponse response = jobFacade.getJob(jobId, userId);
        return ApiResponse.success(response);
    }

    /**
     * 获取当前用户的所有职位列表
     * Get list of jobs for the current user
     */
    @GetMapping
    public ApiResponse<List<JobResponse>> listJobs(
            @CurrentUser UUID userId) {
        log.info("User {} listing all jobs", userId);
        List<JobResponse> response = jobFacade.listJobs(userId);
        return ApiResponse.success(response);
    }

    /**
     * 启动异步职位匹配
     * Start async job matching
     */
    @PostMapping("/match")
    public ApiResponse<JobMatchResponse> matchJobs(
            @CurrentUser UUID userId,
            @RequestBody JobMatchRequest request) {
        log.info("User {} requesting job match", userId);
        JobMatchResponse response = jobFacade.matchJobs(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 查询匹配结果
     * Get match result by match ID
     */
    @GetMapping("/match/{matchId}")
    public ApiResponse<JobMatchResponse> getMatchResult(
            @CurrentUser UUID userId,
            @PathVariable("matchId") String matchId) {
        log.info("User {} fetching match result: {}", userId, matchId);
        JobMatchResponse response = jobFacade.getMatchResult(matchId);
        return ApiResponse.success(response);
    }

    /**
     * 获取历史匹配列表
     * Get match history list
     */
    @GetMapping("/match/history")
    public ApiResponse<List<JobMatchHistoryResponse>> getMatchHistory(
            @CurrentUser UUID userId) {
        log.info("User {} fetching match history", userId);
        List<JobMatchHistoryResponse> response = jobFacade.getMatchHistory(userId);
        return ApiResponse.success(response);
    }
}
