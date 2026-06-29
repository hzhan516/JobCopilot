# Backend Architecture Details

## Module Dependency Graph

```
types (no deps)
  ↑
domain → types
  ↑
api → domain, types
  ↑
infrastructure → api, domain, types (implements domain/api ports)
  ↑
trigger → api, domain, infrastructure, types (controllers + listeners)
  ↑
app → trigger (wiring + startup; aggregates all modules transitively)
```

Maven modules are declared in `backend/pom.xml`: `types`, `domain`, `api`, `infrastructure`, `trigger`, `app`.

## Key Domain Packages

### domain/resume/
- **Entities**: `ResumeGroup` (aggregate root), `ResumeVersion`
- **Repositories**: `ResumeGroupRepository`, `ResumeVersionRepository`
- **Services**: `ResumeConverterService`, `VectorGenerationService`
- **Value objects**: `ParseStatus`
- **Invariants**: only one `ACTIVE` version per `VersionType` (`ORIGINAL`, `CONVERTED`, `AI_OPTIMIZED`) inside a group; `ORIGINAL` versions are immutable.

### domain/job/
- **Entities**: `Job` (aggregate root), `JobScoreRecord`
- **Repositories**: `JobRepository`, `JobScoreRepository`
- **Port**: `AiScoringPort` — synchronous AI suitability scoring interface
- **Services**: `ScreenshotValidator`, `VectorSimilarityService`
- **Value objects**: `JobStatus`, `ParsedJobContent`, `ScrapeResult`

### domain/embedding/
- **Entities**: `JobVector`, `ResumeVector`
- **Ports**: `VectorEmbeddingPort`, `VectorGenerationPort`, `ModelRetrainingPort`
- **Repositories**: `JobVectorRepository`, `ResumeVectorRepository`
- **Value objects**: `JobVectorSearchResult`, `VectorStatus`

### domain/matching/
- **Entities**: `JobMatchResult` (aggregate root), `JobDataset`, `MatchingModel`
- **Port**: `VectorSearchPort` — pgvector-backed recall interface
- **Repositories**: `JobMatchResultRepository`, `JobDatasetRepository`, `MatchingModelRepository`
- **Service**: `ModelManagementService`
- **Value objects**: `MatchAlgorithm`, `MatchStatus`, `ModelType`, `RankedJob`, `RecallResult`

### domain/conversation/
- **Entities**: `Conversation` (aggregate root), `Message`
- **Repository**: `ConversationRepository`
- **Value objects**: `ConversationStatus`, `MessageRole`

### domain/tracking/
- **Entities**: `ApplicationTracking` (aggregate root), `TrackingEvent`
- **Repository**: `ApplicationTrackingRepository`
- **Service**: `ApplicationTrackingDomainService`
- **Value object**: `ApplicationStatus`

### domain/user/
- **Entities**: `User` (aggregate root), `UserCredential`, `UserOAuthBinding`, `UserProfile`
- **Ports**: `CaptchaStorePort`, `EmailSenderPort`, `GoogleTokenVerifierPort`
- **Repositories**: `UserRepository`, `UserCredentialRepository`, `UserProfileRepository`, `UserOAuthBindingRepository`
- **Service**: `PasswordEncoder`

### domain/shared/
- **Entities**: `AggregateRoot`, `Entity` (base types), `OutboxMessage`
- **Events**: `AiResultEvent`, `ConversationRequestCommand`, `JobParseCommand`, `JobRankCommand`, `ResumeParseCommand`, `UserFeedbackCommand`, `VectorGenCommand`
- **Exceptions**: `BusinessException`, `DomainException`, `LocalizedException`, `AiServiceUnavailableException`, `StorageException`
- **Ports**: `AiMessagePublisherPort`, `FileStorageService`
- **Repository**: `OutboxMessageRepository`
- **Services**: `DocumentFormatConverter`, `MessageProvider`
- **Value object**: `DocumentFormat`

## Key Infrastructure Adapters

| Port (domain/api) | Adapter (infrastructure) | Technology |
|-------------------|--------------------------|------------|
| `ResumeGroupRepository` | `ResumeGroupRepositoryImpl` + `JpaResumeGroupRepository` | Spring Data JPA |
| `ResumeVersionRepository` | `ResumeVersionRepositoryImpl` + `JpaResumeVersionRepository` | Spring Data JPA |
| `JobRepository` | `JobRepositoryImpl` + `JpaJobRepository` | Spring Data JPA |
| `JobScoreRepository` | `JobScoreRepositoryImpl` + `JpaJobScoreRepository` | Spring Data JPA |
| `UserRepository` | `UserRepositoryImpl` + `JpaUserRepository` | Spring Data JPA |
| `UserCredentialRepository` | `UserCredentialRepositoryImpl` + `JpaUserCredentialRepository` | Spring Data JPA |
| `UserProfileRepository` | `UserProfileRepositoryImpl` + `JpaUserProfileRepository` | Spring Data JPA |
| `UserOAuthBindingRepository` | `UserOAuthBindingRepositoryImpl` + `UserOAuthBindingJpaRepository` | Spring Data JPA |
| `ConversationRepository` | `ConversationRepositoryImpl` + `JpaConversationRepository` | Spring Data JPA |
| `ApplicationTrackingRepository` | `ApplicationTrackingRepositoryImpl` + `JpaApplicationTrackingRepository` | Spring Data JPA |
| `JobVectorRepository` | `JobVectorRepositoryImpl` + `JobVectorJpaRepository` | Spring Data JPA + pgvector |
| `ResumeVectorRepository` | `ResumeVectorRepositoryImpl` + `ResumeVectorJpaRepository` | Spring Data JPA + pgvector |
| `JobDatasetRepository` | `JobDatasetRepositoryImpl` + `JpaJobDatasetRepository` | Spring Data JPA |
| `JobMatchResultRepository` | `JobMatchResultRepositoryImpl` + `JpaJobMatchResultRepository` | Spring Data JPA |
| `MatchingModelRepository` | `MatchingModelRepositoryImpl` + `JpaMatchingModelRepository` | Spring Data JPA |
| `OutboxMessageRepository` | `OutboxMessageRepositoryImpl` + `JpaOutboxMessageRepository` | Spring Data JPA |
| `AiMessagePublisherPort` | `AiMessagePublisherAdapter` | Outbox pattern (writes `OutboxMessage`, relayed by `OutboxRelayScheduler`) |
| `AiScoringPort` | `AiScoringRestAdapter` | REST to AI Service `/api/v1/suitability` |
| `VectorEmbeddingPort` | `VectorEmbeddingRestAdapter` | REST to AI Service `/api/v1/ai/embeddings` |
| `ModelRetrainingPort` | `ModelRetrainingRestAdapter` | REST to AI Service `/api/v1/admin/recompute-model` |
| `VectorGenerationPort` | `VectorGenerationFacadeAdapter` | Bridges to API-layer `VectorFacade` |
| `VectorSearchPort` | `PgVectorSearchService` | JPA native SQL over `job_vectors` |
| `FileStorageService` | `MinioFileStorageService` / `LocalFileStorageService` | Conditional on `storage.type` (`minio` default, `local`, `s3`, `oss`) |
| `CaptchaStorePort` | `RedisCaptchaStoreAdapter` | Redis |
| `EmailSenderPort` | `JavaMailEmailSenderAdapter` / `NoOpEmailSenderAdapter` | Spring Mail / no-op |
| `DocumentFormatConverter` | `CompositeDocumentConverter`, `PdfConverter`, `WordConverter`, `MarkdownConverter` | PDFBox / OpenPDF, documents4j, Flexmark, Pandoc |
| `PasswordEncoder` | `BCryptPasswordEncoderImpl` | BCrypt |
| `GoogleTokenVerifierPort` | `GoogleIdTokenVerifier` | Google IdToken |

## Key Application Services

Located in `backend/app/src/main/java/.../application/`:

- `ResumeApplicationService`, `ResumeUploadHandler`, `ResumeParseResultHandler`, `ResumeDownloadService`, `ResumeDeletionService`, `ResumeVersionChainManager`, `ResumeAccessControl`
- `JobApplicationService`, `JobSubmissionService`, `JobScoringContextLoader`, `JobScoringResultSaver`, `JobResultTransactionService`, `JobDatasetSyncService`, `JobAccessControl`
- `MatchingApplicationService`, `ModelManagementApplicationService`
- `VectorApplicationService`, `JobVectorBatchService`, `ResumeVectorBatchService`, `JobVectorSearchService`, `FailedVectorPersistenceService`
- `ConversationApplicationService`, `ConversationMessageService`, `ConversationQueryService`, `ConversationLifecycleService`, `ConversationContextService`, `ConversationAttachmentService`, `ConversationFailureMessageResolver`, `AiOptimizedResumeService`
- `TrackingApplicationService`, `TrackingStatsService`
- `AuthApplicationService`, `ProfileApplicationService`, `CaptchaService`, `VerificationCodeService`

Spring wiring for plain-Java domain services is provided by `ApplicationDomainServiceConfig` and `DomainServiceConfig` in `app`.

## Key Schedulers

All schedulers live in `backend/app/src/main/java/.../application/shared/scheduler/` and are protected by ShedLock using Redis:

- `OutboxRelayScheduler` — polls every 2 seconds for `PENDING` `OutboxMessage` rows and relays them to RabbitMQ via `OutboxRelayTransactionService` (per-message `REQUIRES_NEW` transaction)
- `OutboxCleanupScheduler` — daily at 03:00 deletes `SENT` outbox records older than 7 days
- `IncrementalRetrainingScheduler` — daily at 02:00 calls `ModelRetrainingPort.triggerRetraining()` to request AI Service model weight recomputation

## Key REST Controllers

Located in `backend/trigger/src/main/java/.../trigger/http/controller/`:

| Controller | Path | Purpose |
|------------|------|---------|
| `AuthController` | `/v1/auth/**` | email registration/login, Google login, refresh, logout, verification-code |
| `CaptchaController` | `/v1/auth/captcha*` | slider CAPTCHA challenge and verify |
| `ProfileController` | `/v1/profile` | get/update profile and avatar |
| `ResumeController` | `/v1/resumes` | upload, download, group/version CRUD, activate version |
| `JobController` | `/v1/jobs` | submit, CRUD, score, track action, match, match history, score history, vector search |
| `JobDatasetController` | `/v1/job-dataset` | internal AI-service dataset endpoint (protected by `INTERNAL_API_KEY`) |
| `ConversationController` | `/v1/conversations` | create, send message, list, get, close, delete, upload attachment, pseudo-stream reply |
| `TrackingController` | `/v1/trackings` | create, list, get, update, delete, stats |
| `JobVectorController` | `/v1/job-vectors` | batch upsert of job vectors |
| `ResumeVectorController` | `/v1/resume-vectors` | batch upsert of resume vectors |

Inbound AI result handling is performed by `AiResultMessageListener` in `backend/trigger/src/main/java/.../trigger/listener/ai/`, which listens to response queues and delegates to API-layer facades.

## RabbitMQ Topology

Configured in `backend/infrastructure/src/main/java/.../infrastructure/messaging/config/RabbitMqConfig.java`:

- Exchange: `ai.direct.exchange`
- Dead-letter exchange: `ai.dlx.exchange`, queue: `ai.dlq.queue`
- Request queues (to AI Service): `ai.queue.job.parse`, `ai.queue.resume.parse`, `ai.queue.conversation`, `ai.queue.job.rank`, `ai.queue.feedback`
- Response queues (from AI Service): `backend.queue.job.parse`, `backend.queue.resume.parse`, `backend.queue.conversation`, `backend.queue.job.rank`

## API Response Format

All API responses use `ApiResponse<T>` from `backend/api/src/main/java/.../api/common/dto/ApiResponse.java`:

```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

Success messages are resolved via the injected `MessageProvider` (i18n). Error responses use the same envelope with an appropriate `code` and `message`.

## Profile-Specific Behavior

- **dev** (`application-dev.yml`):
  - Flyway disabled by default (`spring.flyway.enabled: false`)
  - JWT access token expires in 24 hours, refresh token in 7 days
  - Debug logging for Hibernate SQL and application logs
  - Email verification disabled by default
  - CAPTCHA enabled by default
  - H2 console permitted by `SecurityConfig`
  - Default storage: MinIO (`storage.type: minio`)

- **prod** (`application-prod.yml`):
  - Flyway enabled (`spring.flyway.enabled: true`) with migrations in `classpath:db/migration`
  - Hibernate `ddl-auto: validate`
  - JWT access token expires in 1 hour, refresh token in 7 days
  - Info-level logging
  - H2 console denied
  - Default storage: MinIO (`storage.type: minio`)
  - `INTERNAL_API_KEY` required for `/v1/job-dataset` when set
