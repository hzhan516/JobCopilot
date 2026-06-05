<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../adr/ADR-0003-rabbitmq-outbox.md) | [简体中文](../../zh-Hans-CN/adr/ADR-0003-rabbitmq-outbox.md) | [繁體中文](ADR-0003-rabbitmq-outbox.md)

# ADR-0003: 採用 RabbitMQ + Outbox 模式實現異步消息通信

| 屬性 | 內容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | 後端架構團隊 |
| **Affected Modules** | `backend/infrastructure/messaging/`, `backend/domain/shared/entity/OutboxMessage.java`, `backend/app/shared/scheduler/OutboxRelayScheduler.java` |

---

## 1. Context / 背景

ResumeAssistant 存在大量 **異步处理場景**，需要可靠的消息傳遞機制：

| 場景 | 触發方 | 消費方 | 可靠性要求 |
|------|--------|--------|------------|
| AI 簡歷解析请求 | `JobApplicationService` | AI 微服務 | 至少一次投遞（At-Least-Once） |
| AI 評分結果回調 | AI 微服務 | `AiResultMessageListener` | 幂等消費（Duplicate-Safe） |
| 對話上下文流式更新 | `ConversationApplicationService` | WebSocket / 前端 | 允許丢失，低延迟優先 |
| 定時任務調度（簡歷增量重训練） | `IncrementalRetrainingScheduler` | 內部 Worker | 分布式鎖防重復執行 |

### 1.1 核心挑戰：資料庫事務與消息發送的原子性

典型的反模式：

```java
// ❌ 错誤示例：事務與消息非原子
@Transactional
public void applyForJob(JobApplicationCommand cmd) {
    jobRepo.save(application);          // ① 資料庫事務內
    rabbitTemplate.convertAndSend(...); // ② 事務外 — 若 ① 成功 ② 失败，消息丢失
}
```

若應用服務在 `rabbitTemplate.convertAndSend()` 後崩溃，資料庫已提交但消息未發出，導致 AI 服務永遠收不到解析请求。

### 1.2 候選方案

| 方案 | 說明 | 評估 |
|------|------|------|
| **A. RabbitMQ + Outbox 模式** | 將消息寫入業務資料庫的 `outbox` 表，與業務資料同一事務；由獨立 Relay 行程異步讀取並投遞到 RabbitMQ | 實現複雜度中等，可靠性最高，業界標準方案（Debezium / 自定義 Relay） |
| **B. RabbitMQ 事務（AMQP Tx）** | 使用 RabbitMQ 的 Channel 事務，將 DB commit 與 MQ commit 聯動 | 性能極差（吞吐量下降 10x+），不支持分布式事務兩階段提交 |
| **C. 分布式事務（Seata / Atomikos）** | 引入 XA 事務協調器 | 過重，與六边形架構"轻量、可替換"原則衝突 |
| **D. Kafka + Kafka Connect** | 使用 Debezium CDC 捕获 PostgreSQL binlog 自動發 Kafka | 引入 Kafka 運維負擔，團隊當前無 Kafka 運維經驗 |

---

## 2. Decision / 決策

**採用方案 A：RabbitMQ 作為消息中間件，結合 Outbox 模式保證事務與消息的原子性。**

### 2.1 RabbitMQ 選型理由

| 對比維度 | RabbitMQ | Kafka | Redis Streams |
|----------|----------|-------|---------------|
| **協定支持** | AMQP 0.9.1（標準、成熟） | 自有協定 | 专有命令 |
| **消息模型** | 佇列 + 路由（Exchange/Binding） | 分區日誌（Topic/Partition） | 轻量 Stream |
| **延迟消息** | 原生支持（TTL + Dead Letter） | 需外部組件（如 Pulsar） | 不支持 |
| **運維複雜度** | 低（Docker 單容器即可運行） | 中（ZooKeeper/KRaft、多 Broker） | 低（已引入 Redis 做缓存） |
| **團隊熟悉度** | 高（Spring AMQP 成熟封裝） | 低 | 中 |
| **消息回溯** | 有限（需 Dead Letter Queue） | 強（持久化日誌按時間保留） | 弱 |

**RabbitMQ 胜出原因**：
1. 項目消息場景以 **任務佇列** 和 **事件通知** 為主，非日誌流式处理，Kafka 的日誌回溯能力非刚需。
2. 需要 **延迟重試**（AI 服務超時後 30s 重試），RabbitMQ TTL + DLX 原生支持。
3. Spring Boot 對 AMQP 的一等公民支持（`spring-boot-starter-amqp`），設定即開即用。
4. Docker Compose 部署仅需一个容器，與項目"本地一键啟動"目標一致。

### 2.2 Outbox 模式實現

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Service                      │
│  ┌─────────────────┐         ┌──────────────────────────┐   │
│  │ @Transactional  │         │ OutboxMessageRepository │   │
│  │   jobRepo.save()│────┬────│   save(outboxMessage)   │   │
│  │   outboxRepo.save│    │    │   (同一事務內)          │   │
│  └─────────────────┘    │    └──────────────────────────┘   │
│                         │                                   │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              PostgreSQL (同一 ACID 事務)                ││
│  │  ┌──────────────┐      ┌────────────────────────────┐ ││
│  │  │ job_applications│     │        outbox_messages      │ ││
│  │  │  (業務資料)   │      │  id | exchange | routingKey │ ││
│  │  └──────────────┘      │      | payload | status     │ ││
│  │                         └────────────────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
│                         │                                   │
│                         │ 事務提交後                          │
│                         ▼                                   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │           OutboxRelayScheduler (獨立執行緒)              ││
│  │  ① 輪询 `status = PENDING` 的 Outbox 記錄               ││
│  │  ② 投遞到 RabbitMQ Exchange                               ││
│  │  ③ 成功後 `status = SENT` / 失败則 `status = FAILED`     ││
│  │  ④ ShedLock 保證分布式环境下仅一个实例執行 Relay          ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 2.3 核心程式碼結構

```java
// domain 層 — 仅定義 Outbox 实體，無任何外部依賴
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
// app 層 — OutboxRelayScheduler，依賴 domain 的 Port 介面
@Component
@ShedLock(name = "outbox-relay", lockAtMostFor = "PT5M")
public class OutboxRelayScheduler {
    // 輪询 PENDING 消息，通過 MessagePublisherPort 發送
}
```

```java
// infrastructure 層 — RabbitMQ 具體實現
@Component
public class AiMessagePublisherAdapter implements MessagePublisherPort {
    private final RabbitTemplate rabbitTemplate;
    
    public void publish(String exchange, String routingKey, String payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }
}
```

### 2.4 消費端幂等設计

| 策略 | 實現 |
|------|------|
| **消息 ID 去重** | 消費者端維护 `processed_message_ids` 表（Redis Set / PG 表），以 `messageId` 為主键，重復投遞時直接 ACK。 |
| **業務键幂等** | AI 評分結果以 `(resumeId, jobId)` 為唯一键，重復評分結果覆蓋寫入。 |

---

## 3. Consequences / 後果

### 3.1 Positive / 正面

| 收益 | 說明 |
|------|------|
| **Exactly-Once 語義（工程可接受範围）** | Outbox 保證消息至少發出一次，消費端幂等保證最多处理一次，合起來達到業務上的 Exactly-Once。 |
| **故障自愈** | Outbox Relay 失败的消息保留 `FAILED` 狀态，支持人工介入重發或自動退避重試。 |
| **分布式鎖防重復** | ShedLock + Redis 保證多实例部署時仅一个 Relay 工作，避免消息重復投遞。 |
| **領域純净** | `OutboxMessage` 定義在 `domain` 層，不依賴任何消息佇列 SDK，六边形架構边界清晰。 |

### 3.2 Negative / 負面

| 成本 | 說明 |
|------|------|
| **消息延迟** | Outbox Relay 輪询間隔（當前 5s）引入端到端延迟，不適合实時性要求 < 1s 的場景。 |
| **資料庫寫入放大** | 每條異步消息額外寫入 `outbox_messages` 表，高吞吐場景需評估清理策略。 |
| **Relay 單點瓶颈** | 所有消息通過一个調度器執行緒發出，極端並發下可能成為瓶颈（當前業務量遠未触及）。 |
| **運維複雜度** | 需要同時維护 RabbitMQ（消息佇列） + Redis（ShedLock） + PostgreSQL（Outbox 表）三者健康。 |

### 3.3 Risks / 風險與缓解

| 風險 | 缓解措施 |
|------|----------|
| Outbox 表無限膨胀 | **TTL 清理**：`OutboxRelayScheduler` 在成功發送後軟刪除，夜間定時任務物理清理 7 天前的 SENT 記錄。 |
| Relay 執行緒崩溃導致消息積壓 | **监控告警**：Prometheus 指標 `outbox_pending_count` > 100 時触發告警，Dashboard 可視化積壓趨勢。 |
| RabbitMQ 單節點故障 | **镜像佇列**：生產环境啟用 RabbitMQ Mirror Queue 或迁移至託管服務（AWS MQ / CloudAMQP）。 |
| AMQP 連接長期空閒被防火墙切斷 | **心跳設定**：`spring.rabbitmq.requested-heartbeat=30` 保持連接活跃。 |

---

## 4. Compliance / 合規驗證

- **整合測試**：`OutboxRelaySchedulerTest` 驗證 Relay 正確輪询並更新狀态。
- **幂等測試**：`AiResultMessageListenerTest` 模擬重復投遞，驗證消費者仅处理一次。
- **架構门禁**：`code-analyzer` 掃描 `app` 層，禁止直接注入 `RabbitTemplate`，必須通過 `MessagePublisherPort` 發送。
- **消息軌迹**：關键消息（AI 解析请求）在 `outbox_messages` 保留 7 天，支持審计追踪。

---

## 5. Related / 相關決策

- ADR-0001 — 六边形架構（`MessagePublisherPort` / `MessageListenerPort` 作為架構边界）
- ADR-0002 — PostgreSQL（Outbox 表與業務表共享同一資料庫实例）
- ADR-0004 — Redis 用於分布式鎖（ShedLock）與缓存

---

## 6. Notes / 備注

> Outbox 模式由 Chris Richardson 在《Microservices Patterns》中係統阐述，被 Eventuate、Debezium 等項目广泛採用。本項目的轻量級實現（自定義 Relay + ShedLock）在資料量 < 百萬級時足夠可靠，若未來擴展到 Event Sourcing 架構，可平滑迁移至 Debezium CDC 方案，無需修改 domain 層。
>
> **關键紀律**：嚴禁在 `@Transactional` 方法內直接呼叫 `RabbitTemplate.convertAndSend()` — 这是事務與消息非原子的經典反模式，每次 Code Review 中若發現直接扣分。

---

*End of ADR-0003*
