package io.jobcopilot.resumeassistant.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatSchemaHealthIndicatorTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AiChatSchemaHealthIndicator indicator = new AiChatSchemaHealthIndicator(jdbcTemplate);

    @Test
    void shouldReportUpWhenAllRequiredObjectsExist() {
        when(jdbcTemplate.queryForList(anyString(), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(List.of());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("schema", "ai-chat-v26");
    }

    @Test
    void shouldReportDownAndListMissingObjects() {
        when(jdbcTemplate.queryForList(anyString(), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(List.of("column:conversations.context_tokens"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("reason", "required-schema-objects-missing")
                .containsEntry("missingObjects", List.of("column:conversations.context_tokens"));
    }

    @Test
    void shouldFailClosedWhenReadinessQueryFails() {
        when(jdbcTemplate.queryForList(anyString(), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("reason", "schema-readiness-query-failed");
    }
}
