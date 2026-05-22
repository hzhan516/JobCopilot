package io.jobcopilot.resumeassistant.domain.shared.event.ai;

import java.time.Instant;
import java.util.UUID;

public record UserFeedbackCommand(
        String matchId,
        UUID userId,
        String resumeVersionId,
        String jobId,
        String feedbackType,
        Double score,
        String context,
        Instant timestamp
) {
}
