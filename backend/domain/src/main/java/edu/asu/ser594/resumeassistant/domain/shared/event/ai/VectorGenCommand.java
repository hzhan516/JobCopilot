package edu.asu.ser594.resumeassistant.domain.shared.event.ai;

public record VectorGenCommand(
        String referenceId,
        String entityType,
        String text
) {
}
