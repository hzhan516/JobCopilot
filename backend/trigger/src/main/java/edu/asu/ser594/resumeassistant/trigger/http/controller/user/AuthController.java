package edu.asu.ser594.resumeassistant.trigger.http.controller.user;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByGoogleRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RefreshTokenRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.SendVerificationCodeRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.AuthResponse;
import edu.asu.ser594.resumeassistant.api.user.facade.AuthFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            @Valid @RequestBody RegisterByEmailRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authFacade.registerByEmail(request)));
    }

    /**
     * 邮箱登录
     * Login by email
     */
    @PostMapping("/login/email")
    public ResponseEntity<ApiResponse<AuthResponse>> loginByEmail(
            @Valid @RequestBody LoginByEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authFacade.loginByEmail(request)));
    }

    /**
     * Google 登录
     * Login by Google
     */
    @PostMapping("/login/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginByGoogle(
            @Valid @RequestBody LoginByGoogleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authFacade.loginByGoogle(request)));
    }

    /**
     * 刷新访问令牌
     * Refresh access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authFacade.refreshToken(request.getRefreshToken())));
    }

    /**
     * 用户注销
     * User logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        authFacade.logout(token);
        return ResponseEntity.ok(ApiResponse.success(null));
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
