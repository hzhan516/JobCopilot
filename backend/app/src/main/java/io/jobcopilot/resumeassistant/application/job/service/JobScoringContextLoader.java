package io.jobcopilot.resumeassistant.application.job.service;

import io.jobcopilot.resumeassistant.api.job.dto.request.JobScoreRequest;
import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.exception.JobContentNotReadyException;
import io.jobcopilot.resumeassistant.domain.job.exception.JobException;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.job.valueobject.ParsedJobContent;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loads all data required for AI scoring within a transactional boundary.
 * 在事务边界内加载 AI 评分所需的所有数据。
 */
@Component
@RequiredArgsConstructor
public class JobScoringContextLoader {

    private final JobRepository jobRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeGroupRepository resumeGroupRepository;

    @Transactional(timeout = 30)
    public ScoringContext load(String jobId, UUID userId, JobScoreRequest request) {
        Job job = resolveAccessibleJob(jobId, userId);
        if (job.getParsedContent() == null) {
            throw new JobContentNotReadyException();
        }
        ResumeVersion version = resumeVersionRepository.findById(UUID.fromString(request.resumeVersionId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Resume version not found: " + request.resumeVersionId()));
        return new ScoringContext(
                parseResumeJson(resolveResumeContent(version)),
                buildJobMap(job.getParsedContent()));
    }

    private Job resolveAccessibleJob(String jobId, UUID userId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobException("job.not.found"));
        if (job.isHidden()) throw new JobException("job.not.found");
        if (!job.getUserId().equals(userId)) throw new JobException("access.denied");
        return job;
    }

    private String resolveResumeContent(ResumeVersion version) {
        String content = version.getParsedContent();
        if (content != null && !content.isEmpty()) return content;

        ResumeGroup group = resumeGroupRepository.findById(version.getGroupId()).orElse(null);
        if (group == null) return null;

        ResumeVersion original = group.getVersions().stream()
                .filter(v -> v.getVersionType() == ResumeVersion.VersionType.ORIGINAL)
                .findFirst().orElse(null);
        if (original != null && original.getParsedContent() != null && !original.getParsedContent().isEmpty()) {
            return original.getParsedContent();
        }

        String raw = version.getContent();
        if (raw != null && !raw.isEmpty()) return raw;

        return group.getVersions().stream()
                .filter(v -> v.getContent() != null && !v.getContent().isEmpty())
                .findFirst().map(ResumeVersion::getContent).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResumeJson(String resumeJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> map = mapper.readValue(resumeJson, Map.class);
            if (map.containsKey("parsedContent")) {
                return mapper.convertValue(map.get("parsedContent"), Map.class);
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse resume JSON", e);
        }
    }

    private Map<String, Object> buildJobMap(ParsedJobContent pc) {
        return Map.of(
                "title", pc.title() != null ? pc.title() : "",
                "company", pc.company() != null ? pc.company() : "",
                "description", pc.description() != null ? pc.description() : "",
                "requirements", pc.requirements() != null ? pc.requirements() : List.of()
        );
    }

    public record ScoringContext(Map<String, Object> resume, Map<String, Object> job) {}
}
