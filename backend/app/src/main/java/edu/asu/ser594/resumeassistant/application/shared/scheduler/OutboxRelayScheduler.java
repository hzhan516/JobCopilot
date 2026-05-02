package edu.asu.ser594.resumeassistant.application.shared.scheduler;

import edu.asu.ser594.resumeassistant.domain.shared.entity.OutboxMessage;
import edu.asu.ser594.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import edu.asu.ser594.resumeassistant.types.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox 消息转发调度器
 * Outbox message relay scheduler
 * <p>
 * 定期扫描 PENDING 状态的 Outbox 记录，投递到 RabbitMQ。
 * Periodically scans PENDING outbox records and delivers them to RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    //    private static final int BATCH_SIZE = 100;
    private final OutboxMessageRepository outboxMessageRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 每 2 秒执行一次转发
     * Relay every 2 seconds
     */
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void relayPendingMessages() {
        List<OutboxMessage> pendingMessages = outboxMessageRepository.findByStatus(OutboxStatus.PENDING);

        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("Relaying {} pending outbox messages", pendingMessages.size());

        int successCount = 0;
        int failCount = 0;

        for (OutboxMessage message : pendingMessages) {
            try {
                rabbitTemplate.convertAndSend(
                        message.getExchange(),
                        message.getRoutingKey(),
                        message.getPayload()
                );
                message.markSent();
                outboxMessageRepository.save(message);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to relay outbox message: {}, exchange: {}, routingKey: {}",
                        message.getId(), message.getExchange(), message.getRoutingKey(), e);
                message.markFailed();
                outboxMessageRepository.save(message);
                failCount++;
            }
        }

        log.info("Outbox relay completed: {} succeeded, {} failed", successCount, failCount);
    }
}
