package io.jobcopilot.resumeassistant.application.user.service;

import io.jobcopilot.resumeassistant.app.config.CaptchaProperties;
import io.jobcopilot.resumeassistant.domain.user.exception.AuthException;
import io.jobcopilot.resumeassistant.domain.user.port.CaptchaStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 滑动验证码服务测试 / Slider CAPTCHA service tests
 * <p>
 * 通过 {@link CaptchaStorePort} 抽象存储接口验证挑战生成、Token 签发与核销、
 * 滑动窗口限流逻辑，与具体 Redis 实现解耦。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CaptchaService Tests")
class CaptchaServiceTest {

    @Mock
    private CaptchaProperties captchaProperties;

    @Mock
    private CaptchaStorePort captchaStorePort;

    @InjectMocks
    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        when(captchaProperties.getTrackWidth()).thenReturn(300);
        when(captchaProperties.getTolerance()).thenReturn(8);
        when(captchaProperties.getMaxAttempts()).thenReturn(5);
        when(captchaProperties.getTokenExpirySeconds()).thenReturn(300);
    }

    @Test
    @DisplayName("Should generate challenge and store via CaptchaStorePort")
    void shouldGenerateChallenge() {
        CaptchaService.ChallengeResult result = captchaService.generateChallenge();

        assertThat(result).isNotNull();
        assertThat(result.captchaId()).isNotBlank();
        assertThat(result.targetX()).isBetween(50, 250);

        ArgumentCaptor<String> captchaIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> targetXCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> attemptsCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(captchaStorePort).storeChallenge(
                captchaIdCaptor.capture(),
                targetXCaptor.capture(),
                attemptsCaptor.capture(),
                ttlCaptor.capture()
        );

        assertThat(captchaIdCaptor.getValue()).isEqualTo(result.captchaId());
        assertThat(targetXCaptor.getValue()).isEqualTo(result.targetX());
        assertThat(attemptsCaptor.getValue()).isZero();
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(300));
    }

    @Test
    @DisplayName("Should verify challenge and issue token")
    void shouldVerifyChallengeAndIssueToken() {
        String captchaId = "test-captcha-id";
        int targetX = 100;
        when(captchaStorePort.loadChallenge(captchaId))
                .thenReturn(new CaptchaStorePort.ChallengeEntry(targetX, 0));

        String token = captchaService.verifyChallenge(captchaId, targetX);

        assertThat(token).isNotBlank();
        verify(captchaStorePort).deleteChallenge(captchaId);
        verify(captchaStorePort).storeToken(anyString(), eq(Duration.ofSeconds(300)));
    }

    @Test
    @DisplayName("Should throw CAPTCHA_EXPIRED when challenge not found")
    void shouldThrowWhenChallengeNotFound() {
        when(captchaStorePort.loadChallenge(anyString())).thenReturn(null);

        assertThatThrownBy(() -> captchaService.verifyChallenge("missing-id", 100))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorType())
                        .isEqualTo(AuthException.ErrorType.CAPTCHA_EXPIRED));
    }

    @Test
    @DisplayName("Should throw CAPTCHA_EXPIRED when max attempts exceeded")
    void shouldThrowWhenMaxAttemptsExceeded() {
        String captchaId = "expired-challenge";
        when(captchaStorePort.loadChallenge(captchaId))
                .thenReturn(new CaptchaStorePort.ChallengeEntry(100, 6));

        assertThatThrownBy(() -> captchaService.verifyChallenge(captchaId, 100))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorType())
                        .isEqualTo(AuthException.ErrorType.CAPTCHA_EXPIRED));

        verify(captchaStorePort).deleteChallenge(captchaId);
    }

    @Test
    @DisplayName("Should throw CAPTCHA_INVALID when offset is out of tolerance")
    void shouldThrowWhenOffsetOutOfTolerance() {
        String captchaId = "bad-offset";
        int targetX = 100;
        when(captchaStorePort.loadChallenge(captchaId))
                .thenReturn(new CaptchaStorePort.ChallengeEntry(targetX, 1));

        assertThatThrownBy(() -> captchaService.verifyChallenge(captchaId, 120))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorType())
                        .isEqualTo(AuthException.ErrorType.CAPTCHA_INVALID));

        verify(captchaStorePort).storeChallenge(eq(captchaId), eq(targetX), eq(2), any(Duration.class));
    }

    @Test
    @DisplayName("Should validate and consume token")
    void shouldValidateAndConsumeToken() {
        String token = "test-token";
        when(captchaStorePort.consumeToken(token)).thenReturn(true);

        captchaService.validateToken(token, true);

        verify(captchaStorePort).consumeToken(token);
    }

    @Test
    @DisplayName("Should validate token without consuming")
    void shouldValidateTokenWithoutConsuming() {
        String token = "test-token";
        when(captchaStorePort.checkToken(token)).thenReturn(true);

        captchaService.validateToken(token, false);

        verify(captchaStorePort).checkToken(token);
        verify(captchaStorePort, never()).consumeToken(anyString());
    }

    @Test
    @DisplayName("Should throw CAPTCHA_EXPIRED when token not found")
    void shouldThrowWhenTokenNotFound() {
        when(captchaStorePort.consumeToken(anyString())).thenReturn(false);

        assertThatThrownBy(() -> captchaService.validateToken("missing-token"))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorType())
                        .isEqualTo(AuthException.ErrorType.CAPTCHA_EXPIRED));
    }

    @Test
    @DisplayName("Should check rate limit via CaptchaStorePort")
    void shouldCheckRateLimit() {
        String clientIp = "192.168.1.1";
        when(captchaStorePort.isRateLimited(clientIp, 20, Duration.ofMinutes(1)))
                .thenReturn(true);

        boolean limited = captchaService.isRateLimited(clientIp);

        assertThat(limited).isTrue();
        verify(captchaStorePort).isRateLimited(clientIp, 20, Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("Should throw CAPTCHA_REQUIRED when token is blank")
    void shouldThrowWhenTokenIsBlank() {
        assertThatThrownBy(() -> captchaService.validateToken("", true))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorType())
                        .isEqualTo(AuthException.ErrorType.CAPTCHA_REQUIRED));

        assertThatThrownBy(() -> captchaService.validateToken(null, true))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorType())
                        .isEqualTo(AuthException.ErrorType.CAPTCHA_REQUIRED));
    }
}
