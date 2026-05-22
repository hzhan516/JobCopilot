package io.jobcopilot.resumeassistant.application.user.service;

import io.jobcopilot.resumeassistant.app.config.CaptchaProperties;
import io.jobcopilot.resumeassistant.domain.user.exception.AuthException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 滑动验证码服务 / Slider CAPTCHA service
 * <p>
 * 负责验证码挑战的生成、拖动偏移校验和一次性 Token 的签发与核销。
 * 使用 Redis 作为分布式存储，支持多实例共享状态。
 * Responsible for generating challenges, validating drag offsets, and issuing/consuming one-time tokens.
 * Uses Redis as distributed storage to support state sharing across multiple instances.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final CaptchaProperties captchaProperties;
    private static final String CHALLENGE_PREFIX = "ra:captcha:challenge:";
    private static final String TOKEN_PREFIX = "ra:captcha:token:";
    private static final String RATE_LIMIT_PREFIX = "ra:captcha:ratelimit:";
    private final StringRedisTemplate redisTemplate;

    /**
     * 生成验证码挑战 / Generate a CAPTCHA challenge
     *
     * @return 挑战 ID 与目标位置 / Challenge ID and target position
     */
    public ChallengeResult generateChallenge() {
        String captchaId = UUID.randomUUID().toString();
        int trackWidth = captchaProperties.getTrackWidth();
        // 目标位置在 [50, trackWidth - 50] 范围内随机生成
        // Target position randomly generated within [50, trackWidth - 50]
        int targetX = 50 + (int) (Math.random() * (trackWidth - 100));

        String key = CHALLENGE_PREFIX + captchaId;
        // 格式: targetX:attempts
        // Format: targetX:attempts
        String value = targetX + ":0";
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(captchaProperties.getTokenExpirySeconds()));

        log.info("CAPTCHA challenge generated / 验证码挑战已生成: captchaId={}, targetX={}", captchaId, targetX);
        return new ChallengeResult(captchaId, targetX);
    }

    /**
     * 校验拖动结果并签发一次性 Token / Verify drag result and issue a one-time token
     *
     * @param captchaId 挑战 ID / Challenge ID
     * @param offsetX   用户拖动的 X 偏移量 / User drag X offset
     * @return 一次性 captchaToken / One-time captchaToken
     */
    public String verifyChallenge(String captchaId, int offsetX) {
        String key = CHALLENGE_PREFIX + captchaId;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            throw new AuthException(AuthException.ErrorType.CAPTCHA_EXPIRED);
        }

        String[] parts = value.split(":");
        int targetX = Integer.parseInt(parts[0]);
        int attempts = Integer.parseInt(parts[1]);

        attempts++;
        if (attempts > captchaProperties.getMaxAttempts()) {
            redisTemplate.delete(key);
            throw new AuthException(AuthException.ErrorType.CAPTCHA_EXPIRED);
        }

        int diff = Math.abs(offsetX - targetX);
        if (diff > captchaProperties.getTolerance()) {
            // 更新尝试次数 / Update attempt count
            redisTemplate.opsForValue().set(key, targetX + ":" + attempts,
                    Duration.ofSeconds(captchaProperties.getTokenExpirySeconds()));
            throw new AuthException(AuthException.ErrorType.CAPTCHA_INVALID);
        }

        // 验证通过，删除 challenge，生成一次性 token
        // Verification passed, delete challenge, generate one-time token
        redisTemplate.delete(key);
        String captchaToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + captchaToken,
                "1",
                Duration.ofSeconds(captchaProperties.getTokenExpirySeconds())
        );

        log.info("CAPTCHA verified successfully / 验证码校验通过: captchaId={}", captchaId);
        return captchaToken;
    }

    /**
     * 校验一次性 captchaToken / Validate a one-time captchaToken
     *
     * @param captchaToken 验证码 Token / CAPTCHA token
     * @param consume      是否消耗（删除）/ Whether to consume (delete) the token
     */
    public void validateToken(String captchaToken, boolean consume) {
        if (captchaToken == null || captchaToken.isBlank()) {
            throw new AuthException(AuthException.ErrorType.CAPTCHA_REQUIRED);
        }

        String key = TOKEN_PREFIX + captchaToken;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(exists)) {
            throw new AuthException(AuthException.ErrorType.CAPTCHA_EXPIRED);
        }

        if (consume) {
            // 一次性使用，验证后立即删除
            // One-time use, delete immediately after validation
            redisTemplate.delete(key);
            log.debug("CAPTCHA token consumed / 验证码 Token 已消耗");
        }
    }

    /**
     * 校验并消耗一次性 captchaToken / Validate and consume a one-time captchaToken
     *
     * @param captchaToken 验证码 Token / CAPTCHA token
     */
    public void validateToken(String captchaToken) {
        validateToken(captchaToken, true);
    }

    /**
     * 检查 IP 是否超出速率限制 / Check if IP exceeds rate limit
     * <p>
     * 使用 Redis Sorted Set 实现滑动窗口限流。
     * Uses Redis Sorted Set for sliding-window rate limiting.
     *
     * @param clientIp 客户端 IP / Client IP
     * @return true if exceeded / 是否超限
     */
    public boolean isRateLimited(String clientIp) {
        String key = RATE_LIMIT_PREFIX + clientIp;
        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - 60_000;

        // 清理过期时间戳 / Clean up expired timestamps
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, oneMinuteAgo);
        // 添加当前时间戳 / Add current timestamp
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        // 设置 1 分钟 TTL / Set 1-minute TTL
        redisTemplate.expire(key, Duration.ofMinutes(1));

        Long count = redisTemplate.opsForZSet().size(key);
        return count != null && count > 20;
    }

    /**
     * 验证码挑战结果 / CAPTCHA challenge result
     */
    public record ChallengeResult(String captchaId, int targetX) {
    }

    @Getter
    private static class ChallengeEntry {
        private final int targetX;
        private final Instant createdAt;
        private int attempts;

        ChallengeEntry(int targetX) {
            this.targetX = targetX;
            this.createdAt = Instant.now();
            this.attempts = 0;
        }

        void incrementAttempts() {
            this.attempts++;
        }
    }
}
