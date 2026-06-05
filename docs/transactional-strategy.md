# @Transactional Unified Strategy Document

> **Language**: English | [简体中文](./i18n/zh-Hans-CN/transactional-strategy.md) | [繁體中文](./i18n/zh-Hant-TW/transactional-strategy.md)

> This document defines the usage policy for Spring `@Transactional` in the JobCopilot backend `application` module.
> Goal: eliminate transaction boundary leaks, nested transaction pollution, and long-transaction blocking.
>
> Version: 2026-05-25 | Branch: `sanitize-for-oss`

---

## 1. Core Principles

| # | Principle | Rationale |
|---|---|---|
| 1 | **Commands use `@Transactional`, queries use `@Transactional(readOnly = true)`** | Leverage read-only tx optimization (Hibernate skips dirty-check / flush), reducing DB write locks. |
| 2 | **Class-level readOnly, method-level override** | For read-heavy Services, declare `@Transactional(readOnly = true)` on the class; write methods override with `@Transactional`. Reduces duplication. |
| 3 | **No network I/O inside transactions (HTTP, MQ, external files)** | Long transactions = long connection hold, hurting throughput; external failure causes rollback, conflicting with business semantics. |
| 4 | **Cross-aggregate writes have independent transactions** | Writes to different aggregate roots (Job, Resume, Conversation, etc.) should not share a transaction; use `Propagation.REQUIRES_NEW` or split to independent Services. |
| 5 | **Schedulers / async tasks must have independent transaction boundaries** | Scheduled tasks run long; default transaction should be minimal; when using Outbox, relay and cleanup are separate transactions. |
| 6 | **All `@Transactional` must declare explicit `timeout`** | Prevent infinite connection hold from slow queries, lock waits, or accidental network I/O inside the transaction. Pure DB ops: 30s; batch/sync: 60s. |
| 7 | **Aggregate roots must have optimistic locking (`@Version`)** | Prevent lost updates in concurrent write scenarios. Currently applied to `Job`, `ResumeGroup`, `Conversation`, `JobMatchResult`, `ApplicationTracking`, `User`, `MatchingModel`. |

---

## 2. Annotation Matrix

| Scenario | Recommended Annotation | Current Status |
|---|---|---|
| **Pure queries** (get/list/find/search) | Class-level `@Transactional(readOnly = true)` + no extra method annotation | ✅ Unified |
| **Single aggregate write** (save/update/delete) | Method-level `@Transactional` | ✅ Unified |
| **Batch writes** (batchUpsert) | Method-level `@Transactional` | ✅ Unified |
| **Write then send MQ / HTTP** | **Only write DB inside tx**, send MQ after tx commit (tx listener, ApplicationEvent, or Outbox) | ⚠️ See §3.1 |
| **Failure log persistence** (non-interfering) | `@Transactional(propagation = REQUIRES_NEW, timeout = 30)` | ✅ Correct |
| **Scheduler (Outbox relay)** | Read `@Transactional(readOnly = true, timeout = 30)` + update after send with `REQUIRES_NEW, timeout = 30` | ⚠️ See §3.2 |
| **Scheduler (cleanup)** | `@Transactional(timeout = 30)` (short tx, batch delete) | ✅ Unified |

---

## 3. Known Issues

### 3.1 [FIXED] Sending MQ Inside Transaction — `JobSubmissionService.submit()`

**File**: `app/.../JobSubmissionService.java:31`

```java
@Transactional
public JobResponse submit(UUID userId, SubmitJobRequest request) {
    ...
    jobRepository.save(job);          // ← DB write
    aiMessagePublisherPort.sendJobForParsing(...);  // ← Network I/O (RabbitMQ / HTTP)
    ...
}
```

**Risks**:
1. MQ send fails → entire tx rolls back → saved Job is lost, but user already received success response.
2. MQ send succeeds but tx rolls back → message sent but no DB record → consumer cannot find Job.

**Status**: ✅ **Fixed** — No code change required. `AiMessagePublisherPort` already uses the **Outbox Pattern** (only writes to the `outbox` table inside the transaction; `OutboxRelayScheduler` polls and sends to RabbitMQ). Verified during audit.

---

### 3.2 [FIXED] Scheduler Transaction Boundary Ambiguity

**File**: `app/.../OutboxRelayScheduler.java:38`

```java
@Transactional
public void relayPendingMessages() { ... }
```

**Risk**: Entire relay runs in one transaction: read pending → send one by one → update status. If message volume is large, the transaction becomes extremely long.

**Status**: ✅ **Fixed** in commit `618757f6`.

**Fix applied**:
- Polling uses `@Transactional(readOnly = true)`.
- Extracted `OutboxRelayTransactionService` with `@Transactional(propagation = Propagation.REQUIRES_NEW)`; each message's RabbitMQ `convertAndSend` + `outboxMessageRepository.save` runs in an independent transaction, preventing a single network failure or congestion from rolling back already-completed deliveries in the same batch.

---

### 3.3 [FIXED] Duplicate `@Transactional` on `JobApplicationService.submitJob()` and `JobSubmissionService.submit()`

**Files**:
- `JobApplicationService.java:53` (`@Transactional`)
- `JobSubmissionService.java:31` (`@Transactional`)

`JobApplicationService.submitJob()` calls `JobSubmissionService.submit()`, both annotated with `@Transactional`. Spring defaults to `REQUIRED`, so the inner layer joins the outer transaction. Functionally correct, but **architecturally redundant**: transaction boundaries should be declared at the outermost layer; inner pure-business-logic layers should not repeat the annotation.

**Status**: ✅ **Fixed** in commit `618757f6`.

**Fix applied**: Removed `@Transactional` from `JobSubmissionService.submit()`; transaction boundary is now controlled exclusively by `JobApplicationService.submitJob()`.
---

### 3.4 `MatchingApplicationService.startJobMatch()` Contains Vector Generation

**File**: `MatchingApplicationService.java:58`

**Status**: ✅ **Fixed**

**Problem**: `startJobMatch()` held `@Transactional` and, when the vector was missing, synchronously called `vectorGenerationPort.generateAndSaveVector()`, which ultimately triggers an HTTP call to the Embedding service. The long HTTP call blocked the DB transaction, and failure caused the entire match request to roll back.

**Fix**:
- Extract `MatchTransactionService` (package-private Bean), encapsulating only DB operations (recall + persist + outbox-write), declared with `@Transactional`.
- `MatchingApplicationService.startJobMatch()` drops `@Transactional` and is responsible for:
  1. Querying model version, resume vector (outside tx).
  2. If vector missing, triggering on-demand vector generation via HTTP (outside tx).
  3. Once vector is ready, delegating to `matchTransactionService.execute()` to enter a short transaction and complete recall + ranking request delivery.
- Transaction boundary: HTTP call (vector generation) is outside tx; lightweight DB operations are inside tx.

**Modified Files**:
- `MatchingApplicationService.java`: split into `MatchTransactionService` + `MatchingApplicationService`
- `MatchingApplicationServiceTest.java`: updated tests to match the split architecture

---

### 3.5 [FIXED] `JobApplicationService.handleJobProcessResult()` — HTTP Vector Generation Inside Transaction

**File**: `app/.../JobApplicationService.java`

```java
@Transactional
public void handleJobProcessResult(AiResultEvent event) {
    ...
    vectorGenerationService.generateForJob(...);  // ← HTTP call (Embedding service)
    jobDatasetSyncService.sync(job, event);       // ← DB write
    jobRepository.save(job);
}
```

**Risk**: `vectorGenerationService.generateForJob()` triggers an HTTP call to the Embedding service. If the Embedding service is slow or times out, the database connection is held for the entire duration, depleting the connection pool.

**Status**: ✅ **Fixed** in commit `XXXXXXX`.

**Fix applied**:
- Extract `JobResultTransactionService` (package-private Bean), encapsulating only DB operations (`markCompleted`/`markFailed` + `sync` + `save`), declared with `@Transactional(timeout = 60)`.
- `JobApplicationService.handleJobProcessResult()` drops `@Transactional`. After the short DB transaction completes, vector generation runs **outside** the transaction.
- All remaining `@Transactional` methods in `JobApplicationService` and across the `application` module now carry explicit `timeout` values (30s for pure DB writes, 60s for batch/sync operations).

**Modified Files**:
- `JobApplicationService.java`: refactored `handleJobProcessResult()`; added `timeout` to all `@Transactional`
- `JobResultTransactionService.java`: new file (package-private)

---

## 4. Best-Practice Cheat Sheet

```java
/**
 * Read-heavy Service: class-level readOnly, write methods override.
 */
@Service
@Transactional(readOnly = true)
public class JobApplicationService {

    public JobResponse getJob(...) { ... }          // inherits readOnly

    @Transactional(timeout = 30)
    public JobResponse updateJob(...) { ... }    // write with optimistic lock

    /** Optimistic locking on aggregate root. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Domain model exposes version for conflict detection. */
    public void markCompleted(ParsedJobContent content) {
        if (this.status != JobStatus.PARSING) {
            throw new IllegalStateException("Job must be PARSING to complete.");
        }
        this.status = JobStatus.COMPLETED;
        this.parsedContent = content;
    }
}

/**
 * Pure command Service: no class-level readOnly.
 */
@Service
@RequiredArgsConstructor
public class JobSubmissionService {
    // Do not declare class-level @Transactional; let caller control the boundary
}

/**
 * Independent transaction (failure log) with explicit timeout.
 */
@Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
public void saveFailedVector(...) { ... }
```

---

## 5. Checklist (for Code Review)

- [ ] Are query methods missing `readOnly = true`?
- [ ] Do write methods contain HTTP / MQ / file I/O? If so, are they moved outside the transaction?
- [ ] Is there nested `@Transactional` redundancy (both outer and inner Services declare it)?
- [ ] Are scheduler / async-task transactions minimized?
- [ ] Is `REQUIRES_NEW` abused (only for logs / compensation scenarios)?
- [ ] Does every `@Transactional` declare an explicit `timeout` (never rely on the default)?
- [ ] Does every aggregate root entity have `@Version` for optimistic locking?

---

## 6. Appendix: Full `@Transactional` Inventory

### 6.1 Class-Level Annotations

| Class | Annotation | Note |
|---|---|---|
| `ConversationApplicationService` | `@Transactional(readOnly = true)` | Class-level readOnly, individual write methods override |
| `ConversationQueryService` | `@Transactional(readOnly = true)` | Pure queries |
| `AuthApplicationService` | `@Transactional(readOnly = true)` | Class-level readOnly, register/login override |
| `ProfileApplicationService` | `@Transactional(readOnly = true)` | Class-level readOnly, update overrides |
| `ModelManagementApplicationService` | — | Two get methods use readOnly, switch uses read-write |

### 6.2 Write Operations (`@Transactional` without readOnly)

| Class | Method | Note |
|---|---|---|
| `ResumeUploadHandler.upload` | Upload resume | |
| `ResumeApplicationService.handleUpload` | Handle upload | |
| `ResumeApplicationService.handleEdit` | Edit | |
| `ResumeApplicationService.handleCreateVersion` | Create version | |
| `ResumeApplicationService.handleDelete` | Delete group | |
| `ResumeApplicationService.handleDeleteVersion` | Delete version | |
| `ResumeApplicationService.handleActivateVersion` | Activate version | |
| `ResumeApplicationService.handleParseResult` | Parse result | |
| `ResumeVersionChainManager.createVersion` | Version chain | |
| `ResumeDeletionService.deleteGroup` | Delete group | |
| `ResumeDeletionService.deleteVersion` | Delete version | |
| `ResumeParseResultHandler.handle` | Parse result | |
| `FailedVectorPersistenceService.saveFailedVector` | `REQUIRES_NEW` | ✅ Correct usage |
| `JobVectorBatchService.batchUpsert` | Batch upsert | |
| `ResumeVectorBatchService.batchUpsert` | Batch upsert | |
| `ConversationApplicationService.createConversation` | Create conversation | Overrides class-level readOnly |
| `ConversationApplicationService.sendMessage` | Send message | |
| `ConversationApplicationService.saveAiReply` | Save AI reply | Two overloads |
| `ConversationApplicationService.uploadAttachment` | Upload attachment | Contains file stream |
| `ConversationApplicationService.closeConversation` | Close conversation | |
| `ConversationApplicationService.deleteConversation` | Delete conversation | |
| `AuthApplicationService.registerByEmail` | Register | |
| `AuthApplicationService.loginByGoogle` | Google login | |
| `ProfileApplicationService.updateProfile` | Update profile | |
| `ProfileApplicationService.updateAvatar` | Update avatar | |
| `OutboxCleanupScheduler.cleanup` | Clean Outbox | |
| `OutboxRelayScheduler.relayPendingMessages` | ⚠️ Long transaction | |
| `ModelManagementApplicationService.switchActiveModel` | Switch model | |
| `MatchingApplicationService.startJobMatch` | ⚠️ External call? | |
| `MatchingApplicationService.saveMatchResult` | Save result | |
| `TrackingApplicationService.createTracking` | Create tracking | |
| `TrackingApplicationService.updateTracking` | Update tracking | |
| `TrackingApplicationService.deleteTracking` | Delete tracking | |
| `JobScoringContextLoader.load` | Load scoring context | |
| `JobScoringResultSaver.save` | Save scoring result | |
| `JobSubmissionService.submit` | ⚠️ Send MQ inside tx | See §3.1 |
| `JobApplicationService.submitJob` | Submit job | |
| `JobApplicationService.handleJobProcessResult` | Handle result | |
| `JobApplicationService.updateJob` | Update job | |
| `JobApplicationService.deleteJob` | Delete job | |
| `JobApplicationService.trackUserAction` | Track action | |

### 6.3 Read-Only Operations (`@Transactional(readOnly = true)`)

| Class | Method |
|---|---|
| `ResumeApplicationService.getGroup` | |
| `ResumeApplicationService.listUserGroups` | |
| `ResumeApplicationService.getVersion` | |
| `ConversationApplicationService` | (class-level) |
| `ConversationQueryService` | (class-level) |
| `AuthApplicationService` | (class-level, register/login override) |
| `ProfileApplicationService` | (class-level, update overrides) |
| `ModelManagementApplicationService.getActiveRecallModel` | |
| `ModelManagementApplicationService.getActiveRankerModel` | |
| `MatchingApplicationService.getMatchResult` | |
| `MatchingApplicationService.listMatchHistory` | |
| `TrackingStatsService.calculateStats` | |
| `TrackingApplicationService.getTracking` | |
| `TrackingApplicationService.listTrackings` | |
| `JobApplicationService.getJob` | |
| `JobApplicationService.listJobs` | |
| `JobApplicationService.getScoreHistory` | |

---

## 7. Revision History

| Date | Author | Content |
|---|---|---|
| 2026-05-25 | AI Architecture Audit | Added `@Version` optimistic locking to `Job` aggregate root (§1.7, §6.4). Fixed `handleJobProcessResult` HTTP-in-tx issue (§3.5). Global `timeout` enforcement across all `@Transactional` methods. |
