# AI Chat Phase 3-5 release gate

This document is the operational completion contract for the Phase 3-5 repair. It does not replace the existing DDD/hexagonal boundaries: the frontend still calls only the backend, the backend remains the conversation source of truth, RabbitMQ carries asynchronous work, and `ai-service` owns provider and prompt behavior.

## Delivered scope

### Phase 3 - provider, prompt, and output reliability

- Provider configuration is validated by model prefix without exposing credentials.
- `/health/live` checks only process liveness; `/health/ready` checks MQ plus the cached provider probe; `/api/status` exposes only sanitized capability state.
- Provider calls use a single end-to-end deadline across attempts and the one allowed JSON repair.
- Native response schemas are requested only when the selected LiteLLM model reports support; strict Pydantic validation and one bounded repair remain the provider-neutral fallback.
- Resume, job, history, and attachment content are clipped to deterministic budgets and enclosed in explicit untrusted-data boundaries.

### Phase 4 - conversation read path and attachments

- The list endpoint uses an `updated_at DESC` summary projection and no longer hydrates every message.
- The detail endpoint reads a bounded page of messages (`size` defaults to 50 and is capped at 100), returned in ascending sequence order.
- Chat supports up to three PDF, DOCX, TXT, or Markdown attachments, each at most 10 MB. The backend validates ownership and managed-storage references before publishing them to AI.
- The frontend restores server-backed conversation and exact `aiReply.requestId` state after remounting. Compaction exposes and retains its exact terminal correlation ID.

The current message model stores one display `fileUrl`; all validated references are still forwarded to AI. A future schema migration is required if every attachment must be rendered independently on historical messages.

### Phase 5 - evaluation and release evidence

- `ai-service/evals/chat_golden_v1.jsonl` contains 30 synthetic, non-production cases spanning English, Simplified Chinese, Traditional Chinese, missing context, long history, attachments, and prompt-injection attempts.
- `ai-service/scripts/evaluate_chat_golden.py` validates the fixture offline or runs it against the configured provider. Its report contains scores, latency, token counts, and estimated cost, but no prompts or model replies.
- `frontend/acceptance/ai-chat.acceptance.spec.ts` verifies the exact initialization, message, assistant sequence, nonce response, and compaction request IDs. On success it writes a sanitized provider-smoke report.
- `scripts/check-ai-chat-release.py` fails closed unless golden evaluation, real-provider smoke, and fault-injection evidence all meet the configured thresholds.

## Required fault-injection evidence

Run these scenarios in an isolated non-production environment and write one sanitized JSON report. Record IDs, timestamps, state transitions, latency, and stable error codes only—never prompts, resume text, model output, credentials, or signed URLs.

1. `mqInterruption`: interrupt RabbitMQ during a pending request; verify bounded outbox/result retry, no lost terminal state, and no duplicate assistant message.
2. `providerTimeout`: force the provider beyond the configured deadline; verify `UPSTREAM_TIMEOUT` and a retryable terminal UI state.
3. `duplicateResult`: redeliver the same successful result; verify request-ID idempotency and exactly one assistant message.
4. `lateResult`: deliver a result after the backend watchdog has marked the request timed out; verify it cannot overwrite a newer request.

Required report shape:

```json
{
  "completedAt": "2026-07-18T12:00:00Z",
  "scenarios": {
    "mqInterruption": { "passed": true },
    "providerTimeout": { "passed": true },
    "duplicateResult": { "passed": true },
    "lateResult": { "passed": true }
  }
}
```

## Release command

After producing fresh evidence (default maximum age: 24 hours):

```powershell
python scripts/check-ai-chat-release.py `
  --golden-report ai-service/evals/reports/chat-golden-latest.json `
  --provider-smoke-report frontend/test-results/ai-chat-provider-smoke.json `
  --fault-report test-results/ai-chat-fault-injection.json
```

Default GO thresholds are: at least 30 cases, pass rate at least 90%, zero schema failures, zero safety failures, p95 at most 90 seconds, exact provider-smoke correlation IDs, and all four fault scenarios passed. Missing, malformed, stale, or incomplete evidence is an automatic NO-GO.

## Current decision

`NO-GO` until the changed database projection is exercised against PostgreSQL, all migrations are applied in an isolated environment, a real configured provider completes the acceptance test, and all four fault-injection scenarios pass. Unit tests, compilation, and fixture validation cannot substitute for these runtime gates.
