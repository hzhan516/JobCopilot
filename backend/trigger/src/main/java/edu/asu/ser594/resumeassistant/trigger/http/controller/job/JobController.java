package edu.asu.ser594.resumeassistant.trigger.http.controller.job;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.trigger.http.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
}
