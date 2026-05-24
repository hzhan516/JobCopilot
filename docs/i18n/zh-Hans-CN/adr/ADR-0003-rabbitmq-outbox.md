# ADR-0003: 采用 RabbitMQ + Outbox 模式实现异步消息通信

| 属性 | 内容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | 后端架构团队 |
| **Affected Modules** | `backend/infrastructure/messaging/`, `backend/domain/shared/entity/OutboxMessage.java`, `backend/app/shared/scheduler/OutboxRelayScheduler.java` |

---

## 1. Context / 背景

ResumeAssistant 存在大量 **异步处理场景**，需要可靠的消息传递机制：

| 场景 | 触发方 | 消费方 | 可靠性要求 |
|------|--------|--------|------------|
| AI 简历解析请求 | `JobApplicationService` | AI 微服务 | 至少一次投递（At-Least-Once） |
| AI 评分结果回调 | AI 微服务 | `AiResultMessageListener` | 幂等消费（Duplicate-Safe） |
| 对话上下文流式更新 | `ConversationApplicationService` | WebSocket / 前端 | 允许丢失，低延迟优先 |
| 定时任务调度（简历增量重训练） | `IncrementalRetrainingScheduler` | 内部 Worker | 分布式锁防重复执行 |

### 1.1 核心挑战：数据库事务与消息发送的原子性

典型的反模式：

```java
// ❌ 错误示例：事务与消息非原子
@Transactional
public void applyForJob(JobApplicationCommand cmd) {
    jobRepo.save(application);          // ① 数据库事务内
    rabbitTemplate.convertAndSend(...); // ② 事务外 — 若 ① 成功 ② 失败，消息丢失
}
```

若应用服务在 `rabbitTemplate.convertAndSend()` 后崩溃，数据库已提交但消息未发出，导致 AI 服务永远收不到解析请求。

### 1.2 候选方案

| 方案 | 说明 | 评估 |
|------|------|------|
| **A. RabbitMQ + Outbox 模式** | 将消息写入业务数据库的 `outbox` 表，与业务数据同一事务；由独立 Relay 进程异步读取并投递到 RabbitMQ | 实现复杂度中等，可靠性最高，业界标准方案（Debezium / 自定义 Relay） |
| **B. RabbitMQ 事务（AMQP Tx）** | 使用 RabbitMQ 的 Channel 事务，将 DB commit 与 MQ commit 联动 | 性能极差（吞吐量下降 10x+），不支持分布式事务两阶段提交 |
| **C. 分布式事务（Seata / Atomikos）** | 引入 XA 事务协调器 | 过重，与六边形架构"轻量、可替换"原则冲突 |
| **D. Kafka + Kafka Connect** | 使用 Debezium CDC 捕获 PostgreSQL binlog 自动发 Kafka | 引入 Kafka 运维负担，团队当前无 Kafka 运维经验 |

---

## 2. Decision / 决策

**采用方案 A：RabbitMQ 作为消息中间件，结合 Outbox 模式保证事务与消息的原子性。**

### 2.1 RabbitMQ 选型理由

| 对比维度 | RabbitMQ | Kafka | Redis Streams |
|----------|----------|-------|---------------|
| **协议支持** | AMQP 0.9.1（标准、成熟） | 自有协议 | 专有命令 |
| **消息模型** | 队列 + 路由（Exchange/Binding） | 分区日志（Topic/Partition） | 轻量 Stream |
| **延迟消息** | 原生支持（TTL + Dead Letter） | 需外部组件（如 Pulsar） | 不支持 |
| **运维复杂度** | 低（Docker 单容器即可运行） | 中（ZooKeeper/KRaft、多 Broker） | 低（已引入 Redis 做缓存） |
| **团队熟悉度** | 高（Spring AMQP 成熟封装） | 低 | 中 |
| **消息回溯** | 有限（需 Dead Letter Queue） | 强（持久化日志按时间保留） | 弱 |

**RabbitMQ 胜出原因**：
1. 项目消息场景以 **任务队列** 和 **事件通知** 为主，非日志流式处理，Kafka 的日志回溯能力非刚需。
2. 需要 **延迟重试**（AI 服务超时后 30s 重试），RabbitMQ TTL + DLX 原生支持。
3. Spring Boot 对 AMQP 的一等公民支持（`spring-boot-starter-amqp`），配置即开即用。
4. Docker Compose 部署仅需一个容器，与项目"本地一键启动"目标一致。

### 2.2 Outbox 模式实现

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Service                      │
│  ┌─────────────────┐         ┌──────────────────────────┐   │
│  │ @Transactional  │         │ OutboxMessageRepository │   │
│  │   jobRepo.save()│────┬────│   save(outboxMessage)   │   │
│  │   outboxRepo.save│    │    │   (同一事务内)          │   │
│  └─────────────────┘    │    └──────────────────────────┘   │
│                         │                                   │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              PostgreSQL (同一 ACID 事务)                ││
│  │  ┌──────────────┐      ┌────────────────────────────┐ ││
│  │  │ job_applications│     │        outbox_messages      │ ││
│  │  │  (业务数据)   │      │  id | exchange | routingKey │ ││
│  │  └──────────────┘      │      | payload | status     │ ││
│  │                         └────────────────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
│                         │                                   │
│                         │ 事务提交后                          │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │           OutboxRelayScheduler (独立线程)              ││
│  │  ① 轮询 `status = PENDING` 的 Outbox 记录               ││
│  │  ② 投递到 RabbitMQ Exchange                               ││
│  │  ③ 成功后 `status = SENT` / 失败则 `status = FAILED`     ││
│  │  ④ ShedLock 保证分布式环境下仅一个实例执行 Relay          ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 2.3 核心代码结构

```java
// domain 层 — 仅定义 Outbox 实体，无任何外部依赖
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
// app 层 — OutboxRelayScheduler，依赖 domain 的 Port 接口
@Component
@ShedLock(name = "outbox-relay", lockAtMostFor = "PT5M")
public class OutboxRelayScheduler {
    // 轮询 PENDING 消息，通过 MessagePublisherPort 发送
}
```

```java
// infrastructure 层 — RabbitMQ 具体实现
@Component
public class AiMessagePublisherAdapter implements MessagePublisherPort {
    private final RabbitTemplate rabbitTemplate;
    
    public void publish(String exchange, String routingKey, String payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }
}
```

### 2.4 消费端幂等设计

| 策略 | 实现 |
|------|------|
| **消息 ID 去重** | 消费者端维护 `processed_message_ids` 表（Redis Set / PG 表），以 `messageId` 为主键，重复投递时直接 ACK。 |
| **业务键幂等** | AI 评分结果以 `(resumeId, jobId)` 为唯一键，重复评分结果覆盖写入。 |

---

## 3. Consequences / 后果

### 3.1 Positive / 正面

| 收益 | 说明 |
|------|------|
| **Exactly-Once 语义（工程可接受范围）** | Outbox 保证消息至少发出一次，消费端幂等保证最多处理一次，合起来达到业务上的 Exactly-Once。 |
| **故障自愈** | Outbox Relay 失败的消息保留 `FAILED` 状态，支持人工介入重发或自动退避重试。 |
| **分布式锁防重复** | ShedLock + Redis 保证多实例部署时仅一个 Relay 工作，避免消息重复投递。 |
| **领域纯净** | `OutboxMessage` 定义在 `domain` 层，不依赖任何消息队列 SDK，六边形架构边界清晰。 |

### 3.2 Negative / 负面

| 成本 | 说明 |
|------|------|
| **消息延迟** | Outbox Relay 轮询间隔（当前 5s）引入端到端延迟，不适合实时性要求 < 1s 的场景。 |
| **数据库写入放大** | 每条异步消息额外写入 `outbox_messages` 表，高吞吐场景需评估清理策略。 |
| **Relay 单点瓶颈** | 所有消息通过一个调度器线程发出，极端并发下可能成为瓶颈（当前业务量远未触及）。 |
| **运维复杂度** | 需要同时维护 RabbitMQ（消息队列） + Redis（ShedLock） + PostgreSQL（Outbox 表）三者健康。 |

### 3.3 Risks / 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| Outbox 表无限膨胀 | **TTL 清理**：`OutboxRelayScheduler` 在成功发送后软删除，夜间定时任务物理清理 7 天前的 SENT 记录。 |
| Relay 线程崩溃导致消息积压 | **监控告警**：Prometheus 指标 `outbox_pending_count` > 100 时触发告警，Dashboard 可视化积压趋势。 |
| RabbitMQ 单节点故障 | **镜像队列**：生产环境启用 RabbitMQ Mirror Queue 或迁移至托管服务（AWS MQ / CloudAMQP）。 |
| AMQP 连接长期空闲被防火墙切断 | **心跳配置**：`spring.rabbitmq.requested-heartbeat=30` 保持连接活跃。 |

---

## 4. Compliance / 合规验证

- **集成测试**：`OutboxRelaySchedulerTest` 验证 Relay 正确轮询并更新状态。
- **幂等测试**：`AiResultMessageListenerTest` 模拟重复投递，验证消费者仅处理一次。
- **架构门禁**：`code-analyzer` 扫描 `app` 层，禁止直接注入 `RabbitTemplate`，必须通过 `MessagePublisherPort` 发送。
- **消息轨迹**：关键消息（AI 解析请求）在 `outbox_messages` 保留 7 天，支持审计追踪。

---

## 5. Related / 相关决策

- ADR-0001 — 六边形架构（`MessagePublisherPort` / `MessageListenerPort` 作为架构边界）
- ADR-0002 — PostgreSQL（Outbox 表与业务表共享同一数据库实例）
- ADR-0004 — Redis 用于分布式锁（ShedLock）与缓存

---

## 6. Notes / 备注

> Outbox 模式由 Chris Richardson 在《Microservices Patterns》中系统阐述，被 Eventuate、Debezium 等项目广泛采用。本项目的轻量级实现（自定义 Relay + ShedLock）在数据量 < 百万级时足够可靠，若未来扩展到 Event Sourcing 架构，可平滑迁移至 Debezium CDC 方案，无需修改 domain 层。
>
> **关键纪律**：严禁在 `@Transactional` 方法内直接调用 `RabbitTemplate.convertAndSend()` — 这是事务与消息非原子的经典反模式，每次 Code Review 中若发现直接扣分。

---

*End of ADR-0003*
