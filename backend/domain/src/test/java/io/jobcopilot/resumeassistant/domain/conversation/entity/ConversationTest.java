package io.jobcopilot.resumeassistant.domain.conversation.entity;

import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
import io.jobcopilot.resumeassistant.domain.conversation.exception.AiReplyInProgressException;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.AiReplyStatus;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.ConversationStatus;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.MessageRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Conversation Entity Unit Tests
 * 对话领域实体单元测试
 */
@DisplayName("Conversation Entity Tests")
class ConversationTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID TEST_RESUME_VERSION_ID = UUID.randomUUID();

    @Test
    @DisplayName("Should create conversation with factory method and default title")
    void shouldCreateConversationWithFactoryMethodAndDefaultTitle() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, TEST_RESUME_VERSION_ID, null);

        assertThat(conversation).isNotNull();
        assertThat(conversation.getId()).isNotNull();
        assertThat(conversation.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(conversation.getTitle()).isEqualTo("New Conversation");
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversation.getResumeVersionId()).isEqualTo(TEST_RESUME_VERSION_ID);
        assertThat(conversation.getMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should create conversation with custom title")
    void shouldCreateConversationWithCustomTitle() {
        Conversation conversation = Conversation.create(TEST_USER_ID, "Custom Title", null, null);

        assertThat(conversation.getTitle()).isEqualTo("Custom Title");
        assertThat(conversation.getResumeVersionId()).isNull();
    }

    @Test
    @DisplayName("Should add message to active conversation")
    void shouldAddMessageToActiveConversation() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, null, null);
        conversation.addMessage(MessageRole.USER, "Hello");

        assertThat(conversation.getMessages()).hasSize(1);
        assertThat(conversation.getMessages().get(0).getContent()).isEqualTo("Hello");
        assertThat(conversation.getMessages().get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(conversation.getMessages().get(0).getSequence()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should add message with file URL")
    void shouldAddMessageWithFileUrl() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, null, null);
        conversation.addMessage(MessageRole.ASSISTANT, "Here is your file", "https://minio.example.com/file.pdf");

        assertThat(conversation.getMessages()).hasSize(1);
        assertThat(conversation.getMessages().get(0).getFileUrl()).isEqualTo("https://minio.example.com/file.pdf");
    }

    @Test
    @DisplayName("Should assign incremental sequence to messages")
    void shouldAssignIncrementalSequenceToMessages() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, null, null);
        conversation.addMessage(MessageRole.USER, "First");
        conversation.addMessage(MessageRole.ASSISTANT, "Second");
        conversation.addMessage(MessageRole.USER, "Third");

        assertThat(conversation.getMessages()).hasSize(3);
        assertThat(conversation.getMessages().get(0).getSequence()).isEqualTo(1);
        assertThat(conversation.getMessages().get(1).getSequence()).isEqualTo(2);
        assertThat(conversation.getMessages().get(2).getSequence()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should throw exception when adding message to closed conversation")
    void shouldThrowExceptionWhenAddingMessageToClosedConversation() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, null, null);
        conversation.close();

        assertThatThrownBy(() -> conversation.addMessage(MessageRole.USER, "Hello"))
                .isInstanceOf(ConversationException.class)
                .hasMessageContaining("conversation.message.send.failed");
    }

    @Test
    @DisplayName("Should close conversation and update status")
    void shouldCloseConversationAndUpdateStatus() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, null, null);
        conversation.close();

        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
    }

    @Test
    @DisplayName("Should verify ownership correctly")
    void shouldVerifyOwnershipCorrectly() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Conversation conversation = Conversation.create(ownerId, null, null, null);

        assertThat(conversation.isOwnedBy(ownerId)).isTrue();
        assertThat(conversation.isOwnedBy(otherId)).isFalse();
    }

    @Test
    @DisplayName("Should auto-generate title from first message")
    void shouldAutoGenerateTitleFromFirstMessage() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, null, null);
        conversation.autoGenerateTitle("帮我优化一下项目经验部分的内容和结构，以及技术栈的描述和亮点提炼");

        assertThat(conversation.getTitle()).isEqualTo("帮我优化一下项目经验部分的内容和结构，以及技术栈的描述和亮点...");
    }

    @Test
    @DisplayName("Should not override custom title when auto-generating")
    void shouldNotOverrideCustomTitleWhenAutoGenerating() {
        Conversation conversation = Conversation.create(TEST_USER_ID, "Custom Title", null, null);
        conversation.autoGenerateTitle("New message content");

        assertThat(conversation.getTitle()).isEqualTo("Custom Title");
    }

    @Test
    @DisplayName("Should auto-generate title without truncation for short message")
    void shouldAutoGenerateTitleWithoutTruncationForShortMessage() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, null, null);
        conversation.autoGenerateTitle("Short msg");

        assertThat(conversation.getTitle()).isEqualTo("Short msg");
    }

    @Test
    @DisplayName("Should reconstruct conversation from repository")
    void shouldReconstructConversationFromRepository() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();

        Conversation conversation = Conversation.reconstruct(
                id, userId, "Reconstructed", ConversationStatus.ACTIVE,
                resumeVersionId, null, null, LocalDateTime.now(), LocalDateTime.now(),
                Collections.emptyList(), 0L, 0, 0L, null, 0
        );

        assertThat(conversation.getId()).isEqualTo(id);
        assertThat(conversation.getTitle()).isEqualTo("Reconstructed");
        assertThat(conversation.getMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should complete only the matching pending AI request exactly once")
    void shouldCompleteOnlyMatchingPendingAiRequestExactlyOnce() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, null, null);
        conversation.addMessage(MessageRole.USER, "Help me");
        UUID requestId = UUID.randomUUID();
        conversation.requestAiReply(requestId, 1);

        assertThat(conversation.completeAiReply(UUID.randomUUID(), "stale", null, 1, 1)).isFalse();
        assertThat(conversation.completeAiReply(requestId, "answer", null, 10, 3)).isTrue();
        assertThat(conversation.completeAiReply(requestId, "duplicate", null, 10, 3)).isFalse();
        assertThat(conversation.getAiReplyState().status()).isEqualTo(AiReplyStatus.COMPLETED);
        assertThat(conversation.getMessages()).hasSize(2);
        assertThat(conversation.getMessages().get(1).getContent()).isEqualTo("answer");
    }

    @Test
    @DisplayName("Should reject a second user turn while an AI reply is pending")
    void shouldRejectSecondPendingAiRequest() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, null, null);
        conversation.addMessage(MessageRole.USER, "First");
        conversation.requestAiReply(UUID.randomUUID(), 1);

        assertThatThrownBy(() -> conversation.requestAiReply(UUID.randomUUID(), 2))
                .isInstanceOf(AiReplyInProgressException.class);
    }

    @Test
    @DisplayName("Should retry a failed AI request with a new request id")
    void shouldRetryFailedAiRequest() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, null, null);
        conversation.addMessage(MessageRole.USER, "Retry this");
        UUID firstRequestId = UUID.randomUUID();
        UUID retryRequestId = UUID.randomUUID();
        conversation.requestAiReply(firstRequestId, 1);

        assertThat(conversation.failAiReply(firstRequestId, "UPSTREAM_TIMEOUT")).isTrue();
        conversation.retryAiReply(retryRequestId);

        assertThat(conversation.getAiReplyState().status()).isEqualTo(AiReplyStatus.PENDING);
        assertThat(conversation.getAiReplyState().requestId()).isEqualTo(retryRequestId);
        assertThat(conversation.getAiReplyState().userMessageSequence()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should correlate compaction completion and release failed compaction")
    void shouldCorrelateCompactionLifecycle() {
        Conversation conversation = Conversation.create(TEST_USER_ID, null, null, null);
        UUID requestId = UUID.randomUUID();
        conversation.markCompacting(requestId);

        assertThat(conversation.completeCompaction(UUID.randomUUID(), "stale", 1, 2)).isFalse();
        assertThat(conversation.failCompaction(requestId)).isTrue();
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversation.getCompactionRequestId()).isEqualTo(requestId);

        UUID retryId = UUID.randomUUID();
        conversation.markCompacting(retryId);
        assertThat(conversation.completeCompaction(retryId, "summary", 4, 20)).isTrue();
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(conversation.getContextSummary()).isEqualTo("summary");
        assertThat(conversation.getCompactionRequestId()).isEqualTo(retryId);
    }
}
