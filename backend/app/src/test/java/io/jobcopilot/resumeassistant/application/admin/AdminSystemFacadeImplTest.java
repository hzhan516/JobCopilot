package io.jobcopilot.resumeassistant.application.admin;

import io.jobcopilot.resumeassistant.api.admin.dto.ComponentHealthResponse;
import io.jobcopilot.resumeassistant.api.admin.dto.SystemStatsResponse;
import io.jobcopilot.resumeassistant.domain.admin.repository.AuditLogRepository;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.tracking.repository.ApplicationTrackingRepository;
import io.jobcopilot.resumeassistant.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/** AdminSystemFacadeImpl 单元测试 / Unit tests for admin system facade */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Admin System Facade Tests")
class AdminSystemFacadeImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ResumeGroupRepository resumeGroupRepository;
    @Mock private JobRepository jobRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private ApplicationTrackingRepository applicationTrackingRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private AdminSystemFacadeImpl facade;

    @BeforeEach
    void setUp() {
        when(userRepository.count()).thenReturn(10L);
        when(resumeGroupRepository.count()).thenReturn(20L);
        when(jobRepository.count()).thenReturn(30L);
        when(conversationRepository.count()).thenReturn(40L);
        when(auditLogRepository.countByActionContaining("ai")).thenReturn(50L);
        when(applicationTrackingRepository.count()).thenReturn(60L);

        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

        RedisConnectionFactory connectionFactory = org.mockito.Mockito.mock(RedisConnectionFactory.class);
        RedisConnection connection = org.mockito.Mockito.mock(RedisConnection.class);
        when(stringRedisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");

        when(rabbitTemplate.execute(any())).thenReturn(null);

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));
    }

    @Test
    @DisplayName("Should return real system stats from repositories")
    void shouldReturnRealSystemStats() {
        SystemStatsResponse stats = facade.getStats();

        assertThat(stats.userCount()).isEqualTo(10L);
        assertThat(stats.resumeCount()).isEqualTo(20L);
        assertThat(stats.jobCount()).isEqualTo(30L);
        assertThat(stats.conversationCount()).isEqualTo(40L);
        assertThat(stats.aiCallCount()).isEqualTo(50L);
        assertThat(stats.applicationCount()).isEqualTo(60L);
    }

    @Test
    @DisplayName("Should report all components healthy when dependencies are up")
    void shouldReportAllComponentsHealthy() {
        ComponentHealthResponse health = facade.getHealth();

        assertThat(health.postgres()).isTrue();
        assertThat(health.redis()).isTrue();
        assertThat(health.rabbitmq()).isTrue();
        assertThat(health.aiService()).isTrue();
        assertThat(health.minio()).isTrue();
    }

    @Test
    @DisplayName("Should report postgres unhealthy when JDBC query fails")
    void shouldReportPostgresUnhealthy() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class)))
                .thenThrow(new RuntimeException("connection refused"));

        ComponentHealthResponse health = facade.getHealth();

        assertThat(health.postgres()).isFalse();
        assertThat(health.redis()).isTrue();
        assertThat(health.rabbitmq()).isTrue();
        assertThat(health.aiService()).isTrue();
    }

    @Test
    @DisplayName("Should report redis unhealthy when ping fails")
    void shouldReportRedisUnhealthy() {
        RedisConnectionFactory connectionFactory = org.mockito.Mockito.mock(RedisConnectionFactory.class);
        RedisConnection connection = org.mockito.Mockito.mock(RedisConnection.class);
        when(stringRedisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("timeout");

        ComponentHealthResponse health = facade.getHealth();

        assertThat(health.redis()).isFalse();
        assertThat(health.postgres()).isTrue();
        assertThat(health.rabbitmq()).isTrue();
        assertThat(health.aiService()).isTrue();
    }

    @Test
    @DisplayName("Should report rabbitmq unhealthy when template execution fails")
    void shouldReportRabbitMqUnhealthy() {
        when(rabbitTemplate.execute(any()))
                .thenThrow(new RuntimeException("broker down"));

        ComponentHealthResponse health = facade.getHealth();

        assertThat(health.rabbitmq()).isFalse();
        assertThat(health.postgres()).isTrue();
        assertThat(health.redis()).isTrue();
        assertThat(health.aiService()).isTrue();
    }

    @Test
    @DisplayName("Should report ai-service unhealthy when health endpoint fails")
    void shouldReportAiServiceUnhealthy() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("service unavailable"));

        ComponentHealthResponse health = facade.getHealth();

        assertThat(health.aiService()).isFalse();
        assertThat(health.postgres()).isTrue();
        assertThat(health.redis()).isTrue();
        assertThat(health.rabbitmq()).isTrue();
    }
}
