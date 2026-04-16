package edu.asu.ser594.resumeassistant.trigger.http.controller.job;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.request.JobMatchRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobMatchResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.trigger.http.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobFacade jobFacade;

    @PostMapping
    public ApiResponse<JobResponse> submitJob(
            @CurrentUser String userId,
            @Validated @RequestBody SubmitJobRequest request) {
        log.info("User {} submitting job link: {}", userId, request.url());
        JobResponse response = jobFacade.submitJob(userId, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/{jobId}")
    public ApiResponse<JobResponse> getJob(
            @CurrentUser String userId,
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
            @CurrentUser String userId) {
        log.info("User {} listing all jobs", userId);
        List<JobResponse> response = jobFacade.listJobs(userId);
        return ApiResponse.success(response);
    }

    /**
     * 获取职位匹配推荐
     * Get job matching recommendations
     */
    @PostMapping("/match")
    public ApiResponse<JobMatchResponse> matchJobs(
            @CurrentUser String userId,
            @RequestBody JobMatchRequest request) {
        log.info("User {} requesting job match", userId);
        JobMatchResponse response = jobFacade.matchJobs(userId, request);
        return ApiResponse.success(response);
    }
}
