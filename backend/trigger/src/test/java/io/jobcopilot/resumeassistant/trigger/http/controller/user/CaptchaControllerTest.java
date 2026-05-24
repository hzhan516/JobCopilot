package io.jobcopilot.resumeassistant.trigger.http.controller.user;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.user.dto.request.CaptchaVerifyRequest;
import io.jobcopilot.resumeassistant.api.user.dto.response.CaptchaChallengeResponse;
import io.jobcopilot.resumeassistant.api.user.facade.CaptchaFacade;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * CaptchaController 单元测试
 * 验证码控制器单元测试
 * <p>
 * 测试验证码挑战与校验 API 入口：
 * Tests the CAPTCHA challenge and verification API entrypoint:
 * - 正常获取挑战
 * - Normal challenge retrieval
 * - 速率限制返回 429
 * - Rate limit returns 429
 * - 正常校验
 * - Normal verification
 * - IP 解析
 * - IP resolution
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Captcha Controller Tests")
class CaptchaControllerTest {

    @Mock
    private CaptchaFacade captchaFacade;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CaptchaController controller;

    // ==================== 获取挑战 ====================

    @Test
    @DisplayName("Should return challenge when not rate limited")
    void shouldReturnChallengeWhenNotRateLimited() {
        // 给定 / Given
        CaptchaChallengeResponse challenge = new CaptchaChallengeResponse("token", "bg", "slider", "gap");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(captchaFacade.isRateLimited("192.168.1.1")).thenReturn(false);
        when(captchaFacade.generateChallenge()).thenReturn(challenge);

        // 当 / When
        ResponseEntity<ApiResponse<CaptchaChallengeResponse>> response = controller.getCaptcha(request);

        // 那么 / Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(challenge);
    }

    @Test
    @DisplayName("Should return 429 when rate limited")
    void shouldReturn429WhenRateLimited() {
        // 给定 / Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(captchaFacade.isRateLimited("192.168.1.1")).thenReturn(true);

        // 当 / When
        ResponseEntity<ApiResponse<CaptchaChallengeResponse>> response = controller.getCaptcha(request);

        // 那么 / Then
        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getBody().getMessage()).contains("Too many requests");
    }

    @Test
    @DisplayName("Should use X-Forwarded-For header for client IP")
    void shouldUseXForwardedForHeaderForClientIp() {
        // 给定 / Given
        CaptchaChallengeResponse challenge = new CaptchaChallengeResponse("token", "bg", "slider", "gap");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
        when(captchaFacade.isRateLimited("10.0.0.1")).thenReturn(false);
        when(captchaFacade.generateChallenge()).thenReturn(challenge);

        // 当 / When
        ResponseEntity<ApiResponse<CaptchaChallengeResponse>> response = controller.getCaptcha(request);

        // 那么 / Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should fallback to remoteAddr when no X-Forwarded-For")
    void shouldFallbackToRemoteAddrWhenNoXForwardedFor() {
        // 给定 / Given
        CaptchaChallengeResponse challenge = new CaptchaChallengeResponse("token", "bg", "slider", "gap");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(captchaFacade.isRateLimited("192.168.1.1")).thenReturn(false);
        when(captchaFacade.generateChallenge()).thenReturn(challenge);

        // 当 / When
        ResponseEntity<ApiResponse<CaptchaChallengeResponse>> response = controller.getCaptcha(request);

        // 那么 / Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    // ==================== 校验挑战 ====================

    @Test
    @DisplayName("Should return captcha token on successful verification")
    void shouldReturnCaptchaTokenOnSuccessfulVerification() {
        // 给定 / Given
        CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest("challenge-token", 42, 100L);
        when(captchaFacade.verifyChallenge(verifyRequest)).thenReturn("verified-token");

        // 当 / When
        ResponseEntity<ApiResponse<Map<String, String>>> response = controller.verifyCaptcha(verifyRequest);

        // 那么 / Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().get("captchaToken")).isEqualTo("verified-token");
    }
}
