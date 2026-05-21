package edu.asu.ser594.resumeassistant.trigger.http.controller.job;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.request.*;
import edu.asu.ser594.resumeassistant.api.job.dto.response.*;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.api.job.facade.JobVectorSearchFacade;
import edu.asu.ser594.resumeassistant.api.matching.dto.response.JobMatchHistoryResponse;
import edu.asu.ser594.resumeassistant.api.matching.facade.MatchingFacade;
import edu.asu.ser594.resumeassistant.trigger.http.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobFacade jobFacade;
    private final MatchingFacade matchingFacade;
    private final JobVectorSearchFacade jobVectorSearchFacade;

    /**
     * 提交新职位（Multipart 上传链接+截图）
     * Submit a new job with URL and screenshot.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<JobResponse> submitJob(
            @CurrentUser UUID userId,
            @RequestParam("url") String url,
            @RequestPart(value = "screenshot", required = false) MultipartFile screenshot) {
        log.info("User {} submitting job link: {}", userId, url);

        String screenshotBase64 = null;
        if (screenshot != null && !screenshot.isEmpty()) {
            try {
                screenshotBase64 = Base64.getEncoder().encodeToString(screenshot.getBytes());
            } catch (Exception e) {
                log.error("Failed to read screenshot file", e);
                throw new IllegalArgumentException("Failed to read screenshot file: " + e.getMessage());
            }
        }

        SubmitJobRequest request = new SubmitJobRequest(url, false, screenshotBase64);
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
     * 更新职位解析内容
     * Update job parsed content.
     */
    @PutMapping("/{jobId}")
    public ApiResponse<JobResponse> updateJob(
            @CurrentUser UUID userId,
            @PathVariable("jobId") String jobId,
            @Validated @RequestBody UpdateJobRequest request) {
        log.info("User {} updating job: {}", userId, jobId);
        JobResponse response = jobFacade.updateJob(jobId, userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 隐藏职位，让它不再出现在职位列表中，但保留数据库记录。
     * Hide a job from user-facing lists while preserving its database record.
     */
    @DeleteMapping("/{jobId}")
    public ApiResponse<Void> deleteJob(
            @CurrentUser UUID userId,
            @PathVariable("jobId") String jobId) {
        log.info("User {} deleting job: {}", userId, jobId);
        jobFacade.deleteJob(jobId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 对单个职位进行简历评分
     * Score a single job against a resume.
     */
    @PostMapping("/{jobId}/score")
    public ApiResponse<JobScoreResponse> scoreJob(
            @CurrentUser UUID userId,
            @PathVariable("jobId") String jobId,
            @Validated @RequestBody JobScoreRequest request) {
        log.info("User {} scoring job {} with resume {}", userId, jobId, request.resumeVersionId());
        JobScoreResponse response = jobFacade.scoreJob(jobId, userId, request);
        return ApiResponse.success(response);
    }

    /**
     * Track user action (CLICK, APPLY, REJECT)
     */
    @PostMapping("/{jobId}/track")
    public ApiResponse<Void> trackJobAction(
            @CurrentUser UUID userId,
            @PathVariable("jobId") String jobId,
            @RequestParam("action") String actionType,
            @RequestParam(value = "resumeVersionId", required = false) String resumeVersionId) {
        log.info("User {} action {} on job {} with resume {}", userId, actionType, jobId, resumeVersionId);
        jobFacade.trackJobAction(jobId, userId, actionType, resumeVersionId);
        return ApiResponse.success(null);
    }

    /**
     * 启动异步职位匹配
     * Start async job matching
     */
    @PostMapping("/match")
    public ApiResponse<JobMatchResponse> matchJobs(
            @CurrentUser UUID userId,
            @Validated @RequestBody JobMatchRequest request) {
        log.info("User {} requesting job match", userId);
        JobMatchResponse response = matchingFacade.matchJobs(userId, request);
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
        JobMatchResponse response = matchingFacade.getMatchResult(matchId);
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
        List<JobMatchHistoryResponse> response = matchingFacade.getMatchHistory(userId);
        return ApiResponse.success(response);
    }

    /**
     * 获取评分历史列表
     * Get score history list
     */
    @GetMapping("/scores/history")
    public ApiResponse<List<JobScoreHistoryResponse>> getScoreHistory(
            @CurrentUser UUID userId) {
        log.info("User {} fetching score history", userId);
        List<JobScoreHistoryResponse> response = jobFacade.getScoreHistory(userId);
        return ApiResponse.success(response);
    }

    /**
     * 向量搜索职位
     * Vector search for jobs
     */
    @PostMapping("/vector-search")
    public ApiResponse<List<VectorSearchResponse>> vectorSearch(
            @CurrentUser UUID userId,
            @Validated @RequestBody VectorSearchRequest request) {
        log.info("User {} requesting vector search", userId);
        List<VectorSearchResponse> response = jobVectorSearchFacade.search(request);
        return ApiResponse.success(response);
    }
}
