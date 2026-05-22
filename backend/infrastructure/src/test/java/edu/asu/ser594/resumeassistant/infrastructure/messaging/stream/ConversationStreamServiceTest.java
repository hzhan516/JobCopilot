package edu.asu.ser594.resumeassistant.infrastructure.messaging.stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 对话流式回复服务测试 / Conversation stream service tests
 * <p>
 * 使用 Mockito 模拟 Redis 依赖，验证本地快速路径与跨实例桥接逻辑。
 */
@DisplayName("ConversationStreamService Tests")
class ConversationStreamServiceTest {

    private ConversationStreamService streamService;
    private StringRedisTemplate redisTemplate;
    private RedisMessageListenerContainer redisContainer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        redisContainer = mock(RedisMessageListenerContainer.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        streamService = new ConversationStreamService(redisTemplate, redisContainer);
    }

    private void mockRedisEarlyReply(String conversationId, String reply) {
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        when(valueOps.get("ra:conv:early:" + conversationId)).thenReturn(reply);
    }

    @Test
    @DisplayName("Should receive reply when completeReply is called after awaitReply")
    void shouldReceiveReplyWhenCompleteAfterAwait() throws Exception {
        // Given
        String conversationId = "conv-123";
        String expectedReply = "Hello, this is the AI reply.";

        // When: 在另一个线程中稍后完成回复 / Complete reply in another thread after a short delay
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
                streamService.completeReply(conversationId, expectedReply);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        String actualReply = streamService.awaitReply(conversationId, 5, TimeUnit.SECONDS);

        // Then
        assertThat(actualReply).isEqualTo(expectedReply);
    }

    @Test
    @DisplayName("Should return early reply when completeReply arrives before awaitReply")
    void shouldReturnEarlyReply() {
        // Given
        String conversationId = "conv-456";
        String earlyReply = "Early arrived reply.";

        // When: 先完成回复，再等待 / Complete first, then await
        streamService.completeReply(conversationId, earlyReply);
        mockRedisEarlyReply(conversationId, earlyReply);
        String actualReply = streamService.awaitReply(conversationId, 5, TimeUnit.SECONDS);

        // Then
        assertThat(actualReply).isEqualTo(earlyReply);
    }

    @Test
    @DisplayName("Should return null when awaitReply times out")
    void shouldReturnNullOnTimeout() {
        // Given
        String conversationId = "conv-789";

        // When: 不调用 completeReply，直接等待 / Wait without completing
        String actualReply = streamService.awaitReply(conversationId, 50, TimeUnit.MILLISECONDS);

        // Then
        assertThat(actualReply).isNull();
    }

    @Test
    @DisplayName("Should return null when failReply is called")
    void shouldReturnNullOnFailReply() throws Exception {
        // Given
        String conversationId = "conv-abc";

        // When: 在另一个线程中标记失败 / Fail in another thread
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                streamService.failReply(conversationId, "AI service error");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        String actualReply = streamService.awaitReply(conversationId, 5, TimeUnit.SECONDS);

        // Then: 失败时返回 null / Returns null on failure
        assertThat(actualReply).isNull();
    }

    @Test
    @DisplayName("Should handle multiple conversations independently")
    void shouldHandleMultipleConversationsIndependently() throws Exception {
        // Given
        String conv1 = "conv-1";
        String conv2 = "conv-2";

        // When
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                streamService.completeReply(conv1, "Reply for conv1");
                Thread.sleep(50);
                streamService.completeReply(conv2, "Reply for conv2");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        String reply1 = streamService.awaitReply(conv1, 5, TimeUnit.SECONDS);
        String reply2 = streamService.awaitReply(conv2, 5, TimeUnit.SECONDS);

        // Then
        assertThat(reply1).isEqualTo("Reply for conv1");
        assertThat(reply2).isEqualTo("Reply for conv2");
    }

    @Test
    @DisplayName("Should reuse existing future for duplicate await on same conversation")
    void shouldReuseExistingFuture() throws Exception {
        // Given
        String conversationId = "conv-dup";

        // When: 两个线程同时等待同一个对话 / Two threads await the same conversation
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() ->
                streamService.awaitReply(conversationId, 5, TimeUnit.SECONDS));
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() ->
                streamService.awaitReply(conversationId, 5, TimeUnit.SECONDS));

        Thread.sleep(50);
        streamService.completeReply(conversationId, "Shared reply");

        // Then: 两个等待都应收到相同回复 / Both awaits should receive the same reply
        assertThat(future1.get(5, TimeUnit.SECONDS)).isEqualTo("Shared reply");
        assertThat(future2.get(5, TimeUnit.SECONDS)).isEqualTo("Shared reply");
    }
}
