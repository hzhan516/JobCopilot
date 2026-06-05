package io.jobcopilot.resumeassistant.application.job.service;

import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.exception.JobException;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Enforces ownership and visibility rules for job access.
 * 强制执行职位访问的所有权和可见性规则。
 */
@Component
@RequiredArgsConstructor
public class JobAccessControl {

    private final JobRepository jobRepository;

    public Job requireAccessible(String jobId, UUID userId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobException("job.not.found"));
        if (job.isHidden()) throw new JobException("job.not.found");
        if (!job.getUserId().equals(userId)) throw new JobException("access.denied");
        return job;
    }

    public Job requireOwned(String jobId, UUID userId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobException("job.not.found"));
        if (!job.getUserId().equals(userId)) throw new JobException("access.denied");
        return job;
    }
}
