package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.MessageRole;
import io.jobcopilot.resumeassistant.domain.embedding.port.VectorGenerationPort;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Conversation context orchestration tests.
 * 对话上下文编排测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Conversation Context Service Tests")
class ConversationContextServiceTest {

    @Mock
    private ResumeVersionRepository resumeVersionRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private AiMessagePublisherPort aiMessagePublisherPort;

    @Mock
    private VectorGenerationPort vectorGenerationPort;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    @DisplayName("Should queue conversation request inside active transaction / 应在活动事务内立即写入对话 Outbox")
    void queueConversationRequest_WhenTransactionActive_ShouldWriteOutboxImmediately() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        ConversationContextService service = new ConversationContextService(
                resumeVersionRepository,
                jobRepository,
                aiMessagePublisherPort,
                vectorGenerationPort
        );
        UUID userId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "AI Chat", null, null);
        conversation.addMessage(MessageRole.USER, "Hello");

        service.queueConversationRequest(conversation, "Hello", true);

        ArgumentCaptor<ConversationRequestCommand> captor = ArgumentCaptor.forClass(ConversationRequestCommand.class);
        verify(aiMessagePublisherPort).sendConversationRequest(captor.capture());
        assertEquals(conversation.getId().toString(), captor.getValue().conversationId());
        assertEquals("Hello", captor.getValue().currentMessage());
        assertNotNull(captor.getValue().requestId());
        assertTrue(captor.getValue().init());
    }

    @Test
    @DisplayName("Should defer vector generation until after commit / 应将向量生成延迟到事务提交后")
    void deferVectorGeneration_WhenTransactionActive_ShouldWaitForAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        ConversationContextService service = new ConversationContextService(
                resumeVersionRepository,
                jobRepository,
                aiMessagePublisherPort,
                vectorGenerationPort
        );
        UUID versionId = UUID.randomUUID();

        service.deferVectorGeneration(versionId, "# Resume");

        verify(vectorGenerationPort, never()).generateAndSaveVector(versionId.toString(), "RESUME", "# Resume");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCommit());
        verify(vectorGenerationPort).generateAndSaveVector(versionId.toString(), "RESUME", "# Resume");
    }
}
