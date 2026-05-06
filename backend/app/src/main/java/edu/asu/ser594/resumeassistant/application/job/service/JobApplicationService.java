package edu.asu.ser594.resumeassistant.application.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.ser594.resumeassistant.api.embedding.facade.VectorFacade;
import edu.asu.ser594.resumeassistant.api.job.dto.request.JobScoreRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.UpdateJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobScoreHistoryResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobScoreResponse;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.entity.JobScoreRecord;
import edu.asu.ser594.resumeassistant.domain.job.exception.JobContentNotReadyException;
import edu.asu.ser594.resumeassistant.domain.job.exception.JobException;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobScoreRepository;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.exception.AiServiceUnavailableException;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 职位应用服务 / Job application service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private static final long MAX_SCREENSHOT_SIZE_BYTES = 5L * 1024 * 1024;      // 5MB
    private static final long MAX_BASE64_LENGTH = 3L * 1024 * 1024;              // 3MB Base64

    private final JobRepository jobRepository;
    private final JobScoreRepository jobScoreRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeGroupRepository resumeGroupRepository;
    private final AiMessagePublisherPort aiMessagePublisherPort;
    private final VectorFacade vectorFacade;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    /**
     * 提交职位并进行异步处理 / Submit a job for async processing
     */
    @Transactional
    public JobResponse submitJob(UUID userId, SubmitJobRequest request) {
        log.info("Submitting new job for async processing for user: {}", userId);

        // 截图大小校验 / Validate screenshot size
        if (request.screenshotBase64() != null && !request.screenshotBase64().isEmpty()) {
            long base64Len = request.screenshotBase64().length();
            if (base64Len > MAX_BASE64_LENGTH) {
                throw new IllegalArgumentException("Screenshot too large after Base64 encoding. Max allowed: 3MB");
            }
            // 估算原始大小约为 Base64 的 3/4
            long estimatedOriginal = base64Len * 3 / 4;
            if (estimatedOriginal > MAX_SCREENSHOT_SIZE_BYTES) {
                throw new IllegalArgumentException("Screenshot too large. Max allowed: 5MB");
            }
        }

        // 创建职位实体并标记为抓取中 / Create job entity and mark as scraping
        Job job = Job.create(userId, request.url(), request.imageCheckEnabled());
        job.markScraping();
        job = jobRepository.save(job);

        try {
            // 标记为解析中并保存 / Mark as parsing and save
            job.markParsing();
            job = jobRepository.save(job);

            // 构建解析命令并发送到消息队列 / Build parse command and send to message queue
            // 将 screenshotBase64 通过 MQ 发送给 AI 服务
            JobParseCommand command = new JobParseCommand(
                    job.getId(),
                    job.getOriginalUrl(),
                    job.isImageCheckEnabled(),
                    request.screenshotBase64()
            );
            aiMessagePublisherPort.sendJobForParsing(command);
            return mapToResponse(job);
        } catch (Exception e) {
            log.error("Failed to publish job processing request: {}", job.getId(), e);
            job.markFailed("Failed to publish job processing request: " + e.getMessage());
            jobRepository.save(job);
            throw new RuntimeException("Failed to submit job: " + e.getMessage(), e);
        }
    }

    /**
     * 处理 AI 解析结果 / Handle AI parse result
     */
    @Transactional
    public void handleJobProcessResult(AiResultEvent event) {
        Job job = jobRepository.findById(event.referenceId())
                .orElseThrow(() -> new JobException("job.not.found"));

        if ("COMPLETED".equals(event.status()) && event.data() != null) {
            try {
                // 反序列化 AI 返回的解析内容 / Deserialize AI parsed content
                ParsedJobContent content = objectMapper.convertValue(event.data(), ParsedJobContent.class);
                job.markCompleted(content);
            } catch (Exception e) {
                job.markFailed("Failed to deserialize AI result data");
                log.error("Deserialization error for job {}: ", event.referenceId(), e);
                jobRepository.save(job);
                return;
            }

            try {
                // 同步生成向量并保存 / Synchronously generate and save vector
                ParsedJobContent pc = job.getParsedContent();
                String vectorText = pc.title() + "\n" + pc.company() + "\n" + pc.description() + "\n" + String.join("\n", pc.requirements());
                vectorFacade.generateAndSaveVector(job.getId(), "JOB", vectorText);
                log.info("Vector generated and saved for job: {}", job.getId());
            } catch (Exception e) {
                log.error("Failed to generate vector for job: {}", job.getId(), e);
            }
        } else {
            job.markFailed(event.errorMessage() != null ? event.errorMessage() : "Unknown AI processing error");
        }

        jobRepository.save(job);
        log.info("Job {} updated to status {}", job.getId(), job.getStatus());
    }

    /**
     * 根据 ID 获取职位 / Get job by ID
     */
    @Transactional(readOnly = true)
    public JobResponse getJob(String jobId, UUID userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobException("job.not.found"));

        // 校验用户权限 / Verify user access
        if (!job.getUserId().equals(userId)) {
            throw new JobException("access.denied");
        }

        return mapToResponse(job);
    }

    /**
     * 列出用户的所有职位 / List all jobs for a user
     */
    @Transactional(readOnly = true)
    public List<JobResponse> listJobs(UUID userId) {
        List<Job> jobs = jobRepository.findAllByUserId(userId);
        return jobs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 更新职位的解析内容
     * Updates a job's parsed content.
     */
    @Transactional
    public JobResponse updateJob(String jobId, UUID userId, UpdateJobRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobException("job.not.found"));

        if (!job.getUserId().equals(userId)) {
            throw new JobException("access.denied");
        }

        ParsedJobContent newContent = new ParsedJobContent(
                request.title(),
                request.company(),
                request.salary(),
                request.location(),
                request.description(),
                request.requirements()
        );
        job.updateParsedContent(newContent);
        jobRepository.save(job);

        log.info("Job {} parsed content updated by user {}", jobId, userId);
        return mapToResponse(job);
    }

    /**
     * 对单个职位进行简历评分
     * Scores a single job against a resume by calling AI service /api/v1/suitability.
     */
    @Transactional
    public JobScoreResponse scoreJob(String jobId, UUID userId, JobScoreRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobException("job.not.found"));

        if (!job.getUserId().equals(userId)) {
            throw new JobException("access.denied");
        }

        ResumeVersion resumeVersion = resumeVersionRepository.findById(UUID.fromString(request.resumeVersionId()))
                .orElseThrow(() -> new IllegalArgumentException("Resume version not found: " + request.resumeVersionId()));

        // 校验简历所有权
        // 由于 ResumeVersion 没有直接的 userId，需要通过查询 resume group 校验
        // 这里简化处理：如果找不到 resume 则报错，实际应由 ResumeVersionRepository 提供足够的查询能力

        if (job.getParsedContent() == null) {
            throw new JobContentNotReadyException();
        }

        String url = aiServiceBaseUrl + "/api/v1/suitability";
        try {
            // 构造 AI 服务请求体
            // 简历 parsedContent 是 JSON 字符串，直接解析为 Map
            String resumeJson = resumeVersion.getParsedContent();
            // 回退：若当前版本无 parsedContent，尝试使用同组 ORIGINAL 版本的
            if (resumeJson == null || resumeJson.isEmpty()) {
                ResumeGroup resumeGroup = resumeGroupRepository.findById(resumeVersion.getGroupId()).orElse(null);
                if (resumeGroup != null) {
                    ResumeVersion original = resumeGroup.getVersions().stream()
                            .filter(v -> v.getVersionType() == ResumeVersion.VersionType.ORIGINAL)
                            .findFirst()
                            .orElse(null);
                    if (original != null && original.getParsedContent() != null && !original.getParsedContent().isEmpty()) {
                        resumeJson = original.getParsedContent();
                    }
                }
            }
            if (resumeJson == null || resumeJson.isEmpty()) {
                throw new JobContentNotReadyException("resume.content.not.ready");
            }
            Map<String, Object> resumeMap = objectMapper.readValue(resumeJson, Map.class);
            // 兼容旧数据：如果保存的是包装格式 {"parsedContent": {...}, "summary": ""}，则提取内部内容
            // Compatibility: unwrap old data format that wrapped parsedContent in an outer object
            if (resumeMap.containsKey("parsedContent")) {
                Object inner = resumeMap.get("parsedContent");
                resumeMap = objectMapper.convertValue(inner, Map.class);
            }

            // 如果结构化解析结果为空（skills 和 experience 均为空），使用原始 Markdown 内容作为 fallback
            // Fallback to raw Markdown content when structured extraction yielded empty results
            Object skillsObj = resumeMap.get("skills");
            Object expObj = resumeMap.get("experience");
            boolean hasNoSkills = skillsObj == null || (skillsObj instanceof List && ((List<?>) skillsObj).isEmpty());
            boolean hasNoExperience = expObj == null || (expObj instanceof List && ((List<?>) expObj).isEmpty());
            if (hasNoSkills && hasNoExperience) {
                String rawContent = resumeVersion.getContent();
                if (rawContent == null || rawContent.isEmpty()) {
                    ResumeGroup rg = resumeGroupRepository.findById(resumeVersion.getGroupId()).orElse(null);
                    if (rg != null) {
                        rawContent = rg.getVersions().stream()
                                .filter(v -> v.getContent() != null && !v.getContent().isEmpty())
                                .findFirst()
                                .map(ResumeVersion::getContent)
                                .orElse(null);
                    }
                }
                if (rawContent != null && !rawContent.isEmpty()) {
                    String truncated = rawContent.length() > 8000 ? rawContent.substring(0, 8000) : rawContent;
                    resumeMap.put("experience", List.of(Map.of("summary", truncated)));
                    log.info("Resume structured data empty for versionId={}, fallback to raw content ({} chars)",
                            resumeVersion.getId(), truncated.length());
                }
            }

            // 职位数据只发送 AI 服务已知的字段（避免 Pydantic 校验失败）
            ParsedJobContent pc = job.getParsedContent();
            Map<String, Object> jobMap = Map.of(
                    "title", pc.title() != null ? pc.title() : "",
                    "company", pc.company() != null ? pc.company() : "",
                    "description", pc.description() != null ? pc.description() : "",
                    "requirements", pc.requirements() != null ? pc.requirements() : List.of()
            );

            Map<String, Object> suitabilityRequest = Map.of(
                    "resume", resumeMap,
                    "job", jobMap
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(suitabilityRequest, headers);

            log.info("Calling AI service suitability endpoint for jobId={}, resumeVersionId={}", jobId, request.resumeVersionId());

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = restTemplate.postForObject(url, entity, Map.class);

            if (responseBody == null) {
                throw new RuntimeException("AI service returned empty response");
            }

            // 解析响应
            boolean suitable = Boolean.TRUE.equals(responseBody.get("suitable"));
            String summary = (String) responseBody.get("summary");
            Number finalScoreNum = (Number) responseBody.get("finalScore");
            float finalScore = finalScoreNum != null ? finalScoreNum.floatValue() : 0.0f;

            @SuppressWarnings("unchecked")
            Map<String, Object> breakdownMap = (Map<String, Object>) responseBody.get("breakdown");
            float skillScore = 0.0f;
            float experienceScore = 0.0f;
            float overallScore = 0.0f;
            if (breakdownMap != null) {
                Number skillNum = (Number) breakdownMap.get("skillScore");
                Number expNum = (Number) breakdownMap.get("experienceScore");
                Number overallNum = (Number) breakdownMap.get("overallScore");
                skillScore = skillNum != null ? skillNum.floatValue() : 0.0f;
                experienceScore = expNum != null ? expNum.floatValue() : 0.0f;
                overallScore = overallNum != null ? overallNum.floatValue() : 0.0f;
            }

            // 保存评分记录
            JobScoreRecord record = JobScoreRecord.create(
                    jobId,
                    request.resumeVersionId(),
                    userId,
                    suitable,
                    finalScore,
                    skillScore,
                    experienceScore,
                    overallScore,
                    summary
            );
            jobScoreRepository.save(record);

            return new JobScoreResponse(
                    suitable,
                    summary != null ? summary : "",
                    finalScore,
                    new JobScoreResponse.ScoreBreakdown(skillScore, experienceScore, overallScore)
            );

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("AI service unavailable at {} for jobId={}: {}", url, jobId, e.getMessage());
            throw new AiServiceUnavailableException();
        } catch (Exception e) {
            log.error("Failed to score job {} against resume {}", jobId, request.resumeVersionId(), e);
            throw new RuntimeException("Failed to score job: " + e.getMessage(), e);
        }
    }

    /**
     * 获取用户的评分历史记录
     * Gets the score history for a user.
     */
    @Transactional(readOnly = true)
    public List<JobScoreHistoryResponse> getScoreHistory(UUID userId) {
        List<JobScoreRecord> records = jobScoreRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return records.stream()
                .map(this::mapToScoreHistoryResponse)
                .collect(Collectors.toList());
    }

    private JobScoreHistoryResponse mapToScoreHistoryResponse(JobScoreRecord record) {
        return new JobScoreHistoryResponse(
                record.getId(),
                record.getJobId(),
                record.getResumeVersionId(),
                Boolean.TRUE.equals(record.getSuitable()),
                record.getFinalScore() != null ? record.getFinalScore() : 0.0f,
                record.getSkillScore() != null ? record.getSkillScore() : 0.0f,
                record.getExperienceScore() != null ? record.getExperienceScore() : 0.0f,
                record.getOverallScore() != null ? record.getOverallScore() : 0.0f,
                record.getSummary(),
                record.getCreatedAt()
        );
    }

    // 将领域实体映射为响应 DTO / Map domain entity to response DTO
    private JobResponse mapToResponse(Job job) {
        JobResponse.ParsedJobContentResponse parsedContentResponse = null;
        if (job.getParsedContent() != null) {
            parsedContentResponse = new JobResponse.ParsedJobContentResponse(
                    job.getParsedContent().title(),
                    job.getParsedContent().company(),
                    job.getParsedContent().salary(),
                    job.getParsedContent().location(),
                    job.getParsedContent().description(),
                    job.getParsedContent().requirements()
            );
        }

        return new JobResponse(
                job.getId(),
                job.getUserId().toString(),
                job.getOriginalUrl(),
                job.getStatus().name(),
                parsedContentResponse,
                job.isImageCheckEnabled(),
                job.getErrorMessage()
        );
    }
}
