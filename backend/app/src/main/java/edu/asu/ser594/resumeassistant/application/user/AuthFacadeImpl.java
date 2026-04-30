package edu.asu.ser594.resumeassistant.application.user;

import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByGoogleRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.AuthResponse;
import edu.asu.ser594.resumeassistant.api.user.facade.AuthFacade;
import edu.asu.ser594.resumeassistant.application.user.command.LoginByEmailCommand;
import edu.asu.ser594.resumeassistant.application.user.command.LoginByGoogleCommand;
import edu.asu.ser594.resumeassistant.application.user.command.RegisterByEmailCommand;
import edu.asu.ser594.resumeassistant.api.user.dto.TokenPair;
import edu.asu.ser594.resumeassistant.application.user.service.AuthApplicationService;
import edu.asu.ser594.resumeassistant.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Authentication facade implementation
@Component
@RequiredArgsConstructor
public class AuthFacadeImpl implements AuthFacade {

    private final AuthApplicationService authService;

    @Override
    public AuthResponse registerByEmail(RegisterByEmailRequest request) {
        RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

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

    @Override
    public AuthResponse loginByEmail(LoginByEmailRequest request) {
        LoginByEmailCommand command = LoginByEmailCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

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

    @Override
    public AuthResponse loginByGoogle(LoginByGoogleRequest request) {
        LoginByGoogleCommand command = LoginByGoogleCommand.builder()
                .idToken(request.idToken())
                .build();

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
}
