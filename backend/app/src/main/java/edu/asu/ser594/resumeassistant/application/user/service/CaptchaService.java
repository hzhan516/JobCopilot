package edu.asu.ser594.resumeassistant.application.user.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.asu.ser594.resumeassistant.app.config.CaptchaProperties;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 滑动验证码服务 / Slider CAPTCHA service
 * <p>
 * 负责验证码挑战的生成、拖动偏移校验和一次性 Token 的签发与核销。
 * 使用两组带前缀隔离的 Caffeine 缓存，防止 key 冲突。
 * Responsible for generating challenges, validating drag offsets, and issuing/consuming one-time tokens.
 * Uses two prefix-isolated Caffeine caches to prevent key collisions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final CaptchaProperties captchaProperties;

    private static final String CHALLENGE_PREFIX = "CAPTCHA_CHALLENGE:";
    private static final String TOKEN_PREFIX = "CAPTCHA_TOKEN:";
    private static final String RATE_LIMIT_PREFIX = "RATE_LIMIT:";

    private final Cache<String, ChallengeEntry> challengeCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private final Cache<String, Boolean> tokenCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private final Cache<String, List<Long>> rateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

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

        ChallengeEntry entry = new ChallengeEntry(targetX);
        challengeCache.put(CHALLENGE_PREFIX + captchaId, entry);

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
        String cacheKey = CHALLENGE_PREFIX + captchaId;
        ChallengeEntry entry = challengeCache.getIfPresent(cacheKey);

        if (entry == null) {
            throw new AuthException(AuthException.ErrorType.CAPTCHA_EXPIRED);
        }

        entry.incrementAttempts();
        if (entry.getAttempts() > captchaProperties.getMaxAttempts()) {
            challengeCache.invalidate(cacheKey);
            throw new AuthException(AuthException.ErrorType.CAPTCHA_EXPIRED);
        }

        int diff = Math.abs(offsetX - entry.getTargetX());
        if (diff > captchaProperties.getTolerance()) {
            throw new AuthException(AuthException.ErrorType.CAPTCHA_INVALID);
        }

        // 验证通过，删除 challenge，生成一次性 token
        // Verification passed, delete challenge, generate one-time token
        challengeCache.invalidate(cacheKey);
        String captchaToken = UUID.randomUUID().toString();
        tokenCache.put(TOKEN_PREFIX + captchaToken, Boolean.TRUE);

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

        String cacheKey = TOKEN_PREFIX + captchaToken;
        Boolean exists = tokenCache.getIfPresent(cacheKey);
        if (exists == null) {
            throw new AuthException(AuthException.ErrorType.CAPTCHA_EXPIRED);
        }

        if (consume) {
            // 一次性使用，验证后立即删除
            // One-time use, delete immediately after validation
            tokenCache.invalidate(cacheKey);
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
     *
     * @param clientIp 客户端 IP / Client IP
     * @return true if exceeded / 是否超限
     */
    public boolean isRateLimited(String clientIp) {
        String cacheKey = RATE_LIMIT_PREFIX + clientIp;
        List<Long> timestamps = rateLimitCache.getIfPresent(cacheKey);
        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - 60_000;

        if (timestamps == null) {
            timestamps = new ArrayList<>();
        }

        // 清理过期时间戳 / Clean up expired timestamps
        timestamps.removeIf(ts -> ts < oneMinuteAgo);
        timestamps.add(now);
        rateLimitCache.put(cacheKey, timestamps);

        return timestamps.size() > 20;
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
