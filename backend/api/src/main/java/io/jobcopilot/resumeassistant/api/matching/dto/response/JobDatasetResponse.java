package io.jobcopilot.resumeassistant.api.matching.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Job dataset record for AI service incremental training.
 * AI 服务增量训练用的职位数据集记录。
 */
public record JobDatasetResponse(
        UUID id,
        String externalId,
        String title,
        String company,
        String description,
        String requirements,
        String location,
        String experienceLevel,
        String source,
        Instant createdAt
) {
}
