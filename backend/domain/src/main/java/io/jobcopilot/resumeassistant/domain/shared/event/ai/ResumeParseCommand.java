package io.jobcopilot.resumeassistant.domain.shared.event.ai;

public record ResumeParseCommand(
        String resumeId,
        String fileUrl,
        String format
) {
}
