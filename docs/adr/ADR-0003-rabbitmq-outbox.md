<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](ADR-0003-rabbitmq-outbox.md) | [简体中文](../i18n/zh-Hans-CN/adr/ADR-0003-rabbitmq-outbox.md) | [繁體中文](../i18n/zh-Hant-TW/adr/ADR-0003-rabbitmq-outbox.md)

# ADR-0003: Adopt RabbitMQ + Outbox Pattern for Asynchronous Message Communication

| Attribute | Value |
|-----------|-------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | Backend Architecture Team |
| **Affected Modules** | `backend/infrastructure/messaging/`, `backend/domain/shared/entity/OutboxMessage.java`, `backend/app/shared/scheduler/OutboxRelayScheduler.java` |

---

## 1. Context

JobCopilot has numerous **asynchronous processing scenarios** requiring reliable message delivery:

| Scenario | Producer | Consumer | Reliability Requirement |
|----------|----------|----------|-------------------------|
| AI resume parsing request | `JobApplicationService` | AI microservice | At-Least-Once delivery |
| AI scoring result callback | AI microservice | `AiResultMessageListener` | Idempotent consumption (Duplicate-Safe) |
| Conversation context streaming update | `ConversationApplicationService` | WebSocket / Frontend | Loss-tolerant, low-latency priority |
| Scheduled task scheduling (resume incremental retraining) | `IncrementalRetrainingScheduler` | Internal Worker | Distributed lock prevents duplicate execution |

### 1.1 Core Challenge: Atomicity Between Database Transaction and Message Sending

Typical anti-pattern:

```java
// ❌ Incorrect example: transaction and message are not atomic
@Transactional
public void applyForJob(JobApplicationCommand cmd) {
    jobRepo.save(application);          // ① Inside database transaction
    rabbitTemplate.convertAndSend(...);   // ② Outside transaction — if ① succeeds but ② fails, message is lost
}
```

If the application crashes after `rabbitTemplate.convertAndSend()`, the database is committed but the message was never sent, leaving the AI service permanently unaware of the parsing request.

### 1.2 Candidate Solutions

| Solution | Description | Evaluation |
|----------|-------------|------------|
| **A. RabbitMQ + Outbox Pattern** | Write messages to an `outbox` table in the business database within the same transaction; an independent Relay process asynchronously reads and delivers to RabbitMQ | Medium implementation complexity; highest reliability; industry-standard (Debezium / custom Relay) |
| **B. RabbitMQ Transaction (AMQP Tx)** | Use RabbitMQ Channel transactions to coordinate DB commit with MQ commit | Extremely poor performance (10x+ throughput degradation); no distributed two-phase commit support |
| **C. Distributed Transaction (Seata / Atomikos)** | Introduce XA transaction coordinator | Too heavy; conflicts with Hexagonal Architecture's "lightweight, replaceable" principle |
| **D. Kafka + Kafka Connect** | Use Debezium CDC to capture PostgreSQL binlog and automatically publish to Kafka | Introduces Kafka operational burden; team currently lacks Kafka operations experience |

---

## 2. Decision

**Adopt Solution A: RabbitMQ as the message broker, combined with the Outbox pattern to guarantee atomicity between transactions and messages.**

### 2.1 RabbitMQ Selection Rationale

| Dimension | RabbitMQ | Kafka | Redis Streams |
|-----------|----------|-------|---------------|
| **Protocol Support** | AMQP 0.9.1 (standard, mature) | Proprietary protocol | Proprietary commands |
| **Message Model** | Queue + Routing (Exchange/Binding) | Partitioned log (Topic/Partition) | Lightweight Stream |
| **Delayed Messages** | Native support (TTL + Dead Letter) | Requires external components (e.g., Pulsar) | Not supported |
| **Operational Complexity** | Low (single Docker container) | Medium (ZooKeeper/KRaft, multi-Broker) | Low (Redis already used for cache) |
| **Team Familiarity** | High (mature Spring AMQP wrapper) | Low | Medium |
| **Message Replay** | Limited (requires Dead Letter Queue) | Strong (persistent log with time-based retention) | Weak |

**RabbitMQ wins because**:
1. Project messaging scenarios are primarily **task queues** and **event notifications**, not log stream processing; Kafka's log replay capability is not a hard requirement.
2. **Delayed retry** is needed (AI service timeout retry after 30s); RabbitMQ TTL + DLX is natively supported.
3. Spring Boot's first-class support for AMQP (`spring-boot-starter-amqp`) provides configuration out of the box.
4. Docker Compose deployment requires only one container, aligned with the project's "local one-click startup" goal.

### 2.2 Outbox Pattern Implementation

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Service                      │
│  ┌─────────────────┐         ┌──────────────────────────┐   │
│  │ @Transactional  │         │ OutboxMessageRepository  │   │
│  │   jobRepo.save()│────┬────│   save(outboxMessage)    │   │
│  │   outboxRepo.save│    │    │   (within same tx)     │   │
│  └─────────────────┘    │    └──────────────────────────┘   │
│                         │                                   │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              PostgreSQL (single ACID transaction)       ││
│  │  ┌──────────────┐      ┌────────────────────────────┐    ││
│  │  │ job_applications│    │        outbox_messages      │  ││
│  │  │  (business)  │      │  id | exchange | routingKey │  ││
│  │  └──────────────┘      │      | payload | status     │  ││
│  │                         └────────────────────────────┘    ││
│  └─────────────────────────────────────────────────────────┘│
│                         │                                   │
│                         │ After transaction commit            │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │           OutboxRelayScheduler (independent thread)    ││
│  │  ① Poll Outbox records with `status = PENDING`           ││
│  │  ② Deliver to RabbitMQ Exchange                          ││
│  │  ③ On success: `status = SENT` / failure: `status = FAILED`││
│  │  ④ ShedLock ensures only one instance runs Relay         ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 2.3 Core Code Structure

```java
// Domain layer — Outbox entity only, zero external dependencies
public class OutboxMessage {
    private String id;
    private String exchange;
    private String routingKey;
    private String payload;
    private OutboxStatus status;  // PENDING → SENT / FAILED
    private Instant createdAt;
    private Instant processedAt;
}
```

```java
// App layer — OutboxRelayScheduler, depends only on domain Port interfaces
@Component
@ShedLock(name = "outbox-relay", lockAtMostFor = "PT5M")
public class OutboxRelayScheduler {
    // Poll PENDING messages, send via MessagePublisherPort
}
```

```java
// Infrastructure layer — RabbitMQ concrete implementation
@Component
public class AiMessagePublisherAdapter implements MessagePublisherPort {
    private final RabbitTemplate rabbitTemplate;
    
    public void publish(String exchange, String routingKey, String payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }
}
```

### 2.4 Consumer-Side Idempotency Design

| Strategy | Implementation |
|----------|----------------|
| **Message ID Deduplication** | Consumer maintains `processed_message_ids` table (Redis Set / PG table), keyed by `messageId`; duplicate deliveries are ACKed directly. |
| **Business Key Idempotency** | AI scoring results use `(resumeId, jobId)` as a unique key; duplicate scoring results are overwritten. |

---

## 3. Consequences

### 3.1 Positive

| Benefit | Description |
|---------|-------------|
| **Exactly-Once Semantics (Engineering-Acceptable Range)** | Outbox guarantees at-least-once sending; consumer-side idempotency guarantees at-most-once processing; combined, they achieve business-level exactly-once. |
| **Self-Healing from Failures** | Outbox Relay failed messages retain `FAILED` status, supporting manual re-send or automatic backoff retry. |
| **Duplicate Prevention via Distributed Lock** | ShedLock + Redis guarantees only one Relay works across multiple instances, preventing duplicate message delivery. |
| **Domain Purity** | `OutboxMessage` is defined in the `domain` layer with no dependency on any message queue SDK; Hexagonal Architecture boundaries are crystal clear. |

### 3.2 Negative

| Cost | Description |
|------|-------------|
| **Message Latency** | Outbox Relay polling interval (current 5s) introduces end-to-end latency; unsuitable for real-time requirements < 1s. |
| **Database Write Amplification** | Every async message incurs an additional `outbox_messages` table write; high-throughput scenarios require cleanup strategy evaluation. |
| **Relay Single-Point Bottleneck** | All messages are sent through one scheduler thread; extreme concurrency could become a bottleneck (current business volume is far from this limit). |
| **Operational Complexity** | Must maintain health for three systems simultaneously: RabbitMQ (message queue) + Redis (ShedLock) + PostgreSQL (Outbox table). |

### 3.3 Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Outbox table grows indefinitely | **TTL Cleanup**: `OutboxRelayScheduler` soft-deletes after successful sending; a nightly scheduled job physically purges SENT records older than 7 days. |
| Relay thread crash causing message backlog | **Monitoring Alert**: Prometheus metric `outbox_pending_count` > 100 triggers an alert; Dashboard visualizes backlog trends. |
| RabbitMQ single-node failure | **Mirror Queues**: Production environment enables RabbitMQ Mirror Queue or migrates to managed service (AWS MQ / CloudAMQP). |
| AMQP connection idle-disconnect by firewall | **Heartbeat Configuration**: `spring.rabbitmq.requested-heartbeat=30` keeps connections alive. |

---

## 4. Compliance Verification

- **Integration Tests**: `OutboxRelaySchedulerTest` verifies correct polling and state updates.
- **Idempotency Tests**: `AiResultMessageListenerTest` simulates duplicate delivery to verify the consumer processes only once.
- **Architecture Gate**: `code-analyzer` scans the `app` layer, forbidding direct injection of `RabbitTemplate`; all sending must go through `MessagePublisherPort`.
- **Message Traceability**: Critical messages (AI parsing requests) are retained in `outbox_messages` for 7 days, supporting audit tracing.

---

## 5. Related Decisions

- ADR-0001 — Hexagonal Architecture (`MessagePublisherPort` / `MessageListenerPort` as architectural boundaries)
- ADR-0002 — PostgreSQL (Outbox table shares the same database instance with business tables)
- ADR-0004 — Redis for Distributed Lock (ShedLock) and Cache

---

## 6. Notes

> The Outbox pattern was systematically articulated by Chris Richardson in *Microservices Patterns*, and is widely adopted by projects such as Eventuate and Debezium. This project's lightweight implementation (custom Relay + ShedLock) is sufficiently reliable at data volumes < one million; if future expansion moves toward Event Sourcing architecture, a smooth migration to the Debezium CDC solution is possible without modifying the domain layer.
>
> **Critical Discipline**: Strictly forbidden to call `RabbitTemplate.convertAndSend()` directly inside a `@Transactional` method — this is the classic anti-pattern of non-atomic transactions and messages. Any such discovery during Code Review results in immediate rejection.

---

*End of ADR-0003*