package io.jobcopilot.resumeassistant.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Verifies the minimum PostgreSQL schema required by the durable AI Chat flow.
 * This operational guard belongs to the composition root and does not move
 * persistence concerns into the Conversation domain.
 */
@Component("aiChatSchema")
public class AiChatSchemaHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(AiChatSchemaHealthIndicator.class);

    private static final String MISSING_OBJECTS_SQL = """
            WITH required_columns(table_name, column_name) AS (
                VALUES
                    ('dynamic_config', 'config_key'),
                    ('conversations', 'context_tokens'),
                    ('conversations', 'total_tokens_used'),
                    ('conversations', 'context_summary'),
                    ('conversations', 'compacted_through_sequence'),
                    ('conversations', 'compaction_request_id'),
                    ('conversations', 'ai_reply_request_id'),
                    ('conversations', 'ai_reply_status'),
                    ('conversations', 'ai_reply_error_code'),
                    ('conversations', 'ai_reply_started_at'),
                    ('conversations', 'ai_reply_completed_at'),
                    ('conversations', 'ai_reply_user_message_sequence'),
                    ('conversations', 'ai_reply_assistant_message_sequence'),
                    ('outbox_message', 'attempt_count'),
                    ('outbox_message', 'next_attempt_at'),
                    ('outbox_message', 'last_error_code'),
                    ('outbox_message', 'locked_at'),
                    ('outbox_message', 'worker_id')
            ), required_indexes(index_name) AS (
                VALUES
                    ('idx_conversations_ai_reply_pending'),
                    ('idx_outbox_due'),
                    ('idx_outbox_stale_processing')
            )
            SELECT 'column:' || required.table_name || '.' || required.column_name
            FROM required_columns required
            LEFT JOIN information_schema.columns actual
              ON actual.table_schema = 'public'
             AND actual.table_name = required.table_name
             AND actual.column_name = required.column_name
            WHERE actual.column_name IS NULL
            UNION ALL
            SELECT 'index:' || required.index_name
            FROM required_indexes required
            LEFT JOIN pg_indexes actual
              ON actual.schemaname = 'public'
             AND actual.indexname = required.index_name
            WHERE actual.indexname IS NULL
            ORDER BY 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public AiChatSchemaHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            List<String> missingObjects = jdbcTemplate.queryForList(MISSING_OBJECTS_SQL, String.class);
            if (missingObjects.isEmpty()) {
                return Health.up()
                        .withDetail("schema", "ai-chat-v26")
                        .build();
            }
            return Health.down()
                    .withDetail("schema", "ai-chat-v26")
                    .withDetail("reason", "required-schema-objects-missing")
                    .withDetail("missingObjects", missingObjects)
                    .build();
        } catch (DataAccessException exception) {
            log.error("AI Chat schema readiness query failed", exception);
            return Health.down()
                    .withDetail("schema", "ai-chat-v26")
                    .withDetail("reason", "schema-readiness-query-failed")
                    .build();
        }
    }
}
