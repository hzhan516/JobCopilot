package edu.asu.ser594.resumeassistant.domain.tracking.valueobject;

import java.util.Set;

/**
 * 求职申请状态枚举
 * Job application status enumeration
 */
public enum ApplicationStatus {
    PENDING,
    APPLIED,
    SCREENING,
    INTERVIEWING,
    OFFER,
    ACCEPTED,
    REJECTED,
    WITHDRAWN;

    private static final Set<Transition> VALID_TRANSITIONS = Set.of(
            new Transition(PENDING, APPLIED),
            new Transition(APPLIED, SCREENING),
            new Transition(APPLIED, REJECTED),
            new Transition(SCREENING, INTERVIEWING),
            new Transition(SCREENING, REJECTED),
            new Transition(INTERVIEWING, OFFER),
            new Transition(INTERVIEWING, REJECTED),
            new Transition(OFFER, ACCEPTED),
            new Transition(OFFER, REJECTED)
    );

    /**
     * 检查当前状态是否可以流转到目标状态
     * Check if current status can transition to target status
     *
     * @param targetStatus 目标状态 / Target status
     * @return 是否允许流转 / Whether transition is allowed
     */
    public boolean canTransitionTo(final ApplicationStatus targetStatus) {
        return VALID_TRANSITIONS.contains(new Transition(this, targetStatus));
    }

    private record Transition(ApplicationStatus from, ApplicationStatus to) {
    }
}
