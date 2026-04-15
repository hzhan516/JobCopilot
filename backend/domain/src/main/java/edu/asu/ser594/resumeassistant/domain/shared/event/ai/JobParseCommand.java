package edu.asu.ser594.resumeassistant.domain.shared.event.ai;

public record JobParseCommand(
    String jobId,
    String url,
    boolean imageCheckEnabled
) {}
