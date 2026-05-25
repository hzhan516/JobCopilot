package io.jobcopilot.resumeassistant.application.user.service;

import io.jobcopilot.resumeassistant.api.user.dto.TokenPair;
import io.jobcopilot.resumeassistant.api.user.dto.response.AuthResponse;
import io.jobcopilot.resumeassistant.api.user.service.TokenService;
import io.jobcopilot.resumeassistant.application.user.command.LoginByEmailCommand;
import io.jobcopilot.resumeassistant.application.user.command.LoginByGoogleCommand;
import io.jobcopilot.resumeassistant.application.user.command.RegisterByEmailCommand;
import io.jobcopilot.resumeassistant.domain.user.entity.User;
import io.jobcopilot.resumeassistant.domain.user.entity.UserCredential;
import io.jobcopilot.resumeassistant.domain.user.entity.UserOAuthBinding;
import io.jobcopilot.resumeassistant.domain.user.entity.UserProfile;
import io.jobcopilot.resumeassistant.domain.user.exception.AuthException;
import io.jobcopilot.resumeassistant.domain.user.port.GoogleTokenVerifierPort;
import io.jobcopilot.resumeassistant.domain.user.repository.UserCredentialRepository;
import io.jobcopilot.resumeassistant.domain.user.repository.UserOAuthBindingRepository;
import io.jobcopilot.resumeassistant.domain.user.repository.UserProfileRepository;
import io.jobcopilot.resumeassistant.domain.user.repository.UserRepository;
import io.jobcopilot.resumeassistant.domain.user.service.PasswordEncoder;
import io.jobcopilot.resumeassistant.app.config.EmailProperties;
import io.jobcopilot.resumeassistant.types.enums.CredentialType;
import io.jobcopilot.resumeassistant.types.enums.OAuthProvider;
import io.jobcopilot.resumeassistant.types.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthApplicationService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserOAuthBindingRepository userOAuthBindingRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final GoogleTokenVerifierPort googleTokenVerifierPort;
    private final VerificationCodeService verificationCodeService;
    private final EmailProperties emailProperties;

    @Transactional(timeout = 30)
    public User registerByEmail(RegisterByEmailCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new AuthException(AuthException.ErrorType.EMAIL_EXISTS);
        }

        if (emailProperties.isEnabled()) {
            verificationCodeService.validateCode(command.email(), command.verificationCode());
        }

        User user = User.create(command.email(), OAuthProvider.EMAIL);
        if (emailProperties.isEnabled()) {
            user.verifyEmail();
        }
        User savedUser = userRepository.save(user);

        if (emailProperties.isEnabled()) {
            verificationCodeService.invalidateCode(command.email());
        }

        UserProfile profile = UserProfile.create(savedUser.getId());
        userProfileRepository.save(profile);

        UserCredential credential = UserCredential.createPassword(
                savedUser.getId(),
                passwordEncoder.encode(command.password())
        );
        userCredentialRepository.save(credential);

        return savedUser;
    }

    public User loginByEmail(LoginByEmailCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new AuthException(AuthException.ErrorType.INVALID_CREDENTIALS));

        UserCredential credential = userCredentialRepository
                .findByUserIdAndType(user.getId(), CredentialType.PASSWORD)
                .orElseThrow(() -> new AuthException(AuthException.ErrorType.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(command.password(), credential.getCredentialValue())) {
            throw new AuthException(AuthException.ErrorType.INVALID_CREDENTIALS);
        }

        return user;
    }

    @Transactional(timeout = 30)
    public User loginByGoogle(LoginByGoogleCommand command) {
        GoogleTokenVerifierPort.GoogleUserInfo googleUserInfo = googleTokenVerifierPort.verify(command.idToken());

        var existingUser = userRepository.findByEmail(googleUserInfo.email());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // Reject Google login for accounts created via email to prevent credential confusion
            // 拒绝通过邮箱注册的账号使用 Google 登录，防止凭据混淆
            if (user.getAuthProvider() == OAuthProvider.EMAIL) {
                throw new AuthException(AuthException.ErrorType.EMAIL_REGISTERED_WITH_PASSWORD);
            }

            if (user.getAuthProvider() == OAuthProvider.GOOGLE) {
                updateOAuthBindingIfNeeded(user.getId(), googleUserInfo);
                return user;
            }
        }

        User user = User.create(googleUserInfo.email(), OAuthProvider.GOOGLE);
        User savedUser = userRepository.save(user);

        UserProfile profile = UserProfile.create(savedUser.getId());
        if (googleUserInfo.displayName() != null) {
            profile.updateProfile(googleUserInfo.displayName(), null, null, null);
        }
        if (googleUserInfo.avatarUrl() != null) {
            profile.updateAvatar(googleUserInfo.avatarUrl());
        }
        userProfileRepository.save(profile);

        UserOAuthBinding binding = UserOAuthBinding.create(
                savedUser.getId(),
                OAuthProvider.GOOGLE,
                googleUserInfo.providerUserId(),
                googleUserInfo.email(),
                googleUserInfo.displayName(),
                googleUserInfo.avatarUrl()
        );
        userOAuthBindingRepository.save(binding);

        return savedUser;
    }

    private void updateOAuthBindingIfNeeded(UUID userId, GoogleTokenVerifierPort.GoogleUserInfo googleUserInfo) {
        var bindingOpt = userOAuthBindingRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE, googleUserInfo.providerUserId());

        if (bindingOpt.isPresent()) {
            UserOAuthBinding binding = bindingOpt.get();
            boolean shouldUpdate = googleUserInfo.displayName() != null && !googleUserInfo.displayName().equals(binding.getDisplayName());

            if (googleUserInfo.avatarUrl() != null && !googleUserInfo.avatarUrl().equals(binding.getAvatarUrl())) {
                shouldUpdate = true;
            }

            if (shouldUpdate) {
                binding.updateDisplayInfo(googleUserInfo.displayName(), googleUserInfo.avatarUrl());
                userOAuthBindingRepository.save(binding);
            }
        }
    }

    public TokenPair generateTokenPair(User user) {
        return tokenService.generateTokenPair(user.getId().toString());
    }

    /**
     * Refreshes the access token using a valid refresh token, enabling seamless session continuation
     * without forcing the user to re-authenticate.
     * 使用有效的刷新令牌重新签发访问令牌，实现无感会话续期
     *
     * @param refreshToken Refresh token / 刷新令牌
     * @return New authentication response / 新认证响应
     */
    public AuthResponse refreshToken(String refreshToken) {
        var validationResult = tokenService.validateTokenDetailed(refreshToken);

        switch (validationResult) {
            case EXPIRED -> throw new AuthException(AuthException.ErrorType.TOKEN_EXPIRED);
            case INVALID -> throw new AuthException(AuthException.ErrorType.TOKEN_INVALID);
        }

        String userId = tokenService.getUserIdFromToken(refreshToken);

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AuthException(AuthException.ErrorType.INVALID_CREDENTIALS));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new AuthException(AuthException.ErrorType.INVALID_CREDENTIALS);
        }

        TokenPair tokens = tokenService.generateTokenPair(userId);

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expiresIn(tokens.getExpiresIn())
                .build();
    }

    /**
     * 发送邮箱验证码 / Send email verification code
     *
     * @param email 目标邮箱 / Target email
     */
    public void sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(AuthException.ErrorType.EMAIL_ALREADY_REGISTERED);
        }
        verificationCodeService.sendCode(email);
    }

    /**
     * 查询邮箱验证功能是否开启 / Check if email verification is enabled
     *
     * @return true if enabled / 是否开启
     */
    public boolean isEmailVerificationEnabled() {
        return emailProperties.isEnabled();
    }

    /**
     * Logs out the user. In the current MVP stage, JWT tokens are stateless, so logout is handled
     * by the frontend clearing cached tokens. A persistent token blacklist (e.g., Redis) could be
     * introduced here in future iterations.
     * 用户注销。当前 MVP 阶段 JWT 为无状态令牌，注销由前端清理缓存完成；后续可在此引入 Redis 黑名单等持久化失效机制
     *
     * @param accessToken Current access token / 当前访问令牌
     */
    public void logout(String accessToken) {
        log.info("User logout / 用户注销: token={}", accessToken);
    }
}
