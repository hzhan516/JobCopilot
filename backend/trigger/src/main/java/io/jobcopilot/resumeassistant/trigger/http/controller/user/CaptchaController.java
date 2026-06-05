package io.jobcopilot.resumeassistant.trigger.http.controller.user;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.user.dto.request.CaptchaVerifyRequest;
import io.jobcopilot.resumeassistant.api.user.dto.response.CaptchaChallengeResponse;
import io.jobcopilot.resumeassistant.api.user.facade.CaptchaFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 滑动验证码控制器 / Slider CAPTCHA controller
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaFacade captchaFacade;

    /**
     * 获取验证码挑战 / Get CAPTCHA challenge
     */
    @GetMapping("/captcha")
    public ResponseEntity<ApiResponse<CaptchaChallengeResponse>> getCaptcha(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        if (captchaFacade.isRateLimited(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error(429, "Too many requests, please try again later"));
        }
        return ResponseEntity.ok(ApiResponse.success(captchaFacade.generateChallenge()));
    }

    /**
     * 校验拖动结果 / Verify drag result
     */
    @PostMapping("/captcha/verify")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyCaptcha(
            @Valid @RequestBody CaptchaVerifyRequest request) {
        String captchaToken = captchaFacade.verifyChallenge(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("captchaToken", captchaToken)));
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        // If request came directly from the internet (no proxy), ignore X-Forwarded-For
        // to prevent client IP forgery. Only trust forwarded headers when behind a proxy.
        if (!isPrivateOrLocalIp(remoteAddr)) {
            return remoteAddr;
        }
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            for (String ip : xfHeader.split(",")) {
                String trimmed = ip.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return remoteAddr;
    }

    private boolean isPrivateOrLocalIp(String ip) {
        return ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("172.")
                || ip.startsWith("127.") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")
                || ip.equals("localhost");
    }
}
