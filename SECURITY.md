# Security Policy

## Supported Versions

| Version | Supported |
|--------:|:---------:|
| 1.0.x   | ✅        |

## Reporting a Vulnerability

If you discover a security vulnerability in ResumeAssistant, please report it privately via email to the project maintainers. Do **not** open a public issue.

**Contact:** Please reach out through the project's designated security contact (maintainer email or private messaging channel).

We aim to respond within **48 hours** and will work with you to validate, prioritize, and fix the issue.

## Security Practices

### Authentication & Authorization
- All inter-service communication requires `X-Internal-API-Key` header validation in production environments.
- Backend JWT tokens use configurable secrets; no hardcoded default secrets in production builds.
- Refresh tokens are transmitted via `HttpOnly`, `SameSite=Strict`, `Secure` cookies.

### Data Protection
- File uploads undergo MIME type validation and filename sanitization (path traversal protection).
- S3/MinIO pre-signed URLs have configurable expiration (default 7 days, overridable via `storage.presigned-url-expiration-hours`).
- Sensitive logs (tokens, credentials) are masked or excluded from application logs.

### Rate Limiting & Resource Guards
- Embedding endpoint enforces batch size limits (`EMBEDDING_MAX_BATCH_SIZE`) and per-text length caps (`EMBEDDING_MAX_TEXT_LENGTH`).
- CAPTCHA verification uses hardened client IP detection (ignores `X-Forwarded-For` from untrusted sources).

### Infrastructure
- Actuator endpoints expose only `/health` and `/info` publicly; all other actuator paths are denied.
- MQ consumers implement idempotency checks to prevent duplicate processing.
- Non-development environments enforce mandatory `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, and `INTERNAL_API_KEY` at startup.

## Disclosure Policy

- Reporter receives confirmation within 48 hours.
- Patch prepared and validated internally.
- Fix released in the next patch version.
- Public disclosure (CVE/advisory) coordinated with the reporter after 30 days or sooner by mutual agreement.

## Known Limitations

- Development profile (`ENV=dev`) intentionally relaxes security checks for local debugging. **Never deploy `dev` to production.**
- Google Cloud credentials for Vertex AI should be mounted via secrets management (Kubernetes secrets, Vault, etc.) rather than committed to the repository.

---

*Last updated: 2026-05-26*
