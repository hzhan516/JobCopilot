package edu.asu.ser594.resumeassistant.application.resume.command;

import lombok.Builder;

import java.util.UUID;

/**
 * 创建简历版本命令
 * Create resume version command
 * <p>
 * 应用层命令对象，用于编排创建简历副本的用例。
 * Application layer command object for orchestrating the resume copy creation use case.
 */
@Builder
public record CreateVersionCommand(
        /**
         * 简历组ID
         * Resume group ID
         */
        UUID groupId,

        /**
         * 源版本ID（可选）
         * Source version ID (optional)
         */
        UUID sourceVersionId,

        /**
         * 当前用户ID
         * Current user ID
         */
        UUID userId
) {
}
