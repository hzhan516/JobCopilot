package io.jobcopilot.resumeassistant.application.admin;

import io.jobcopilot.resumeassistant.api.admin.dto.ComponentHealthResponse;
import io.jobcopilot.resumeassistant.api.admin.dto.SystemStatsResponse;
import io.jobcopilot.resumeassistant.api.admin.facade.AdminSystemFacade;
import io.jobcopilot.resumeassistant.api.version.dto.VersionResponse;
import io.jobcopilot.resumeassistant.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSystemFacadeImpl implements AdminSystemFacade {

    private final UserRepository userRepository;

    @Value("${app.version:dev}")
    private String version;

    @Override
    public SystemStatsResponse getStats() {
        return SystemStatsResponse.builder()
                .userCount(userRepository.count())
                .resumeCount(0)  // ponytail: deferred to Phase 3 monitoring
                .jobCount(0)
                .conversationCount(0)
                .aiCallCount(0)
                .applicationCount(0)
                .build();
    }

    @Override
    public ComponentHealthResponse getHealth() {
        // ponytail: always report healthy; real actuator-based check deferred to Phase 3
        return ComponentHealthResponse.builder()
                .postgres(true).redis(true).rabbitmq(true)
                .aiService(true).minio(true)
                .build();
    }

    @Override
    public VersionResponse getVersion() {
        return new VersionResponse(version, "backend");
    }
}
