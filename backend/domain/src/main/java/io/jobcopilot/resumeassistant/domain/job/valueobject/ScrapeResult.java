package io.jobcopilot.resumeassistant.domain.job.valueobject;

/**
 * 表示网页抓取阶段的原始输出
 * Represents the raw output from the web scraping phase.
 *
 * @param markdownText  The scraped content converted to Markdown.
 * @param screenshotUrl A URL or reference to a screenshot of the page, if image check is enabled.
 */
public record ScrapeResult(
        String markdownText,
        String screenshotUrl
) {
}
