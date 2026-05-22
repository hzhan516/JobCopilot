package edu.asu.ser594.resumeassistant.application.user.service;

import edu.asu.ser594.resumeassistant.app.config.CaptchaProperties;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 滑动验证码服务测试 / Slider CAPTCHA service tests
 * <p>
 * 验证 Redis 分布式存储路径：挑战生成、Token 核销、滑动窗口限流。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CaptchaService Tests")
class CaptchaServiceTest {

    @Mock
    private CaptchaProperties captchaProperties;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    @InjectMocks
    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(captchaProperties.getTrackWidth()).thenReturn(300);
        when(captchaProperties.getTolerance()).thenReturn(8);
        when(captchaProperties.getMaxAttempts()).thenReturn(5);
        when(captchaProperties.getTokenExpirySeconds()).thenReturn(300);
    }

    @Test
    @DisplayName("Should generate challenge and store in Redis")
    void shouldGenerateChallenge() {
        CaptchaService.ChallengeResult result = captchaService.generateChallenge();

        assertThat(result).isNotNull();
        assertThat(result.captchaId()).isNotBlank();
        assertThat(result.targetX()).isBetween(50, 250);

        verify(valueOps).set(
                startsWith("ra:captcha:challenge:"),
                matches("\\d+:0"),
                eq(Duration.ofSeconds(300))
        );
    }

    @Test
    @DisplayName("Should verify challenge and issue token")
    void shouldVerifyChallengeAndIssueToken() {
        String captchaId = "test-captcha-id";
        int targetX = 100;
        when(valueOps.get("ra:captcha:challenge:" + captchaId)).thenReturn(targetX + ":0");

        String token = captchaService.verifyChallenge(captchaId, targetX);

        assertThat(token).isNotBlank();
        verify(redisTemplate).delete("ra:captcha:challenge:" + captchaId);
        verify(valueOps).set(startsWith("ra:captcha:token:"), eq("1"), eq(Duration.ofSeconds(300)));
    }

    @Test
    @DisplayName("Should throw CAPTCHA_EXPIRED when challenge not found in Redis")
    void shouldThrowWhenChallengeNotFound() {
        when(valueOps.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> captchaService.verifyChallenge("missing-id", 100))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorType()).isEqualTo(AuthException.ErrorType.CAPTCHA_EXPIRED));
    }

    @Test
    @DisplayName("Should validate token and consume it")
    void shouldValidateAndConsumeToken() {
        String token = "test-token";
        when(redisTemplate.hasKey("ra:captcha:token:" + token)).thenReturn(true);

        captchaService.validateToken(token, true);

        verify(redisTemplate).delete("ra:captcha:token:" + token);
    }

    @Test
    @DisplayName("Should throw CAPTCHA_EXPIRED when token not found in Redis")
    void shouldThrowWhenTokenNotFound() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        assertThatThrownBy(() -> captchaService.validateToken("missing-token"))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorType()).isEqualTo(AuthException.ErrorType.CAPTCHA_EXPIRED));
    }

    @Test
    @DisplayName("Should check rate limit using Redis Sorted Set")
    void shouldCheckRateLimit() {
        String clientIp = "192.168.1.1";
        when(zSetOps.size("ra:captcha:ratelimit:" + clientIp)).thenReturn(25L);

        boolean limited = captchaService.isRateLimited(clientIp);

        assertThat(limited).isTrue();
        verify(zSetOps).removeRangeByScore(anyString(), eq(0.0), anyDouble());
        verify(zSetOps).add(anyString(), anyString(), anyDouble());
    }
}
