<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](ADR-0001-hexagonal-architecture.md) | [简体中文](../i18n/zh-Hans-CN/adr/ADR-0001-hexagonal-architecture.md) | [繁體中文](../i18n/zh-Hant-TW/adr/ADR-0001-hexagonal-architecture.md)

# ADR-0001: Adopt Hexagonal Architecture as the Backend Core Architecture Paradigm

| Attribute | Value |
|-----------|-------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | Backend Architecture Team |
| **Affected Modules** | `backend/domain`, `backend/app`, `backend/infrastructure`, `backend/api`, `backend/trigger` |

---

## 1. Context

The ResumeAssistant project must support the following long-term evolution goals:

1. **AI Service Decoupling** — Core capabilities such as resume parsing, job matching, and conversation generation depend on external LLM services. Providers may change over time (OpenAI → Claude → self-hosted).
2. **Storage Medium Replaceability** — Currently using PostgreSQL + pgvector for vector storage; future needs may introduce dedicated vector databases (e.g., Milvus, Pinecone) or object storage.
3. **Multi-Delivery Channels** — REST API (current), with potential future expansion to WebSocket real-time push, message queue async triggers, and even gRPC internal communication.
4. **Test Independence** — Core business logic must be unit-testable without starting the database, message queue, or external HTTP services.

Traditional **Layered Architecture** buries business logic in the `service` layer, with infrastructure (database, HTTP clients, message sending) injected directly via `@Autowired`, leading to:
- Business code deeply coupled with Spring Data JPA, RestTemplate, and RabbitMQ Template;
- Any infrastructure replacement requiring changes to the `service` layer;
- Unit tests requiring `@SpringBootTest` with the full context, making them slow and fragile.

---

## 2. Decision

**Adopt Hexagonal Architecture (Ports & Adapters) as the sole backend architecture paradigm.**

Physical isolation is enforced via Maven multi-modules:

```
backend/
├── domain/          ← Core business domain: Entity, ValueObject, DomainService, Repository Port (interfaces)
├── app/             ← Application layer: ApplicationService, transaction boundaries, use case orchestration
├── infrastructure/  ← Infrastructure adapters: JPA Repository implementations, RabbitMQ sender, REST clients, Redis cache
├── api/             ← Inbound adapters: REST Controllers, DTOs, request validation
└── trigger/         ← Inbound adapters: message listeners, scheduled task triggers
```

### 2.1 Dependency Rule

```
      api / trigger         ← Inbound adapters (Driving Adapters)
           ↓
         app                 ← Application layer (Orchestration + Transaction)
           ↓
        domain               ← Domain layer (Business Logic — no external dependencies)
           ↑
    infrastructure           ← Outbound adapters (Driven Adapters)
```

- **domain** depends on no other module, not even Spring Framework.
- **app** depends only on `domain`, responsible for transaction boundaries and use case orchestration.
- **infrastructure** depends on `domain` Port interfaces, providing technical implementations.
- **api / trigger** depend on `app` and `domain`, translating external requests into application layer commands.

### 2.2 Port Definition Example

```java
// domain/src/main/java/.../resume/repository/ResumeRepository.java
public interface ResumeRepository {
    Resume save(Resume resume);           // Drive domain storage, indifferent to PostgreSQL or filesystem
    Optional<Resume> findById(ResumeId id);
    List<Resume> findByOwnerId(UserId ownerId);
}
```

```java
// domain/src/main/java/.../matching/port/AiScoringPort.java
public interface AiScoringPort {
    MatchScore score(Resume resume, JobDescription job);  // Abstract external AI scoring capability
}
```

### 2.3 Adapter Implementation Example

```java
// infrastructure/src/main/java/.../persistence/resume/ResumeJpaRepository.java
@Repository
public class ResumeJpaRepository implements ResumeRepository {
    // Implement domain-defined interfaces using Spring Data JPA
}
```

```java
// infrastructure/src/main/java/.../ai/AiScoringRestAdapter.java
@Component
public class AiScoringRestAdapter implements AiScoringPort {
    // Call external AI service via HTTP, implementing the domain port
}
```

---

## 3. Consequences

### 3.1 Positive

| Benefit | Description |
|---------|-------------|
| **Zero-Invasion Technology Replacement** | Switching vector databases, replacing message queues, or changing AI providers only requires adding a new Adapter — the domain and app layers require zero changes. |
| **Pure Unit Testing** | The domain layer tests need no Spring context; the app layer can be tested by mocking Port interfaces with Mockito. |
| **Clear Boundaries** | Each module's `pom.xml` explicitly declares dependencies; build tooling prevents compilation of rule-violating code. |
| **Faster Onboarding** | Domain logic lives in `domain`, infrastructure noise stays in `infrastructure` — the layout is obvious. |

### 3.2 Negative

| Cost | Description |
|------|-------------|
| **Initial Learning Curve** | Team members must understand the Port/Adapter concept to avoid leaking business logic into infrastructure. |
| **Boilerplate Increase** | Every external dependency requires a Port interface + Adapter implementation + possibly bidirectional Converter — more code than direct `@Autowired`. |
| **DDD Terminology Alignment Cost** | Entity (JPA) vs Entity (DDD) concept conflict must be managed through package naming conventions (`domain/resume/entity/` vs `infrastructure/persistence/entity/`). |
| **IDE Navigation Complexity** | Jumping to a Repository implementation requires one extra hop (interface → sole implementation). |

### 3.3 Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Team members bypassing Ports by directly injecting `JpaRepository` into the app layer | **Architecture Gate**: Enable `code-analyzer` scanning; the `app` module is forbidden from depending on `org.springframework.data` packages. |
| Over-engineering — defining Ports for simple CRUD with no business rules | **Pragmatism Principle**: Pure query scenarios with no business rules may directly call Repository from the API layer, but exceptions must be documented in ADRs. |
| Maven module bloat | Maintain 5 core modules; prohibit further sub-module splitting inside domain/app. |

---

## 4. Compliance Verification

- **Static Scanning**: `code-analyzer` runs on every CI to check for inter-module dependency violations.
- **Code Review**: New `*Port` interfaces or `*Adapter` implementations require Tech Lead review in PRs.
- **Quarterly Architecture Review**: Quarterly sampling checks the infrastructure layer for leaked business logic (if/else decision trees, amount calculations, etc.).

---

## 5. Related Decisions

- ADR-0002 — PostgreSQL + pgvector Selection (Vector storage as a Driven Adapter implementation)
- ADR-0003 — RabbitMQ + Outbox Pattern Selection (Async messaging as a Driven Adapter implementation)
- ADR-0005 — AI Service Invocation Abstracted as `AiScoringPort` / `EmbeddingPort`

---

## 6. Notes

> Hexagonal Architecture was proposed by Alistair Cockburn. The core idea is: "Allow an application to equally be driven by users, programs, automated test or batch scripts, and to be developed and tested in isolation from its eventual run-time devices and databases."
>
> This project does not adopt the complete DDD tactical pattern set (e.g., strict Aggregate Root consistency boundaries), but retains the strategic Bounded Context concept (resume / matching / conversation / user / tracking sub-domains).

---

*End of ADR-0001*
