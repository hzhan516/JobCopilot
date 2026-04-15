package edu.asu.ser594.resumeassistant.domain.job.service;

import edu.asu.ser594.resumeassistant.domain.job.valueobject.ScrapeResult;

/**
 * Outbound port for web scraping services.
 */
public interface WebScraperPort {

    /**
     * Scrapes a job posting URL to extract its text and an optional screenshot.
     * 
     * @param url The target job posting URL.
     * @param captureScreenshot True if a screenshot of the page is required.
     * @return A ScrapeResult containing markdown text and an optional screenshot URL.
     */
    ScrapeResult scrape(String url, boolean captureScreenshot);
}
