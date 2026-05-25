package io.jobcopilot.resumeassistant.application.matching.service;

import io.jobcopilot.resumeassistant.application.matching.command.SaveMatchResultCommand;
import io.jobcopilot.resumeassistant.application.matching.command.StartJobMatchCommand;
import io.jobcopilot.resumeassistant.application.matching.query.GetMatchResultQuery;
import io.jobcopilot.resumeassistant.application.matching.query.ListMatchHistoryQuery;
import io.jobcopilot.resumeassistant.domain.embedding.port.VectorGenerationPort;
import io.jobcopilot.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.matching.entity.JobMatchResult;
import io.jobcopilot.resumeassistant.domain.matching.entity.MatchingModel;
import io.jobcopilot.resumeassistant.domain.matching.exception.ResumeVectorNotReadyException;
import io.jobcopilot.resumeassistant.domain.matching.port.VectorSearchPort;
import io.jobcopilot.resumeassistant.domain.matching.repository.JobMatchResultRepository;
import io.jobcopilot.resumeassistant.domain.matching.repository.MatchingModelRepository;
import io.jobcopilot.resumeassistant.domain.matching.valueobject.RankedJob;
import io.jobcopilot.resumeassistant.domain.matching.valueobject.RecallResult;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.JobRankCommand;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Encapsulates the transactional core of the matching pipeline.
 * Separated from the orchestrator so that long-running non-DB work
 * (vector generation HTTP calls) can run outside the transaction boundary.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MatchTransactionService {

    private final JobMatchResultRepository jobMatchResultRepository;
    private final ResumeVectorRepository resumeVectorRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final JobRepository jobRepository;
    private final AiMessagePublisherPort aiMessagePublisherPort;
    private final VectorSearchPort vectorSearchPort;

    @Transactional
    public String execute(StartJobMatchCommand command, String matchId, String modelVersion) {
        final JobMatchResult result = JobMatchResult.createProcessing(
                matchId, command.userId(), command.resumeVersionId(), command.query(), modelVersion);
        jobMatchResultRepository.save(result);

        final var resumeVectorOpt = resumeVectorRepository.findByResumeVersionId(command.resumeVersionId());
        if (resumeVectorOpt.isEmpty() || resumeVectorOpt.get().getEmbedding() == null) {
            throw new ResumeVectorNotReadyException(command.resumeVersionId());
        }

        final var resumeVector = resumeVectorOpt.get();

        final int topK = command.topK() != null && command.topK() > 0 ? command.topK() : 10;
        final long recallStart = System.currentTimeMillis();
        final List<RecallResult> recallResults = vectorSearchPort.findSimilarJobs(
                resumeVector.getEmbedding(), topK, modelVersion);
        final long recallTimeMs = System.currentTimeMillis() - recallStart;

        final Map<String, Object> jobDetails = buildJobDetailsMap(recallResults);
        final List<RecallResult> visibleRecallResults = recallResults.stream()
                .filter(recall -> jobDetails.containsKey(recall.jobId()))
                .collect(Collectors.toList());

        result.setRecallResults(visibleRecallResults, recallTimeMs);
        jobMatchResultRepository.save(result);

        final List<String> recalledJobIds = visibleRecallResults.stream()
                .map(RecallResult::jobId)
                .collect(Collectors.toList());

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
                    if (job.isHidden()) {
                        return null;
                    }
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

/**
 * Orchestrates the two-stage job-matching pipeline: vector-based recall followed by AI-driven ranking.
 * Persists intermediate and final results so clients can poll for asynchronous completion.
 * 编排两阶段职位匹配流水线：基于向量的召回后接 AI 精排。持久化中间及最终结果，使客户端可轮询异步完成状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingApplicationService {

    private final ResumeVectorRepository resumeVectorRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final MatchingModelRepository matchingModelRepository;
    private final JobMatchResultRepository jobMatchResultRepository;
    private final VectorGenerationPort vectorGenerationPort;
    private final MatchTransactionService matchTransactionService;

    /**
     * Initiates the async matching workflow: recalls top-K similar jobs via pgvector cosine distance,
     * persists the recall stage, then dispatches a ranking request to the AI service via MQ.
     * <p><b>Transaction boundary:</b> vector generation (HTTP call) runs <em>outside</em> the DB
     * transaction; the lightweight recall + persist + outbox-write runs <em>inside</em> a short
     * transaction via {@link MatchTransactionService}.</p>
     * 启动异步匹配工作流：通过 pgvector 余弦距离召回 Top-K 相似职位，持久化召回阶段，然后通过 MQ 向 AI 服务分发精排请求。
     * 事务边界：向量生成（HTTP 调用）在数据库事务外执行；轻量的召回+持久化+Outbox 写入在短事务内完成。
     *
     * @param command Start match command / 启动匹配命令
     * @return Match task ID / 匹配任务 ID
     */
    public String startJobMatch(final StartJobMatchCommand command) {
        final String matchId = UUID.randomUUID().toString();
        log.info("Starting job match for user: {}, matchId: {}", command.userId(), matchId);

        final String modelVersion = matchingModelRepository.findActiveByType(
                        io.jobcopilot.resumeassistant.domain.matching.valueobject.ModelType.RECALL)
                .map(MatchingModel::getVersion)
                .orElse("default");

        final var resumeVectorOpt = resumeVectorRepository.findByResumeVersionId(command.resumeVersionId());

        if (resumeVectorOpt.isEmpty() || resumeVectorOpt.get().getEmbedding() == null) {
            // On-demand vector regeneration: if the vector is missing (e.g., after a resume edit), try to rebuild it before failing
            // 按需向量再生：若向量缺失（例如简历编辑后），在报错前尝试重建
            final ResumeVersion resumeVersion = resumeVersionRepository.findById(UUID.fromString(command.resumeVersionId()))
                    .orElseThrow(() -> new IllegalArgumentException("Resume version not found: " + command.resumeVersionId()));

            final String vectorText = resumeVersion.getParsedContent() != null && !resumeVersion.getParsedContent().isEmpty()
                    ? resumeVersion.getParsedContent()
                    : (resumeVersion.getContent() != null ? resumeVersion.getContent() : "");

            if (!vectorText.isEmpty()) {
                try {
                    vectorGenerationPort.generateAndSaveVector(command.resumeVersionId(), "RESUME", vectorText);
                    log.info("Synchronously generated vector for missing resume vector, versionId={}", command.resumeVersionId());
                } catch (Exception e) {
                    log.error("Failed to generate vector for versionId={}", command.resumeVersionId(), e);
                }
            }

            throw new ResumeVectorNotReadyException(command.resumeVersionId());
        }

        return matchTransactionService.execute(command, matchId, modelVersion);
    }

    /**
     * Persists the final ranked list produced by the AI service, transitioning the match result
     * from PROCESSING to COMPLETED.
     * 持久化 AI 服务产出的最终排序列表，将匹配结果从 PROCESSING 状态转为 COMPLETED
     *
     * @param command Save match result command / 保存匹配结果命令
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

    @Transactional(readOnly = true)
    public Optional<JobMatchResult> getMatchResult(final GetMatchResultQuery query) {
        return jobMatchResultRepository.findById(query.matchId());
    }

    @Transactional(readOnly = true)
    public List<JobMatchResult> listMatchHistory(final ListMatchHistoryQuery query) {
        return jobMatchResultRepository.findAllByUserIdOrderByCreatedAtDesc(query.userId());
    }
}
