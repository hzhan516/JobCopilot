package edu.asu.ser594.resumeassistant.application.matching.service;

import edu.asu.ser594.resumeassistant.application.matching.command.SaveMatchResultCommand;
import edu.asu.ser594.resumeassistant.application.matching.command.StartJobMatchCommand;
import edu.asu.ser594.resumeassistant.application.matching.query.GetMatchResultQuery;
import edu.asu.ser594.resumeassistant.application.matching.query.ListMatchHistoryQuery;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.matching.entity.JobMatchResult;
import edu.asu.ser594.resumeassistant.domain.matching.entity.MatchingModel;
import edu.asu.ser594.resumeassistant.domain.matching.port.VectorSearchPort;
import edu.asu.ser594.resumeassistant.domain.matching.repository.JobMatchResultRepository;
import edu.asu.ser594.resumeassistant.domain.matching.repository.MatchingModelRepository;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.RankedJob;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.RecallResult;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobRankCommand;
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

        final var resumeVector = resumeVectorRepository.findByResumeVersionId(command.resumeVersionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Resume vector not found for version: " + command.resumeVersionId()));

        if (resumeVector.getEmbedding() == null) {
            throw new IllegalStateException("Resume vector embedding is null for version: " + command.resumeVersionId());
        }

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

        final Map<String, Object> jobDetails = buildJobDetailsMap(recalledJobIds);

        final JobRankCommand rankCommand = new JobRankCommand(
                matchId,
                command.userId().toString(),
                command.resumeVersionId(),
                "", // resumeText 可由 Python 端自行从简历版本获取，此处留空
                command.query(),
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
                        item.description()
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

    private Map<String, Object> buildJobDetailsMap(final List<String> jobIds) {
        if (jobIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return jobIds.stream()
                .map(jobRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        Job::getId,
                        job -> Map.of(
                                "title", Optional.ofNullable(job.getParsedContent()).map(pc -> pc.title()).orElse(""),
                                "company", Optional.ofNullable(job.getParsedContent()).map(pc -> pc.company()).orElse(""),
                                "description", Optional.ofNullable(job.getParsedContent()).map(pc -> pc.description()).orElse("")
                        )
                ));
    }
}
