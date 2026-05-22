package io.jobcopilot.resumeassistant.application.matching;

import io.jobcopilot.resumeassistant.api.job.dto.request.JobMatchRequest;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobMatchResponse;
import io.jobcopilot.resumeassistant.api.job.dto.response.MatchFactors;
import io.jobcopilot.resumeassistant.api.job.dto.response.MatchItem;
import io.jobcopilot.resumeassistant.api.matching.dto.response.JobMatchHistoryResponse;
import io.jobcopilot.resumeassistant.api.matching.facade.MatchingFacade;
import io.jobcopilot.resumeassistant.application.matching.command.SaveMatchResultCommand;
import io.jobcopilot.resumeassistant.application.matching.command.StartJobMatchCommand;
import io.jobcopilot.resumeassistant.application.matching.query.GetMatchResultQuery;
import io.jobcopilot.resumeassistant.application.matching.query.ListMatchHistoryQuery;
import io.jobcopilot.resumeassistant.application.matching.service.MatchingApplicationService;
import io.jobcopilot.resumeassistant.domain.matching.entity.JobMatchResult;
import io.jobcopilot.resumeassistant.domain.matching.valueobject.MatchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 职位匹配门面实现
 * Job matching facade implementation
 */
@Component
@RequiredArgsConstructor
public class MatchingFacadeImpl implements MatchingFacade {

    private final MatchingApplicationService matchingApplicationService;

    @Override
    public JobMatchResponse matchJobs(final UUID userId, final JobMatchRequest request) {
        final StartJobMatchCommand command = StartJobMatchCommand.builder()
                .userId(userId)
                .resumeVersionId(request.resumeVersionId())
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
                        new MatchFactors(0.0, 0.0, 0.0),
                        rj.description(),
                        rj.matchReason()
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
                        rj.description(),
                        rj.matchReason()
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
                result.getCreatedAt().atOffset(ZoneOffset.UTC),
                result.getCompletedAt() != null ? result.getCompletedAt().atOffset(ZoneOffset.UTC) : null
        );
    }
}
