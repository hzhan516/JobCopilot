<!-- Language Switcher -->
> **English** | [简体中文](../i18n/zh-Hans-CN/architecture/Architecture.md) | [繁體中文](../i18n/zh-Hant-TW/architecture/Architecture.md)

# JobCopilot Architecture

## Document Information

| Field | Value |
|-------|-------|
| Project | JobCopilot |
| Document | System Architecture |
| Version | 2.0.0 |
| Status | Living document |
| Last Updated | 2026-06-05 |
| Audience | Maintainers, contributors, deployers |

## Overview

JobCopilot is a containerized AI job-search platform. It combines a React frontend, a Java Spring Boot backend, a Python FastAPI AI service, an AI worker, PostgreSQL with pgvector, RabbitMQ, Redis, and MinIO.

The design favors clear service boundaries, asynchronous AI processing, provider-neutral LiteLLM integration, and Docker Compose deployments that expose only the frontend gateway by default.

## Architecture Principles

| Principle | Design Choice |
|-----------|---------------|
| Clear ownership | Frontend owns UI, backend owns business workflows and persistence, AI services own model calls and ranking logic |
| Domain-first backend | Backend modules follow DDD and hexagonal architecture: `types`, `domain`, `api`, `infrastructure`, `trigger`, `app` |
| Async by default for long AI work | RabbitMQ carries parse, rank, conversation, and feedback jobs between backend and AI services |
| Unified vector storage | PostgreSQL stores business entities and pgvector embeddings in one database boundary |
| Provider-neutral AI | LiteLLM-compatible model names and keys are configured through environment variables |
| Isolated runtime tiers | Docker networks isolate public, internal, and database tiers |
| Operational portability | `.env.example` and `docker-compose.yml` provide a reproducible local stack |

## Runtime Topology

```text
Browser
  |
  | HTTP :${FRONTEND_HOST_PORT:-80}
  v
Frontend container
  React static app + Nginx reverse proxy
  |
  | HTTP /api, /health
  v
Backend container
  Spring Boot API, auth, domain workflows, vector persistence
  |-- JDBC --------> PostgreSQL + pgvector
  |-- AMQP --------> RabbitMQ --------> AI worker container
  |-- HTTP --------> AI Service container
  |-- Redis -------> Redis
  `-- Local files -> shared upload volume

AI Service / AI worker
  |-- LiteLLM-compatible provider for parsing, embeddings, ranking, chat
  |-- Backend internal API for vector upserts and baseline features
  |-- Redis feedback buffer, locks, and model reload Pub/Sub
  `-- MinIO model registry for LightGBM artifacts
```

## Components

| Component | Technology | Network Exposure | Responsibility |
|-----------|------------|------------------|----------------|
| Frontend / Gateway | React 19, Vite 7, Nginx | Host `${FRONTEND_HOST_PORT:-80}` -> container `8080` | UI delivery, reverse proxy for backend API and health checks |
| Backend | Java 21, Spring Boot 3.5 | Internal `8080`; optional dev-only host mapping | REST API, authentication, domain workflows, transactions, persistence, MQ publishing/consuming |
| AI Service | Python 3.11, FastAPI, LiteLLM | Internal `8000`; optional dev-only host mapping | Embeddings, parsing, ranking, chat-oriented endpoints |
| AI Worker | Python 3.11, RabbitMQ consumers, LightGBM | Internal worker process | Async task processing, feedback ingestion, incremental model training |
| PostgreSQL | PostgreSQL 15 + pgvector | Internal `5432` on `db-network` | Business tables and vector tables |
| RabbitMQ | RabbitMQ 3 management image | Internal `5672`; management UI disabled by default | Durable queue transport and DLQ support |
| Redis | Redis 7 | Internal `6379` | CAPTCHA state, distributed locks, feedback buffer, model reload Pub/Sub |
| MinIO | S3-compatible object storage | Internal `9000`; console disabled by default | LightGBM model artifacts and model metadata |

## Backend Architecture

The backend is a Maven multi-module Spring Boot system.

| Module | Role | Dependency Direction |
|--------|------|----------------------|
| `types` | Shared enums, value types, constants | No project module dependencies |
| `domain` | Entities, value objects, ports, domain services | Depends on `types` |
| `api` | DTOs, commands, queries, facade interfaces | Depends on `domain`, `types` |
| `infrastructure` | JPA, Redis, RabbitMQ, storage, external service adapters | Implements domain/API ports |
| `trigger` | REST controllers, WebSocket endpoints, event/MQ listeners | Calls application/API interfaces |
| `app` | Spring Boot startup, configuration, application services, schedulers | Wires all modules |

Backend rules:

- Domain code should not depend on Spring, persistence, HTTP, or message broker APIs.
- Application services own transaction boundaries.
- Network I/O should not be performed inside database transactions.
- Asynchronous side effects should use RabbitMQ and outbox-style reliability where applicable.
- Vector dimensions are controlled by `LLM_EMBEDDING_MODEL_DIMENSION` and must match the configured embedding model.

## AI Service Architecture

The Python service contains both synchronous API endpoints and background worker logic.

| Area | Path | Responsibility |
|------|------|----------------|
| API | `ai-service/app/main.py`, `app/api/` | FastAPI endpoints, health checks, model manager |
| Domain | `ai-service/app/domain/` | AI-domain abstractions and shared logic |
| Infrastructure | `ai-service/app/infrastructure/` | Backend internal API client, Redis client, MinIO client |
| Services | `ai-service/app/services/` | LLM calls, embeddings, parsing, matching, ranking |
| MQ | `ai-service/app/mq/`, `app/worker/consumers/` | RabbitMQ consumers and message handlers |
| Worker | `ai-service/app/worker_main.py`, `app/worker/` | Feedback processing and LightGBM retraining |

AI design notes:

- LiteLLM is the abstraction for text, vision, and embedding models.
- Embedding output length is validated against `LLM_EMBEDDING_MODEL_DIMENSION`.
- The AI worker stores feedback samples in Redis before training.
- Trained LightGBM artifacts are uploaded to MinIO and announced through Redis Pub/Sub.
- Backend internal APIs are used for vector upserts and baseline feature retrieval.

## Data Architecture

| Store | Contents | Notes |
|-------|----------|-------|
| PostgreSQL | Users, resumes, jobs, conversations, tracking records, outbox/task state | Source of truth for business data |
| pgvector | Resume and job embeddings | Used for semantic recall and similarity search |
| Redis | CAPTCHA challenge/token/rate-limit state, AI feedback buffer, locks, model reload events | Requires `REDIS_PASSWORD` in compose |
| RabbitMQ | AI request/response queues, feedback queues, DLQ | Internal-only by default |
| Local upload volume | Uploaded resume files in the default Compose profile | Backend Compose config uses `STORAGE_TYPE=local` |
| MinIO | AI model artifacts and metadata | Used by AI worker/model manager; resume storage can use MinIO in custom deployments |

## Primary Flows

### Resume Upload And Parsing

```text
User -> Frontend -> Backend
Backend -> PostgreSQL: store resume metadata and version
Backend -> Local upload volume: store file
Backend -> RabbitMQ: publish parse request
AI worker -> LiteLLM provider: parse document content
AI worker -> Backend: return structured data and vectors
Backend -> PostgreSQL/pgvector: persist parsed data and embeddings
Frontend -> Backend: poll or fetch updated resume status
```

### Job Matching

```text
Frontend -> Backend: request matches
Backend -> PostgreSQL/pgvector: semantic recall
Backend -> AI Service or RabbitMQ: ranking/explanation work
AI service -> LiteLLM provider: rank or explain matches
Backend -> Frontend: return ordered jobs and match metadata
```

### Feedback And Incremental Training

```text
User scoring behavior -> Backend
Backend -> RabbitMQ: publish feedback event
AI worker -> Redis: buffer labeled sample
AI worker -> Backend internal API: fetch baseline features
AI worker -> LightGBM: train when threshold/lock allows
AI worker -> MinIO: upload model artifact and latest metadata
AI worker -> Redis Pub/Sub: publish model reload event
AI Service model manager -> MinIO: load latest model
```

## Deployment Topology

Docker Compose defines three tiers:

| Network | Members | Purpose |
|---------|---------|---------|
| `public-network` | `frontend`, `backend` | Browser-facing gateway reaches backend |
| `internal-network` | `backend`, `ai-service`, `ai-worker`, `rabbitmq`, `redis`, `minio` | Internal service-to-service communication |
| `db-network` | `backend`, `postgres` | Database access isolated from other services |

Only `frontend` maps a host port by default. Backend, AI service, RabbitMQ management, Redis, PostgreSQL, and MinIO are internal unless development-only port mappings are intentionally uncommented.

## Security Boundaries

- Secrets are loaded from `.env`; `.env.example` documents required local values.
- `JWT_SECRET`, `INTERNAL_API_KEY`, database credentials, RabbitMQ credentials, Redis password, and MinIO keys must be changed for shared or production-like environments.
- Backend validates authentication and authorization for user-facing workflows.
- The AI service should use backend internal APIs instead of direct database access.
- Docker network isolation prevents host access to data-plane services by default.
- Uploaded files and model artifacts are stored separately from application code.

## Extensibility

| Change | Expected Extension Point |
|--------|--------------------------|
| Add a storage backend | Implement backend storage adapter and set `STORAGE_TYPE` |
| Add an LLM provider | Configure LiteLLM model names and provider API key in `.env` |
| Add an AI task | Add RabbitMQ message contract, backend publisher/listener, AI consumer |
| Add a domain workflow | Add domain model/port, application service, infrastructure adapter, trigger endpoint |
| Add observability | Extend Compose or deployment manifests with metrics/logging services |

## Documentation Links

- [Docker deployment](../deployment/DOCKER_DEPLOY.md)
- [Environment variables](../deployment/environment-variables.md)
- [Transactional strategy](../transactional-strategy.md)
- [Branching and commits](../BRANCHING_AND_COMMITS.md)
- [Architecture Decision Records](../adr/)
