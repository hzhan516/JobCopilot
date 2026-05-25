package io.jobcopilot.resumeassistant.application.user;

import io.jobcopilot.resumeassistant.api.user.dto.TokenPair;
import io.jobcopilot.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.LoginByGoogleRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.SendVerificationCodeRequest;
import io.jobcopilot.resumeassistant.api.user.dto.response.AuthResult;
import io.jobcopilot.resumeassistant.application.user.dto.AuthTokenResult;
import io.jobcopilot.resumeassistant.api.user.facade.AuthFacade;
import io.jobcopilot.resumeassistant.application.user.command.LoginByEmailCommand;
import io.jobcopilot.resumeassistant.application.user.command.LoginByGoogleCommand;
import io.jobcopilot.resumeassistant.application.user.command.RegisterByEmailCommand;
import io.jobcopilot.resumeassistant.application.user.service.AuthApplicationService;
import io.jobcopilot.resumeassistant.application.user.service.CaptchaService;
import io.jobcopilot.resumeassistant.app.config.CaptchaProperties;
import io.jobcopilot.resumeassistant.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Anti-corruption layer implementation that translates API-layer DTOs into application commands,
 * shielding the domain model from external contract changes.
 * 防腐层实现，将 API 层 DTO 转换为应用层命令，保护领域模型不受外部契约变更影响
 */
@Component
@RequiredArgsConstructor
public class AuthFacadeImpl implements AuthFacade {

    private final AuthApplicationService authService;
    private final CaptchaService captchaService;
    private final CaptchaProperties captchaProperties;

    @Override
    public AuthResult registerByEmail(RegisterByEmailRequest request) {
        if (captchaProperties.isEnabled()) {
            captchaService.validateToken(request.getCaptchaToken());
        }
        RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .verificationCode(request.getVerificationCode())
                .build();

        User user = authService.registerByEmail(command);
        TokenPair tokens = authService.generateTokenPair(user);

        AuthResponse response = AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .accessToken(tokens.getAccessToken())
                .expiresIn(tokens.getExpiresIn())
                .build();
        return new AuthResult(response, tokens.getRefreshToken());
    }

    @Override
    public AuthResult loginByEmail(LoginByEmailRequest request) {
        if (captchaProperties.isEnabled()) {
            captchaService.validateToken(request.getCaptchaToken());
        }
        LoginByEmailCommand command = LoginByEmailCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        User user = authService.loginByEmail(command);
        TokenPair tokens = authService.generateTokenPair(user);

        AuthResponse response = AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .accessToken(tokens.getAccessToken())
                .expiresIn(tokens.getExpiresIn())
                .build();
        return new AuthResult(response, tokens.getRefreshToken());
    }

    @Override
    public AuthResult loginByGoogle(LoginByGoogleRequest request) {
        if (captchaProperties.isEnabled()) {
            captchaService.validateToken(request.captchaToken());
        }
        LoginByGoogleCommand command = LoginByGoogleCommand.builder()
                .idToken(request.idToken())
                .build();

        User user = authService.loginByGoogle(command);
        TokenPair tokens = authService.generateTokenPair(user);

        AuthResponse response = AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .accessToken(tokens.getAccessToken())
                .expiresIn(tokens.getExpiresIn())
                .build();
        return new AuthResult(response, tokens.getRefreshToken());
    }

    @Override
    public AuthResult refreshToken(String refreshToken) {
        AuthTokenResult result = authService.refreshToken(refreshToken);
        AuthResponse response = AuthResponse.builder()
                .userId(result.getUser().getId())
                .email(result.getUser().getEmail())
                .accessToken(result.getTokens().getAccessToken())
                .expiresIn(result.getTokens().getExpiresIn())
                .build();
        return new AuthResult(response, result.getTokens().getRefreshToken());
    }

    @Override
    public void logout(String accessToken) {
        authService.logout(accessToken);
    }

    @Override
    public void sendVerificationCode(SendVerificationCodeRequest request) {
        if (captchaProperties.isEnabled()) {
            // 发送验证码只校验 token 存在性，不消耗，留到注册时再消耗
            // Only peek token validity for sending code; consume it at registration
            captchaService.validateToken(request.getCaptchaToken(), false);
        }
        authService.sendVerificationCode(request.getEmail());
    }

    @Override
    public boolean isEmailVerificationEnabled() {
        return authService.isEmailVerificationEnabled();
    }
}
