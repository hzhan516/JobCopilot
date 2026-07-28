package io.jobcopilot.resumeassistant.trigger.listener.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatContractFixtureTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void pythonResultFixturesDeserializeWithJackson() throws Exception {
        JsonNode success = read("conversation-result-success.json");
        JsonNode failure = read("conversation-result-failure.json");

        AiResultEvent successEvent = objectMapper.treeToValue(success, AiResultEvent.class);
        AiResultEvent failureEvent = objectMapper.treeToValue(failure, AiResultEvent.class);

        assertThat(successEvent.status()).isEqualTo("COMPLETED");
        assertThat(failureEvent.status()).isEqualTo("FAILED");
        assertThat(successEvent.requestId()).isEqualTo(failureEvent.requestId());
        assertThat(successEvent.schemaVersion()).isEqualTo(1);
    }

    @Test
    void requestFixtureContainsVersionedCorrelationEnvelope() throws Exception {
        JsonNode request = read("conversation-request.json");
        assertThat(request.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(request.path("requestId").asText()).isNotBlank();
        assertThat(request.path("eventId").asText()).isNotBlank();
        assertThat(request.path("occurredAt").asText()).isNotBlank();
    }

    private JsonNode read(String fileName) throws Exception {
        Path working = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path repositoryRoot = working.getFileName().toString().equals("backend")
                ? working.getParent()
                : working.getParent().getParent();
        Path path = repositoryRoot.resolve(Path.of("contracts", "ai-chat", "v1", fileName));
        assertThat(Files.exists(path)).as("contract fixture %s", path).isTrue();
        return objectMapper.readTree(path.toFile());
    }
}
