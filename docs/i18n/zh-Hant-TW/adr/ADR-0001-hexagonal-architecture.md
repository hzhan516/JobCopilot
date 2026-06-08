<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../adr/ADR-0001-hexagonal-architecture.md) | [简体中文](../../zh-Hans-CN/adr/ADR-0001-hexagonal-architecture.md) | [繁體中文](ADR-0001-hexagonal-architecture.md)

# ADR-0001: 採用六边形架構（Hexagonal Architecture）作為後端核心架構範式

| 屬性 | 內容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | 後端架構團隊 |
| **Affected Modules** | `backend/domain`, `backend/app`, `backend/infrastructure`, `backend/api`, `backend/trigger` |

---

## 1. Context / 背景

JobCopilot 項目需要同時支撑以下長期演進目標：

1. **AI 服務解耦** — 簡歷解析、职位匹配、對話生成等核心能力依賴外部 LLM 服務，外部提供商可能更換（OpenAI → Claude → 自託管）。
2. **存儲介質可替換** — 當前使用 PostgreSQL + pgvector 存儲向量，未來可能引入专用向量資料庫（如 Milvus、Pinecone）或物件存儲。
3. **多交付渠道** — REST API（當前）、未來可能擴展為 WebSocket 实時推送、消息佇列異步触發、甚至 gRPC 內部通信。
4. **測試可獨立性** — 核心業務逻辑必須能在不啟動資料庫、消息佇列、外部 HTTP 服務的情況下完成單元測試。

傳統的 **分層架構（Layered Architecture）** 將業務逻辑埋在 `service` 層，基礎設施（資料庫、HTTP 客戶端、消息發送）通過 `@Autowired` 直接注入，導致：
- 業務程式碼與 Spring Data JPA、RestTemplate、RabbitMQ Template 深度耦合；
- 任何基礎設施替換都需要改動 `service` 層；
- 單元測試需要 `@SpringBootTest` 啟動完整上下文，慢且脆弱。

---

## 2. Decision / 決策

**採用六边形架構（Hexagonal Architecture / Ports & Adapters）作為後端唯一架構範式。**

具體實現通過 Maven 多模塊物理隔離：

```
backend/
├── domain/          ← 核心業務領域：Entity、ValueObject、DomainService、Repository Port（介面）
├── app/             ← 應用層：ApplicationService、事務边界、用例编排
├── infrastructure/  ← 基礎設施適配器：JPA Repository 實現、RabbitMQ 發送器、REST 客戶端、Redis 缓存
├── api/             ← 入站適配器：REST Controller、DTO、请求校驗
└── trigger/         ← 入站適配器：消息监听器、定時任務触發器
```

### 2.1 依賴規則（Dependency Rule）

```
      api / trigger         ← 入站適配器（Driving Adapters）
           ↓
         app                 ← 應用層（Orchestration + Transaction）
           ↓
        domain               ← 領域層（Business Logic — 無外部依賴）
           ↑
    infrastructure           ← 出站適配器（Driven Adapters）
```

- **domain** 不依賴任何其他模塊，甚至不依賴 Spring Framework。
- **app** 仅依賴 `domain`，負責事務边界和用例编排。
- **infrastructure** 依賴 `domain` 的 Port 介面，提供技術實現。
- **api / trigger** 依賴 `app` 和 `domain`，將外部请求翻譯為應用層命令。

### 2.2 Port 定義示例

```java
// domain/src/main/java/.../resume/repository/ResumeRepository.java
public interface ResumeRepository {
    Resume save(Resume resume);           // 驱動領域存儲，不關心底層是 PostgreSQL 還是檔案係統
    Optional<Resume> findById(ResumeId id);
    List<Resume> findByOwnerId(UserId ownerId);
}
```

```java
// domain/src/main/java/.../matching/port/AiScoringPort.java
public interface AiScoringPort {
    MatchScore score(Resume resume, JobDescription job);  // 抽象外部 AI 評分能力
}
```

### 2.3 Adapter 實現示例

```java
// infrastructure/src/main/java/.../persistence/resume/ResumeJpaRepository.java
@Repository
public class ResumeJpaRepository implements ResumeRepository {
    // 使用 Spring Data JPA 實現領域定義的介面
}
```

```java
// infrastructure/src/main/java/.../ai/AiScoringRestAdapter.java
@Component
public class AiScoringRestAdapter implements AiScoringPort {
    // 通過 HTTP 呼叫外部 AI 服務，實現領域端口
}
```

---

## 3. Consequences / 後果

### 3.1 Positive / 正面

| 收益 | 說明 |
|------|------|
| **技術替換零侵入** | 切換向量資料庫、替換消息佇列、更換 AI 提供商，只需新增 Adapter，domain 與 app 層零改動。 |
| **純粹單元測試** | domain 層測試無需 Spring 上下文；app 層可通過 Mockito 注入 Port 介面完成測試。 |
| **清晰边界** | 每个模塊 `pom.xml` 顯式声明依賴，違反依賴規則的程式碼無法编譯，架構防腐由構建工具強制執行。 |
| **新人上手更快** | 領域逻辑集中在 `domain`，基礎設施噪音隔離在 `infrastructure` — 佈局一目瞭然。 |

### 3.2 Negative / 負面

| 成本 | 說明 |
|------|------|
| **初始學習曲線** | 團隊成員需理解 Port/Adapter 概念，避免將業務逻辑泄露到 infrastructure。 |
| **樣板程式碼增加** | 每个外部依賴需定義 Port 介面 + Adapter 實現 + 可能的双向 Converter，程式碼量高於直接 `@Autowired`。 |
| **DDD 術語統一成本** | Entity（JPA） vs Entity（DDD）概念衝突，需通過包命名規範（`domain/resume/entity/` vs `infrastructure/persistence/entity/`）區分。 |
| **IDE 導航複雜** | 跳轉到 Repository 實現時需要多一次跳轉（介面 → 唯一實現）。 |

### 3.3 Risks / 風險與缓解

| 風險 | 缓解措施 |
|------|----------|
| 團隊成員绕過 Port 直接在 app 層注入 `JpaRepository` | **架構门禁**：啟用 `code-analyzer` 掃描，`app` 模塊禁止依賴 `org.springframework.data` 包。 |
| 過度設计 — 簡單 CRUD 也定義 Port | ** pragmatic 原則**：純查询無業務規則的場景允許在 api 層直接呼叫 Repository，但必須在 ADR 中記錄例外。 |
| Maven 模塊膨胀 | 保持 5 个核心模塊，禁止在 domain/app 內部進一步拆分子模塊。 |

---

## 4. Compliance / 合規驗證

- **靜态掃描**：每次 CI 運行 `code-analyzer` 檢查模塊間依賴違規。
- **程式碼審查**：PR 中新增 `*Port` 介面或 `*Adapter` 實現需由 Tech Lead 審閱。
- **季度架構回顾**：每季度抽樣檢查 infrastructure 層是否存在泄露的業務逻辑（if/else 決策树、金額计算等）。

---

## 5. Related / 相關決策

- ADR-0002 — PostgreSQL + pgvector 選型（向量存儲的 Driven Adapter 實現）
- ADR-0003 — RabbitMQ + Outbox 模式選型（異步消息的 Driven Adapter 實現）
- ADR-0005 — AI 服務呼叫抽象為 `AiScoringPort` / `EmbeddingPort`

---

## 6. Notes / 備注

> Hexagonal Architecture 由 Alistair Cockburn 提出，核心理念是："讓應用程序成為可以在不同环境中運行的獨立主體，而不需要修改自身。"
> 
> 本項目未採用完整的 DDD 戰術模式（如 Aggregate Root 的嚴格一致性边界），但在戰略層面保留了 Bounded Context 思想（resume / matching / conversation / user / tracking 五个子域）。

---

*End of ADR-0001*
