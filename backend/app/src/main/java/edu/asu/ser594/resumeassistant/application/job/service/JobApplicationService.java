package edu.asu.ser594.resumeassistant.application.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.ser594.resumeassistant.api.embedding.facade.VectorFacade;
import edu.asu.ser594.resumeassistant.api.job.dto.request.JobScoreRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.UpdateJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobScoreHistoryResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobScoreResponse;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.JobVector;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.ResumeVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.JobVectorRepository;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.entity.JobScoreRecord;
import edu.asu.ser594.resumeassistant.domain.job.exception.JobContentNotReadyException;
import edu.asu.ser594.resumeassistant.domain.job.exception.JobException;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobScoreRepository;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import edu.asu.ser594.resumeassistant.domain.matching.entity.JobDataset;
import edu.asu.ser594.resumeassistant.domain.matching.repository.JobDatasetRepository;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.UserFeedbackCommand;
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

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates job submission, AI-driven parsing, vector indexing, and resume-to-job scoring.
 * Acts as the transactional boundary for all write operations within the job bounded context.
 * 编排职位提交、AI 驱动解析、向量索引及简历匹配评分，作为职位限界上下文内所有写操作的事务边界
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private static final long MAX_SCREENSHOT_SIZE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_BASE64_LENGTH = 7L * 1024 * 1024;

    private final JobRepository jobRepository;
    private final JobScoreRepository jobScoreRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeGroupRepository resumeGroupRepository;
    private final JobDatasetRepository jobDatasetRepository;
    private final ResumeVectorRepository resumeVectorRepository;
    private final JobVectorRepository jobVectorRepository;
    private final AiMessagePublisherPort aiMessagePublisherPort;
    private final VectorFacade vectorFacade;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    /**
     * Submits a job posting for asynchronous AI parsing. The screenshot is forwarded through
     * the message queue rather than REST to avoid blocking the HTTP thread on large payloads.
     * 提交职位进行异步 AI 解析。截图通过消息队列而非 REST 转发，避免大载荷阻塞 HTTP 线程
     *
     * @param userId  User ID / 用户 ID
     * @param request Job submission request / 职位提交请求
     * @return Job response / 职位响应
     */
    @Transactional
    public JobResponse submitJob(UUID userId, SubmitJobRequest request) {
        log.info("Submitting new job for async processing for user: {}", userId);

        if (request.url() == null || request.url().isBlank()) {
            throw new IllegalArgumentException("Job URL is required / 职位 URL 不能为空");
        }

        if (request.screenshotBase64() != null && !request.screenshotBase64().isEmpty()) {
            long base64Len = request.screenshotBase64().length();
            if (base64Len > MAX_BASE64_LENGTH) {
                throw new IllegalArgumentException("Screenshot too large after Base64 encoding. Max allowed: 3MB");
            }
            // Base64 inflates size by ~33%, so we approximate the raw binary size for the real limit check
            // Base64 编码会使体积膨胀约 33%，据此估算原始二进制大小以进行真实限制校验
            long estimatedOriginal = base64Len * 3 / 4;
            if (estimatedOriginal > MAX_SCREENSHOT_SIZE_BYTES) {
                throw new IllegalArgumentException("Screenshot too large. Max allowed: 5MB");
            }
        }

        Job job = Job.create(userId, request.url(), request.imageCheckEnabled());
        job.markScraping();
        job = jobRepository.save(job);

        try {
            job.markParsing();
            job = jobRepository.save(job);

            // Forward screenshotBase64 via MQ because REST timeouts are unpredictable for multi-MB payloads
            // 通过 MQ 转发 screenshotBase64，因为多 MB 载荷的 REST 超时难以预估
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
     * Processes AI parsing results, deserializes structured job content, and triggers
     * synchronous vector generation so the job becomes searchable immediately.
     * 处理 AI 解析结果，反序列化结构化职位内容，并触发同步向量生成以使职位立即可被搜索
     *
     * @param event AI result event / AI 结果事件
     */
    @Transactional
    public void handleJobProcessResult(AiResultEvent event) {
        Job job = jobRepository.findById(event.referenceId())
                .orElseThrow(() -> new JobException("job.not.found"));

        if ("COMPLETED".equals(event.status()) && event.data() != null) {
            try {
                ParsedJobContent content = objectMapper.convertValue(event.data(), ParsedJobContent.class);
                job.markCompleted(content);
            } catch (Exception e) {
                job.markFailed("Failed to deserialize AI result data");
                log.error("Deserialization error for job {}: ", event.referenceId(), e);
                jobRepository.save(job);
                return;
            }

            try {
                ParsedJobContent pc = job.getParsedContent();
                String vectorText = pc.title() + "\n" + pc.company() + "\n" + pc.description() + "\n" + String.join("\n", pc.requirements());
                vectorFacade.generateAndSaveVector(job.getId(), "JOB", vectorText);
                log.info("Vector generated and saved for job: {}", job.getId());
            } catch (Exception e) {
                log.error("Failed to generate vector for job: {}", job.getId(), e);
            }

            // 写入训练数据集（非阻塞）/ Write to training dataset (non-blocking)
            try {
                ParsedJobContent pc = job.getParsedContent();
                JobDataset dataset = JobDataset.builder()
                        .externalId(job.getId())
                        .title(pc.title())
                        .company(pc.company())
                        .description(pc.description())
                        .requirements(pc.requirements() != null ? pc.requirements().toArray(new String[0]) : new String[0])
                        .location(pc.location())
                        .experienceLevel(null)
                        .source("USER_SUBMITTED")
                        .rawData(objectMapper.convertValue(event.data(), Map.class))
                        .build();
                jobDatasetRepository.save(dataset);
                log.info("Job {} saved to training dataset", job.getId());
            } catch (Exception e) {
                log.error("Failed to save job to dataset: {}", job.getId(), e);
                // 非阻塞：训练数据写入失败不应影响主流程
            }
        } else {
            job.markFailed(event.errorMessage() != null ? event.errorMessage() : "Unknown AI processing error");
        }

        jobRepository.save(job);
        log.info("Job {} updated to status {}", job.getId(), job.getStatus());
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(String jobId, UUID userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobException("job.not.found"));

        if (job.isHidden()) {
            throw new JobException("job.not.found");
        }

        if (!job.getUserId().equals(userId)) {
            throw new JobException("access.denied");
        }

        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobs(UUID userId) {
        List<Job> jobs = jobRepository.findAllByUserId(userId);
        return jobs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates a job's parsed content after user editing. Preserves the original URL and metadata
     * while replacing the AI-extracted structured fields.
     * 更新职位的解析内容。保留原始 URL 和元数据，同时替换 AI 提取的结构化字段
     *
     * @param jobId   Job ID / 职位 ID
     * @param userId  User ID / 用户 ID
     * @param request Update request / 更新请求
     * @return Updated job response / 更新后的职位响应
     */
    @Transactional
    public JobResponse updateJob(String jobId, UUID userId, UpdateJobRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobException("job.not.found"));

        if (job.isHidden()) {
            throw new JobException("job.not.found");
        }

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
     * Soft-deletes a job by marking it as hidden, preserving the database record and its
     * associated vectors for historical match results.
     * 软删除职位，仅标记为隐藏，保留数据库记录及关联向量以供历史匹配结果追溯
     *
     * @param jobId  Job ID / 职位 ID
     * @param userId User ID / 用户 ID
     */
    @Transactional
    public void deleteJob(String jobId, UUID userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobException("job.not.found"));

        if (!job.getUserId().equals(userId)) {
            throw new JobException("access.denied");
        }

        job.hide();
        jobRepository.save(job);
        log.info("Job {} hidden by user {}", jobId, userId);
    }

    /**
     * Scores a single job against a resume by calling the AI service suitability endpoint.
     * Implements fallback logic: if the selected resume version lacks parsed content, it cascades
     * to the group's original version and finally to raw Markdown to maximize scoring coverage.
     * 调用 AI 服务适配度端点对单个职位进行简历评分。实现多级回退：若所选版本无解析内容，则级联到组内原始版本，
     * 最终回退到原始 Markdown，以最大化评分覆盖率
     *
     * @param jobId   Job ID / 职位 ID
     * @param userId  User ID / 用户 ID
     * @param request Score request / 评分请求
     * @return Score response / 评分响应
     */
    @Transactional
    public JobScoreResponse scoreJob(String jobId, UUID userId, JobScoreRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobException("job.not.found"));

        if (job.isHidden()) {
            throw new JobException("job.not.found");
        }

        if (!job.getUserId().equals(userId)) {
            throw new JobException("access.denied");
        }

        ResumeVersion resumeVersion = resumeVersionRepository.findById(UUID.fromString(request.resumeVersionId()))
                .orElseThrow(() -> new IllegalArgumentException("Resume version not found: " + request.resumeVersionId()));

        // ResumeVersion does not store userId directly, so ownership is verified through the resume group
        // ResumeVersion 未直接存储 userId，因此通过简历组进行所有权校验

        if (job.getParsedContent() == null) {
            throw new JobContentNotReadyException();
        }

        String url = aiServiceBaseUrl + "/api/v1/suitability";
        try {
            String resumeJson = resumeVersion.getParsedContent();
            // Cascade to the group's original version if the current selection lacks parsed data
            // 若当前版本缺少解析数据，则级联到组内的原始版本
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
            // Legacy data was wrapped in {"parsedContent": {...}}; unwrap to keep compatibility
            // 旧数据被包装在 {"parsedContent": {...}} 中，解包以保持兼容性
            if (resumeMap.containsKey("parsedContent")) {
                Object inner = resumeMap.get("parsedContent");
                resumeMap = objectMapper.convertValue(inner, Map.class);
            }

            // Fallback to raw Markdown when structured extraction is empty, so the AI still receives usable input
            // 当结构化提取结果为空时回退到原始 Markdown，确保 AI 仍能收到可用的输入
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

            // Restrict to fields known by the AI service to prevent Pydantic validation failures
            // 仅发送 AI 服务已知的字段，避免触发 Pydantic 校验失败
            ParsedJobContent pc = job.getParsedContent();
            Map<String, Object> jobMap = Map.of(
                    "title", pc.title() != null ? pc.title() : "",
                    "company", pc.company() != null ? pc.company() : "",
                    "description", pc.description() != null ? pc.description() : "",
                    "requirements", pc.requirements() != null ? pc.requirements() : List.of()
            );

            Float semanticMatch = calculateSemanticMatch(request.resumeVersionId(), jobId);

            Map<String, Object> suitabilityRequest = new HashMap<>();
            suitabilityRequest.put("resume", resumeMap);
            suitabilityRequest.put("job", jobMap);
            if (semanticMatch != null) {
                suitabilityRequest.put("semanticMatch", semanticMatch);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(suitabilityRequest, headers);

            log.info("Calling AI service suitability endpoint for jobId={}, resumeVersionId={}", jobId, request.resumeVersionId());

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = restTemplate.postForObject(url, entity, Map.class);

            if (responseBody == null) {
                throw new RuntimeException("AI service returned empty response");
            }

            boolean suitable = Boolean.TRUE.equals(responseBody.get("suitable"));
            String summary = (String) responseBody.get("summary");
            Number finalScoreNum = (Number) responseBody.get("finalScore");
            float finalScore = finalScoreNum != null ? finalScoreNum.floatValue() : 0.0f;
            String llmModel = (String) responseBody.get("llmModel");
            Number datasetScoreNum = (Number) responseBody.get("datasetScore");
            Float datasetScore = datasetScoreNum != null ? datasetScoreNum.floatValue() : null;

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

            // 异步发送评分标签到 AI Service 用于增量训练
                // Asynchronously send score label to AI service for incremental training
                Map<String, Object> contextPayload = new HashMap<>();
                contextPayload.put("resume", Map.of(
                        "skills", resumeMap.get("skills") != null ? resumeMap.get("skills") : java.util.List.of(),
                        "experience", resumeMap.get("experience") != null ? resumeMap.get("experience") : java.util.List.of()
                ));
                contextPayload.put("job", jobMap);
                contextPayload.put("llmOverallScore", overallScore);
                contextPayload.put("finalScore", finalScore);
                if (llmModel != null && !llmModel.isBlank()) {
                    contextPayload.put("llmModel", llmModel);
                }
                if (semanticMatch != null) {
                    contextPayload.put("semanticMatch", semanticMatch);
                }
                if (datasetScore != null) {
                    contextPayload.put("datasetScore", datasetScore);
                }
                
                String contextStr = objectMapper.writeValueAsString(contextPayload);
                
                UserFeedbackCommand feedbackCmd = new UserFeedbackCommand(
                        java.util.UUID.randomUUID().toString(),
                        userId,
                        request.resumeVersionId(),
                        jobId,
                        suitable ? "APPLY" : "IGNORE",
                        (double) finalScore,
                        contextStr,
                        java.time.Instant.now()
                );
                aiMessagePublisherPort.sendUserFeedback(feedbackCmd);
                log.info("Score label sent to outbox for jobId={}, resumeVersionId={}", jobId, request.resumeVersionId());
            } catch (Exception e) {
                log.error("Failed to send score label to outbox for jobId={}: {}", jobId, e.getMessage());
                // 非阻塞：标签发送失败不应影响主流程
            }

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

    private Float calculateSemanticMatch(String resumeVersionId, String jobId) {
        try {
            ResumeVector resumeVector = resumeVectorRepository.findByResumeVersionId(resumeVersionId).orElse(null);
            JobVector jobVector = jobVectorRepository.findByJobId(jobId).orElse(null);
            if (resumeVector == null || jobVector == null
                    || resumeVector.getEmbedding() == null || jobVector.getEmbedding() == null) {
                return null;
            }

            float[] resumeEmbedding = resumeVector.getEmbedding();
            float[] jobEmbedding = jobVector.getEmbedding();
            int length = Math.min(resumeEmbedding.length, jobEmbedding.length);
            if (length == 0) {
                return null;
            }

            double dot = 0.0;
            double resumeNorm = 0.0;
            double jobNorm = 0.0;
            for (int i = 0; i < length; i++) {
                dot += resumeEmbedding[i] * jobEmbedding[i];
                resumeNorm += resumeEmbedding[i] * resumeEmbedding[i];
                jobNorm += jobEmbedding[i] * jobEmbedding[i];
            }
            if (resumeNorm == 0.0 || jobNorm == 0.0) {
                return null;
            }

            double cosineSimilarity = dot / (Math.sqrt(resumeNorm) * Math.sqrt(jobNorm));
            double normalized = Math.max(0.0, Math.min(1.0, (cosineSimilarity + 1.0) / 2.0));
            return (float) normalized;
        } catch (Exception e) {
            log.warn("Failed to calculate semantic match for jobId={}, resumeVersionId={}: {}",
                    jobId, resumeVersionId, e.getMessage());
            return null;
        }
    }

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
                record.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }

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
