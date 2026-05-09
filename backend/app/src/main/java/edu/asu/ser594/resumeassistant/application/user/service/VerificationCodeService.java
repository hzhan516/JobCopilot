package edu.asu.ser594.resumeassistant.application.user.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import edu.asu.ser594.resumeassistant.domain.user.port.EmailSenderPort;
import edu.asu.ser594.resumeassistant.app.config.EmailProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务 / Verification code service
 * <p>
 * 负责验证码的生成、存储、校验和销毁。
 * 使用 Caffeine 本地缓存，支持 TTL 自动过期和最大尝试次数限制。
 * Responsible for generating, storing, validating and invalidating verification codes.
 * Uses Caffeine local cache with TTL auto-expiration and max-attempt limit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private final EmailProperties emailProperties;
    private final EmailSenderPort emailSenderPort;

    private final Cache<String, CodeEntry> codeCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private final Random random = new Random();

    /**
     * 发送验证码到指定邮箱 / Send verification code to the specified email
     *
     * @param email 目标邮箱 / Target email
     */
    public void sendCode(String email) {
        CodeEntry existing = codeCache.getIfPresent(email);
        if (existing != null && existing.isInCooldown(emailProperties.getResendCooldownSeconds())) {
            throw new AuthException(AuthException.ErrorType.VERIFICATION_COOLDOWN);
        }

        String code = generateCode();
        CodeEntry entry = new CodeEntry(
                code,
                Instant.now().plusSeconds(emailProperties.getCodeExpirySeconds())
        );
        codeCache.put(email, entry);
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

        CodeEntry entry = codeCache.getIfPresent(email);
        if (entry == null || entry.isExpired()) {
            throw new AuthException(AuthException.ErrorType.VERIFICATION_CODE_EXPIRED);
        }

        if (!entry.getCode().equals(code)) {
            entry.incrementAttempts();
            if (entry.getAttempts() >= emailProperties.getMaxAttempts()) {
                codeCache.invalidate(email);
                throw new AuthException(AuthException.ErrorType.VERIFICATION_CODE_EXPIRED);
            }
            throw new AuthException(AuthException.ErrorType.INVALID_VERIFICATION_CODE);
        }
    }

    /**
     * 使指定邮箱的验证码失效 / Invalidate the verification code for the given email
     *
     * @param email 邮箱 / Email
     */
    public void invalidateCode(String email) {
        codeCache.invalidate(email);
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

    @Getter
    private static class CodeEntry {
        private final String code;
        private final Instant expiryTime;
        private final Instant sentAt;
        private int attempts;

        CodeEntry(String code, Instant expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.sentAt = Instant.now();
            this.attempts = 0;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }

        boolean isInCooldown(int cooldownSeconds) {
            return Instant.now().isBefore(sentAt.plusSeconds(cooldownSeconds));
        }

        void incrementAttempts() {
            this.attempts++;
        }
    }
}
