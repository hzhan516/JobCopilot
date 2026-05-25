package io.jobcopilot.resumeassistant.trigger.http.controller.user;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.LoginByGoogleRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.RefreshTokenRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.SendVerificationCodeRequest;
import io.jobcopilot.resumeassistant.api.user.dto.response.AuthResponse;
import io.jobcopilot.resumeassistant.api.user.dto.response.AuthResult;
import io.jobcopilot.resumeassistant.api.user.facade.AuthFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacade authFacade;

    /**
     * 邮箱注册
     * Register by email
     */
    @PostMapping("/register/email")
    public ResponseEntity<ApiResponse<AuthResponse>> registerByEmail(
            @Valid @RequestBody RegisterByEmailRequest request,
            HttpServletRequest httpRequest) {
        AuthResult result = authFacade.registerByEmail(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(result.getRefreshToken(), httpRequest.isSecure()))
                .body(ApiResponse.success(result.getAuthResponse()));
    }

    /**
     * 邮箱登录
     * Login by email
     */
    @PostMapping("/login/email")
    public ResponseEntity<ApiResponse<AuthResponse>> loginByEmail(
            @Valid @RequestBody LoginByEmailRequest request,
            HttpServletRequest httpRequest) {
        AuthResult result = authFacade.loginByEmail(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(result.getRefreshToken(), httpRequest.isSecure()))
                .body(ApiResponse.success(result.getAuthResponse()));
    }

    /**
     * Google 登录
     * Login by Google
     */
    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginByGoogle(
            @Valid @RequestBody LoginByGoogleRequest request,
            HttpServletRequest httpRequest) {
        AuthResult result = authFacade.loginByGoogle(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(result.getRefreshToken(), httpRequest.isSecure()))
                .body(ApiResponse.success(result.getAuthResponse()));
    }

    /**
     * 刷新访问令牌
     * Refresh access token. Prefers HttpOnly cookie; falls back to body during transition.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            @Valid @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        String refreshToken = refreshTokenCookie != null ? refreshTokenCookie
                : (request != null ? request.getRefreshToken() : null);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Refresh token required"));
        }
        AuthResult result = authFacade.refreshToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(result.getRefreshToken(), httpRequest.isSecure()))
                .body(ApiResponse.success(result.getAuthResponse()));
    }

    /**
     * 用户注销
     * User logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpRequest) {
        String token = extractBearerToken(authorization);
        authFacade.logout(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie(httpRequest.isSecure()))
                .body(ApiResponse.success(null));
    }

    private String buildRefreshTokenCookie(String refreshToken, boolean secure) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .secure(secure)
                .build()
                .toString();
    }

    private String clearRefreshTokenCookie(boolean secure) {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .secure(secure)
                .build()
                .toString();
    }

    /**
     * 发送邮箱验证码
     * Send email verification code
     */
    @PostMapping("/send-verification-code")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Valid @RequestBody SendVerificationCodeRequest request) {
        authFacade.sendVerificationCode(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 查询邮箱验证功能是否开启
     * Check if email verification is enabled
     */
    @GetMapping("/email-verification-enabled")
    public ResponseEntity<ApiResponse<Boolean>> isEmailVerificationEnabled() {
        return ResponseEntity.ok(ApiResponse.success(authFacade.isEmailVerificationEnabled()));
    }

    /**
     * 从 Authorization Header 提取 Bearer Token
     * Extract Bearer token from Authorization header
     */
    private String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
