package io.jobcopilot.resumeassistant.api.version.dto;

/** 运行时版本响应 / Runtime version response */
public record VersionResponse(
    String version,
    String component
) {}
