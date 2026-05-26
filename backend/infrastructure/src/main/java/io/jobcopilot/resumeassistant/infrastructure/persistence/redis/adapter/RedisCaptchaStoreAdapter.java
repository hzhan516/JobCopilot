package io.jobcopilot.resumeassistant.infrastructure.persistence.redis.adapter;

import io.jobcopilot.resumeassistant.domain.user.port.CaptchaStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed implementation of {@link CaptchaStorePort}.
 * Redis 实现的 {@link CaptchaStorePort}。
 */
@Component
@RequiredArgsConstructor
public class RedisCaptchaStoreAdapter implements CaptchaStorePort {

    private final StringRedisTemplate redisTemplate;
    private final io.jobcopilot.resumeassistant.app.config.CaptchaProperties captchaProperties;

    private String getChallengePrefix() { return captchaProperties.getRedisKeyPrefix() + "challenge:"; }
    private String getTokenPrefix() { return captchaProperties.getRedisKeyPrefix() + "token:"; }
    private String getRateLimitPrefix() { return captchaProperties.getRedisKeyPrefix() + "ratelimit:"; }

    @Override
    public void storeChallenge(String captchaId, int targetX, int attempts, Duration ttl) {
        String key = getChallengePrefix() + captchaId;
        String value = targetX + ":" + attempts;
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public ChallengeEntry loadChallenge(String captchaId) {
        String key = getChallengePrefix() + captchaId;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        String[] parts = value.split(":");
        int targetX = Integer.parseInt(parts[0]);
        int attempts = Integer.parseInt(parts[1]);
        return new ChallengeEntry(targetX, attempts);
    }

    @Override
    public void deleteChallenge(String captchaId) {
        redisTemplate.delete(getChallengePrefix() + captchaId);
    }

    @Override
    public void storeToken(String token, Duration ttl) {
        redisTemplate.opsForValue().set(getTokenPrefix() + token, "1", ttl);
    }

    @Override
    public boolean checkToken(String token) {
        String key = getTokenPrefix() + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public boolean consumeToken(String token) {
        String key = getTokenPrefix() + token;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean isRateLimited(String clientIp, int maxRequests, Duration window) {
        String key = getRateLimitPrefix() + clientIp;
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        redisTemplate.expire(key, window.toMillis(), TimeUnit.MILLISECONDS);

        Long count = redisTemplate.opsForZSet().size(key);
        return count != null && count > maxRequests;
    }
}
