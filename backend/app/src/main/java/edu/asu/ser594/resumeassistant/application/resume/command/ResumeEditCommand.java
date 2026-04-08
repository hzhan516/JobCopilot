package edu.asu.ser594.resumeassistant.application.resume.command;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 编辑简历命令
 * Edit Resume Command
 */
@Getter
@Builder
public final class ResumeEditCommand {
    private final UUID versionId;
    private final UUID userId;
    private final String content;
}