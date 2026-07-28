package io.jobcopilot.resumeassistant.application.conversation.scheduler;

import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Converts abandoned PENDING replies into an explicit terminal TIMED_OUT state. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiReplyTimeoutScheduler {

    private final ConversationRepository conversationRepository;

    @Value("${app.ai-chat.end-to-end-deadline-seconds:120}")
    private long deadlineSeconds;

    @Scheduled(fixedDelayString = "${app.ai-chat.watchdog-interval-ms:15000}")
    @SchedulerLock(name = "AiReplyTimeoutWatchdog", lockAtMostFor = "2m", lockAtLeastFor = "1s")
    @Transactional(timeout = 30)
    public void timeoutExpiredReplies() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(deadlineSeconds);
        for (Conversation conversation : conversationRepository.findPendingAiRepliesStartedBefore(cutoff)) {
            var requestId = conversation.getAiReplyState().requestId();
            if (requestId != null && conversation.timeoutAiReply(requestId)) {
                conversationRepository.save(conversation);
                log.warn("AI reply timed out: conversation={}, requestId={}",
                        conversation.getId(), requestId);
            }
        }
    }
}
