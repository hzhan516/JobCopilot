package io.jobcopilot.resumeassistant.domain.conversation.entity;

import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
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
                Collections.emptyList()
        );

        assertThat(conversation.getId()).isEqualTo(id);
        assertThat(conversation.getTitle()).isEqualTo("Reconstructed");
        assertThat(conversation.getMessages()).isEmpty();
    }
}
