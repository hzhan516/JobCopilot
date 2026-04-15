package edu.asu.ser594.resumeassistant.domain.job.service;

import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;

/**
 * Outbound port for Large Language Model processing.
 */
public interface LlmParserPort {

    /**
     * Parses the unstructured markdown text from a job posting into structured content.
     * 
     * @param markdownText The raw text scraped from the job page.
     * @return Structured ParsedJobContent containing the core job details.
     */
    ParsedJobContent parse(String markdownText);
}
