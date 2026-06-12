---
name: jobcopilot-codebase
description: Comprehensive knowledge of the JobCopilot codebase architecture, module structure, key patterns, and navigation guide. Use when working on any part of this project, exploring the codebase, adding new features, fixing bugs, or understanding how components interact. This skill should be consulted before making any code changes to ensure alignment with existing architecture and conventions.
---

# JobCopilot Codebase Guide

## Quick Reference

JobCopilot is an AI-powered job search platform with three main services:

| Layer | Tech | Entry Point | Port (internal) |
|-------|------|-------------|-----------------|
| **Frontend** | React 19 + Vite 7 + TypeScript 5.9 | `frontend/src/App.tsx` → `main.tsx` | 5173 (dev) |
| **Backend** | Java 21 + Spring Boot 3.5 + Maven | `backend/app/.../Application.java` | 8080 |
| **AI Service** | Python 3.11 + FastAPI + LiteLLM | `ai-service/app/main.py` | 8000 |

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

**Three Docker networks**: `public-network` (frontend+backend), `internal-network` (backend+ai+redis+mq+minio), `db-network` (backend+postgres). Only frontend port 80 is exposed.

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
- `domain/job/` — Job descriptions, matching ports
- `domain/user/` — User, profile, OAuth binding
- `domain/conversation/` — Chat messages, context
- `domain/tracking/` — Job application tracking
- `domain/shared/` — Outbox message, file storage port, exceptions

---

## AI Service Module Map

| Area | Path | Responsibility |
|------|------|----------------|
| API endpoints | `ai-service/app/main.py`, `app/api/` | FastAPI routes, health, embedding, matching, suitability |
| Config | `ai-service/app/config.py` | All env vars, queue names, model names |
| Services | `ai-service/app/services/` | LLM calls (`llm_client.py`), resume/job parsing, matching, ranking, embeddings |
| MQ Consumers | `ai-service/app/mq/consumer.py` | RabbitMQ consumer setup |
| Worker Consumers | `ai-service/app/worker/consumers/` | `parse_consumer.py`, `rank_consumer.py`, etc. |
| Worker Main | `ai-service/app/worker_main.py` | Worker entry point (separate container) |
| Infrastructure | `ai-service/app/infrastructure/` | Backend API client, Redis client, MinIO client |
| Schemas | `ai-service/app/schemas.py` | Pydantic models for all requests/responses |

**AI Service and AI Worker share the same Docker image** but run different commands:
- `ai-service`: `uvicorn app.main:app` — serves REST API + runs MQ consumers
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
| Hooks | `frontend/src/hooks/` | `useAuth`, `useAbortableRequest`, `useMobile`, `useTimeZone` |
| i18n | `frontend/src/i18n/`, `src/locales/` | en, zh-Hans, zh-Hant |
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
- `OutboxRelayScheduler` polls PENDING messages and delivers to RabbitMQ
- Uses ShedLock (Redis-backed) to prevent duplicate relay across instances
- **NEVER** call `rabbitTemplate.convertAndSend()` directly inside `@Transactional`

### RabbitMQ Queue Pairs
- Request queues: `ai.queue.{job.parse, resume.parse, conversation, job.rank, feedback}`
- Response queues: `backend.queue.{job.parse, resume.parse, conversation, job.rank}`
- DLQ: `ai.dlq.queue` via `ai.dlx.exchange`

### LiteLLM Provider Abstraction
- All LLM calls go through LiteLLM with provider-prefixed model names (e.g., `gemini/gemini-2.5-flash`)
- Config keys: `LLM_TEXT_MODEL`, `LLM_VISION_MODEL`, `LLM_EMBEDDING_MODEL`
- Embedding dimension (`LLM_EMBEDDING_MODEL_DIMENSION`) must match both backend and AI service

### Frontend Patterns
- **Zustand** for state management (not Redux)
- **shadcn/ui** component library (Radix primitives + Tailwind)
- **i18next** for internationalization (en, zh-Hans, zh-Hant)
- **Axios interceptors** handle JWT refresh automatically
- **Abortable requests** pattern for canceling in-flight API calls on rapid user actions

---

## Key Data Flows

### Resume Upload → Parse
```
User upload → Backend saves file + metadata
  → Outbox → RabbitMQ ai.queue.resume.parse
  → AI worker consumer → LiteLLM parse → Backend callback
  → PostgreSQL (structured data) + pgvector (embedding)
```

### Job Matching
```
Frontend request → Backend pgvector cosine similarity search
  → AI Service / RabbitMQ for ranking → LiteLLM rank/explain
  → Return scored jobs to frontend
```

### Feedback → Incremental Training
```
User scoring → Backend → RabbitMQ ai.queue.feedback
  → AI Worker buffers in Redis → fetches baseline features from backend
  → LightGBM train (when samples ≥ threshold) → MinIO artifact
  → Redis Pub/Sub reload event → AI Service loads new model
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

## External Documentation

- [Architecture overview](file:///home/doyle/Codes/JobCopilot/docs/architecture/Architecture.md)
- [ADRs](file:///home/doyle/Codes/JobCopilot/docs/adr/)
- [API docs](file:///home/doyle/Codes/JobCopilot/docs/api/backend/)
- [Docker deployment](file:///home/doyle/Codes/JobCopilot/docs/deployment/DOCKER_DEPLOY.md)
- [Environment variables](file:///home/doyle/Codes/JobCopilot/docs/deployment/environment-variables.md)
