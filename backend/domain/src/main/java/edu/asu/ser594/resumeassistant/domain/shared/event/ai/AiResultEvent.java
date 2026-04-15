package edu.asu.ser594.resumeassistant.domain.shared.event.ai;

import java.util.Map;

public record AiResultEvent(
    String referenceId,
    String type,
    String status,
    Map<String, Object> data,
    String errorMessage
) {}
