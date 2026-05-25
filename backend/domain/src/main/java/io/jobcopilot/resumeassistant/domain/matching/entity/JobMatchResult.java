package io.jobcopilot.resumeassistant.domain.matching.entity;

import io.jobcopilot.resumeassistant.domain.matching.valueobject.MatchStatus;
import io.jobcopilot.resumeassistant.domain.matching.valueobject.RankedJob;
import io.jobcopilot.resumeassistant.domain.matching.valueobject.RecallResult;
import io.jobcopilot.resumeassistant.domain.shared.entity.AggregateRoot;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 职位匹配结果实体
 * Job match result entity
 * <p>
 * 对应 job_match_results 表 / Corresponds to job_match_results table
 */
@Getter
public class JobMatchResult extends AggregateRoot<String> {

    private final String id;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private String resumeVersionId;
    private String query;
    private MatchStatus status;
    private List<RecallResult> recallResults;
    private List<RankedJob> rankedResults;
    private Long recallTimeMs;
    private Long rankTimeMs;
    private String modelVersion;
    private LocalDateTime completedAt;
    private long version;

    @Builder
    public JobMatchResult(final String id,
                          final UUID userId,
                          final String resumeVersionId,
                          final String query,
                          final MatchStatus status,
                          final List<RecallResult> recallResults,
                          final List<RankedJob> rankedResults,
                          final Long recallTimeMs,
                          final Long rankTimeMs,
                          final String modelVersion,
                          final LocalDateTime createdAt,
                          final LocalDateTime completedAt,
                          final long version) {
        this.id = id;
        this.userId = userId;
        this.resumeVersionId = resumeVersionId;
        this.query = query;
        this.status = status;
        this.recallResults = recallResults != null ? new ArrayList<>(recallResults) : new ArrayList<>();
        this.rankedResults = rankedResults != null ? new ArrayList<>(rankedResults) : new ArrayList<>();
        this.recallTimeMs = recallTimeMs;
        this.rankTimeMs = rankTimeMs;
        this.modelVersion = modelVersion;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.version = version;
    }

    /**
     * 创建新的匹配结果（初始状态为处理中）
     * Create a new match result with PROCESSING status
     *
     * @param matchId         匹配ID / Match ID
     * @param userId          用户ID / User ID
     * @param resumeVersionId 简历版本ID / Resume version ID
     * @param query           查询词 / Query
     * @param modelVersion    模型版本 / Model version
     * @return 新的匹配结果实体 / New match result entity
     */
    public static JobMatchResult createProcessing(final String matchId,
                                                  final UUID userId,
                                                  final String resumeVersionId,
                                                  final String query,
                                                  final String modelVersion) {
        return JobMatchResult.builder()
                .id(matchId)
                .userId(userId)
                .resumeVersionId(resumeVersionId)
                .query(query)
                .status(MatchStatus.PROCESSING)
                .recallResults(new ArrayList<>())
                .rankedResults(new ArrayList<>())
                .modelVersion(modelVersion)
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * 设置召回结果
     * Set recall results
     *
     * @param results      召回结果列表 / Recall result list
     * @param recallTimeMs 召回耗时(毫秒) / Recall time in ms
     */
    public void setRecallResults(final List<RecallResult> results, final Long recallTimeMs) {
        this.recallResults = new ArrayList<>(results);
        this.recallTimeMs = recallTimeMs;
    }

    /**
     * 标记匹配完成并设置精排结果
     * Mark match as completed and set ranked results
     *
     * @param rankedJobs 精排结果 / Ranked jobs
     * @param rankTimeMs 精排耗时(毫秒) / Ranking time in ms
     */
    public void complete(final List<RankedJob> rankedJobs, final Long rankTimeMs) {
        this.rankedResults = new ArrayList<>(rankedJobs);
        this.rankTimeMs = rankTimeMs;
        this.status = MatchStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 标记匹配失败
     * Mark match as failed
     */
    public void markFailed() {
        this.status = MatchStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
}
