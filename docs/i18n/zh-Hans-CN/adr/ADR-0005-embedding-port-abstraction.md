<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../adr/ADR-0005-embedding-port-abstraction.md) | [简体中文](ADR-0005-embedding-port-abstraction.md) | [繁體中文](../../zh-Hant-TW/adr/ADR-0005-embedding-port-abstraction.md)

# ADR-0005: Embedding 服务抽象为 `EmbeddingPort` — 向量生成与业务逻辑解耦

| 属性 | 内容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-05 |
| **Deciders** | 后端架构团队 |
| **Affected Modules** | `backend/domain/*/port/`, `backend/infrastructure/embedding/`, `backend/app/*/service/` |

---

## 1. Context / 背景

JobCopilot 的核心匹配能力依赖 **文本嵌入（Text Embedding）** — 将简历、职位描述、对话上下文转化为高维向量，通过向量相似度计算匹配分数。

### 1.1 原始架构问题

早期实现中，向量生成逻辑直接耦合在 ApplicationService 内部：

```java
// ❌ 错误示例：向量生成与业务逻辑深度耦合
@Service
public class ResumeApplicationService {
    @Autowired private RestTemplate restTemplate;  // 直接依赖 HTTP 客户端
    
    @Transactional
    public void createResume(CreateResumeCommand cmd) {
        Resume resume = resumeFactory.create(cmd);
        resumeRepo.save(resume);
        
        // ① HTTP 调用在事务内 — 长连接阻塞数据库连接池
        float[] embedding = restTemplate.postForObject(
            "http://ai-service:8000/api/v1/ai/embeddings", request, EmbeddingResponse.class);
        
        // ② 若 HTTP 超时，事务回滚，但 AI 服务可能已实际处理（重复生成）
        vectorRepo.save(resume.getId(), embedding);
    }
}
```

该模式存在三重问题：

| 问题 | 影响 |
|------|------|
| **事务边界污染** | HTTP 调用位于 `@Transactional` 内，阻塞数据库连接，高并发时连接池耗尽。 |
| **技术耦合** | `RestTemplate` 直接注入，切换为 `WebClient`、`gRPC` 或内部模型推理时需改动 Service。 |
| **模型切换困难** | 当前调用外部 AI 服务生成 embedding，未来可能切换为本地 ONNX 模型或 Hugging Face Transformers，耦合代码无法平滑迁移。 |

### 1.2 近期重构需求

用户在 2025-05 明确提出：

> 1. 修复"向量生成的嵌套事务"问题
> 2. 为 `EmbeddingService` / `IncrementalRetrainingScheduler` 提取 `EmbeddingPort`
> 3. 将 `ConversationApplicationService` / `MatchingApplicationService` 统一改为 `VectorGenerationPort`

---

## 2. Decision / 决策

**将向量生成能力抽象为 `EmbeddingPort` 接口，由基础设施层提供具体实现，应用层仅通过 Port 调用，严禁在 `@Transactional` 边界内直接执行向量生成。**

### 2.1 Port 接口设计

```java
// domain/src/main/java/.../embedding/port/EmbeddingPort.java
public interface EmbeddingPort {
    /**
     * 将文本转换为向量嵌入
     * Convert text into vector embedding
     * 
     * @param text 输入文本
     * @param modelVersion 模型版本标识，支持多版本向量共存
     * @return 归一化后的向量数组
     */
    float[] embed(String text, String modelVersion);

    /**
     * 批量嵌入 — 用于增量重训练场景
     * Batch embedding for incremental retraining
     */
    Map<String, float[]> embedBatch(List<String> texts, String modelVersion);

    /**
     * 获取当前默认模型版本
     * Get the current default model version
     */
    String getDefaultModelVersion();
}
```

### 2.2 架构分层职责

```
┌─────────────────────────────────────────────────────────┐
│  api / trigger                                           │
│  REST Controller / Message Listener                     │
│  ── 将请求翻译为 Command / Event ──                      │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  app                                                      │
│  ApplicationService                                       │
│  ┌─────────────────────────────────────────────────────┐│
│  │  @Transactional                                      ││
│  │    ① 保存业务实体（Resume / Job / Conversation）       ││
│  │    ② 生成 Outbox 消息："向量生成待执行"               ││
│  │    ③ 事务提交                                          ││
│  └─────────────────────────────────────────────────────┘│
│  ── 不直接调用 EmbeddingPort ──                           │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  trigger / async worker                                   │
│  OutboxRelayScheduler / VectorGenerationScheduler         │
│  ┌─────────────────────────────────────────────────────┐│
│  │  ④ 消费 Outbox 消息                                    ││
│  │  ⑤ 调用 embeddingPort.embed(text, modelVersion)      ││
│  │  ⑥ 将结果写入向量存储（ResumeVectorRepository）       ││
│  └─────────────────────────────────────────────────────┘│
│  ── 异步、无事务、可重试 ──                               │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  infrastructure                                           │
│  EmbeddingRestAdapter / EmbeddingLocalAdapter             │
│  ── HTTP 调用 / ONNX 本地推理 / gRPC ──                   │
└─────────────────────────────────────────────────────────┘
```

### 2.3 事务边界修正

**旧模式（反模式）**：
```java
@Transactional
public void createResume(cmd) {
    resumeRepo.save(resume);           // DB 事务内
    float[] vec = embeddingPort.embed(text);  // ① 长耗时 HTTP 调用 — 占用连接
    vectorRepo.save(vec);              // DB 事务内
}  // 事务总时长 = HTTP 延迟 + DB 写入延迟
```

**新模式（正确）**：
```java
@Transactional
public void createResume(cmd) {
    Resume resume = resumeFactory.create(cmd);
    resumeRepo.save(resume);
    outboxRepo.save(OutboxMessage.createPending(
        "embedding.exchange", "embedding.routing", 
        new EmbeddingTask(resume.getId(), cmd.getContent()).toJson()
    ));
}  // 事务仅包含 DB 写入，< 50ms

// 异步 Worker（trigger 模块）
@ShedLock(name = "vector-generation", lockAtMostFor = "PT5M")
public void processEmbeddingTask(EmbeddingTask task) {
    float[] vec = embeddingPort.embed(task.getContent(), DEFAULT_MODEL);
    vectorRepo.save(task.getResumeId(), vec);  // 独立事务，可重试
}
```

### 2.4 Adapter 实现预留

| 实现类 | 技术 | 场景 |
|--------|------|------|
| `EmbeddingRestAdapter` | HTTP → AI 微服务 | 当前默认实现，调用外部 Embedding API |
| `EmbeddingLocalOnnxAdapter` | ONNX Runtime 本地推理 | 未来离线部署、无外部依赖场景 |
| `EmbeddingGrpcAdapter` | gRPC → AI 微服务 | 未来高性能内部通信场景 |

---

## 3. Consequences / 后果

### 3.1 Positive / 正面

| 收益 | 说明 |
|------|------|
| **事务瘦身** | 数据库事务不再被 HTTP 调用阻塞，连接池周转率提升，系统吞吐量显著增加。 |
| **故障隔离** | Embedding 服务超时/宕机不影响主业务写入，用户仍可创建简历，向量生成异步补偿。 |
| **模型透明切换** | 从 OpenAI `text-embedding-3-small` 切换到本地 `all-MiniLM-L6-v2` ONNX 模型，只需新增 Adapter，零改动 Service。 |
| **可观测性增强** | 异步 Worker 独立监控（成功率、延迟、重试次数），与主业务指标解耦。 |
| **批量重训练** | `embedBatch()` 支持 `IncrementalRetrainingScheduler` 高效处理历史数据全量刷新。 |

### 3.2 Negative / 负面

| 成本 | 说明 |
|------|------|
| **最终一致性延迟** | 简历创建后向量不会立即可用，Outbox Relay + Worker 引入端到端延迟（默认 5~15s）。 |
| **架构复杂度提升** | 单次向量生成需经过：Command → Outbox → Relay → Worker → Port → Adapter → HTTP → DB，链路增长。 |
| **幂等设计成本** | 异步 Worker 可能重复消费同一消息，需确保 `vectorRepo.save()` 具备幂等性（UPSERT 语义）。 |
| **调试困难** | 端到端问题排查需追踪：ApplicationService → Outbox 表 → Relay 日志 → Worker 日志 → AI 服务日志。 |

### 3.3 Risks / 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| 向量生成任务持续积压 | **监控告警**：Worker 消费延迟 > 5min 时触发告警；支持水平扩展 Worker 实例。 |
| 新旧模型向量维度不匹配导致搜索异常 | **模型版本隔离**：`EmbeddingPort.embed()` 返回向量附带 `modelVersion`，存储时按版本分区，禁止跨版本搜索。 |
| 异步 Worker 抛出异常导致无限重试 | **死信队列**：RabbitMQ 配置 DLX，失败 > 3 次后转入死信队列，人工介入处理。 |
| 本地 ONNX 模型与远程模型输出不一致 | **回归测试**：每次新增 Adapter 时，使用标准文本集对比余弦相似度偏差，< 0.001 视为等价。 |

---

## 4. Compliance / 合规验证

- **静态门禁**：`code-analyzer` 扫描 `app` 层，禁止 `RestTemplate` / `WebClient` 直接注入 ApplicationService；禁止 `@Transactional` 方法内调用 `EmbeddingPort.embed()`。
- **集成测试**：`EmbeddingRestAdapterTest` 使用 `MockWebServer` 验证 HTTP 调用与超时处理。
- **契约测试**：`EmbeddingPort` 的每种实现需通过统一的契约测试套件（固定输入 → 固定输出维度/范围）。
- **性能基线**：`embed()` 单文本 P99 < 2s，`embedBatch(100)` P99 < 10s。

---

## 5. Related / 相关决策

- ADR-0001 — 六边形架构（`EmbeddingPort` 作为 Driven Port 的标准实践）
- ADR-0002 — PostgreSQL + pgvector（向量存储的实现层）
- ADR-0003 — RabbitMQ + Outbox（异步向量生成任务的可靠投递机制）
- ADR-0004 — Redis（`embedBatch` 结果的短暂缓存与 Worker 去重）

---

## 6. Notes / 备注

> MVP阶段把HTTP调用直接写在Service里，是为了快速验证业务假设。现在进入工程阶段，非确定性延迟（网络I/O）必须移出事务边界。
>
> **Code Review 红线**：任何在 `ApplicationService` 中直接注入 `RestTemplate`、`WebClient`、`RabbitTemplate` 或 `RedisTemplate` 的代码，直接否决。所有外部调用走 Port。

---

*End of ADR-0005*
