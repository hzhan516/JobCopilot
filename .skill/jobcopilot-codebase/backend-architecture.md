# Backend Architecture Details

## Module Dependency Graph

```
types (no deps)
  ↑
domain → types
  ↑
api → domain, types
  ↑
infrastructure → domain, types (implements domain ports)
  ↑
trigger → api, domain, types
  ↑
app → all modules (wiring + startup)
```

## Key Domain Packages

### domain/resume/
- **Entity**: `Resume`, `ResumeVersion`, `ResumeGroup`
- **Port**: `ResumeRepository` — persistence interface
- **Port**: `ResumeParsePort` — AI parsing interface
- **Service**: `ResumeDomainService` — business rules (version management, etc.)

### domain/job/
- **Entity**: `JobDescription`, `JobMatch`
- **Port**: `JobRepository`
- **Port**: `JobMatchPort` / `AiScoringPort` — AI matching & scoring
- **Service**: `JobMatchingDomainService`

### domain/user/
- **Entity**: `User`, `UserProfile`, `UserOAuthBinding`
- **Port**: `UserRepository`, `UserProfileRepository`, `UserOAuthBindingRepository`
- **Service**: `UserDomainService`

### domain/conversation/
- **Entity**: `Conversation`, `Message`
- **Port**: `ConversationRepository`
- **Service**: `ConversationDomainService`

### domain/tracking/
- **Entity**: `JobApplication`, `ApplicationStatus`
- **Port**: `ApplicationRepository`

### domain/shared/
- **Entity**: `OutboxMessage` — outbox pattern core entity
- **Port**: `FileStorageService` — file storage abstraction
- **Port**: `MessagePublisherPort` — message queue abstraction

## Key Infrastructure Adapters

| Port (domain) | Adapter (infrastructure) | Technology |
|---------------|--------------------------|------------|
| `ResumeRepository` | `ResumeJpaRepository` | Spring Data JPA + pgvector |
| `JobRepository` | `JobJpaRepository` | Spring Data JPA + pgvector |
| `UserRepository` | `UserJpaRepository` | Spring Data JPA |
| `AiScoringPort` | `AiScoringRestAdapter` | REST to AI Service |
| `MessagePublisherPort` | `AiMessagePublisherAdapter` | RabbitMQ via RabbitTemplate |
| `FileStorageService` | `LocalFileStorageService` / `MinioFileStorageService` | Conditional on `STORAGE_TYPE` |

## Key Application Services

Located in `backend/app/src/main/java/.../application/service/`:

- `ResumeApplicationService` — resume upload, parse orchestration, version control
- `JobApplicationService` — job CRUD, matching orchestration
- `UserApplicationService` — auth, profile management
- `ConversationApplicationService` — chat message handling
- `TrackingApplicationService` — application status tracking

## Key Schedulers

- `OutboxRelayScheduler` — polls `outbox_messages`, delivers to RabbitMQ (ShedLock protected)
- `IncrementalRetrainingScheduler` — triggers AI model retraining

## Key REST Controllers

Located in `backend/trigger/src/main/java/.../trigger/`:

- `AuthController` — login, register, refresh, logout, CAPTCHA
- `ResumeController` — resume CRUD, upload, parse trigger
- `JobController` — job CRUD, matching requests
- `ConversationController` — chat messages, WebSocket
- `TrackingController` — application status management
- `ProfileController` — user profile

## API Response Format

All API responses follow:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

## Profile-Specific Behavior

- **dev**: Flyway disabled, 24h JWT, detailed error responses, INTERNAL_API_KEY optional
- **prod**: Flyway enabled, short JWT, validate DDL, INTERNAL_API_KEY required
