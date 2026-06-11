package io.jobcopilot.resumeassistant.domain.job.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * ScreenshotValidator 单元测试
 * Screenshot Validator Unit Tests
 *
 * 测试 Base64 截图大小校验逻辑：
 * Tests Base64 screenshot size validation logic:
 * - 空值处理 / Null handling
 * - 合法大小 / Valid size
 * - 边界值 / Boundary values
 * - 超限拒绝 / Oversized rejection
 */
@DisplayName("Screenshot Validator Tests")
class ScreenshotValidatorTest {

    private static final long MAX_RAW_BYTES = 5L * 1024 * 1024;
    private static final long MAX_BASE64_LEN = 7L * 1024 * 1024;

    // ==================== 空值与边界 ====================

    @Test
    @DisplayName("Should accept null screenshot")
    void shouldAcceptNullScreenshot() {
        assertThatNoException().isThrownBy(() -> ScreenshotValidator.validate(null));
    }

    @Test
    @DisplayName("Should accept empty screenshot")
    void shouldAcceptEmptyScreenshot() {
        assertThatNoException().isThrownBy(() -> ScreenshotValidator.validate(""));
    }

    // ==================== 合法大小 ====================

    @Test
    @DisplayName("Should accept small Base64 screenshot")
    void shouldAcceptSmallBase64Screenshot() {
        String smallBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        assertThatNoException().isThrownBy(() -> ScreenshotValidator.validate(smallBase64));
    }

    @Test
    @DisplayName("Should accept screenshot at exact raw size boundary")
    void shouldAcceptScreenshotAtExactRawSizeBoundary() {
        // Base64 length whose estimated raw size is exactly at 5MB boundary
        StringBuilder sb = new StringBuilder();
        int targetLen = (int) (MAX_RAW_BYTES * 4 / 3);
        while (sb.length() < targetLen) {
            sb.append("A");
        }
        String exactBoundary = sb.toString();

        assertThatNoException().isThrownBy(() -> ScreenshotValidator.validate(exactBoundary));
    }

    // ==================== 超限拒绝 ====================

    @Test
    @DisplayName("Should reject screenshot exceeding Base64 length limit")
    void shouldRejectScreenshotExceedingBase64LengthLimit() {
        StringBuilder sb = new StringBuilder();
        int targetLen = (int) MAX_BASE64_LEN + 1;
        while (sb.length() < targetLen) {
            sb.append("A");
        }
        String oversized = sb.toString();

        assertThatThrownBy(() -> ScreenshotValidator.validate(oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too large");
    }

    @Test
    @DisplayName("Should reject screenshot exceeding raw size limit")
    void shouldRejectScreenshotExceedingRawSizeLimit() {
        // Base64 just under 7MB but raw would exceed 5MB
        // Base64 of ~5.1MB raw ≈ 6.8MB base64 (5.1 * 4/3 ≈ 6.8)
        StringBuilder sb = new StringBuilder();
        int targetLen = (int) (MAX_RAW_BYTES * 4 / 3) + 100;
        while (sb.length() < targetLen) {
            sb.append("A");
        }
        String oversized = sb.toString();

        assertThatThrownBy(() -> ScreenshotValidator.validate(oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too large");
    }

    @Test
    @DisplayName("Should accept screenshot just under raw size limit")
    void shouldAcceptScreenshotJustUnderRawSizeLimit() {
        // Base64 of ~4.9MB raw ≈ 6.53MB base64 (still under 7MB)
        StringBuilder sb = new StringBuilder();
        int targetLen = (int) (MAX_RAW_BYTES * 4 / 3) - 1000;
        while (sb.length() < targetLen) {
            sb.append("A");
        }
        String underLimit = sb.toString();

        assertThatNoException().isThrownBy(() -> ScreenshotValidator.validate(underLimit));
    }

    // ==================== 工具类不可实例化 ====================

    @Test
    @DisplayName("Should not be instantiable")
    void shouldNotBeInstantiable() {
        assertThatThrownBy(() -> {
            java.lang.reflect.Constructor<ScreenshotValidator> ctor = ScreenshotValidator.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            ctor.newInstance();
        }).isInstanceOf(Exception.class);
    }
}
