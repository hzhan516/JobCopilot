<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../adr/ADR-0005-embedding-port-abstraction.md) | [简体中文](../../zh-Hans-CN/adr/ADR-0005-embedding-port-abstraction.md) | [繁體中文](ADR-0005-embedding-port-abstraction.md)

# ADR-0005: Embedding 服務抽象為 `EmbeddingPort` — 向量生成與業務逻辑解耦

| 屬性 | 內容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-05 |
| **Deciders** | 後端架構團隊 |
| **Affected Modules** | `backend/domain/*/port/`, `backend/infrastructure/embedding/`, `backend/app/*/service/` |

---

## 1. Context / 背景

JobCopilot 的核心匹配能力依賴 **文本嵌入（Text Embedding）** — 將簡歷、职位描述、對話上下文轉化為高維向量，通過向量相似度计算匹配分數。

### 1.1 原始架構問題

早期實現中，向量生成逻辑直接耦合在 ApplicationService 內部：

```java
// ❌ 错誤示例：向量生成與業務逻辑深度耦合
@Service
public class ResumeApplicationService {
    @Autowired private RestTemplate restTemplate;  // 直接依賴 HTTP 客戶端
    
    @Transactional
    public void createResume(CreateResumeCommand cmd) {
        Resume resume = resumeFactory.create(cmd);
        resumeRepo.save(resume);
        
        // ① HTTP 呼叫在事務內 — 長連接阻塞資料庫連接池
        float[] embedding = restTemplate.postForObject(
            "http://ai-service:8000/api/v1/ai/embeddings", request, EmbeddingResponse.class);
        
        // ② 若 HTTP 超時，事務回滚，但 AI 服務可能已实際处理（重復生成）
        vectorRepo.save(resume.getId(), embedding);
    }
}
```

該模式存在三重問題：

| 問題 | 影响 |
|------|------|
| **事務边界污染** | HTTP 呼叫位於 `@Transactional` 內，阻塞資料庫連接，高並發時連接池耗盡。 |
| **技術耦合** | `RestTemplate` 直接注入，切換為 `WebClient`、`gRPC` 或內部模型推理時需改動 Service。 |
| **模型切換困難** | 當前呼叫外部 AI 服務生成 embedding，未來可能切換為本地 ONNX 模型或 Hugging Face Transformers，耦合程式碼無法平滑迁移。 |

### 1.2 近期重構需求

用戶在 2025-05 明確提出：

> 1. 修復"向量生成的嵌套事務"問題
> 2. 為 `EmbeddingService` / `IncrementalRetrainingScheduler` 提取 `EmbeddingPort`
> 3. 將 `ConversationApplicationService` / `MatchingApplicationService` 統一改為 `VectorGenerationPort`

---

## 2. Decision / 決策

**將向量生成能力抽象為 `EmbeddingPort` 介面，由基礎設施層提供具體實現，應用層仅通過 Port 呼叫，嚴禁在 `@Transactional` 边界內直接執行向量生成。**

### 2.1 Port 介面設计

```java
// domain/src/main/java/.../embedding/port/EmbeddingPort.java
public interface EmbeddingPort {
    /**
     * 將文本轉換為向量嵌入
     * Convert text into vector embedding
     * 
     * @param text 输入文本
     * @param modelVersion 模型版本標識，支持多版本向量共存
     * @return 歸一化後的向量陣列
     */
    float[] embed(String text, String modelVersion);

    /**
     * 批量嵌入 — 用於增量重训練場景
     * Batch embedding for incremental retraining
     */
    Map<String, float[]> embedBatch(List<String> texts, String modelVersion);

    /**
     * 获取當前預設模型版本
     * Get the current default model version
     */
    String getDefaultModelVersion();
}
```

### 2.2 架構分層职責

```
┌─────────────────────────────────────────────────────────┐
│  api / trigger                                           │
│  REST Controller / Message Listener                     │
│  ── 將请求翻譯為 Command / Event ──                      │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  app                                                      │
│  ApplicationService                                       │
│  ┌─────────────────────────────────────────────────────┐│
│  │  @Transactional                                      ││
│  │    ① 保存業務实體（Resume / Job / Conversation）       ││
│  │    ② 生成 Outbox 消息："向量生成待執行"               ││
│  │    ③ 事務提交                                          ││
│  └─────────────────────────────────────────────────────┘│
│  ── 不直接呼叫 EmbeddingPort ──                           │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  trigger / async worker                                   │
│  OutboxRelayScheduler / VectorGenerationScheduler         │
│  ┌─────────────────────────────────────────────────────┐│
│  │  ④ 消費 Outbox 消息                                    ││
│  │  ⑤ 呼叫 embeddingPort.embed(text, modelVersion)      ││
│  │  ⑥ 將結果寫入向量存儲（ResumeVectorRepository）       ││
│  └─────────────────────────────────────────────────────┘│
│  ── 異步、無事務、可重試 ──                               │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  infrastructure                                           │
│  EmbeddingRestAdapter / EmbeddingLocalAdapter             │
│  ── HTTP 呼叫 / ONNX 本地推理 / gRPC ──                   │
└─────────────────────────────────────────────────────────┘
```

### 2.3 事務边界修正

**舊模式（反模式）**：
```java
@Transactional
public void createResume(cmd) {
    resumeRepo.save(resume);           // DB 事務內
    float[] vec = embeddingPort.embed(text);  // ① 長耗時 HTTP 呼叫 — 占用連接
    vectorRepo.save(vec);              // DB 事務內
}  // 事務總時長 = HTTP 延迟 + DB 寫入延迟
```

**新模式（正確）**：
```java
@Transactional
public void createResume(cmd) {
    Resume resume = resumeFactory.create(cmd);
    resumeRepo.save(resume);
    outboxRepo.save(OutboxMessage.createPending(
        "embedding.exchange", "embedding.routing", 
        new EmbeddingTask(resume.getId(), cmd.getContent()).toJson()
    ));
}  // 事務仅包含 DB 寫入，< 50ms

// 異步 Worker（trigger 模塊）
@ShedLock(name = "vector-generation", lockAtMostFor = "PT5M")
public void processEmbeddingTask(EmbeddingTask task) {
    float[] vec = embeddingPort.embed(task.getContent(), DEFAULT_MODEL);
    vectorRepo.save(task.getResumeId(), vec);  // 獨立事務，可重試
}
```

### 2.4 Adapter 實現預留

| 實現類 | 技術 | 場景 |
|--------|------|------|
| `EmbeddingRestAdapter` | HTTP → AI 微服務 | 當前預設實現，呼叫外部 Embedding API |
| `EmbeddingLocalOnnxAdapter` | ONNX Runtime 本地推理 | 未來離線部署、無外部依賴場景 |
| `EmbeddingGrpcAdapter` | gRPC → AI 微服務 | 未來高性能內部通信場景 |

---

## 3. Consequences / 後果

### 3.1 Positive / 正面

| 收益 | 說明 |
|------|------|
| **事務瘦身** | 資料庫事務不再被 HTTP 呼叫阻塞，連接池周轉率提升，係統吞吐量顯著增加。 |
| **故障隔離** | Embedding 服務超時/宕機不影响主業務寫入，用戶仍可建立簡歷，向量生成異步補偿。 |
| **模型透明切換** | 从 OpenAI `text-embedding-3-small` 切換到本地 `all-MiniLM-L6-v2` ONNX 模型，只需新增 Adapter，零改動 Service。 |
| **可觀測性增強** | 異步 Worker 獨立监控（成功率、延迟、重試次數），與主業務指標解耦。 |
| **批量重训練** | `embedBatch()` 支持 `IncrementalRetrainingScheduler` 高效处理歷史資料全量刷新。 |

### 3.2 Negative / 負面

| 成本 | 說明 |
|------|------|
| **最終一致性延迟** | 簡歷建立後向量不會立即可用，Outbox Relay + Worker 引入端到端延迟（預設 5~15s）。 |
| **架構複雜度提升** | 單次向量生成需經過：Command → Outbox → Relay → Worker → Port → Adapter → HTTP → DB，链路增長。 |
| **幂等設计成本** | 異步 Worker 可能重復消費同一消息，需確保 `vectorRepo.save()` 具備幂等性（UPSERT 語義）。 |
| **調試困難** | 端到端問題排查需追踪：ApplicationService → Outbox 表 → Relay 日誌 → Worker 日誌 → AI 服務日誌。 |

### 3.3 Risks / 風險與缓解

| 風險 | 缓解措施 |
|------|----------|
| 向量生成任務持續積壓 | **监控告警**：Worker 消費延迟 > 5min 時触發告警；支持水平擴展 Worker 实例。 |
| 新舊模型向量維度不匹配導致搜尋例外 | **模型版本隔離**：`EmbeddingPort.embed()` 返回向量附帶 `modelVersion`，存儲時按版本分區，禁止跨版本搜尋。 |
| 異步 Worker 丟出例外導致無限重試 | **死信佇列**：RabbitMQ 設定 DLX，失败 > 3 次後轉入死信佇列，人工介入处理。 |
| 本地 ONNX 模型與遠程模型输出不一致 | **回歸測試**：每次新增 Adapter 時，使用標準文本集對比餘弦相似度偏差，< 0.001 視為等價。 |

---

## 4. Compliance / 合規驗證

- **靜态门禁**：`code-analyzer` 掃描 `app` 層，禁止 `RestTemplate` / `WebClient` 直接注入 ApplicationService；禁止 `@Transactional` 方法內呼叫 `EmbeddingPort.embed()`。
- **整合測試**：`EmbeddingRestAdapterTest` 使用 `MockWebServer` 驗證 HTTP 呼叫與超時处理。
- **契約測試**：`EmbeddingPort` 的每種實現需通過統一的契約測試套件（固定输入 → 固定输出維度/範围）。
- **性能基線**：`embed()` 單文本 P99 < 2s，`embedBatch(100)` P99 < 10s。

---

## 5. Related / 相關決策

- ADR-0001 — 六边形架構（`EmbeddingPort` 作為 Driven Port 的標準实踐）
- ADR-0002 — PostgreSQL + pgvector（向量存儲的實現層）
- ADR-0003 — RabbitMQ + Outbox（異步向量生成任務的可靠投遞機制）
- ADR-0004 — Redis（`embedBatch` 結果的短暫缓存與 Worker 去重）

---

## 6. Notes / 備注

> MVP階段把HTTP呼叫直接寫在Service裡，是為了快速驗證業務假設。現在進入工程階段，非確定性延遲（網路 I/O）必須移出事務邊界。
>
> **Code Review 紅線**：任何在 `ApplicationService` 中直接注入 `RestTemplate`、`WebClient`、`RabbitTemplate` 或 `RedisTemplate` 的程式碼，直接否決。所有外部呼叫走 Port。

---

*End of ADR-0005*
