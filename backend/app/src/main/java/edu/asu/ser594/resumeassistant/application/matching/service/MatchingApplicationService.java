package edu.asu.ser594.resumeassistant.application.matching.service;

import edu.asu.ser594.resumeassistant.application.matching.command.SaveMatchResultCommand;
import edu.asu.ser594.resumeassistant.application.matching.command.StartJobMatchCommand;
import edu.asu.ser594.resumeassistant.application.matching.query.GetMatchResultQuery;
import edu.asu.ser594.resumeassistant.application.matching.query.ListMatchHistoryQuery;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.matching.entity.JobMatchResult;
import edu.asu.ser594.resumeassistant.domain.matching.entity.MatchingModel;
import edu.asu.ser594.resumeassistant.domain.matching.exception.ResumeVectorNotReadyException;
import edu.asu.ser594.resumeassistant.domain.matching.port.VectorSearchPort;
import edu.asu.ser594.resumeassistant.domain.matching.repository.JobMatchResultRepository;
import edu.asu.ser594.resumeassistant.domain.matching.repository.MatchingModelRepository;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.RankedJob;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.RecallResult;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobRankCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.VectorGenCommand;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 职位匹配应用服务
 * Job matching application service
 * <p>
 * 协调召回、MQ发送、结果保存 / Coordinates recall, MQ sending, and result persistence
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingApplicationService {

    private final ResumeVectorRepository resumeVectorRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final JobRepository jobRepository;
    private final JobMatchResultRepository jobMatchResultRepository;
    private final MatchingModelRepository matchingModelRepository;
    private final AiMessagePublisherPort aiMessagePublisherPort;
    private final VectorSearchPort vectorSearchPort;

    /**
     * 启动异步职位匹配流程
     * Start async job matching process
     *
     * @param command 启动命令 / Start command
     * @return 匹配任务ID / Match task ID
     */
    @Transactional
    public String startJobMatch(final StartJobMatchCommand command) {
        final String matchId = UUID.randomUUID().toString();
        log.info("Starting job match for user: {}, matchId: {}", command.userId(), matchId);

        final String modelVersion = matchingModelRepository.findActiveByType(
                        edu.asu.ser594.resumeassistant.domain.matching.valueobject.ModelType.RECALL)
                .map(MatchingModel::getVersion)
                .orElse("default");

        final JobMatchResult result = JobMatchResult.createProcessing(
                matchId, command.userId(), command.resumeVersionId(), command.query(), modelVersion);
        jobMatchResultRepository.save(result);

        final var resumeVectorOpt = resumeVectorRepository.findByResumeVersionId(command.resumeVersionId());

        if (resumeVectorOpt.isEmpty() || resumeVectorOpt.get().getEmbedding() == null) {
            // 向量缺失或生成失败，尝试触发重新生成并提示用户稍后重试
            // Vector missing or generation failed, trigger re-generation and ask user to retry later
            final ResumeVersion resumeVersion = resumeVersionRepository.findById(UUID.fromString(command.resumeVersionId()))
                    .orElseThrow(() -> new IllegalArgumentException("Resume version not found: " + command.resumeVersionId()));

            final String vectorText = resumeVersion.getParsedContent() != null && !resumeVersion.getParsedContent().isEmpty()
                    ? resumeVersion.getParsedContent()
                    : (resumeVersion.getContent() != null ? resumeVersion.getContent() : "");

            if (!vectorText.isEmpty()) {
                try {
                    VectorGenCommand vectorCmd = new VectorGenCommand(
                            command.resumeVersionId(),
                            "RESUME",
                            vectorText
                    );
                    aiMessagePublisherPort.sendTextForVectorGeneration(vectorCmd);
                    log.info("Triggered async vector re-generation for missing resume vector, versionId={}", command.resumeVersionId());
                } catch (Exception e) {
                    log.error("Failed to trigger vector re-generation for versionId={}", command.resumeVersionId(), e);
                }
            }

            throw new ResumeVectorNotReadyException(command.resumeVersionId());
        }

        final var resumeVector = resumeVectorOpt.get();

        final int topK = command.topK() != null && command.topK() > 0 ? command.topK() : 10;
        final long recallStart = System.currentTimeMillis();
        final List<RecallResult> recallResults = vectorSearchPort.findSimilarJobs(
                resumeVector.getEmbedding(), topK, modelVersion);
        final long recallTimeMs = System.currentTimeMillis() - recallStart;

        result.setRecallResults(recallResults, recallTimeMs);
        jobMatchResultRepository.save(result);

        final List<String> recalledJobIds = recallResults.stream()
                .map(RecallResult::jobId)
                .collect(Collectors.toList());

        final Map<String, Object> jobDetails = buildJobDetailsMap(recallResults);

        final ResumeVersion resumeVersion = resumeVersionRepository.findById(UUID.fromString(command.resumeVersionId()))
                .orElseThrow(() -> new IllegalArgumentException("Resume version not found: " + command.resumeVersionId()));
        
        final String resumeText = resumeVersion.getParsedContent() != null && !resumeVersion.getParsedContent().isEmpty() ?
                resumeVersion.getParsedContent() :
                (resumeVersion.getContent() != null ? resumeVersion.getContent() : "");

        final String query = command.query() != null ? command.query() : "";

        final JobRankCommand rankCommand = new JobRankCommand(
                matchId,
                command.userId().toString(),
                command.resumeVersionId(),
                resumeText,
                query,
                recalledJobIds,
                jobDetails
        );

        aiMessagePublisherPort.sendJobForRanking(rankCommand);
        log.info("Sent job ranking request to MQ for matchId: {}, recalled {} jobs, recallTimeMs: {}",
                matchId, recalledJobIds.size(), recallTimeMs);

        return matchId;
    }

    /**
     * 保存精排结果
     * Save ranking result
     *
     * @param command 保存命令 / Save command
     */
    @Transactional
    public void saveMatchResult(final SaveMatchResultCommand command) {
        log.info("Saving match result for matchId: {}", command.matchId());

        final JobMatchResult result = jobMatchResultRepository.findById(command.matchId())
                .orElseThrow(() -> new IllegalArgumentException("Match result not found: " + command.matchId()));

        final List<RankedJob> rankedJobs = command.rankedResults().stream()
                .map(item -> new RankedJob(
                        item.jobId(),
                        item.title(),
                        item.company(),
                        item.matchScore(),
                        item.description(),
                        item.matchReason()
                ))
                .collect(Collectors.toList());

        result.complete(rankedJobs, command.rankTimeMs());
        jobMatchResultRepository.save(result);
        log.info("Match result saved successfully for matchId: {}, ranked {} jobs", command.matchId(), rankedJobs.size());
    }

    /**
     * 查询匹配结果
     * Get match result
     *
     * @param query 查询对象 / Query object
     * @return 匹配结果实体(可选) / Optional match result entity
     */
    @Transactional(readOnly = true)
    public Optional<JobMatchResult> getMatchResult(final GetMatchResultQuery query) {
        return jobMatchResultRepository.findById(query.matchId());
    }

    /**
     * 查询用户匹配历史
     * List match history
     *
     * @param query 查询对象 / Query object
     * @return 匹配历史列表 / Match history list
     */
    @Transactional(readOnly = true)
    public List<JobMatchResult> listMatchHistory(final ListMatchHistoryQuery query) {
        return jobMatchResultRepository.findAllByUserIdOrderByCreatedAtDesc(query.userId());
    }

    private Map<String, Object> buildJobDetailsMap(final List<RecallResult> recallResults) {
        if (recallResults.isEmpty()) {
            return Collections.emptyMap();
        }
        return recallResults.stream()
                .map(recall -> {
                    Optional<Job> jobOpt = jobRepository.findById(recall.jobId());
                    if (jobOpt.isEmpty()) {
                        return null;
                    }
                    Job job = jobOpt.get();
                    // PGVector distance is Cosine Distance (1 - Cosine Similarity)
                    // We map distance to a semantic match score [0, 1]
                    // Cosine Similarity = 1 - distance
                    // Normalized Semantic Match = (Cosine Similarity + 1) / 2 = 1 - distance / 2
                    double semanticMatch = Math.max(0.0, Math.min(1.0, 1.0 - (recall.distance() / 2.0)));
                    
                    return Map.entry(
                            job.getId(),
                            Map.of(
                                    "title", Optional.ofNullable(job.getParsedContent()).map(pc -> pc.title()).orElse(""),
                                    "company", Optional.ofNullable(job.getParsedContent()).map(pc -> pc.company()).orElse(""),
                                    "description", Optional.ofNullable(job.getParsedContent()).map(pc -> pc.description()).orElse(""),
                                    "semanticMatch", semanticMatch
                            )
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
