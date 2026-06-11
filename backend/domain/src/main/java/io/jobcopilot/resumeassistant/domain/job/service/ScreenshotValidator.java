package io.jobcopilot.resumeassistant.domain.job.service;

/**
 * Validates screenshot Base64 payload against size limits.
 * Screenshots are encoded in Base64 which inflates raw binary size by ~33%.
 * 校验截图 Base64 载荷是否符合大小限制。Base64 编码会使原始二进制体积膨胀约 33%。
 */
public final class ScreenshotValidator {

    private static final long MAX_SCREENSHOT_SIZE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_BASE64_LENGTH = 7L * 1024 * 1024;

    private ScreenshotValidator() {
        throw new AssertionError("No instances");
        // Utility class — prevent instantiation
    }

    /**
     * Validates that the Base64-encoded screenshot does not exceed the allowed size.
     * 校验 Base64 编码后的截图未超过允许大小。
     *
     * @param base64Screenshot Base64-encoded image data / Base64 编码的图片数据
     * @throws IllegalArgumentException if the payload exceeds limits / 若载荷超限则抛出
     */
    public static void validate(String base64Screenshot) {
        if (base64Screenshot == null || base64Screenshot.isEmpty()) {
            return;
        }

        long base64Len = base64Screenshot.length();
        if (base64Len > MAX_BASE64_LENGTH) {
            throw new IllegalArgumentException(
                "Screenshot too large after Base64 encoding. Max allowed: 5MB raw (≈7MB Base64)"
            );
        }

        long estimatedOriginal = base64Len * 3 / 4;
        if (estimatedOriginal > MAX_SCREENSHOT_SIZE_BYTES) {
            throw new IllegalArgumentException(
                "Screenshot too large. Max allowed: 5MB"
            );
        }
    }
}
