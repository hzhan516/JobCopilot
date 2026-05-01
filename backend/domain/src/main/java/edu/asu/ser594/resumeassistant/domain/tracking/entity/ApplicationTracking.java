package edu.asu.ser594.resumeassistant.domain.tracking.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.AggregateRoot;
import edu.asu.ser594.resumeassistant.domain.tracking.exception.TrackingException;
import edu.asu.ser594.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 求职申请跟踪聚合根
 * Job application tracking aggregate root
 */
@Getter
public class ApplicationTracking extends AggregateRoot<String> {

    private final String id;
    private final UUID userId;
    private final List<TrackingEvent> events;
    private String jobId;
    private String companyName;
    private String jobTitle;
    private ApplicationStatus status;
    private LocalDate appliedAt;
    private String notes;
    private LocalDateTime updatedAt;

    @Builder
    public ApplicationTracking(final String id,
                               final UUID userId,
                               final String jobId,
                               final String companyName,
                               final String jobTitle,
                               final ApplicationStatus status,
                               final LocalDate appliedAt,
                               final String notes,
                               final LocalDateTime updatedAt,
                               final List<TrackingEvent> events) {
        this.id = id;
        this.userId = userId;
        this.jobId = jobId;
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.status = status;
        this.appliedAt = appliedAt;
        this.notes = notes;
        this.updatedAt = updatedAt;
        this.events = events != null ? new ArrayList<>(events) : new ArrayList<>();
    }

    /**
     * 创建新的求职跟踪记录
     * Create a new application tracking record
     *
     * @param userId      用户ID / User ID
     * @param jobId       职位ID(可选) / Job ID (optional)
     * @param companyName 公司名称 / Company name
     * @param jobTitle    职位标题 / Job title
     * @param appliedAt   投递日期(可选) / Applied date (optional)
     * @param notes       备注(可选) / Notes (optional)
     * @return 新的跟踪实体 / New tracking entity
     */
    public static ApplicationTracking create(final UUID userId,
                                             final String jobId,
                                             final String companyName,
                                             final String jobTitle,
                                             final LocalDate appliedAt,
                                             final String notes) {
        final String id = UUID.randomUUID().toString();
        final ApplicationTracking tracking = ApplicationTracking.builder()
                .id(id)
                .userId(userId)
                .jobId(jobId)
                .companyName(companyName)
                .jobTitle(jobTitle)
                .status(ApplicationStatus.PENDING)
                .appliedAt(appliedAt)
                .notes(notes)
                .updatedAt(LocalDateTime.now())
                .events(new ArrayList<>())
                .build();
        return tracking;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * 更新基本信息
     * Update basic information
     *
     * @param companyName 公司名称 / Company name
     * @param jobTitle    职位标题 / Job title
     * @param notes       备注 / Notes
     */
    public void updateInfo(final String companyName, final String jobTitle, final String notes) {
        if (companyName != null) {
            this.companyName = companyName;
        }
        if (jobTitle != null) {
            this.jobTitle = jobTitle;
        }
        if (notes != null) {
            this.notes = notes;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 变更状态
     * Change status
     *
     * @param newStatus 新状态 / New status
     * @param note      备注 / Note
     */
    public void changeStatus(final ApplicationStatus newStatus, final String note) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new TrackingException("tracking.status.invalid", this.status, newStatus);
        }
        final TrackingEvent event = TrackingEvent.create(this.status, newStatus, note);
        this.events.add(event);
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置投递日期
     * Set applied date
     *
     * @param appliedAt 投递日期 / Applied date
     */
    public void setAppliedAt(final LocalDate appliedAt) {
        this.appliedAt = appliedAt;
        this.updatedAt = LocalDateTime.now();
    }
}
