package edu.asu.ser594.resumeassistant.domain.job.valueobject;

/**
 * 表示职位处理工作流的当前状态
 * Represents the current status of a Job processing workflow.
 */
public enum JobStatus {
    /**
     * 职位已提交但尚未开始处理
     * Job has been submitted but processing has not yet started.
     */
    PENDING,

    /**
     * 系统正在抓取职位发布URL
     * The system is currently scraping the job posting URL.
     */
    SCRAPING,

    /**
     * 系统正在将抓取内容解析为结构化数据
     * The system is currently parsing the scraped content into structured data.
     */
    PARSING,

    /**
     * 职位处理已成功完成
     * The job processing has completed successfully.
     */
    COMPLETED,

    /**
     * 职位处理已失败
     * The job processing has failed.
     */
    FAILED
}
