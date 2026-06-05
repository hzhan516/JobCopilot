package io.jobcopilot.resumeassistant.application.user.service;

import io.jobcopilot.resumeassistant.app.config.CaptchaProperties;
import io.jobcopilot.resumeassistant.domain.user.exception.AuthException;
import io.jobcopilot.resumeassistant.domain.user.port.CaptchaStorePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 滑动验证码服务 / Slider CAPTCHA service
 * <p>
 * 负责验证码挑战的生成、拖动偏移校验和一次性 Token 的签发与核销。
 * 通过 {@link CaptchaStorePort} 与存储解耦，支持多实例共享状态。
 * Responsible for generating challenges, validating drag offsets, and issuing/consuming one-time tokens.
 * Decoupled from storage via {@link CaptchaStorePort} to support state sharing across multiple instances.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final CaptchaProperties captchaProperties;
    private final CaptchaStorePort captchaStorePort;

    /**
     * 生成验证码挑战 / Generate a CAPTCHA challenge
     *
     * @return 挑战 ID 与目标位置 / Challenge ID and target position
     */
    public ChallengeResult generateChallenge() {
        String captchaId = UUID.randomUUID().toString();
        int trackWidth = captchaProperties.getTrackWidth();
        int targetX = 50 + (int) (Math.random() * (trackWidth - 100));

        captchaStorePort.storeChallenge(captchaId, targetX, 0,
                Duration.ofSeconds(captchaProperties.getTokenExpirySeconds()));

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
        CaptchaStorePort.ChallengeEntry entry = captchaStorePort.loadChallenge(captchaId);

        if (entry == null) {
            throw new AuthException(AuthException.ErrorType.CAPTCHA_EXPIRED);
        }

        int attempts = entry.attempts() + 1;
        if (attempts > captchaProperties.getMaxAttempts()) {
            captchaStorePort.deleteChallenge(captchaId);
            throw new AuthException(AuthException.ErrorType.CAPTCHA_EXPIRED);
        }

        int diff = Math.abs(offsetX - entry.targetX());
        if (diff > captchaProperties.getTolerance()) {
            captchaStorePort.storeChallenge(captchaId, entry.targetX(), attempts,
                    Duration.ofSeconds(captchaProperties.getTokenExpirySeconds()));
            throw new AuthException(AuthException.ErrorType.CAPTCHA_INVALID);
        }

        captchaStorePort.deleteChallenge(captchaId);
        String captchaToken = UUID.randomUUID().toString();
        captchaStorePort.storeToken(captchaToken,
                Duration.ofSeconds(captchaProperties.getTokenExpirySeconds()));

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

        boolean valid = consume
                ? captchaStorePort.consumeToken(captchaToken)
                : captchaStorePort.checkToken(captchaToken);

        if (!valid) {
            throw new AuthException(AuthException.ErrorType.CAPTCHA_EXPIRED);
        }

        if (consume) {
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
        return captchaStorePort.isRateLimited(clientIp, 20, Duration.ofMinutes(1));
    }

    /**
     * 验证码挑战结果 / CAPTCHA challenge result
     */
    public record ChallengeResult(String captchaId, int targetX) {
    }
}
