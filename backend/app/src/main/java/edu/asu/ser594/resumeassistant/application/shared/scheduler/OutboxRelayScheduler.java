package edu.asu.ser594.resumeassistant.application.shared.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.ser594.resumeassistant.domain.shared.entity.OutboxMessage;
import edu.asu.ser594.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import edu.asu.ser594.resumeassistant.types.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Relays pending outbox records to RabbitMQ on a fixed schedule, implementing the Transactional Outbox
 * pattern to guarantee at-least-once message delivery without coupling domain transactions to MQ availability.
 * 按固定周期将待处理的 Outbox 记录转发到 RabbitMQ，实现事务性发件箱模式，确保至少一次消息投递，同时避免将领域事务与 MQ 可用性耦合
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxMessageRepository outboxMessageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Polls every 2 seconds for PENDING outbox rows and attempts delivery.
     * Failed messages are marked so downstream monitoring can alert on persistent relay failures.
     * 每 2 秒轮询 PENDING 状态的 Outbox 行并尝试投递。失败消息会被标记，以便下游监控对持续投递失败发出告警
     */
    @Scheduled(fixedDelay = 2000)
    @SchedulerLock(name = "OutboxRelay", lockAtMostFor = "5m", lockAtLeastFor = "1s")
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
                Object payloadObject = objectMapper.readValue(message.getPayload(), Object.class);
                rabbitTemplate.convertAndSend(
                        message.getExchange(),
                        message.getRoutingKey(),
                        payloadObject
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
