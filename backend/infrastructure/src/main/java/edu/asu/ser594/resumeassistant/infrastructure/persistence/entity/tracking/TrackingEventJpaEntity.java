package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.tracking;

import edu.asu.ser594.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 跟踪事件 JPA 内嵌实体
 * Tracking event JPA embedded entity
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEventJpaEntity {

    private LocalDateTime timestamp;
    private ApplicationStatus fromStatus;
    private ApplicationStatus toStatus;
    private String note;
}
