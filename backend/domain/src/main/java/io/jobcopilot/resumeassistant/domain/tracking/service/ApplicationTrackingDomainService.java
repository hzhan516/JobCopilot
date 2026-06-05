package io.jobcopilot.resumeassistant.domain.tracking.service;

import io.jobcopilot.resumeassistant.domain.tracking.exception.TrackingException;
import io.jobcopilot.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * Centralizes status-transition rules so that invalid business-state changes are rejected at the domain level,
 * not just at the database constraint level.
 * 集中管理状态流转规则，使无效的业务状态变更在领域层即被拒绝，而非仅依赖数据库约束。
 */
@Slf4j
public class ApplicationTrackingDomainService {

    public void validateStatusTransition(final ApplicationStatus fromStatus, final ApplicationStatus toStatus) {
        if (!fromStatus.canTransitionTo(toStatus)) {
            log.warn("Invalid status transition attempted: {} -> {}", fromStatus, toStatus);
            throw new TrackingException("tracking.status.invalid", fromStatus, toStatus);
        }
    }
}
