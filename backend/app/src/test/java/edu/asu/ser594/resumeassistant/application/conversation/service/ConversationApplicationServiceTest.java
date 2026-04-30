package edu.asu.ser594.resumeassistant.application.conversation.service;

import edu.asu.ser594.resumeassistant.application.conversation.command.CreateConversationCommand;
import edu.asu.ser594.resumeassistant.application.conversation.command.SendMessageCommand;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Conversation;
import edu.asu.ser594.resumeassistant.domain.conversation.exception.ConversationException;
import edu.asu.ser594.resumeassistant.domain.conversation.repository.ConversationRepository;
import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.MessageRole;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.JobStatus;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
import edu.asu.ser594.resumeassistant.domain.shared.service.MessageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConversationApplicationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ResumeVersionRepository resumeVersionRepository;

    @Mock
    private ResumeGroupRepository resumeGroupRepository;

    @Mock
    private AiMessagePublisherPort aiMessagePublisherPort;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MessageProvider messageProvider;

    @InjectMocks
    private ConversationApplicationService applicationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(messageProvider.getMessage(anyString())).thenReturn("Compare the current job posting with my resume and tell me the match score.");
    }

    @Test
    void createConversation_Success() {
        UUID userId = UUID.randomUUID();
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test Conversation")
                .resumeVersionId(null)
                .jobId(null)
                .build();

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Conversation conversation = applicationService.createConversation(command);

        assertNotNull(conversation);
        assertEquals("Test Conversation", conversation.getTitle());
        assertEquals(userId, conversation.getUserId());
        verify(conversationRepository, times(2)).save(any(Conversation.class));
        verify(aiMessagePublisherPort, times(1)).sendConversationRequest(any(ConversationRequestCommand.class));
    }

    @Test
    void createConversation_WithResumeVersionAndJob_Success() {
        UUID userId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ResumeVersion resumeVersion = mock(ResumeVersion.class);
        when(resumeVersion.getContent()).thenReturn("# Resume");

        Job job = Job.create(userId, "https://example.com/job", false);
        job.markScraping();
        job.markParsing();
        job.markCompleted(new ParsedJobContent("Engineer", "Company", "Description", List.of("Req1")));

        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test")
                .resumeVersionId(resumeVersionId)
                .jobId(jobId)
                .build();

        when(resumeVersionRepository.findById(resumeVersionId)).thenReturn(Optional.of(resumeVersion));
        when(jobRepository.findById(jobId.toString())).thenReturn(Optional.of(job));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        Conversation conversation = applicationService.createConversation(command);

        assertNotNull(conversation);
        assertEquals(resumeVersionId, conversation.getResumeVersionId());
        assertEquals(jobId, conversation.getJobId());
        verify(resumeVersionRepository, times(2)).findById(resumeVersionId);
        verify(jobRepository, times(2)).findById(jobId.toString());
        verify(aiMessagePublisherPort).sendConversationRequest(any(ConversationRequestCommand.class));
    }

    @Test
    void createConversation_WithInvalidResumeVersion_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test")
                .resumeVersionId(resumeVersionId)
                .jobId(null)
                .build();

        when(resumeVersionRepository.findById(resumeVersionId)).thenReturn(Optional.empty());

        assertThrows(ConversationException.class, () -> applicationService.createConversation(command));
    }

    @Test
    void createConversation_WithInvalidJob_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test")
                .resumeVersionId(null)
                .jobId(jobId)
                .build();

        when(jobRepository.findById(jobId.toString())).thenReturn(Optional.empty());

        assertThrows(ConversationException.class, () -> applicationService.createConversation(command));
    }

    @Test
    void sendMessage_Success_PublishesMqEvent() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "New Conversation", null, null);

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
        assertFalse(captor.getValue().init()); // not init because preset message exists
    }

    @Test
    void sendMessage_NotOwned_ThrowsException() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(ownerId, "Title", null, null);

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
        Conversation conversation = Conversation.create(userId, "Title", null, null);

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
    void saveAiReply_WithAiOptimizedMarkdown_SavesOptimizedResume() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        Conversation conversation = Conversation.create(userId, "Title", resumeVersionId, null);
        ResumeVersion version = mock(ResumeVersion.class);
        ResumeGroup group = mock(ResumeGroup.class);

        when(version.getGroupId()).thenReturn(groupId);
        when(group.getId()).thenReturn(groupId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resumeVersionRepository.findById(resumeVersionId)).thenReturn(Optional.of(version));
        when(resumeGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        applicationService.saveAiReply(conversationId, "AI reply", null, "# Optimized Resume");

        assertEquals(1, conversation.getMessages().size());
        verify(resumeVersionRepository).save(any(ResumeVersion.class));
        verify(aiMessagePublisherPort).sendTextForVectorGeneration(any());
    }

    @Test
    void uploadAttachment_Success() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "Title", null, null);

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
        Conversation conversation = Conversation.create(userId, "Title", null, null);

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
        Conversation conversation = Conversation.create(userId, "Title", null, null);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        applicationService.deleteConversation(conversationId, userId);

        verify(conversationRepository, times(1)).deleteById(conversationId);
    }
}
