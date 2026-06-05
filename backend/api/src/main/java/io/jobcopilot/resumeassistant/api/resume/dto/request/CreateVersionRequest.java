package io.jobcopilot.resumeassistant.api.resume.dto.request;

import lombok.Builder;

import java.util.UUID;

/**
 * 创建简历版本请求
 * Create resume version request
 * <p>
 * 用于基于现有版本创建新的 CONVERTED 副本。
 * Used to create a new CONVERTED copy based on an existing version.
 */
@Builder
public record CreateVersionRequest(
        /**
         * 源版本ID（可选）
         * Source version ID (optional)
         * <p>
         * 若为空，则基于当前 ACTIVE 的 CONVERTED 版本创建副本；
         * If empty, creates a copy based on the current ACTIVE CONVERTED version;
         * 若该组不存在 CONVERTED 版本，则创建空白副本。
         * if no CONVERTED version exists in the group, creates a blank copy.
         */
        UUID sourceVersionId
) {
}
