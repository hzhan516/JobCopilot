package io.jobcopilot.resumeassistant.domain.shared.event.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 职位解析命令 / Job parse command sent to AI service.
 */
public class JobParseCommand {

    private final String jobId;
    private final String url;
    private final boolean imageCheckEnabled;
    private final String screenshotBase64;

    public JobParseCommand(String jobId, String url, boolean imageCheckEnabled) {
        this(jobId, url, imageCheckEnabled, null);
    }

    public JobParseCommand(String jobId, String url, boolean imageCheckEnabled, String screenshotBase64) {
        this.jobId = jobId;
        this.url = url;
        this.imageCheckEnabled = imageCheckEnabled;
        this.screenshotBase64 = screenshotBase64;
    }

    @JsonProperty("jobId")
    public String getJobId() {
        return jobId;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("imageCheckEnabled")
    public boolean isImageCheckEnabled() {
        return imageCheckEnabled;
    }

    @JsonProperty("screenshotBase64")
    public String getScreenshotBase64() {
        return screenshotBase64;
    }
}
