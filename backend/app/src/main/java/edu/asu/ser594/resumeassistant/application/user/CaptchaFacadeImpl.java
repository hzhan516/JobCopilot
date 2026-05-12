package edu.asu.ser594.resumeassistant.application.user;

import edu.asu.ser594.resumeassistant.api.user.dto.request.CaptchaVerifyRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.CaptchaChallengeResponse;
import edu.asu.ser594.resumeassistant.api.user.facade.CaptchaFacade;
import edu.asu.ser594.resumeassistant.application.user.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 验证码门面实现 / CAPTCHA facade implementation
 */
@Component
@RequiredArgsConstructor
public class CaptchaFacadeImpl implements CaptchaFacade {

    private final CaptchaService captchaService;

    @Override
    public CaptchaChallengeResponse generateChallenge() {
        CaptchaService.ChallengeResult result = captchaService.generateChallenge();
        return CaptchaChallengeResponse.builder()
                .captchaId(result.captchaId())
                .targetX(result.targetX())
                .build();
    }

    @Override
    public String verifyChallenge(CaptchaVerifyRequest request) {
        return captchaService.verifyChallenge(request.getCaptchaId(), request.getOffsetX());
    }

    @Override
    public boolean isRateLimited(String clientIp) {
        return captchaService.isRateLimited(clientIp);
    }
}
