package io.jobcopilot.resumeassistant.domain.job.entity;

import io.jobcopilot.resumeassistant.domain.job.valueobject.JobStatus;
import io.jobcopilot.resumeassistant.domain.job.valueobject.ParsedJobContent;
import io.jobcopilot.resumeassistant.domain.shared.entity.AggregateRoot;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 职位聚合根。管理职位发布URL的处理生命周期
 * The Job aggregate root. Manages the lifecycle of processing a job posting url.
 */
public class Job extends AggregateRoot<String> {

    private final String id;
    @Getter
    private final UUID userId;
    @Getter
    private final String originalUrl;
    @Getter
    private final boolean imageCheckEnabled;
    @Getter
    private JobStatus status;
    @Getter
    private ParsedJobContent parsedContent;
    @Getter
    private String errorMessage;
    @Getter
    private LocalDateTime hiddenAt;
    @Getter
    private long version;

    public Job(String id, UUID userId, String originalUrl, boolean imageCheckEnabled, JobStatus status, ParsedJobContent parsedContent, String errorMessage, LocalDateTime hiddenAt, long version) {
        this.id = id;
        this.userId = userId;
        this.originalUrl = originalUrl;
        this.imageCheckEnabled = imageCheckEnabled;
        this.status = status;
        this.parsedContent = parsedContent;
        this.errorMessage = errorMessage;
        this.hiddenAt = hiddenAt;
        this.version = version;
    }

    private Job(String id, UUID userId, String originalUrl, boolean imageCheckEnabled, JobStatus status) {
        this(id, userId, originalUrl, imageCheckEnabled, status, null, null, null, 0L);
    }

    /**
     * 创建新职位用于处理
     * Creates a new Job for processing.
     *
     * @param userId            The ID of the user requesting the job parse.
     * @param url               The URL of the job posting.
     * @param imageCheckEnabled Whether visual verification is required.
     * @return A newly initialized Job aggregate root.
     */
    public static Job create(UUID userId, String url, boolean imageCheckEnabled) {
        return new Job(UUID.randomUUID().toString(), userId, url, imageCheckEnabled, JobStatus.PENDING);
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * 转换职位状态以表示抓取已开始
     * Transitions the job state to indicate scraping has started.
     */
    public void markScraping() {
        if (this.status != JobStatus.PENDING) {
            throw new IllegalStateException("Job must be PENDING to start scraping.");
        }
        this.status = JobStatus.SCRAPING;
    }

    /**
     * 转换职位状态以表示解析已开始
     * Transitions the job state to indicate parsing has started.
     */
    public void markParsing() {
        if (this.status != JobStatus.SCRAPING) {
            throw new IllegalStateException("Job must be SCRAPING to start parsing.");
        }
        this.status = JobStatus.PARSING;
    }

    /**
     * 标记职位为成功完成并携带解析内容
     * Marks the job as successfully completed with the parsed content.
     *
     * @param parsedContent The structured data extracted from the job posting.
     */
    public void markCompleted(ParsedJobContent parsedContent) {
        if (this.status != JobStatus.PARSING) {
            throw new IllegalStateException("Job must be PARSING to complete.");
        }
        this.status = JobStatus.COMPLETED;
        this.parsedContent = parsedContent;
    }

    /**
     * 标记职位为失败并记录错误原因
     * Marks the job as failed and records the error reason.
     *
     * @param error A description of why the job processing failed.
     */
    public void markFailed(String error) {
        this.status = JobStatus.FAILED;
        this.errorMessage = error;
    }

    /**
     * 直接更新解析后的职位内容（用户手动编辑）
     * Updates the parsed content directly (user manual edit).
     *
     * @param newContent The new parsed content.
     */
    public void updateParsedContent(ParsedJobContent newContent) {
        this.parsedContent = newContent;
    }

    /**
     * 隐藏职位，使其不再出现在用户列表中，但保留数据库记录。
     * Hide the job from user-facing lists while keeping the database record.
     */
    public void hide() {
        if (this.hiddenAt == null) {
            this.hiddenAt = LocalDateTime.now();
        }
    }

    public boolean isHidden() {
        return hiddenAt != null;
    }

}
