---
name: jobcopilot-codebase
description: Comprehensive knowledge of the JobCopilot codebase architecture, module structure, key patterns, and navigation guide. Use when working on any part of this project, exploring the codebase, adding new features, fixing bugs, or understanding how components interact. This skill should be consulted before making any code changes to ensure alignment with existing architecture and conventions.
---

# JobCopilot Codebase Guide

> Last synced with origin/main on 2026-06-30. Phases 0–2, 5, 3 implemented (46 files).

## Management Plane — Implementation Status

| Phase | Status | Description |
|-------|--------|-------------|
| **Phase 0** | ✅ | Version control: VERSION, CHANGELOG, bump-version.sh, release CI, APP_VERSION everywhere |
| **Phase 1** | ✅ | RBAC: JWT role claim, SecurityConfig admin routes, AdminUserInitializer, V11 migration |
| **Phase 2** | ✅ | Admin API: `/api/admin/v1/users/**`, `/api/admin/v1/system/**`, `/api/admin/v1/audit-logs` |
| **Phase 5** | ✅ | Admin UI: AdminLayout, AdminRoute, Dashboard/Users/AuditLogs pages, adminService, Zustand store |
| **Phase 3** | ✅ | Monitoring: Prometheus metrics, BusinessMetricsService, AI service admin router (`/admin/*`) |
| **Phase 4** | ✅ | Dynamic config: DB table, CRUD API, AI hot-reload endpoint, audit trail |

### Key new files (back-end)
- `VERSION`, `CHANGELOG.md`, `scripts/bump-version.sh`, `.github/workflows/release.yml`
- `api/admin/dto/*`, `api/admin/facade/*`, `api/version/dto/VersionResponse`
- `domain/admin/entity/AuditLog`, `domain/admin/repository/AuditLogRepository`
- `infrastructure/.../admin/AuditLogJpaEntity`, `AuditLogJpaRepository`
- `app/.../admin/AdminUserFacadeImpl`, `AdminSystemFacadeImpl`, `AdminAuditFacadeImpl`, `AdminUserInitializer`, `BusinessMetricsService`
- `trigger/.../admin/AdminUserController`, `AdminSystemController`, `AdminAuditController`, `AdminMonitoringController`
- `trigger/.../version/VersionController`
- `types/common/PageResult`
- `V11__admin_infrastructure.sql`
- `ai-service/app/api/admin_router.py`, `ai-service/app/__version__.py`

### Key new files (front-end)
- `components/AdminRoute.tsx`, `components/admin/AdminLayout.tsx`, `StatCard.tsx`, `VersionBanner.tsx`
- `pages/admin/AdminDashboard.tsx`, `AdminUsers.tsx`, `AdminAuditLogs.tsx`, `AdminMonitoring.tsx`, `AdminConfig.tsx`, `AdminAIService.tsx`
- `services/adminService.ts`, `store/admin.store.ts`, `utils/version.ts`, `global.d.ts`

This project uses **Semantic Versioning 2.0** (`MAJOR.MINOR.PATCH`):
- **MAJOR**: Incompatible API changes (e.g., DB schema breaking change)
- **MINOR**: Backward-compatible new features (e.g., new management plane)
- **PATCH**: Backward-compatible bug fixes

**Single source of truth**: `VERSION` file at project root. All components (backend `pom.xml`, frontend `package.json`, AI service `__version__.py`) read from this file via `scripts/bump-version.sh`.

**Current version**: `0.1.0` (pre-1.0 development)

**Release process**: Push a `v*.*.*` tag → `.github/workflows/release.yml` builds Docker images with version tags, creates GitHub Release with CHANGELOG entries.

**Runtime version exposure**:
- Backend: `/v1/version` (public), `/actuator/info` (admin), `VersionController`
- Frontend: `__APP_VERSION__` global (injected via Vite `define`)
- AI Service: `/` root endpoint returns version

## Quick Reference

JobCopilot is an AI-powered job search platform with three main services:

| Layer | Tech | Entry Point | Port (internal) |
|-------|------|-------------|-----------------|
| **Frontend** | React 19.2.7 + Vite 7.2.4 + TypeScript 5.9 | `frontend/src/App.tsx` → `main.tsx` | 5173 (dev) |
| **Backend** | Java 21 + Spring Boot 3.5.16 + Maven | `backend/app/.../Application.java` | 8080 |
| **AI Service** | Python 3.11+ + FastAPI 0.138.0 + LiteLLM | `ai-service/app/main.py` | 8000 |

**Quick start**: `docker compose --env-file .env up -d --build`
**Root env file**: `.env.example` → copy to `.env` and fill secrets.

---

## Architecture at a Glance

```
Browser → Nginx (frontend) → Backend (Spring Boot)
                                ├── PostgreSQL + pgvector (business + vectors)
                                ├── RabbitMQ → AI Service / AI Worker (async)
                                ├── Redis (cache, locks, Pub/Sub)
                                └── MinIO (model artifacts)
```

**Docker Compose services**: `frontend`, `backend`, `ai-service`, `ai-worker`, `rabbitmq`, `redis`, `minio`, `postgres`.

**Three Docker networks**: `public-network` (frontend + backend), `internal-network` (backend + ai-service + ai-worker + rabbitmq + redis + minio), `db-network` (backend + postgres). Only frontend port 80 is exposed to the host.

---

## Backend Module Map (Maven Multi-Module)

Maven modules in dependency order (top→bottom = caller→callee):

| Module | Path | Responsibility | Key Contents |
|--------|------|----------------|--------------|
| **types** | `backend/types/` | Shared enums, value types, constants | `OAuthProvider`, enums, no logic |
| **domain** | `backend/domain/` | Entities, value objects, **Port interfaces**, domain services | `Resume`, `JobDescription`, `*Repository`, `*Port` interfaces |
| **api** | `backend/api/` | DTOs, commands, queries, facade interfaces | Request/Response objects, API contracts |
| **infrastructure** | `backend/infrastructure/` | JPA repos, RabbitMQ, Redis, MinIO, REST clients | Adapter implementations of domain ports |
| **trigger** | `backend/trigger/` | REST controllers, MQ listeners, WebSocket | `*Controller`, `*MessageListener` |
| **app** | `backend/app/` | Spring Boot startup, config, application services, schedulers | `Application.java`, `*ApplicationService`, `*Scheduler` |

**Dependency rule**: `domain` depends on nothing (not even Spring). `infrastructure` implements domain ports. `app` orchestrates. `trigger/api` are inbound adapters. See [backend-architecture.md](backend-architecture.md) for details.

### Domain Sub-packages (Bounded Contexts)

- `domain/resume/` — Resume entities, versions, parsing ports
- `domain/job/` — Job descriptions, scoring, matching ports
- `domain/embedding/` — `JobVector`, `ResumeVector`, embedding/generation ports
- `domain/matching/` — `JobMatchResult`, `MatchingModel`, recall/rank ports
- `domain/conversation/` — Chat messages, context
- `domain/tracking/` — Job application tracking
- `domain/user/` — User, profile, OAuth binding
- `domain/shared/` — Outbox message, file storage port, exceptions

---

## AI Service Module Map

| Area | Path | Responsibility |
|------|------|----------------|
| API endpoints | `ai-service/app/main.py`, `app/api/` | FastAPI routes, health, embedding, matching, suitability |
| Config | `ai-service/app/config.py` | All env vars, queue names, model names |
| Services | `ai-service/app/services/` | LLM calls (`llm_client.py`), resume/job parsing, matching, ranking, embeddings, orchestrators |
| MQ Consumers | `ai-service/app/mq/consumer.py` | RabbitMQ consumer setup |
| Worker Consumers | `ai-service/app/worker/consumers/` | `feedback.py`, `rabbitmq_setup.py` |
| Worker Main | `ai-service/app/worker_main.py` | Worker entry point (separate container) |
| Model Manager | `ai-service/app/api/model_manager.py` | Loads LightGBM model from MinIO, watches `ai.model.reload` |
| Infrastructure | `ai-service/app/infrastructure/` | Backend API client, Redis client, MinIO client |
| Schemas | `ai-service/app/schemas.py` | Pydantic models for all requests/responses |

**AI Service and AI Worker share the same Docker image** but run different commands:
- `ai-service`: `uvicorn app.main:app` — serves REST API + runs workflow MQ consumers
- `ai-worker`: `python -m app.worker_main` — feedback processing + LightGBM retraining

---

## Frontend Module Map

| Area | Path | Responsibility |
|------|------|----------------|
| App entry | `frontend/src/App.tsx` | Router setup, auth guard, layout |
| Pages | `frontend/src/pages/` | `Dashboard`, `auth/`, `resumes/`, `jobs/`, `chat/`, `tracking/`, `profile/` |
| Components | `frontend/src/components/` | `layout/` (MainLayout, ErrorBoundary), `resume/`, `ui/` (shadcn/ui) |
| Services | `frontend/src/services/` | API client (`api.ts`), `resumeService`, `jobService`, `chatService`, `trackingService`, `profileService` |
| Store (Zustand) | `frontend/src/store/` | `resume.store.ts`, `job.store.ts`, `profile.store.ts`, `language.store.ts` |
| Hooks | `frontend/src/hooks/` | `useAuth`, `use-mobile` (`useIsMobile`), `useTimeZone` |
| i18n | `frontend/src/i18n/`, `src/locales/` | `en`, `zh-CN`, `zh-TW` |
| Types | `frontend/src/types/` | TypeScript type definitions |
| Utils | `frontend/src/utils/` | Helper functions |

---

## Key Patterns and Constraints

### Hexagonal Architecture (Backend)
- **Ports** are defined in `domain` as Java interfaces (e.g., `ResumeRepository`, `AiScoringPort`, `MessagePublisherPort`)
- **Adapters** are in `infrastructure` implementing those ports (e.g., `ResumeJpaRepository`, `AiScoringRestAdapter`)
- **NEVER** inject `JpaRepository` or `RabbitTemplate` directly into `app` layer services
- Domain code must not import Spring, JPA, HTTP, or MQ classes

### Outbox Pattern (Reliable Messaging)
- All async messages go through `outbox_messages` table in the same DB transaction
- `OutboxRelayScheduler` polls every 2 seconds for `PENDING` messages and delivers to RabbitMQ via `OutboxRelayTransactionService` (per-message `REQUIRES_NEW` transaction)
- `OutboxCleanupScheduler` deletes `SENT` records older than 7 days daily at 03:00
- Uses ShedLock (Redis-backed) to prevent duplicate relay/retrain across instances
- **NEVER** call `rabbitTemplate.convertAndSend()` directly inside `@Transactional`

### RabbitMQ Queue Pairs
- Request queues: `ai.queue.job.parse`, `ai.queue.resume.parse`, `ai.queue.conversation`, `ai.queue.job.rank`, `ai.queue.feedback`
- Response queues: `backend.queue.job.parse`, `backend.queue.resume.parse`, `backend.queue.conversation`, `backend.queue.job.rank`
- Exchange: `ai.direct.exchange`
- DLQ: `ai.dlq.queue` via `ai.dlx.exchange`

### LiteLLM Provider Abstraction
- All LLM calls go through LiteLLM with provider-prefixed model names (e.g., `gemini/gemini-2.5-flash`)
- Config keys: `LLM_TEXT_MODEL`, `LLM_VISION_MODEL`, `LLM_EMBEDDING_MODEL`
- Embedding dimension (`LLM_EMBEDDING_MODEL_DIMENSION`) must match both backend and AI service

### Frontend Patterns
- **Zustand** for state management (not Redux)
- **shadcn/ui** component library (Radix primitives + Tailwind)
- **i18next** for internationalization (`en`, `zh-CN`, `zh-TW`)
- **Axios interceptors** attach JWT, auto-refresh on 401, retry transient GET errors, and send `Accept-Language`
- API client timeout is 30 seconds; services are plain exported functions/objects, not classes
- **AbortSignal** support in services (e.g., `jobService.getJobs`) and stores (e.g., `resumeStore.pollParseStatus`) for canceling in-flight requests and polling

---

## Agent Architecture

The project uses a subagent architecture with 9 specialized agents. Each agent owns a specific business domain end-to-end across frontend/backend/ai-service. Agent definitions live in `.qoder/agents/`.

| Agent | Layer Coverage | Domain | Use When |
|-------|---------------|--------|----------|
| **architect-agent** | Cross-layer (design) | Architecture decisions, task decomposition | Cross-module changes, implementation path decisions, plan reviews |
| **auth-profile-agent** | Frontend + Backend | Auth (JWT/OAuth/CAPTCHA) + user profile | Login, register, OAuth, JWT refresh, CAPTCHA, profile, permissions |
| **resume-agent** | Frontend + Backend + AI | Resume upload → parse → versions → edit → download | Resume CRUD, PDF/DOCX parsing, versions, parse status |
| **job-matching-agent** | Frontend + Backend + AI | Job CRUD → pgvector recall → ranking → suitability → display | Job matching, match scores, pgvector search, suitability |
| **conversation-agent** | Frontend + Backend + AI | Chat lifecycle → context assembly → AI reply → WebSocket push | Chat page, conversation API, context references, AI replies |
| **tracking-agent** | Frontend + Backend | Application status flow → kanban → stats → follow-up notes | Applications, status transitions, statistics, follow-up records |
| **ai-pipeline-agent** | AI Service only | LiteLLM gateway → embedding → worker → training → model mgmt → eval | LLM providers, embedding dims, workers, LightGBM, MinIO, Redis pipeline |
| **platform-agent** | Infrastructure | Docker, MQ, Outbox, pgvector, Redis, MinIO, env vars, CI/CD | docker-compose, networking, queues, DB, deployment, CI workflows |
| **qa-review-agent** | Cross-layer (quality) | Code review, test strategy, architecture rules, regression risk, security scan | Feature review, test failures, CI failures, coverage gaps, pre-release gate |

**Agent interaction flow**:
```
architect-agent (planning & delegation)
  ├── auth-profile-agent (auth + profile, security-critical)
  ├── resume-agent (business implementation)
  ├── job-matching-agent (business implementation)
  ├── conversation-agent (business implementation)
  ├── tracking-agent (business implementation, no AI dependency)
  ├── ai-pipeline-agent (AI infrastructure, called by business agents)
  ├── platform-agent (infrastructure foundation, used by all)
  └── qa-review-agent (quality gatekeeper, reviews all agents' output)
```

**Key rules**:
- architect-agent designs and delegates, does NOT implement
- Business agents (resume/job-matching/conversation) call ai-pipeline-agent for LLM/embedding/model needs
- ai-pipeline-agent is the ONLY entry point for LLM calls (via LiteLLM)
- platform-agent owns Docker, MQ, Outbox, DB, env vars — all other agents depend on it
- auth-profile-agent owns the security perimeter — all other agents respect its token/cookie boundaries
- qa-review-agent is the final gatekeeper — must pass review before any feature is considered complete
- Each agent has explicit MUST DO / MUST NOT DO constraints in its definition file

---

## Key Data Flows

### Resume Upload → Parse
```
User upload → Backend ResumeController → ResumeApplicationService.handleUpload
  → saves file via FileStorageService, creates resume group + original/converted versions
  → vector generation → resume_vectors (pgvector)
  → Outbox → RabbitMQ ai.queue.resume.parse
  → AI Service process_resume() → file_parser.extract_resume_text() → resume_parser.parse_resume_text()
  → backend.queue.resume.parse
  → Backend AiResultMessageListener → ResumeParseResultHandler
  → PostgreSQL (structured data) + pgvector (embedding)
```
Frontend polls `GET /v1/resumes/groups` to observe `parseStatus` on each version.

### Job Matching
```
Frontend POST /v1/jobs/match → Backend MatchingApplicationService.startJobMatch
  → selects active recall model, regenerates missing resume vector via VectorEmbeddingPort if needed
  → VectorSearchPort.findSimilarJobs (pgvector Euclidean-distance recall against job_vectors)
  → Outbox → RabbitMQ ai.queue.job.rank
  → AI Service rank_jobs() → LightGBM ModelManager.predict() or heuristic fallback
  → backend.queue.job.rank
  → Backend AiResultMessageListener → MatchingApplicationService.saveMatchResult
  → Frontend polls GET /v1/jobs/match/{matchId}
```

### Conversation / Chat
```
User message → Backend ConversationController → ConversationMessageService.sendMessage
  → saves USER message, auto-generates title
  → ConversationContextService loads resume + primary job + up to 5 related jobs
  → Outbox → RabbitMQ ai.queue.conversation
  → AI Service conversation_service.process_conversation() → LiteLLM generate reply
  → backend.queue.conversation
  → Backend AiResultMessageListener.onConversationReply → ConversationFacadeImpl.saveAiReply
  → ConversationStreamService bridges async MQ reply with HTTP stream via Redis Pub/Sub channel `ra:conv:reply`
  → Frontend renders AI reply (with Markdown, context labels)
```

### Feedback → Incremental Training
```
User action (CLICK/APPLY/REJECT) → Backend JobApplicationService.trackUserAction
  → Outbox → RabbitMQ ai.queue.feedback
  → AI Worker feedback consumer → Redis list ai:feedback:buffer
  → IncrementalTrainer.try_retrain() daily at 02:00 UTC (lock: ai:model:retrain:lock)
  → fetches baseline features via InternalApiClient GET /api/internal/ai/baseline-features
  → LightGBM train (when samples ≥ MIN_SAMPLES_FOR_RETRAIN) → MinIO bucket ai-models
  → Redis Pub/Sub ai.model.reload → AI Service ModelManager loads new model
```

---

## Testing

| Layer | Command | Framework |
|-------|---------|-----------|
| Backend unit | `mvn test` | JUnit 5 + Mockito + AssertJ |
| Backend architecture | `mvn verify` | ArchUnit |
| Frontend unit | `npm run test:run` | Vitest + Testing Library |
| Frontend coverage | `npm run test:coverage` | @vitest/coverage-v8 |
| AI Service | `pytest` | Pytest + pytest-asyncio |
| AI Service lint | `ruff check .` | Ruff |
| E2E eval | `cd eval && python run_eval.py` | Custom eval framework |

Backend code coverage minimums: 60% instruction, 60% line, 40% branch.

---

## Rolling Update Mechanism

This skill is a **living document**. When you make significant code changes, update this skill:

1. **After adding/removing modules or services**: Update the module maps above
2. **After changing architectural patterns**: Update the "Key Patterns" section
3. **After adding new data flows**: Add to "Key Data Flows"
4. **After changing technology versions**: Update the Quick Reference
5. **After adding new bounded contexts**: Update domain sub-packages list
6. **After adding/removing agents**: Update the Agent Architecture section
7. **After changing agent responsibilities**: Update the agent table and interaction flow

### Update Procedure
1. Read the relevant section of this SKILL.md
2. Use `search_replace` to update the changed parts
3. If a reference file needs updating, edit the corresponding file in this directory
4. Keep SKILL.md under 500 lines; move detailed content to reference files

### When NOT to update
- Minor bug fixes that don't change architecture
- Cosmetic UI changes
- Dependency version bumps that don't change APIs
- Test-only changes

---

## Reference Files

- [backend-architecture.md](backend-architecture.md) — Detailed backend module structure, key classes, and domain model
- [frontend-architecture.md](frontend-architecture.md) — Frontend component tree, state management, routing details
- [ai-service-architecture.md](ai-service-architecture.md) — AI service internals, consumers, model training pipeline
- [data-flows.md](data-flows.md) — Detailed sequence diagrams for all primary flows
- [conventions.md](conventions.md) — Coding conventions, naming, code review standards

## Agent Definitions

- [architect-agent](../../.qoder/agents/architect-agent.md) — Architecture decisions, task decomposition, delegation
- [auth-profile-agent](../../.qoder/agents/auth-profile-agent.md) — Auth (JWT/OAuth/CAPTCHA) + user profile
- [resume-agent](../../.qoder/agents/resume-agent.md) — Resume upload/parse/versions/edit/download
- [job-matching-agent](../../.qoder/agents/job-matching-agent.md) — Job CRUD, pgvector recall, ranking, suitability
- [conversation-agent](../../.qoder/agents/conversation-agent.md) — Chat lifecycle, context assembly, AI reply, WebSocket
- [tracking-agent](../../.qoder/agents/tracking-agent.md) — Application status flow, kanban, statistics, follow-up notes
- [ai-pipeline-agent](../../.qoder/agents/ai-pipeline-agent.md) — LiteLLM gateway, embedding, worker, training, model mgmt
- [platform-agent](../../.qoder/agents/platform-agent.md) — Docker, MQ, Outbox, pgvector, Redis, MinIO, env vars, CI/CD
- [qa-review-agent](../../.qoder/agents/qa-review-agent.md) — Code review, test strategy, architecture rules, security scan, pre-release gate

## External Documentation

- [Architecture overview](../../docs/architecture/Architecture.md)
- [ADRs](../../docs/adr/)
- [API docs](../../docs/api/backend/)
- [Docker deployment](../../docs/deployment/DOCKER_DEPLOY.md)
- [Environment variables](../../docs/deployment/environment-variables.md)
