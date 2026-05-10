package edu.asu.ser594.resumeassistant.application.user.service;

import edu.asu.ser594.resumeassistant.app.config.EmailProperties;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import edu.asu.ser594.resumeassistant.domain.user.port.EmailSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Random;

/**
 * 验证码服务 / Verification code service
 * <p>
 * 负责验证码的生成、存储、校验和销毁。
 * 使用 Redis Hash 分布式存储，支持 TTL 自动过期和最大尝试次数限制，兼容多实例部署。
 * Responsible for generating, storing, validating and invalidating verification codes.
 * Uses Redis Hash with TTL auto-expiration and max-attempt limit, compatible with multi-instance deployment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private final EmailProperties emailProperties;
    private final EmailSenderPort emailSenderPort;
    private static final String CODE_PREFIX = "ra:verify:code:";
    private final StringRedisTemplate redisTemplate;

    private final Random random = new Random();

    /**
     * 发送验证码到指定邮箱 / Send verification code to the specified email
     *
     * @param email 目标邮箱 / Target email
     */
    public void sendCode(String email) {
        String key = CODE_PREFIX + email;
        Map<Object, Object> existing = redisTemplate.opsForHash().entries(key);

        if (!existing.isEmpty()) {
            long sentAt = Long.parseLong((String) existing.getOrDefault("sentAt", "0"));
            long cooldownMillis = emailProperties.getResendCooldownSeconds() * 1000L;
            if (System.currentTimeMillis() - sentAt < cooldownMillis) {
                throw new AuthException(AuthException.ErrorType.VERIFICATION_COOLDOWN);
            }
        }

        String code = generateCode();
        long now = System.currentTimeMillis();
        long expiry = now + emailProperties.getCodeExpirySeconds() * 1000L;

        Map<String, String> fields = Map.of(
                "code", code,
                "expiry", String.valueOf(expiry),
                "sentAt", String.valueOf(now),
                "attempts", "0"
        );

        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, Duration.ofSeconds(emailProperties.getCodeExpirySeconds()));
        emailSenderPort.sendVerificationCode(email, code);

        log.info("Verification code sent to email / 验证码已发送至邮箱: {}", email);
    }

    /**
     * 校验验证码 / Validate verification code
     *
     * @param email 邮箱 / Email
     * @param code  验证码 / Code
     */
    public void validateCode(String email, String code) {
        if (code == null || code.isBlank()) {
            throw new AuthException(AuthException.ErrorType.VERIFICATION_CODE_REQUIRED);
        }

        String key = CODE_PREFIX + email;
        Map<Object, Object> entry = redisTemplate.opsForHash().entries(key);

        if (entry.isEmpty()) {
            throw new AuthException(AuthException.ErrorType.VERIFICATION_CODE_EXPIRED);
        }

        long expiry = Long.parseLong((String) entry.getOrDefault("expiry", "0"));
        if (System.currentTimeMillis() > expiry) {
            redisTemplate.delete(key);
            throw new AuthException(AuthException.ErrorType.VERIFICATION_CODE_EXPIRED);
        }

        String storedCode = (String) entry.get("code");
        if (!storedCode.equals(code)) {
            long attempts = redisTemplate.opsForHash().increment(key, "attempts", 1);
            if (attempts >= emailProperties.getMaxAttempts()) {
                redisTemplate.delete(key);
                throw new AuthException(AuthException.ErrorType.VERIFICATION_CODE_EXPIRED);
            }
            throw new AuthException(AuthException.ErrorType.INVALID_VERIFICATION_CODE);
        }

        // 验证通过，删除记录 / Validation passed, delete record
        redisTemplate.delete(key);
    }

    /**
     * 使指定邮箱的验证码失效 / Invalidate the verification code for the given email
     *
     * @param email 邮箱 / Email
     */
    public void invalidateCode(String email) {
        redisTemplate.delete(CODE_PREFIX + email);
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1000000));
    }
}
