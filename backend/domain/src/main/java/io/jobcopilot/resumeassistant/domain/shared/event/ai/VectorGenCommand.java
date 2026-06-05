package io.jobcopilot.resumeassistant.domain.shared.event.ai;

public record VectorGenCommand(
        String referenceId,
        String entityType,
        String text
) {
}
