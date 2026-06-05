<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../adr/ADR-0001-hexagonal-architecture.md) | [简体中文](ADR-0001-hexagonal-architecture.md) | [繁體中文](../../zh-Hant-TW/adr/ADR-0001-hexagonal-architecture.md)

# ADR-0001: 采用六边形架构（Hexagonal Architecture）作为后端核心架构范式

| 属性 | 内容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | 后端架构团队 |
| **Affected Modules** | `backend/domain`, `backend/app`, `backend/infrastructure`, `backend/api`, `backend/trigger` |

---

## 1. Context / 背景

JobCopilot 项目需要同时支撑以下长期演进目标：

1. **AI 服务解耦** — 简历解析、职位匹配、对话生成等核心能力依赖外部 LLM 服务，外部提供商可能更换（OpenAI → Claude → 自托管）。
2. **存储介质可替换** — 当前使用 PostgreSQL + pgvector 存储向量，未来可能引入专用向量数据库（如 Milvus、Pinecone）或对象存储。
3. **多交付渠道** — REST API（当前）、未来可能扩展为 WebSocket 实时推送、消息队列异步触发、甚至 gRPC 内部通信。
4. **测试可独立性** — 核心业务逻辑必须能在不启动数据库、消息队列、外部 HTTP 服务的情况下完成单元测试。

传统的 **分层架构（Layered Architecture）** 将业务逻辑埋在 `service` 层，基础设施（数据库、HTTP 客户端、消息发送）通过 `@Autowired` 直接注入，导致：
- 业务代码与 Spring Data JPA、RestTemplate、RabbitMQ Template 深度耦合；
- 任何基础设施替换都需要改动 `service` 层；
- 单元测试需要 `@SpringBootTest` 启动完整上下文，慢且脆弱。

---

## 2. Decision / 决策

**采用六边形架构（Hexagonal Architecture / Ports & Adapters）作为后端唯一架构范式。**

具体实现通过 Maven 多模块物理隔离：

```
backend/
├── domain/          ← 核心业务领域：Entity、ValueObject、DomainService、Repository Port（接口）
├── app/             ← 应用层：ApplicationService、事务边界、用例编排
├── infrastructure/  ← 基础设施适配器：JPA Repository 实现、RabbitMQ 发送器、REST 客户端、Redis 缓存
├── api/             ← 入站适配器：REST Controller、DTO、请求校验
└── trigger/         ← 入站适配器：消息监听器、定时任务触发器
```

### 2.1 依赖规则（Dependency Rule）

```
      api / trigger         ← 入站适配器（Driving Adapters）
           ↓
         app                 ← 应用层（Orchestration + Transaction）
           ↓
        domain               ← 领域层（Business Logic — 无外部依赖）
           ↑
    infrastructure           ← 出站适配器（Driven Adapters）
```

- **domain** 不依赖任何其他模块，甚至不依赖 Spring Framework。
- **app** 仅依赖 `domain`，负责事务边界和用例编排。
- **infrastructure** 依赖 `domain` 的 Port 接口，提供技术实现。
- **api / trigger** 依赖 `app` 和 `domain`，将外部请求翻译为应用层命令。

### 2.2 Port 定义示例

```java
// domain/src/main/java/.../resume/repository/ResumeRepository.java
public interface ResumeRepository {
    Resume save(Resume resume);           // 驱动领域存储，不关心底层是 PostgreSQL 还是文件系统
    Optional<Resume> findById(ResumeId id);
    List<Resume> findByOwnerId(UserId ownerId);
}
```

```java
// domain/src/main/java/.../matching/port/AiScoringPort.java
public interface AiScoringPort {
    MatchScore score(Resume resume, JobDescription job);  // 抽象外部 AI 评分能力
}
```

### 2.3 Adapter 实现示例

```java
// infrastructure/src/main/java/.../persistence/resume/ResumeJpaRepository.java
@Repository
public class ResumeJpaRepository implements ResumeRepository {
    // 使用 Spring Data JPA 实现领域定义的接口
}
```

```java
// infrastructure/src/main/java/.../ai/AiScoringRestAdapter.java
@Component
public class AiScoringRestAdapter implements AiScoringPort {
    // 通过 HTTP 调用外部 AI 服务，实现领域端口
}
```

---

## 3. Consequences / 后果

### 3.1 Positive / 正面

| 收益 | 说明 |
|------|------|
| **技术替换零侵入** | 切换向量数据库、替换消息队列、更换 AI 提供商，只需新增 Adapter，domain 与 app 层零改动。 |
| **纯粹单元测试** | domain 层测试无需 Spring 上下文；app 层可通过 Mockito 注入 Port 接口完成测试。 |
| **清晰边界** | 每个模块 `pom.xml` 显式声明依赖，违反依赖规则的代码无法编译，架构防腐由构建工具强制执行。 |
| **新人上手更快** | 领域逻辑在 `domain`，基础设施噪音隔离在 `infrastructure` — 布局一目了然。 |

### 3.2 Negative / 负面

| 成本 | 说明 |
|------|------|
| **初始学习曲线** | 团队成员需理解 Port/Adapter 概念，避免将业务逻辑泄露到 infrastructure。 |
| **样板代码增加** | 每个外部依赖需定义 Port 接口 + Adapter 实现 + 可能的双向 Converter，代码量高于直接 `@Autowired`。 |
| **DDD 术语统一成本** | Entity（JPA） vs Entity（DDD）概念冲突，需通过包命名规范（`domain/resume/entity/` vs `infrastructure/persistence/entity/`）区分。 |
| **IDE 导航复杂** | 跳转到 Repository 实现时需要多一次跳转（接口 → 唯一实现）。 |

### 3.3 Risks / 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| 团队成员绕过 Port 直接在 app 层注入 `JpaRepository` | **架构门禁**：启用 `code-analyzer` 扫描，`app` 模块禁止依赖 `org.springframework.data` 包。 |
| 过度设计 — 简单 CRUD 也定义 Port | ** pragmatic 原则**：纯查询无业务规则的场景允许在 api 层直接调用 Repository，但必须在 ADR 中记录例外。 |
| Maven 模块膨胀 | 保持 5 个核心模块，禁止在 domain/app 内部进一步拆分子模块。 |

---

## 4. Compliance / 合规验证

- **静态扫描**：每次 CI 运行 `code-analyzer` 检查模块间依赖违规。
- **代码审查**：PR 中新增 `*Port` 接口或 `*Adapter` 实现需由 Tech Lead 审阅。
- **季度架构回顾**：每季度抽样检查 infrastructure 层是否存在泄露的业务逻辑（if/else 决策树、金额计算等）。

---

## 5. Related / 相关决策

- ADR-0002 — PostgreSQL + pgvector 选型（向量存储的 Driven Adapter 实现）
- ADR-0003 — RabbitMQ + Outbox 模式选型（异步消息的 Driven Adapter 实现）
- ADR-0005 — AI 服务调用抽象为 `AiScoringPort` / `EmbeddingPort`

---

## 6. Notes / 备注

> Hexagonal Architecture 由 Alistair Cockburn 提出，核心理念是："让应用程序成为可以在不同环境中运行的独立主体，而不需要修改自身。"
> 
> 本项目未采用完整的 DDD 战术模式（如 Aggregate Root 的严格一致性边界），但在战略层面保留了 Bounded Context 思想（resume / matching / conversation / user / tracking 五个子域）。

---

*End of ADR-0001*
