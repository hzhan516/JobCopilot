package edu.asu.ser594.resumeassistant.application.user.service;

import edu.asu.ser594.resumeassistant.app.config.EmailProperties;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import edu.asu.ser594.resumeassistant.domain.user.port.EmailSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 邮箱验证码服务测试 / Email verification code service tests
 * <p>
 * 验证 Redis Hash 分布式存储路径：发送、冷却期、校验、尝试次数、过期。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VerificationCodeService Tests")
class VerificationCodeServiceTest {

    @Mock
    private EmailProperties emailProperties;

    @Mock
    private EmailSenderPort emailSenderPort;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    @InjectMocks
    private VerificationCodeService verificationCodeService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(emailProperties.getCodeExpirySeconds()).thenReturn(300);
        when(emailProperties.getResendCooldownSeconds()).thenReturn(60);
        when(emailProperties.getMaxAttempts()).thenReturn(3);
    }

    @Test
    @DisplayName("Should send code and store in Redis Hash")
    void shouldSendCode() {
        String email = "test@example.com";
        when(hashOps.entries(anyString())).thenReturn(Map.of());

        verificationCodeService.sendCode(email);

        verify(hashOps).putAll(
                eq("ra:verify:code:" + email),
                argThat((Map<String, String> map) ->
                        map.containsKey("code") &&
                                map.containsKey("expiry") &&
                                map.containsKey("sentAt") &&
                                map.containsKey("attempts") &&
                                map.get("attempts").equals("0")
                )
        );
        verify(redisTemplate).expire(eq("ra:verify:code:" + email), eq(Duration.ofSeconds(300)));
        verify(emailSenderPort).sendVerificationCode(eq(email), anyString());
    }

    @Test
    @DisplayName("Should throw cooldown exception when resending too quickly")
    void shouldThrowCooldownException() {
        String email = "test@example.com";
        long now = System.currentTimeMillis();
        when(hashOps.entries("ra:verify:code:" + email))
                .thenReturn(Map.of("sentAt", String.valueOf(now)));

        assertThatThrownBy(() -> verificationCodeService.sendCode(email))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorType()).isEqualTo(AuthException.ErrorType.VERIFICATION_COOLDOWN));
    }

    @Test
    @DisplayName("Should validate correct code and delete it")
    void shouldValidateCorrectCode() {
        String email = "test@example.com";
        String code = "123456";
        when(hashOps.entries("ra:verify:code:" + email))
                .thenReturn(Map.of(
                        "code", code,
                        "expiry", String.valueOf(System.currentTimeMillis() + 60000),
                        "attempts", "0"
                ));

        verificationCodeService.validateCode(email, code);

        verify(redisTemplate).delete("ra:verify:code:" + email);
    }

    @Test
    @DisplayName("Should throw expired exception when code not found")
    void shouldThrowExpiredWhenNotFound() {
        when(hashOps.entries(anyString())).thenReturn(Map.of());

        assertThatThrownBy(() -> verificationCodeService.validateCode("test@example.com", "123456"))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorType()).isEqualTo(AuthException.ErrorType.VERIFICATION_CODE_EXPIRED));
    }

    @Test
    @DisplayName("Should increment attempts on wrong code")
    void shouldIncrementAttemptsOnWrongCode() {
        String email = "test@example.com";
        when(hashOps.entries("ra:verify:code:" + email))
                .thenReturn(Map.of(
                        "code", "123456",
                        "expiry", String.valueOf(System.currentTimeMillis() + 60000),
                        "attempts", "0"
                ));
        when(hashOps.increment("ra:verify:code:" + email, "attempts", 1)).thenReturn(1L);

        assertThatThrownBy(() -> verificationCodeService.validateCode(email, "000000"))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorType()).isEqualTo(AuthException.ErrorType.INVALID_VERIFICATION_CODE));

        verify(hashOps).increment("ra:verify:code:" + email, "attempts", 1);
    }

    @Test
    @DisplayName("Should invalidate code")
    void shouldInvalidateCode() {
        String email = "test@example.com";
        verificationCodeService.invalidateCode(email);
        verify(redisTemplate).delete("ra:verify:code:" + email);
    }
}
