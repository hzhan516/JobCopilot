package edu.asu.ser594.resumeassistant.domain.job.valueobject;

/**
 * Represents the raw output from the web scraping phase.
 * 
 * @param markdownText The scraped content converted to Markdown.
 * @param screenshotUrl A URL or reference to a screenshot of the page, if image check is enabled.
 */
public record ScrapeResult(
    String markdownText,
    String screenshotUrl
) {
}
