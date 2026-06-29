# Coding Conventions

## General

- **Language**: Code comments and docs in English; user-facing text in `en` / `zh-Hans` / `zh-Hant` (see `frontend/src/locales/`).
- **Git**: Conventional Commits (`feat(scope):`, `fix(scope):`, `chore(deps):`, etc.) using `.gitmessage` as the commit template.
- **Branching**: Short-lived branches off `main` per `docs/BRANCHING_AND_COMMITS.md`. Naming: `feat/`, `fix/`, `hotfix/`, `chore/`, `docs/`, `refactor/`, `test/`, `perf/`, `ci/`, `style/`.
- **CI triggers**: `push` and `pull_request` to `main` and `develop` (`.github/workflows/ci.yml`).

## Backend (Java)

### Package Naming

Maven multi-module layout (`backend/pom.xml`):

| Module | Base package | Typical sub-packages |
|--------|--------------|----------------------|
| `types` | `io.jobcopilot.resumeassistant.types` | `types.enums` |
| `domain` | `io.jobcopilot.resumeassistant.domain` | `domain.<context>.{entity\|valueobject\|repository\|port\|service\|exception\|event}` |
| `app` | `io.jobcopilot.resumeassistant.application` | `application.<context>.{service\|command\|query\|dto\|mapper\|facade}`; config lives under `io.jobcopilot.resumeassistant.app.config` |
| `api` | `io.jobcopilot.resumeassistant.api` | `api.<context>.{facade\|dto/request\|dto/response}` |
| `infrastructure` | `io.jobcopilot.resumeassistant.infrastructure` | `infrastructure.{persistence\|messaging\|cache\|lock\|storage\|security\|rest\|embedding\|i18n\|config}.{adapter\|config\|service\|repository}` |
| `trigger` | `io.jobcopilot.resumeassistant.trigger` | `trigger.http.controller.<context>`, `trigger.http.security`, `trigger.listener.ai` |

### Key Rules

- Domain layer: **zero Spring/Framework imports**. Only Java stdlib + `types` module.
- Domain entities are plain Java (no `@Entity`). JPA entities live in `infrastructure/persistence/entity/`.
- Application layer owns `@Transactional` boundaries.
- Use Lombok (`@RequiredArgsConstructor`, `@Slf4j`, `@Getter`) — but **not** `@Data`.
- Use MapStruct for DTO ↔ Entity mapping (`backend/pom.xml` declares `mapstruct` and `mapstruct-processor`).
- Port interfaces in domain end with `*Port` or `*Repository` (e.g., `VectorEmbeddingPort`, `ResumeRepository`).
- Adapter implementations in infrastructure end with `*Adapter`, `*JpaRepository`, or `*RepositoryImpl` (e.g., `VectorEmbeddingRestAdapter`, `ResumeVersionRepositoryImpl`).
- Asynchronous messaging uses the **Outbox pattern**: `OutboxMessage` in `domain`, `OutboxRelayScheduler` in `app`, `AiMessagePublisherAdapter` in `infrastructure/messaging/publisher/`.
- RabbitMQ topology is declared in `RabbitMqConfig.java` with request/result queues for job parse, resume parse, conversation, job rank, and feedback.

### Testing

- Domain tests: Pure JUnit 5 + AssertJ, no Spring context.
- App tests: Mock ports with Mockito.
- Infrastructure tests: `@DataJpaTest`, `@SpringBootTest` with Testcontainers where needed.
- Architecture tests: ArchUnit in `backend/app/src/test/java/.../architecture/`.
- Coverage is enforced by JaCoCo: **60% instruction/line**, **40% branch** (`backend/pom.xml`).

### Anti-Patterns (NEVER DO)

- `rabbitTemplate.convertAndSend()` inside `@Transactional` business methods — go through the Outbox.
- Importing `org.springframework.*` in the `domain` module.
- Direct `@Autowired` of infrastructure beans in the app layer — use Port interfaces.
- Calling LLM/embedding HTTP or Redis operations inside a `@Transactional` method.

## Frontend (TypeScript/React)

### File Structure

- Pages: `frontend/src/pages/{feature}/` — one file per route.
- Components: `frontend/src/components/{feature}/` — shared/reusable; UI primitives live in `src/components/ui/`.
- Services: `frontend/src/services/{feature}Service.ts` — plain functions/objects wrapping API calls.
- Store: `frontend/src/store/{feature}.store.ts` — Zustand stores (e.g., `language.store.ts`, `profile.store.ts`).
- Hooks: `frontend/src/hooks/use{Name}.ts` — custom hooks.
- Types: `frontend/src/types/`.
- i18n: `frontend/src/i18n/index.ts` with locales in `frontend/src/locales/{en,zh-CN,zh-TW}.json`.

### Key Rules

- TypeScript strict mode (`frontend/tsconfig.json` references `tsconfig.app.json`).
- Zustand over Redux for state.
- Services are plain exported functions/objects, not classes.
- API calls go through `apiClient` from `frontend/src/services/api.ts` (JWT attach/refresh, `Accept-Language`, transient GET retry, 401 redirect).
- shadcn/ui components imported from `@/components/ui/` (configured in `components.json`).
- Tailwind CSS for all styling; avoid inline styles.

### Testing

- Test files co-located: `Component.test.tsx` next to `Component.tsx`.
- Use Vitest + Testing Library + `happy-dom`.
- Mock API calls at the service level, not Axios level.

## AI Service (Python)

### Key Rules

- All configuration is centralized in `ai-service/app/config.py` and read from environment variables.
- LLM calls go through **LiteLLM** (`app/services/llm_client.py`); no direct provider SDK calls in business code.
- FastAPI entry point: `app/main.py`; AI worker entry point: `app/worker_main.py`.
- RabbitMQ topology mirrors the backend (`app/config.py` and `app/mq/consumer.py`).
- AI service consumes from:
  - `ai.queue.job.parse`
  - `ai.queue.resume.parse`
  - `ai.queue.conversation`
  - `ai.queue.job.rank`
- AI worker consumes from `ai.queue.feedback` and runs incremental model retraining.
- Publishes results to `backend.queue.job.parse`, `backend.queue.resume.parse`, `backend.queue.conversation`, and `backend.queue.job.rank`.
- `INTERNAL_API_KEY` is mandatory in non-dev environments; the FastAPI middleware validates `X-Internal-API-Key`.
- Embedding dimensions must match the backend (`LLM_EMBEDDING_MODEL_DIMENSION`).
- Logging uses the standard library `logging` module; `structlog` is available in dependencies.

### Testing

- Pytest with `pytest-asyncio` for async tests.
- Mock external calls (LiteLLM, backend client, Redis, RabbitMQ).
- Use Ruff for linting and Black for formatting; both run in CI.

## Infrastructure (Docker Compose)

- `.env` is gitignored; `.env.example` is committed and documented.
- All secrets stay in `.env`; `docker-compose.yml` contains no secrets.
- Only the frontend Nginx port is exposed to the host by default (`FRONTEND_HOST_PORT`, default `80:8080`).
- Three-tier network isolation (`docker-compose.yml`):
  - `public-network` — frontend, backend
  - `internal-network` — backend, ai-service, ai-worker, rabbitmq, redis, minio
  - `db-network` — backend, postgres
- Backend is the only multi-homed service (spans all three networks).
- Services: `frontend`, `backend`, `ai-service`, `ai-worker`, `rabbitmq`, `redis`, `minio`, `postgres`.
- Named volumes: `postgres-data`, `rabbitmq-data`, `redis-data`, `shared-storage`, `minio-data`.
- Health checks with `depends_on` conditions are configured for all long-running services.
- `VITE_API_BASE_URL` must be empty or a relative path (e.g. `/api`); absolute URLs bypass the reverse proxy.
- Backend→AI service REST calls and AI service→backend callbacks carry `X-Internal-API-Key`.

## CI/CD (`.github/workflows`)

| Workflow | File | Purpose |
|----------|------|---------|
| **CI** | `.github/workflows/ci.yml` | Backend build/test (JDK 21, Maven), frontend build/test (Node 20, npm), AI service build/test (Python 3.11), Trivy security scan, Docker build/push to GHCR. |
| **Dependabot Auto-Merge** | `.github/workflows/dependabot-auto-merge.yml` | Enables auto-merge for Dependabot patch updates; comments on minor updates. |
| **Dependency Check Nightly** | `.github/workflows/dependency-check-nightly.yml` | OWASP dependency-check every Sunday at 02:00 UTC. |
| **Qodana** | `.github/workflows/qodana_code_quality.yml` | Static analysis on PRs/pushes to `main` and `releases/*`; skipped for Dependabot. |
| **Block Major Upgrades** | `.github/workflows/block-major-upgrades.yml` | Detects and closes Dependabot major-version bump PRs. |

Dependabot configuration (`.github/dependabot.yml`) schedules weekly grouped updates for Maven, npm, pip, and GitHub Actions; major-version updates are ignored.

Current CI notes (as observed in `ci.yml`):
- Backend tests are limited to `types` and `domain` modules (`mvn test --batch-mode -pl types,domain -am`).
- ArchUnit runs separately and currently excludes `AppLayerArchitectureTest`.
- Docker images are pushed to `ghcr.io/${{ github.repository_owner }}/jobcopilot-{backend,frontend,ai-service}`.

## Architecture Decisions (`docs/adr`)

| ADR | Decision |
|-----|----------|
| [ADR-0001](../../docs/adr/ADR-0001-hexagonal-architecture.md) | Hexagonal Architecture (Ports & Adapters) for the backend. |
| [ADR-0002](../../docs/adr/ADR-0002-postgresql-pgvector.md) | PostgreSQL + pgvector as unified structured and vector storage. |
| [ADR-0003](../../docs/adr/ADR-0003-rabbitmq-outbox.md) | RabbitMQ + Outbox pattern for reliable async messaging. |
| [ADR-0004](../../docs/adr/ADR-0004-redis-cache-lock.md) | Redis for distributed cache and distributed locks (ShedLock). |
| [ADR-0005](../../docs/adr/ADR-0005-embedding-port-abstraction.md) | `EmbeddingPort` / `VectorGenerationPort` abstraction decouples vector generation from business transactions. |
| [ADR-0006](../../docs/adr/ADR-0006-docker-compose-three-tier-network.md) | Docker Compose three-tier network architecture. |

## Code Review Standards

- **Architecture compliance**: Check domain layer purity, port/adapter patterns, and correct transaction boundaries.
- **Security**: No hardcoded secrets, proper auth checks (`JwtAuthenticationFilter`, `INTERNAL_API_KEY`), input validation, slider CAPTCHA on auth endpoints.
- **Testing**: New features must include tests covering happy path + edge cases.
- **Coverage**: Backend JaCoCo thresholds are 60% instruction/line and 40% branch.
