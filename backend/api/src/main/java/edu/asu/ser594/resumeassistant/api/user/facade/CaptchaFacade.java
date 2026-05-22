package edu.asu.ser594.resumeassistant.api.user.facade;

import edu.asu.ser594.resumeassistant.api.user.dto.request.CaptchaVerifyRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.CaptchaChallengeResponse;

/**
 * 验证码门面 / CAPTCHA facade
 * <p>
 * 抽象验证码挑战生成与校验逻辑，隔离触发层与具体实现。
 * Abstracts CAPTCHA challenge generation and verification logic.
 */
public interface CaptchaFacade {

    /**
     * 生成新的验证码挑战 / Generate a new CAPTCHA challenge
     *
     * @return 挑战数据 / Challenge data
     */
    CaptchaChallengeResponse generateChallenge();

    /**
     * 校验拖动结果并签发一次性 Token / Verify drag result and issue a one-time token
     *
     * @param request 校验请求 / Verification request
     * @return captchaToken / One-time captcha token
     */
    String verifyChallenge(CaptchaVerifyRequest request);

    /**
     * 检查 IP 是否超出速率限制 / Check if IP exceeds rate limit
     *
     * @param clientIp 客户端 IP / Client IP
     * @return true if exceeded / 是否超限
     */
    boolean isRateLimited(String clientIp);
}
