package io.jobcopilot.resumeassistant.trigger.listener.ai;

import io.jobcopilot.resumeassistant.api.conversation.facade.ConversationFacade;
import io.jobcopilot.resumeassistant.api.embedding.facade.VectorFacade;
import io.jobcopilot.resumeassistant.api.job.facade.JobFacade;
import io.jobcopilot.resumeassistant.api.matching.facade.MatchingFacade;
import io.jobcopilot.resumeassistant.api.resume.facade.ResumeFacade;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;
import io.jobcopilot.resumeassistant.infrastructure.messaging.RedisIdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private MatchingFacade matchingFacade;

    @Mock
    private RedisIdempotencyService idempotencyService;

    @InjectMocks
    private AiResultMessageListener listener;

    @Test
    @DisplayName("Should delegate job parse result to JobFacade / 应将职位解析结果委托给 JobFacade")
    void onJobParseResult_ShouldCallJobFacade() {
        // 准备 / Given
        AiResultEvent event = new AiResultEvent("job-1", "JOB_PARSE", "COMPLETED", null, null, null);

        // 执行 / When
        listener.onJobParseResult(event);

        // 验证 / Then
        verify(jobFacade).handleJobProcessResult(event);
    }

    @Test
    @DisplayName("Should delegate resume parse result to ResumeFacade / 应将简历解析结果委托给 ResumeFacade")
    void onResumeParseResult_ShouldCallResumeFacade() {
        // 准备 / Given
        AiResultEvent event = new AiResultEvent("resume-1", "RESUME_PARSE", "COMPLETED", null, null, null);

        // 执行 / When
        listener.onResumeParseResult(event);

        // 验证 / Then
        verify(resumeFacade).handleParseResult(event);
    }

    @Test
    @DisplayName("Should delegate job rank result to MatchingFacade / 应将职位排名结果委托给 MatchingFacade")
    void onJobRankResult_ShouldCallMatchingFacade() {
        // 准备 / Given
        Map<String, Object> data = Map.of(
                "rankTimeMs", 150L,
                "rankedResults", List.of(
                        Map.of("jobId", "job-1", "title", "Title", "company", "Company",
                                "matchScore", 0.95, "description", "Description")
                )
        );
        AiResultEvent event = new AiResultEvent("match-001", "JOB_RANK", "COMPLETED", data, null, null);

        // 执行 / When
        listener.onJobRankResult(event);

        // 验证 / Then
        verify(matchingFacade).saveJobRankResult(eq("match-001"), any(List.class), eq(150L));
    }

    @Test
    @DisplayName("Should not call MatchingFacade when job rank fails / 应在职位排名失败时不调用 MatchingFacade")
    void onJobRankResult_WhenFailed_ShouldNotCallMatchingFacade() {
        // 准备 / Given
        AiResultEvent event = new AiResultEvent("match-002", "JOB_RANK", "FAILED", null, "AI service error", null);

        // 执行 / When
        listener.onJobRankResult(event);

        // 验证 / Then — matchingFacade 不应被调用
    }

    @Test
    @DisplayName("Should delegate conversation reply to ConversationFacade / 应将对话回复委托给 ConversationFacade")
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
    @DisplayName("Should extract markdown when conversation reply includes resume modification / 应在对话回复包含简历修改时提取 Markdown")
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
    @DisplayName("Should pass null when resume modification is not modified / 应在简历未修改时传入 null")
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
    @DisplayName("Should not call facade when conversation reply fails / 应在对话回复失败时不调用 Facade")
    void onConversationReply_WhenFailed_ShouldSaveLocalizedFailureMessage() {
        // 准备 / Given
        AiResultEvent event = new AiResultEvent(
                "conv-1",
                "CONVERSATION_REPLY",
                "FAILED",
                Map.of("requestId", "req-1", "locale", "zh-CN", "errorCode", "RATE_LIMITED"),
                "RATE_LIMITED",
                null
        );
        when(conversationFacade.resolveAiFailureMessage("RATE_LIMITED", "zh-CN"))
                .thenReturn("AI 请求暂时过于频繁，请几分钟后再试。");

        // 执行 / When
        listener.onConversationReply(event);

        // 验证 / Then
        verify(conversationFacade).saveAiReply("conv-1", "AI 请求暂时过于频繁，请几分钟后再试。", null, null);
        verify(conversationFacade).failAiReply("conv-1", "AI 请求暂时过于频繁，请几分钟后再试。");
        verify(idempotencyService).markProcessed("conversation:conv-1:req-1:FAILED");
    }

    @Test
    @DisplayName("Should process conversation replies with different request ids independently")
    void onConversationReply_WithDifferentRequestIds_ShouldProcessIndependently() {
        // 鍑嗗 / Given
        AiResultEvent first = new AiResultEvent(
                "conv-1",
                "CONVERSATION_REPLY",
                "COMPLETED",
                Map.of("requestId", "req-1", "content", "First reply"),
                null,
                null
        );
        AiResultEvent second = new AiResultEvent(
                "conv-1",
                "CONVERSATION_REPLY",
                "COMPLETED",
                Map.of("requestId", "req-2", "content", "Second reply"),
                null,
                null
        );

        // 鎵ц / When
        listener.onConversationReply(first);
        listener.onConversationReply(second);

        // 楠岃瘉 / Then
        verify(conversationFacade).saveAiReply("conv-1", "First reply", null, null);
        verify(conversationFacade).saveAiReply("conv-1", "Second reply", null, null);
        verify(idempotencyService).markProcessed("conversation:conv-1:req-1:COMPLETED");
        verify(idempotencyService).markProcessed("conversation:conv-1:req-2:COMPLETED");
    }
}
