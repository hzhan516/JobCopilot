package io.jobcopilot.resumeassistant.application.shared.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.domain.shared.entity.OutboxMessage;
import io.jobcopilot.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import io.jobcopilot.resumeassistant.types.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Short database transactions for claim and terminal outbox state changes. */
@Component
@RequiredArgsConstructor
class OutboxRelayTransactionService {

    private final OutboxMessageRepository outboxMessageRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10)
    public OutboxMessage claim(String messageId, String workerId, LocalDateTime now) {
        OutboxMessage message = outboxMessageRepository.findById(messageId).orElse(null);
        if (message == null
                || (message.getStatus() != OutboxStatus.PENDING && message.getStatus() != OutboxStatus.FAILED)
                || (message.getNextAttemptAt() != null && message.getNextAttemptAt().isAfter(now))) {
            return null;
        }
        message.markProcessing(workerId, now);
        return outboxMessageRepository.save(message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10)
    public void markSent(String messageId) {
        OutboxMessage message = outboxMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalStateException("Outbox message disappeared: " + messageId));
        message.markSent();
        outboxMessageRepository.save(message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10)
    public void markFailed(String messageId, String errorCode, LocalDateTime nextAttemptAt, int maxAttempts) {
        OutboxMessage message = outboxMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalStateException("Outbox message disappeared: " + messageId));
        message.markFailed(errorCode, nextAttemptAt, maxAttempts);
        outboxMessageRepository.save(message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10)
    public void recoverStale(String messageId, LocalDateTime now) {
        outboxMessageRepository.findById(messageId).ifPresent(message -> {
            message.recoverStaleClaim(now);
            outboxMessageRepository.save(message);
        });
    }
}

/** Performs RabbitMQ I/O outside database transactions and requires publisher confirmation. */
@Component
@RequiredArgsConstructor
class OutboxRabbitPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publish(OutboxMessage message, long confirmTimeoutSeconds) throws Exception {
        Object payloadObject = objectMapper.readValue(message.getPayload(), Object.class);
        CorrelationData correlationData = new CorrelationData(message.getId());
        rabbitTemplate.convertAndSend(message.getExchange(), message.getRoutingKey(), payloadObject,
                correlationData);
        CorrelationData.Confirm confirm = correlationData.getFuture()
                .get(confirmTimeoutSeconds, TimeUnit.SECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ NACK: " + confirm.getReason());
        }
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ returned unroutable message");
        }
    }
}

/**
 * Claims due outbox rows, publishes them with confirms, and applies bounded backoff retries.
 * ShedLock prevents concurrent schedulers while PROCESSING/lockedAt also makes crash recovery explicit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private static final long[] BACKOFF_SECONDS = {2, 10, 30, 120, 600};

    private final OutboxMessageRepository outboxMessageRepository;
    private final OutboxRelayTransactionService transactionService;
    private final OutboxRabbitPublisher publisher;

    @Value("${app.outbox.batch-size:100}")
    private int batchSize;

    @Value("${app.outbox.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.outbox.stale-processing-seconds:120}")
    private long staleProcessingSeconds;

    @Value("${app.outbox.publisher-confirm-timeout-seconds:10}")
    private long confirmTimeoutSeconds;

    @Scheduled(fixedDelayString = "${app.outbox.relay-interval-ms:2000}")
    @SchedulerLock(name = "OutboxRelay", lockAtMostFor = "5m", lockAtLeastFor = "1s")
    public void relayPendingMessages() {
        LocalDateTime now = LocalDateTime.now();
        recoverStaleClaims(now);

        List<OutboxMessage> dueMessages = outboxMessageRepository.findDueForDelivery(now, batchSize);
        if (dueMessages.isEmpty()) {
            return;
        }

        String workerId = UUID.randomUUID().toString();
        int sent = 0;
        int failed = 0;
        for (OutboxMessage candidate : dueMessages) {
            OutboxMessage claimed = transactionService.claim(candidate.getId(), workerId, LocalDateTime.now());
            if (claimed == null) {
                continue;
            }
            try {
                publisher.publish(claimed, confirmTimeoutSeconds);
                transactionService.markSent(claimed.getId());
                sent++;
            } catch (Exception e) {
                failed++;
                String errorCode = classifyError(e);
                LocalDateTime nextAttempt = LocalDateTime.now().plusSeconds(backoffSeconds(claimed.getAttemptCount()));
                transactionService.markFailed(claimed.getId(), errorCode, nextAttempt, maxAttempts);
                log.error("Outbox publish failed: id={}, attempt={}, errorCode={}",
                        claimed.getId(), claimed.getAttemptCount(), errorCode, e);
            }
        }
        log.info("Outbox relay completed: sent={}, failed={}", sent, failed);
    }

    private void recoverStaleClaims(LocalDateTime now) {
        LocalDateTime cutoff = now.minusSeconds(staleProcessingSeconds);
        for (OutboxMessage stale : outboxMessageRepository.findStaleProcessing(cutoff, batchSize)) {
            transactionService.recoverStale(stale.getId(), now);
            log.warn("Recovered stale outbox claim: id={}, workerId={}", stale.getId(), stale.getWorkerId());
        }
    }

    private static long backoffSeconds(int attemptCount) {
        int index = Math.max(0, Math.min(attemptCount - 1, BACKOFF_SECONDS.length - 1));
        return BACKOFF_SECONDS[index];
    }

    private static String classifyError(Exception exception) {
        String name = exception.getClass().getSimpleName().toUpperCase();
        if (name.contains("TIMEOUT")) {
            return "PUBLISH_TIMEOUT";
        }
        if (name.contains("JSON") || name.contains("MAPPING")) {
            return "INVALID_PAYLOAD";
        }
        return "BROKER_UNAVAILABLE";
    }
}
