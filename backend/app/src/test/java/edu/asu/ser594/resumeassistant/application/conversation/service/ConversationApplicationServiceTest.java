package edu.asu.ser594.resumeassistant.application.conversation.service;

import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import edu.asu.ser594.resumeassistant.application.conversation.command.CreateConversationCommand;
import edu.asu.ser594.resumeassistant.application.conversation.command.SendMessageCommand;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Conversation;
import edu.asu.ser594.resumeassistant.domain.conversation.exception.ConversationException;
import edu.asu.ser594.resumeassistant.domain.conversation.repository.ConversationRepository;
import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.MessageRole;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConversationApplicationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ResumeFacade resumeFacade;

    @Mock
    private AiMessagePublisherPort aiMessagePublisherPort;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ConversationApplicationService applicationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createConversation_Success() {
        UUID userId = UUID.randomUUID();
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test Conversation")
                .resumeVersionId(null)
                .build();

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Conversation conversation = applicationService.createConversation(command);

        assertNotNull(conversation);
        assertEquals("Test Conversation", conversation.getTitle());
        assertEquals(userId, conversation.getUserId());
        verify(conversationRepository, times(1)).save(any(Conversation.class));
    }

    @Test
    void createConversation_WithInvalidResumeVersion_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test")
                .resumeVersionId(resumeVersionId)
                .build();

        doThrow(new RuntimeException("Invalid")).when(resumeFacade).getVersion(resumeVersionId, userId);

        assertThrows(ConversationException.class, () -> applicationService.createConversation(command));
    }

    @Test
    void sendMessage_Success_PublishesMqEvent() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "New Conversation", null);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SendMessageCommand command = SendMessageCommand.builder()
                .conversationId(conversationId)
                .userId(userId)
                .role(MessageRole.USER)
                .content("Hello AI")
                .fileUrls(new ArrayList<>())
                .build();

        Conversation result = applicationService.sendMessage(command);

        assertNotNull(result);
        assertEquals(1, result.getMessages().size());
        assertEquals("Hello AI", result.getMessages().get(0).getContent());
        assertEquals("Hello AI", result.getTitle()); // auto-generated title

        verify(conversationRepository, times(1)).save(any(Conversation.class));

        ArgumentCaptor<ConversationRequestCommand> captor = ArgumentCaptor.forClass(ConversationRequestCommand.class);
        verify(aiMessagePublisherPort, times(1)).sendConversationRequest(captor.capture());
        assertEquals("Hello AI", captor.getValue().currentMessage());
    }

    @Test
    void sendMessage_NotOwned_ThrowsException() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(ownerId, "Title", null);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        SendMessageCommand command = SendMessageCommand.builder()
                .conversationId(conversationId)
                .userId(otherId)
                .role(MessageRole.USER)
                .content("Hello")
                .build();

        assertThrows(ConversationException.class, () -> applicationService.sendMessage(command));
    }

    @Test
    void saveAiReply_Success() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "Title", null);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        applicationService.saveAiReply(conversationId, "AI reply", "https://minio.example.com/file.pdf");

        assertEquals(1, conversation.getMessages().size());
        assertEquals("AI reply", conversation.getMessages().get(0).getContent());
        assertEquals(MessageRole.ASSISTANT, conversation.getMessages().get(0).getRole());
        assertEquals("https://minio.example.com/file.pdf", conversation.getMessages().get(0).getFileUrl());
    }

    @Test
    void uploadAttachment_Success() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "Title", null);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(fileStorageService.generatePresignedUrl(anyString(), any()))
                .thenReturn("https://minio.example.com/presigned-url");

        String url = applicationService.uploadAttachment(
                conversationId, userId,
                new ByteArrayInputStream("content".getBytes()),
                100L, "text/plain", "test.txt"
        );

        assertEquals("https://minio.example.com/presigned-url", url);
        verify(fileStorageService, times(1)).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void closeConversation_Success() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "Title", null);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        applicationService.closeConversation(conversationId, userId);

        assertTrue(conversation.getStatus().name().equals("CLOSED"));
    }

    @Test
    void deleteConversation_Success() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "Title", null);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        applicationService.deleteConversation(conversationId, userId);

        verify(conversationRepository, times(1)).deleteById(conversationId);
    }
}
