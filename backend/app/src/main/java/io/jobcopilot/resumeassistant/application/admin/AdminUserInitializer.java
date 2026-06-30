package io.jobcopilot.resumeassistant.application.admin;

import io.jobcopilot.resumeassistant.domain.user.entity.User;
import io.jobcopilot.resumeassistant.domain.user.entity.UserCredential;
import io.jobcopilot.resumeassistant.domain.user.entity.UserProfile;
import io.jobcopilot.resumeassistant.domain.user.repository.UserCredentialRepository;
import io.jobcopilot.resumeassistant.domain.user.repository.UserProfileRepository;
import io.jobcopilot.resumeassistant.domain.user.repository.UserRepository;
import io.jobcopilot.resumeassistant.domain.user.service.PasswordEncoder;
import io.jobcopilot.resumeassistant.types.enums.CredentialType;
import io.jobcopilot.resumeassistant.types.enums.OAuthProvider;
import io.jobcopilot.resumeassistant.types.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 首次启动自动创建管理员 / Auto-create admin user on first startup */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.admin.email")
public class AdminUserInitializer implements ApplicationRunner {

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_EMAIL is set but ADMIN_PASSWORD is empty — skipping admin seed");
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists: {}", adminEmail);
            return;
        }
        User admin = User.create(adminEmail, OAuthProvider.EMAIL);
        admin.updateRole(UserRole.ADMIN);
        admin.verifyEmail();
        User savedAdmin = userRepository.save(admin);

        UserProfile profile = UserProfile.create(savedAdmin.getId());
        userProfileRepository.save(profile);

        UserCredential credential = UserCredential.createPassword(
                savedAdmin.getId(), passwordEncoder.encode(adminPassword));
        userCredentialRepository.save(credential);

        log.info("Admin user created: {}", adminEmail);
    }
}
