package edu.asu.ser594.resumeassistant.domain.shared.event.ai;

public record ResumeParseCommand(
    String resumeId,
    String fileUrl,
    String format
) {}
