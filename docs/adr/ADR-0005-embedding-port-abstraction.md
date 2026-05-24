<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](ADR-0005-embedding-port-abstraction.md) | [简体中文](../i18n/zh-Hans-CN/adr/ADR-0005-embedding-port-abstraction.md) | [繁體中文](../i18n/zh-Hant-TW/adr/ADR-0005-embedding-port-abstraction.md)

# ADR-0005: Abstract Embedding Service as `EmbeddingPort` — Decoupling Vector Generation from Business Logic

| Attribute | Value |
|-----------|-------|
| **Status** | Accepted |
| **Date** | 2025-05 |
| **Deciders** | Backend Architecture Team |
| **Affected Modules** | `backend/domain/*/port/`, `backend/infrastructure/embedding/`, `backend/app/*/service/` |

---

## 1. Context

ResumeAssistant's core matching capability depends on **text embedding** — transforming resumes, job descriptions, and conversation contexts into high-dimensional vectors, then computing match scores via vector similarity.

### 1.1 Original Architecture Problem

In the early implementation, vector generation logic was directly coupled inside ApplicationService:

```java
// ❌ Incorrect example: vector generation deeply coupled with business logic
@Service
public class ResumeApplicationService {
    @Autowired private RestTemplate restTemplate;  // Direct HTTP client dependency
    
    @Transactional
    public void createResume(CreateResumeCommand cmd) {
        Resume resume = resumeFactory.create(cmd);
        resumeRepo.save(resume);
        
        // ① HTTP call inside transaction — long connection blocks DB connection pool
        float[] embedding = restTemplate.postForObject(
            "http://ai-service:8080/embed", cmd.getContent(), float[].class);
        
        // ② If HTTP times out, transaction rolls back, but AI service may have already processed (duplicate generation)
        vectorRepo.save(resume.getId(), embedding);
    }
}
```

This pattern has three concrete issues:

| Problem | Impact |
|---------|--------|
| **Transaction Boundary Pollution** | HTTP call inside `@Transactional` blocks database connections; connection pool exhaustion under high concurrency. |
| **Technology Coupling** | `RestTemplate` directly injected; switching to `WebClient`, `gRPC`, or internal model inference requires Service changes. |
| **Model Switching Difficulty** | Currently calls external AI service for embeddings; future migration to local ONNX models or Hugging Face Transformers requires modifying coupled code. |

### 1.2 Recent Refactoring Requirements

In May 2025, the user explicitly requested:

> 1. Fix the "vector generation nested transaction" problem
> 2. Extract `EmbeddingPort` for `EmbeddingService` / `IncrementalRetrainingScheduler`
> 3. Unify `ConversationApplicationService` / `MatchingApplicationService` to use `VectorGenerationPort`

---

## 2. Decision

**Abstract vector generation capability as the `EmbeddingPort` interface; infrastructure layer provides concrete implementations, application layer calls only through the Port, and direct vector generation inside `@Transactional` boundaries is strictly prohibited.**

### 2.1 Port Interface Design

```java
// domain/src/main/java/.../embedding/port/EmbeddingPort.java
public interface EmbeddingPort {
    /**
     * Convert text into vector embedding
     * 
     * @param text Input text
     * @param modelVersion Model version identifier, supporting coexistence of multi-version vectors
     * @return Normalized vector array
     */
    float[] embed(String text, String modelVersion);

    /**
     * Batch embedding — for incremental retraining scenarios
     */
    Map<String, float[]> embedBatch(List<String> texts, String modelVersion);

    /**
     * Get the current default model version
     */
    String getDefaultModelVersion();
}
```

### 2.2 Layered Architecture Responsibilities

```
┌─────────────────────────────────────────────────────────┐
│  api / trigger                                           │
│  REST Controller / Message Listener                     │
│  — Translate requests into Command / Event —            │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  app                                                      │
│  ApplicationService                                       │
│  ┌─────────────────────────────────────────────────────┐│
│  │  @Transactional                                      ││
│  │    ① Save business entity (Resume / Job / Conversation)││
│  │    ② Generate Outbox message: "embedding task pending" ││
│  │    ③ Commit transaction                                ││
│  └─────────────────────────────────────────────────────┘│
│  — Does NOT directly call EmbeddingPort —                │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  trigger / async worker                                   │
│  OutboxRelayScheduler / VectorGenerationScheduler       │
│  ┌─────────────────────────────────────────────────────┐│
│  │  ④ Consume Outbox message                              ││
│  │  ⑤ Call embeddingPort.embed(text, modelVersion)        ││
│  │  ⑥ Write result to vector storage (ResumeVectorRepository)││
│  └─────────────────────────────────────────────────────┘│
│  — Async, no transaction, retryable —                     │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  infrastructure                                           │
│  EmbeddingRestAdapter / EmbeddingLocalAdapter             │
│  — HTTP call / ONNX local inference / gRPC —            │
└─────────────────────────────────────────────────────────┘
```

### 2.3 Transaction Boundary Correction

**Old Pattern (Anti-pattern)**:
```java
@Transactional
public void createResume(cmd) {
    resumeRepo.save(resume);           // Inside DB transaction
    float[] vec = embeddingPort.embed(text);  // ① Long HTTP call — holds connection
    vectorRepo.save(vec);              // Inside DB transaction
}  // Total transaction duration = HTTP latency + DB write latency
```

**New Pattern (Correct)**:
```java
@Transactional
public void createResume(cmd) {
    Resume resume = resumeFactory.create(cmd);
    resumeRepo.save(resume);
    outboxRepo.save(OutboxMessage.createPending(
        "embedding.exchange", "embedding.routing", 
        new EmbeddingTask(resume.getId(), cmd.getContent()).toJson()
    ));
}  // Transaction contains only DB writes, < 50ms

// Async Worker (trigger module)
@ShedLock(name = "vector-generation", lockAtMostFor = "PT5M")
public void processEmbeddingTask(EmbeddingTask task) {
    float[] vec = embeddingPort.embed(task.getContent(), DEFAULT_MODEL);
    vectorRepo.save(task.getResumeId(), vec);  // Independent transaction, retryable
}
```

### 2.4 Adapter Implementation Reserve

| Implementation | Technology | Scenario |
|----------------|------------|----------|
| `EmbeddingRestAdapter` | HTTP → AI microservice | Current default implementation, calls external Embedding API |
| `EmbeddingLocalOnnxAdapter` | ONNX Runtime local inference | Future offline deployment, no external dependency scenario |
| `EmbeddingGrpcAdapter` | gRPC → AI microservice | Future high-performance internal communication scenario |

---

## 3. Consequences

### 3.1 Positive

| Benefit | Description |
|---------|-------------|
| **Slimmer Transactions** | Database transactions are no longer blocked by HTTP calls; connection pool turnover increases, system throughput improves significantly. |
| **Failure Isolation** | Embedding service timeout/outage does not affect main business writes; users can still create resumes, with vector generation asynchronously compensated. |
| **Transparent Model Switching** | Switching from OpenAI `text-embedding-3-small` to local `all-MiniLM-L6-v2` ONNX model only requires adding an Adapter; zero Service changes. |
| **Enhanced Observability** | Async Worker monitoring is independent (success rate, latency, retry count), decoupled from main business metrics. |
| **Batch Retraining** | `embedBatch()` supports `IncrementalRetrainingScheduler` efficiently processing historical data full refresh. |

### 3.2 Negative

| Cost | Description |
|------|-------------|
| **Eventual Consistency Delay** | Vectors are not immediately available after resume creation; Outbox Relay + Worker introduces end-to-end latency (default 5~15s). |
| **Architecture Complexity Increase** | Single vector generation now passes through: Command → Outbox → Relay → Worker → Port → Adapter → HTTP → DB, extending the chain. |
| **Idempotency Design Cost** | Async Workers may consume duplicate messages; `vectorRepo.save()` must be idempotent (UPSERT semantics). |
| **Debugging Difficulty** | End-to-end troubleshooting requires tracing: ApplicationService → Outbox table → Relay logs → Worker logs → AI service logs. |

### 3.3 Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Vector generation tasks continuously backlogged | **Monitoring Alert**: Worker consumption delay > 5min triggers alert; supports horizontal Worker instance scaling. |
| New/old model vector dimension mismatch causing search anomalies | **Model Version Isolation**: `EmbeddingPort.embed()` returns vectors with attached `modelVersion`; storage partitioned by version; cross-version search is prohibited. |
| Async Worker exceptions causing infinite retry | **Dead Letter Queue**: RabbitMQ configured with DLX; after > 3 failures, transfer to dead letter queue for manual intervention. |
| Local ONNX model output inconsistent with remote model | **Regression Test**: When adding a new Adapter, compare cosine similarity deviation using a standard text corpus; deviation < 0.001 considered equivalent. |

---

## 4. Compliance Verification

- **Static Gate**: `code-analyzer` scans the `app` layer, forbidding direct injection of `RestTemplate` / `WebClient` into ApplicationService; forbidding `EmbeddingPort.embed()` calls inside `@Transactional` methods.
- **Integration Tests**: `EmbeddingRestAdapterTest` uses `MockWebServer` to verify HTTP calls and timeout handling.
- **Contract Tests**: Each `EmbeddingPort` implementation must pass a unified contract test suite (fixed input → fixed output dimension/range).
- **Performance Baseline**: `embed()` single text P99 < 2s; `embedBatch(100)` P99 < 10s.

---

## 5. Related Decisions

- ADR-0001 — Hexagonal Architecture (`EmbeddingPort` as standard Driven Port practice)
- ADR-0002 — PostgreSQL + pgvector (Vector storage implementation layer)
- ADR-0003 — RabbitMQ + Outbox (Reliable delivery mechanism for async vector generation tasks)
- ADR-0004 — Redis (Short-lived cache for `embedBatch` results and Worker deduplication)

---

## 6. Notes

> MVP阶段把HTTP调用直接写在Service里，是为了快速验证业务假设。现在进入工程阶段，非确定性延迟（网络I/O）必须移出事务边界。
>
> **Code Review 红线**：任何在 `ApplicationService` 中直接注入 `RestTemplate`、`WebClient`、`RabbitTemplate` 或 `RedisTemplate` 的代码，直接否决。所有外部调用走 Port。

---

*End of ADR-0005*