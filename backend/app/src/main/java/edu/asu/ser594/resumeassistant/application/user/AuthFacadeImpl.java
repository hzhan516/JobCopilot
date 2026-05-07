package edu.asu.ser594.resumeassistant.application.user;

import edu.asu.ser594.resumeassistant.api.user.dto.TokenPair;
import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByGoogleRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.AuthResponse;
import edu.asu.ser594.resumeassistant.api.user.facade.AuthFacade;
import edu.asu.ser594.resumeassistant.application.user.command.LoginByEmailCommand;
import edu.asu.ser594.resumeassistant.application.user.command.LoginByGoogleCommand;
import edu.asu.ser594.resumeassistant.application.user.command.RegisterByEmailCommand;
import edu.asu.ser594.resumeassistant.application.user.service.AuthApplicationService;
import edu.asu.ser594.resumeassistant.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 身份验证外观实现 / Authentication facade implementation
 */
@Component
@RequiredArgsConstructor
public class AuthFacadeImpl implements AuthFacade {

    private final AuthApplicationService authService;

    /**
     * 通过邮箱注册 / Register by email
     */
    @Override
    public AuthResponse registerByEmail(RegisterByEmailRequest request) {
        // 构建注册命令 / Build register command
        RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        // 执行注册并生成令牌 / Execute registration and generate tokens
        User user = authService.registerByEmail(command);
        TokenPair tokens = authService.generateTokenPair(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresIn(tokens.getExpiresIn())
                .build();
    }

    /**
     * 通过邮箱登录 / Login by email
     */
    @Override
    public AuthResponse loginByEmail(LoginByEmailRequest request) {
        // 构建登录命令 / Build login command
        LoginByEmailCommand command = LoginByEmailCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        // 执行登录并生成令牌 / Execute login and generate tokens
        User user = authService.loginByEmail(command);
        TokenPair tokens = authService.generateTokenPair(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresIn(tokens.getExpiresIn())
                .build();
    }

    /**
     * 通过 Google 登录 / Login by Google
     */
    @Override
    public AuthResponse loginByGoogle(LoginByGoogleRequest request) {
        // 构建 Google 登录命令 / Build Google login command
        LoginByGoogleCommand command = LoginByGoogleCommand.builder()
                .idToken(request.idToken())
                .build();

        // 执行登录并生成令牌 / Execute login and generate tokens
        User user = authService.loginByGoogle(command);
        TokenPair tokens = authService.generateTokenPair(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresIn(tokens.getExpiresIn())
                .build();
    }

    /**
     * 刷新访问令牌 / Refresh access token
     */
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        return authService.refreshToken(refreshToken);
    }

    /**
     * 用户注销 / User logout
     */
    @Override
    public void logout(String accessToken) {
        authService.logout(accessToken);
    }
}
