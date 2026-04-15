package edu.asu.ser594.resumeassistant.application.user.service;

import edu.asu.ser594.resumeassistant.application.user.command.LoginByEmailCommand;
import edu.asu.ser594.resumeassistant.application.user.command.RegisterByEmailCommand;
import edu.asu.ser594.resumeassistant.api.user.dto.TokenPair;
import edu.asu.ser594.resumeassistant.api.user.service.TokenService;
import edu.asu.ser594.resumeassistant.domain.user.entity.User;
import edu.asu.ser594.resumeassistant.domain.user.entity.UserCredential;
import edu.asu.ser594.resumeassistant.domain.user.entity.UserProfile;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserCredentialRepository;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserProfileRepository;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserRepository;
import edu.asu.ser594.resumeassistant.domain.user.service.PasswordEncoder;
import edu.asu.ser594.resumeassistant.types.enums.CredentialType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Authentication application service
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthApplicationService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public User registerByEmail(RegisterByEmailCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new AuthException(AuthException.ErrorType.EMAIL_EXISTS);
        }

        User user = User.create(command.email());
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

    public TokenPair generateTokenPair(User user) {
        return tokenService.generateTokenPair(user.getId().toString());
    }
}
