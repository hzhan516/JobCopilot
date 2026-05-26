package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.api.embedding.facade.VectorFacade;
import io.jobcopilot.resumeassistant.application.conversation.command.CreateConversationCommand;
import io.jobcopilot.resumeassistant.application.conversation.command.SendMessageCommand;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.MessageRole;
import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.job.valueobject.ParsedJobContent;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import io.jobcopilot.resumeassistant.domain.shared.service.MessageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 对话应用服务单元测试 / Conversation application service unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Conversation Application Service Tests")
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
    private VectorFacade vectorFacade;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MessageProvider messageProvider;

    @InjectMocks
    private ConversationApplicationService applicationService;

    // 准备 / Given
    @BeforeEach
    void setUp() {
        when(messageProvider.getMessage(anyString())).thenReturn("Compare the current job posting with my resume and tell me the match score.");
    }

    @Test
    void createConversation_Success() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test Conversation")
                .resumeVersionId(null)
                .jobId(null)
                .build();

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 执行 / When
        Conversation conversation = applicationService.createConversation(command);

        // 验证 / Then
        assertNotNull(conversation);
        assertEquals("Test Conversation", conversation.getTitle());
        assertEquals(userId, conversation.getUserId());
        verify(conversationRepository, times(2)).save(any(Conversation.class));
        verify(aiMessagePublisherPort, times(1)).sendConversationRequest(any(ConversationRequestCommand.class));
    }

    @Test
    void createConversation_ShouldPersistPresetMessage() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test Conversation")
                .resumeVersionId(null)
                .jobId(null)
                .build();

        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 执行 / When
        Conversation conversation = applicationService.createConversation(command);

        // 验证 / Then
        assertNotNull(conversation);
        assertEquals(1, conversation.getMessages().size());
        assertEquals(MessageRole.USER, conversation.getMessages().get(0).getRole());
        verify(conversationRepository, times(2)).save(any(Conversation.class));
    }

    @Test
    void createConversation_WithResumeVersionAndJob_Success() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        ResumeVersion resumeVersion = mock(ResumeVersion.class);
        when(resumeVersion.getContent()).thenReturn("# Resume");

        Job job = Job.create(userId, "https://example.com/job", false);
        job.markScraping();
        job.markParsing();
        job.markCompleted(new ParsedJobContent("Engineer", "Company", "100K", "Remote", "Description", List.of("Req1")));

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

        // 执行 / When
        Conversation conversation = applicationService.createConversation(command);

        // 验证 / Then
        assertNotNull(conversation);
        assertEquals(resumeVersionId, conversation.getResumeVersionId());
        assertEquals(jobId, conversation.getJobId());
        verify(resumeVersionRepository, times(2)).findById(resumeVersionId);
        verify(jobRepository, times(2)).findById(jobId.toString());
        verify(aiMessagePublisherPort).sendConversationRequest(any(ConversationRequestCommand.class));
    }

    @Test
    void createConversation_WithInvalidResumeVersion_ThrowsException() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test")
                .resumeVersionId(resumeVersionId)
                .jobId(null)
                .build();

        when(resumeVersionRepository.findById(resumeVersionId)).thenReturn(Optional.empty());

        // 执行与验证 / When & Then
        assertThrows(ConversationException.class, () -> applicationService.createConversation(command));
    }

    @Test
    void createConversation_WithInvalidJob_ThrowsException() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test")
                .resumeVersionId(null)
                .jobId(jobId)
                .build();

        when(jobRepository.findById(jobId.toString())).thenReturn(Optional.empty());

        // 执行与验证 / When & Then
        assertThrows(ConversationException.class, () -> applicationService.createConversation(command));
    }

    @Test
    void createConversation_WithResumeVersionOnly_ThrowsException() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test")
                .resumeVersionId(resumeVersionId)
                .jobId(null)
                .build();

        // 执行与验证 / When & Then
        ConversationException exception = assertThrows(
                ConversationException.class,
                () -> applicationService.createConversation(command)
        );
        assertEquals("conversation.jobId.required", exception.getMessageKey());
    }

    @Test
    void createConversation_WithJobOnly_ThrowsException() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title("Test")
                .resumeVersionId(null)
                .jobId(jobId)
                .build();

        // 执行与验证 / When & Then
        ConversationException exception = assertThrows(
                ConversationException.class,
                () -> applicationService.createConversation(command)
        );
        assertEquals("conversation.resumeVersionId.required", exception.getMessageKey());
    }

    @Test
    void sendMessage_Success_PublishesMqEvent() {
        // 准备 / Given
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

        // 执行 / When
        Conversation result = applicationService.sendMessage(command);

        // 验证 / Then
        assertNotNull(result);
        assertEquals(1, result.getMessages().size());
        assertEquals("Hello AI", result.getMessages().get(0).getContent());
        assertEquals("Hello AI", result.getTitle()); // auto-generated title

        verify(conversationRepository, times(1)).save(any(Conversation.class));

        ArgumentCaptor<ConversationRequestCommand> captor = ArgumentCaptor.forClass(ConversationRequestCommand.class);
        verify(aiMessagePublisherPort, times(1)).sendConversationRequest(captor.capture());
        assertEquals("Hello AI", captor.getValue().currentMessage());
        assertTrue(captor.getValue().init()); // init because no AI reply exists yet
    }

    @Test
    void sendMessage_WithPresetAndNoAiReply_ShouldSendInitTrue() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "Title", null, null);
        conversation.addMessage(MessageRole.USER, "Preset message");

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

        // 执行 / When
        applicationService.sendMessage(command);

        // 验证 / Then
        ArgumentCaptor<ConversationRequestCommand> captor = ArgumentCaptor.forClass(ConversationRequestCommand.class);
        verify(aiMessagePublisherPort, times(1)).sendConversationRequest(captor.capture());
        assertTrue(captor.getValue().init()); // init because no AI reply exists
    }

    @Test
    void sendMessage_WithAiReply_ShouldSendInitFalse() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "Title", null, null);
        conversation.addMessage(MessageRole.USER, "Preset message");
        conversation.addMessage(MessageRole.ASSISTANT, "AI reply");

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SendMessageCommand command = SendMessageCommand.builder()
                .conversationId(conversationId)
                .userId(userId)
                .role(MessageRole.USER)
                .content("Follow up")
                .fileUrls(new ArrayList<>())
                .build();

        // 执行 / When
        applicationService.sendMessage(command);

        // 验证 / Then
        ArgumentCaptor<ConversationRequestCommand> captor = ArgumentCaptor.forClass(ConversationRequestCommand.class);
        verify(aiMessagePublisherPort, times(1)).sendConversationRequest(captor.capture());
        assertFalse(captor.getValue().init()); // not init because AI already replied
    }

    @Test
    void sendMessage_NotOwned_ThrowsException() {
        // 准备 / Given
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

        // 执行与验证 / When & Then
        assertThrows(ConversationException.class, () -> applicationService.sendMessage(command));
    }

    @Test
    void saveAiReply_Success() {
        // 准备 / Given
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "Title", null, null);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 执行 / When
        applicationService.saveAiReply(conversationId, "AI reply", "https://minio.example.com/file.pdf");

        // 验证 / Then
        assertEquals(1, conversation.getMessages().size());
        assertEquals("AI reply", conversation.getMessages().get(0).getContent());
        assertEquals(MessageRole.ASSISTANT, conversation.getMessages().get(0).getRole());
        assertEquals("https://minio.example.com/file.pdf", conversation.getMessages().get(0).getFileUrl());
    }

    @Test
    void saveAiReply_FirstModification_CreatesAiOptimizedVersion() {
        // 准备 / Given
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        Conversation conversation = Conversation.create(userId, "Title", resumeVersionId, null);
        ResumeVersion originalVersion = mock(ResumeVersion.class);
        ResumeGroup group = mock(ResumeGroup.class);

        when(originalVersion.getGroupId()).thenReturn(groupId);
        when(group.getId()).thenReturn(groupId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resumeVersionRepository.findById(resumeVersionId)).thenReturn(Optional.of(originalVersion));
        when(resumeGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // 执行 / When
        applicationService.saveAiReply(conversationId, "AI reply", null, "# Optimized Resume");

        // 验证 / Then
        assertEquals(1, conversation.getMessages().size());
        // 验证消息内容包含 AI 回复和修改后的简历
        String messageContent = conversation.getMessages().get(0).getContent();
        assertTrue(messageContent.contains("AI reply"));
        assertTrue(messageContent.contains("---"));
        assertTrue(messageContent.contains("# Optimized Resume"));
        // 验证使用了 group.addVersion（而不是直接 repository.save）
        verify(group).addVersion(any(ResumeVersion.class));
        verify(resumeGroupRepository).save(any(ResumeGroup.class));
        verify(vectorFacade).generateAndSaveVector(anyString(), eq("RESUME"), anyString());
        // 验证 conversation 的 aiOptimizedVersionId 被设置
        assertNotNull(conversation.getAiOptimizedVersionId());
    }

    @Test
    void saveAiReply_SubsequentModification_UpdatesExistingVersion() {
        // 准备 / Given
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID workingVersionId = UUID.randomUUID();

        Conversation conversation = Conversation.create(userId, "Title", resumeVersionId, null);
        conversation.setAiOptimizedVersionId(workingVersionId);

        ResumeVersion originalVersion = mock(ResumeVersion.class);
        ResumeVersion workingVersion = mock(ResumeVersion.class);
        ResumeGroup group = mock(ResumeGroup.class);

        when(originalVersion.getGroupId()).thenReturn(groupId);
        when(workingVersion.getId()).thenReturn(workingVersionId);
        when(workingVersion.getStatus()).thenReturn(ResumeVersion.Status.ACTIVE);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resumeVersionRepository.findById(resumeVersionId)).thenReturn(Optional.of(originalVersion));
        when(resumeVersionRepository.findById(workingVersionId)).thenReturn(Optional.of(workingVersion));
        when(resumeGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // 执行 / When
        applicationService.saveAiReply(conversationId, "AI reply", null, "# Updated Resume");

        // 验证 / Then
        assertEquals(1, conversation.getMessages().size());
        // 验证消息内容包含 AI 回复和修改后的简历
        String messageContent = conversation.getMessages().get(0).getContent();
        assertTrue(messageContent.contains("AI reply"));
        assertTrue(messageContent.contains("---"));
        assertTrue(messageContent.contains("# Updated Resume"));
        // 验证直接编辑了现有版本（没有创建新的）
        verify(workingVersion).editContent("# Updated Resume");
        verify(resumeVersionRepository).save(workingVersion);
        verify(vectorFacade).generateAndSaveVector(workingVersionId.toString(), "RESUME", "# Updated Resume");
        // 验证没有走 group.addVersion 路径
        verify(group, never()).addVersion(any(ResumeVersion.class));
    }

    @Test
    void saveAiReply_WhenVersionArchived_RecreatesAiOptimized() {
        // 准备 / Given
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID workingVersionId = UUID.randomUUID();

        Conversation conversation = Conversation.create(userId, "Title", resumeVersionId, null);
        conversation.setAiOptimizedVersionId(workingVersionId);

        ResumeVersion originalVersion = mock(ResumeVersion.class);
        ResumeVersion archivedVersion = mock(ResumeVersion.class);
        ResumeGroup group = mock(ResumeGroup.class);

        when(originalVersion.getGroupId()).thenReturn(groupId);
        when(archivedVersion.getStatus()).thenReturn(ResumeVersion.Status.ARCHIVED);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resumeVersionRepository.findById(resumeVersionId)).thenReturn(Optional.of(originalVersion));
        when(resumeVersionRepository.findById(workingVersionId)).thenReturn(Optional.of(archivedVersion));
        when(resumeGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // 执行 / When
        applicationService.saveAiReply(conversationId, "AI reply", null, "# Re-created Resume");

        // 验证 / Then
        assertEquals(1, conversation.getMessages().size());
        // 验证消息内容包含 AI 回复和修改后的简历
        String messageContent = conversation.getMessages().get(0).getContent();
        assertTrue(messageContent.contains("AI reply"));
        assertTrue(messageContent.contains("---"));
        assertTrue(messageContent.contains("# Re-created Resume"));
        // 验证重新创建了 AI_OPTIMIZED（因为旧的被归档了）
        verify(group).addVersion(any(ResumeVersion.class));
        verify(resumeGroupRepository).save(any(ResumeGroup.class));
        // 验证 conversation 的 aiOptimizedVersionId 被更新
        assertNotNull(conversation.getAiOptimizedVersionId());
    }

    @Test
    void sendConversationRequestWithContext_PrefersAiOptimizedVersion() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID resumeVersionId = UUID.randomUUID();
        UUID aiOptimizedVersionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        Conversation conversation = Conversation.create(userId, "Title", resumeVersionId, jobId);
        conversation.setAiOptimizedVersionId(aiOptimizedVersionId);
        // 注意：需要保存 conversation 以使其可被 repository 返回

        ResumeVersion aiVersion = mock(ResumeVersion.class);
        when(aiVersion.getContent()).thenReturn("# AI Optimized Content");

        Job job = Job.create(userId, "https://example.com/job", false);
        job.markScraping();
        job.markParsing();
        job.markCompleted(new ParsedJobContent("Engineer", "Company", "100K", "Remote", "Description", List.of("Req1")));

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resumeVersionRepository.findById(aiOptimizedVersionId)).thenReturn(Optional.of(aiVersion));
        when(jobRepository.findById(jobId.toString())).thenReturn(Optional.of(job));
        when(jobRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        SendMessageCommand command = SendMessageCommand.builder()
                .conversationId(conversationId)
                .userId(userId)
                .role(MessageRole.USER)
                .content("Hello AI")
                .fileUrls(new ArrayList<>())
                .build();

        // 执行 / When
        applicationService.sendMessage(command);

        // 验证 / Then
        ArgumentCaptor<ConversationRequestCommand> captor = ArgumentCaptor.forClass(ConversationRequestCommand.class);
        verify(aiMessagePublisherPort, times(1)).sendConversationRequest(captor.capture());
        // 验证优先使用了 AI_OPTIMIZED 版本的内容
        assertEquals("# AI Optimized Content", captor.getValue().resumeText());
        // 验证没有查询原始版本
        verify(resumeVersionRepository, never()).findById(resumeVersionId);
    }

    @Test
    void uploadAttachment_Success() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "Title", null, null);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(fileStorageService.generatePresignedUrl(anyString(), any()))
                .thenReturn("https://minio.example.com/presigned-url");

        // 执行 / When
        String url = applicationService.uploadAttachment(
                conversationId, userId,
                new ByteArrayInputStream("content".getBytes()),
                100L, "text/plain", "test.txt"
        );

        // 验证 / Then
        assertEquals("https://minio.example.com/presigned-url", url);
        verify(fileStorageService, times(1)).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void closeConversation_Success() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "Title", null, null);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 执行 / When
        applicationService.closeConversation(conversationId, userId);

        // 验证 / Then
        assertTrue(conversation.getStatus().name().equals("CLOSED"));
    }

    @Test
    void deleteConversation_Success() {
        // 准备 / Given
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, "Title", null, null);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // 执行 / When
        applicationService.deleteConversation(conversationId, userId);

        // 验证 / Then
        verify(conversationRepository, times(1)).deleteById(conversationId);
    }
}
