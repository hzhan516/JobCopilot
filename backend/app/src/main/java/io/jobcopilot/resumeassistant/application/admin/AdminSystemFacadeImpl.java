package io.jobcopilot.resumeassistant.application.admin;

import io.jobcopilot.resumeassistant.api.admin.dto.ComponentHealthResponse;
import io.jobcopilot.resumeassistant.api.admin.dto.SystemStatsResponse;
import io.jobcopilot.resumeassistant.api.admin.facade.AdminSystemFacade;
import io.jobcopilot.resumeassistant.api.version.dto.VersionResponse;
import io.jobcopilot.resumeassistant.domain.admin.repository.AuditLogRepository;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.tracking.repository.ApplicationTrackingRepository;
import io.jobcopilot.resumeassistant.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminSystemFacadeImpl implements AdminSystemFacade {

    private final UserRepository userRepository;
    private final ResumeGroupRepository resumeGroupRepository;
    private final JobRepository jobRepository;
    private final ConversationRepository conversationRepository;
    private final ApplicationTrackingRepository applicationTrackingRepository;
    private final AuditLogRepository auditLogRepository;

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;

    @Value("${ai.service.base-url}")
    private String aiServiceUrl;

    @Value("${app.version:dev}")
    private String version;

    @Override
    public SystemStatsResponse getStats() {
        return SystemStatsResponse.builder()
                .userCount(userRepository.count())
                .resumeCount(resumeGroupRepository.count())
                .jobCount(jobRepository.count())
                .conversationCount(conversationRepository.count())
                .aiCallCount(auditLogRepository.countByActionContaining("ai"))
                .applicationCount(applicationTrackingRepository.count())
                .build();
    }

    @Override
    public ComponentHealthResponse getHealth() {
        return ComponentHealthResponse.builder()
                .postgres(checkPostgres())
                .redis(checkRedis())
                .rabbitmq(checkRabbitMq())
                .aiService(checkAiService())
                .minio(checkMinio())
                .build();
    }

    @Override
    public VersionResponse getVersion() {
        return new VersionResponse(version, "backend");
    }

    private boolean checkPostgres() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.warn("PostgreSQL health check failed", e);
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            return "PONG".equals(stringRedisTemplate.getConnectionFactory()
                    .getConnection().ping());
        } catch (Exception e) {
            log.warn("Redis health check failed", e);
            return false;
        }
    }

    private boolean checkRabbitMq() {
        try {
            rabbitTemplate.execute(channel -> {
                channel.basicQos(0);
                return null;
            });
            return true;
        } catch (Exception e) {
            log.warn("RabbitMQ health check failed", e);
            return false;
        }
    }

    private boolean checkAiService() {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(
                    aiServiceUrl + "/health", String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("AI service health check failed", e);
            return false;
        }
    }

    private boolean checkMinio() {
        // MinIO health is currently delegated to the AI service proxy or considered N/A.
        return true;
    }
}
