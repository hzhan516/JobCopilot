package edu.asu.ser594.resumeassistant.domain.tracking.service;

import edu.asu.ser594.resumeassistant.domain.tracking.exception.TrackingException;
import edu.asu.ser594.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 求职申请跟踪领域服务单元测试
 * Application tracking domain service unit tests
 */
@DisplayName("Application Tracking Domain Service Tests")
class ApplicationTrackingDomainServiceTest {

    private final ApplicationTrackingDomainService domainService = new ApplicationTrackingDomainService();

    @ParameterizedTest
    @DisplayName("Should allow valid status transitions")
    @CsvSource({
            "PENDING, APPLIED",
            "APPLIED, SCREENING",
            "APPLIED, REJECTED",
            "SCREENING, INTERVIEWING",
            "SCREENING, REJECTED",
            "INTERVIEWING, OFFER",
            "INTERVIEWING, REJECTED",
            "OFFER, ACCEPTED",
            "OFFER, REJECTED"
    })
    void shouldAllowValidStatusTransitions(String from, String to) {
        ApplicationStatus fromStatus = ApplicationStatus.valueOf(from);
        ApplicationStatus toStatus = ApplicationStatus.valueOf(to);

        assertThatNoException()
                .isThrownBy(() -> domainService.validateStatusTransition(fromStatus, toStatus));
    }

    @ParameterizedTest
    @DisplayName("Should throw for invalid status transitions")
    @CsvSource({
            "PENDING, PENDING",
            "PENDING, REJECTED",
            "APPLIED, PENDING",
            "APPLIED, OFFER",
            "REJECTED, APPLIED",
            "ACCEPTED, REJECTED",
            "WITHDRAWN, PENDING"
    })
    void shouldThrowForInvalidStatusTransitions(String from, String to) {
        ApplicationStatus fromStatus = ApplicationStatus.valueOf(from);
        ApplicationStatus toStatus = ApplicationStatus.valueOf(to);

        assertThatThrownBy(() -> domainService.validateStatusTransition(fromStatus, toStatus))
                .isInstanceOf(TrackingException.class)
                .hasMessageContaining("tracking.status.invalid");
    }
}
