package edu.asu.ser594.resumeassistant.trigger.listener.ai;

import edu.asu.ser594.resumeassistant.api.conversation.facade.ConversationFacade;
import edu.asu.ser594.resumeassistant.api.embedding.facade.VectorFacade;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

/**
 * AI 结果消息监听器测试 / AI result message listener tests
 * <p>
 * 验证 Listener 仅通过 API 层 Facade 接口委托，不直接依赖 Domain 或 Infrastructure。
 * Verifies that the listener only delegates through API-layer Facade interfaces
 * without direct dependency on Domain or Infrastructure.
 */
@ExtendWith(MockitoExtension.class)
class AiResultMessageListenerTest {

    @Mock
    private JobFacade jobFacade;

    @Mock
    private ResumeFacade resumeFacade;

    @Mock
    private ConversationFacade conversationFacade;

    @Mock
    private VectorFacade vectorFacade;

    @InjectMocks
    private AiResultMessageListener listener;

    @Test
    void onJobParseResult_ShouldCallJobFacade() {
        // 准备 / Given
        AiResultEvent event = new AiResultEvent("job-1", "JOB_PARSE", "COMPLETED", null, null, null);

        // 执行 / When
        listener.onJobParseResult(event);

        // 验证 / Then
        verify(jobFacade).handleJobProcessResult(event);
    }

    @Test
    void onResumeParseResult_ShouldCallResumeFacade() {
        // 准备 / Given
        AiResultEvent event = new AiResultEvent("resume-1", "RESUME_PARSE", "COMPLETED", null, null, null);

        // 执行 / When
        listener.onResumeParseResult(event);

        // 验证 / Then
        verify(resumeFacade).handleParseResult(event);
    }

    @Test
    void onVectorGenResult_ShouldCallVectorFacade() {
        // 准备 / Given
        AiResultEvent event = new AiResultEvent(
                "job-123",
                "VECTOR_GEN",
                "COMPLETED",
                Map.of("embedding", java.util.List.of(0.1, 0.2)),
                null,
                "JOB"
        );

        // 执行 / When
        listener.onVectorGenResult(event);

        // 验证 / Then
        verify(vectorFacade).handleVectorGenResult(event);
    }

    @Test
    void onConversationReply_ShouldCallConversationFacade() {
        // 准备 / Given
        AiResultEvent event = new AiResultEvent(
                "conv-1",
                "CONVERSATION_REPLY",
                "COMPLETED",
                Map.of("content", "Hello from AI", "fileUrl", "http://minio/file.pdf"),
                null,
                null
        );

        // 执行 / When
        listener.onConversationReply(event);

        // 验证 / Then
        verify(conversationFacade).saveAiReply("conv-1", "Hello from AI", "http://minio/file.pdf", null);
        verify(conversationFacade).completeAiReply("conv-1", "Hello from AI");
    }

    @Test
    void onConversationReply_WithResumeModification_ShouldExtractMarkdown() {
        // 准备 / Given
        AiResultEvent event = new AiResultEvent(
                "conv-1",
                "CONVERSATION_REPLY",
                "COMPLETED",
                Map.of(
                        "content", "Here is your optimized resume",
                        "resumeModification", Map.of("modified", true, "markdown", "# Optimized Resume")
                ),
                null,
                null
        );

        // 执行 / When
        listener.onConversationReply(event);

        // 验证 / Then
        verify(conversationFacade).saveAiReply("conv-1", "Here is your optimized resume", null, "# Optimized Resume");
        verify(conversationFacade).completeAiReply("conv-1", "Here is your optimized resume");
    }

    @Test
    void onConversationReply_WithResumeModificationNotModified_ShouldPassNull() {
        // 准备 / Given
        AiResultEvent event = new AiResultEvent(
                "conv-1",
                "CONVERSATION_REPLY",
                "COMPLETED",
                Map.of(
                        "content", "No changes needed",
                        "resumeModification", Map.of("modified", false, "markdown", "")
                ),
                null,
                null
        );

        // 执行 / When
        listener.onConversationReply(event);

        // 验证 / Then
        verify(conversationFacade).saveAiReply("conv-1", "No changes needed", null, null);
        verify(conversationFacade).completeAiReply("conv-1", "No changes needed");
    }

    @Test
    void onConversationReply_WhenFailed_ShouldNotCallFacade() {
        // 准备 / Given
        AiResultEvent event = new AiResultEvent(
                "conv-1",
                "CONVERSATION_REPLY",
                "FAILED",
                null,
                "AI service error",
                null
        );

        // 执行 / When
        listener.onConversationReply(event);

        // 验证 / Then
        verify(conversationFacade).saveAiReply(eq("conv-1"), contains("AI response failed"), isNull(), isNull());
        verify(conversationFacade).failAiReply(eq("conv-1"), contains("AI response failed"));
    }
}
