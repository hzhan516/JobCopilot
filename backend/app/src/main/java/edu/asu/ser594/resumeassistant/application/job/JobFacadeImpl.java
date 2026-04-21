package edu.asu.ser594.resumeassistant.application.job;

import edu.asu.ser594.resumeassistant.api.job.dto.request.JobMatchRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.matching.dto.response.JobMatchHistoryResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobMatchResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.MatchFactors;
import edu.asu.ser594.resumeassistant.api.job.dto.response.MatchItem;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.application.job.service.JobApplicationService;
import edu.asu.ser594.resumeassistant.application.matching.command.SaveMatchResultCommand;
import edu.asu.ser594.resumeassistant.application.matching.command.StartJobMatchCommand;
import edu.asu.ser594.resumeassistant.application.matching.query.GetMatchResultQuery;
import edu.asu.ser594.resumeassistant.application.matching.query.ListMatchHistoryQuery;
import edu.asu.ser594.resumeassistant.application.matching.service.MatchingApplicationService;
import edu.asu.ser594.resumeassistant.domain.matching.entity.JobMatchResult;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.MatchStatus;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 职位门面实现
 * Job Facade Implementation
 */
@Component
@RequiredArgsConstructor
public class JobFacadeImpl implements JobFacade {

    private final JobApplicationService applicationService;
    private final MatchingApplicationService matchingApplicationService;

    @Override
    public JobResponse submitJob(final UUID userId, final SubmitJobRequest request) {
        return applicationService.submitJob(userId, request);
    }

    @Override
    public JobResponse getJob(final String jobId, final UUID userId) {
        return applicationService.getJob(jobId, userId);
    }

    @Override
    public List<JobResponse> listJobs(final UUID userId) {
        return applicationService.listJobs(userId);
    }

    @Override
    public JobMatchResponse matchJobs(final UUID userId, final JobMatchRequest request) {
        final StartJobMatchCommand command = StartJobMatchCommand.builder()
                .userId(userId)
                .resumeVersionId(request.userId()) // 注意：JobMatchRequest 当前字段设计为 userId 存储 resumeVersionId？需要确认
                .query(request.query())
                .topK(request.topK())
                .build();
        final String matchId = matchingApplicationService.startJobMatch(command);
        return JobMatchResponse.processing(matchId);
    }

    @Override
    public JobMatchResponse getMatchResult(final String matchId) {
        return matchingApplicationService.getMatchResult(new GetMatchResultQuery(matchId))
                .map(this::mapToJobMatchResponse)
                .orElseThrow(() -> new IllegalArgumentException("Match result not found: " + matchId));
    }

    @Override
    public List<JobMatchHistoryResponse> getMatchHistory(final UUID userId) {
        return matchingApplicationService.listMatchHistory(new ListMatchHistoryQuery(userId))
                .stream()
                .map(this::mapToJobMatchHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void handleJobProcessResult(final AiResultEvent event) {
        applicationService.handleJobProcessResult(event);
    }

    @Override
    public void saveJobRankResult(final String matchId, final List<MatchItem> rankedResults, final Long rankTimeMs) {
        final SaveMatchResultCommand command = SaveMatchResultCommand.builder()
                .matchId(matchId)
                .rankedResults(rankedResults)
                .rankTimeMs(rankTimeMs)
                .build();
        matchingApplicationService.saveMatchResult(command);
    }

    private JobMatchResponse mapToJobMatchResponse(final JobMatchResult result) {
        final List<MatchItem> matches = result.getRankedResults().stream()
                .map(rj -> new MatchItem(
                        rj.jobId(),
                        rj.title(),
                        rj.company(),
                        rj.matchScore(),
                        new MatchFactors(0.0, 0.0, 0.0), // 领域层未存储详细因子，使用默认值
                        rj.description()
                ))
                .collect(Collectors.toList());

        if (result.getStatus() == MatchStatus.PROCESSING) {
            return JobMatchResponse.processing(result.getId());
        }
        return JobMatchResponse.completed(
                result.getId(),
                matches,
                matches.size(),
                result.getRecallTimeMs(),
                result.getRankTimeMs()
        );
    }

    private JobMatchHistoryResponse mapToJobMatchHistoryResponse(final JobMatchResult result) {
        final List<MatchItem> matches = result.getRankedResults().stream()
                .map(rj -> new MatchItem(
                        rj.jobId(),
                        rj.title(),
                        rj.company(),
                        rj.matchScore(),
                        new MatchFactors(0.0, 0.0, 0.0),
                        rj.description()
                ))
                .collect(Collectors.toList());

        return new JobMatchHistoryResponse(
                result.getId(),
                result.getUserId().toString(),
                result.getResumeVersionId(),
                result.getQuery(),
                result.getStatus().name(),
                matches,
                matches.size(),
                result.getRecallTimeMs(),
                result.getRankTimeMs(),
                result.getModelVersion(),
                result.getCreatedAt(),
                result.getCompletedAt()
        );
    }
}
