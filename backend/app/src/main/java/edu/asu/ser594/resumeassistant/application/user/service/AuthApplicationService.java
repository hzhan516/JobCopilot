package edu.asu.ser594.resumeassistant.application.user.service;

import edu.asu.ser594.resumeassistant.application.user.command.LoginByEmailCommand;
import edu.asu.ser594.resumeassistant.application.user.command.LoginByGoogleCommand;
import edu.asu.ser594.resumeassistant.application.user.command.RegisterByEmailCommand;
import edu.asu.ser594.resumeassistant.api.user.dto.TokenPair;
import edu.asu.ser594.resumeassistant.api.user.service.TokenService;
import edu.asu.ser594.resumeassistant.domain.user.entity.User;
import edu.asu.ser594.resumeassistant.domain.user.entity.UserCredential;
import edu.asu.ser594.resumeassistant.domain.user.entity.UserOAuthBinding;
import edu.asu.ser594.resumeassistant.domain.user.entity.UserProfile;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserCredentialRepository;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserOAuthBindingRepository;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserProfileRepository;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserRepository;
import edu.asu.ser594.resumeassistant.domain.user.service.PasswordEncoder;
import edu.asu.ser594.resumeassistant.infrastructure.security.GoogleIdTokenVerifier;
import edu.asu.ser594.resumeassistant.types.enums.CredentialType;
import edu.asu.ser594.resumeassistant.types.enums.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Authentication application service
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
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    @Transactional
    public User registerByEmail(RegisterByEmailCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new AuthException(AuthException.ErrorType.EMAIL_EXISTS);
        }

        User user = User.create(command.email(), OAuthProvider.EMAIL);
        User savedUser = userRepository.save(user);

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

    @Transactional
    public User loginByGoogle(LoginByGoogleCommand command) {
        // 1. Verify Google ID Token
        GoogleIdTokenVerifier.GoogleUserInfo googleUserInfo = googleIdTokenVerifier.verify(command.idToken());

        // 2. Find user by email
        var existingUser = userRepository.findByEmail(googleUserInfo.email());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // If registered with EMAIL, reject Google login
            if (user.getAuthProvider() == OAuthProvider.EMAIL) {
                throw new AuthException(AuthException.ErrorType.EMAIL_REGISTERED_WITH_PASSWORD);
            }

            // If registered with GOOGLE, update OAuth binding and return
            if (user.getAuthProvider() == OAuthProvider.GOOGLE) {
                updateOAuthBindingIfNeeded(user.getId(), googleUserInfo);
                return user;
            }
        }

        // 3. Auto-register new user
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

    private void updateOAuthBindingIfNeeded(UUID userId, GoogleIdTokenVerifier.GoogleUserInfo googleUserInfo) {
        var bindingOpt = userOAuthBindingRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE, googleUserInfo.providerUserId());

        if (bindingOpt.isPresent()) {
            UserOAuthBinding binding = bindingOpt.get();
            boolean shouldUpdate = false;

            if (googleUserInfo.displayName() != null && !googleUserInfo.displayName().equals(binding.getDisplayName())) {
                shouldUpdate = true;
            }
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
}
