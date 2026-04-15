package edu.asu.ser594.resumeassistant.domain.job.service;

import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;

/**
 * Outbound port for visual verification of parsed content against a screenshot.
 */
public interface VisionVerificationPort {

    /**
     * Verifies the parsed content against the visual layout of the job posting screenshot
     * and fixes any discrepancies.
     * 
     * @param parsedContent The initially parsed structured content.
     * @param screenshotUrl The URL to the screenshot of the job posting.
     * @return The potentially corrected and verified ParsedJobContent.
     */
    ParsedJobContent verifyAndFix(ParsedJobContent parsedContent, String screenshotUrl);
}
