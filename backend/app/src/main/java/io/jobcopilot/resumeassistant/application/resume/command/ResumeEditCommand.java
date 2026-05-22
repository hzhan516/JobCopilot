package io.jobcopilot.resumeassistant.application.resume.command;

import lombok.Builder;

import java.util.UUID;

/**
 * 编辑简历命令
 * Edit Resume Command
 */
@Builder
public record ResumeEditCommand(
        UUID versionId,
        UUID userId,
        String content
) {
}