package io.jobcopilot.resumeassistant.domain.user.port;

import java.time.Duration;
import java.time.Instant;

/**
 * Abstract storage for CAPTCHA challenges and tokens, decoupling the domain from Redis infrastructure.
 * 验证码挑战和 Token 的抽象存储，将领域层与 Redis 基础设施解耦。
 */
public interface CaptchaStorePort {

    /**
     * Store a new challenge with a TTL.
     * 存储一个新的验证码挑战及其 TTL。
     */
    void storeChallenge(String captchaId, int targetX, int attempts, Duration ttl);

    /**
     * Load a challenge by ID. Returns null if expired or not found.
     * 通过 ID 加载验证码挑战。若已过期或不存在则返回 null。
     */
    ChallengeEntry loadChallenge(String captchaId);

    /**
     * Delete a challenge by ID.
     * 删除指定 ID 的验证码挑战。
     */
    void deleteChallenge(String captchaId);

    /**
     * Store a one-time token with a TTL.
     * 存储一个一次性 Token 及其 TTL。
     */
    void storeToken(String token, Duration ttl);

    /**
     * Check if a token exists without consuming it.
     * 检查 Token 是否存在而不消耗它。
     */
    boolean checkToken(String token);

    /**
     * Check if a token exists and consume (delete) it.
     * 检查 Token 是否存在并消耗（删除）它。
     *
     * @return true if the token existed and was consumed / 若 Token 存在且已消耗则返回 true
     */
    boolean consumeToken(String token);

    /**
     * Check if the given client IP exceeds the rate limit within the sliding window.
     * 检查给定客户端 IP 是否在滑动窗口内超出速率限制。
     *
     * @param clientIp     Client IP / 客户端 IP
     * @param maxRequests  Maximum allowed requests / 最大允许请求数
     * @param window       Time window / 时间窗口
     * @return true if rate limited / 若超出限制则返回 true
     */
    boolean isRateLimited(String clientIp, int maxRequests, Duration window);

    /**
     * CAPTCHA challenge entry returned by the store.
     */
    record ChallengeEntry(int targetX, int attempts) {
    }
}
