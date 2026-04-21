package edu.asu.ser594.resumeassistant.domain.tracking.service;

import edu.asu.ser594.resumeassistant.domain.tracking.exception.TrackingException;
import edu.asu.ser594.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * 求职申请跟踪领域服务
 * Application tracking domain service
 *
 * 负责状态流转校验等核心业务逻辑
 * Responsible for status transition validation and other core business logic
 */
@Slf4j
public class ApplicationTrackingDomainService {

    /**
     * 校验状态流转是否合法
     * Validate if status transition is legal
     *
     * @param fromStatus 当前状态 / Current status
     * @param toStatus 目标状态 / Target status
     */
    public void validateStatusTransition(final ApplicationStatus fromStatus, final ApplicationStatus toStatus) {
        if (!fromStatus.canTransitionTo(toStatus)) {
            log.warn("Invalid status transition attempted: {} -> {}", fromStatus, toStatus);
            throw new TrackingException("tracking.status.invalid", fromStatus, toStatus);
        }
    }
}
