# Security Policy

> Translations: [简体中文](docs/i18n/zh-Hans-CN/SECURITY.md) | [繁體中文](docs/i18n/zh-Hant-TW/SECURITY.md)

## Supported Versions

| Version | Supported |
|--------:|:---------:|
| `main` / latest release | ✅ |

## Reporting a Vulnerability

If you discover a security vulnerability in JobCopilot, please report it privately. Do **not** open a public issue.

**Preferred channel:** Use GitHub private vulnerability reporting:
`https://github.com/<owner>/<repo>/security/advisories/new`

If private vulnerability reporting is not available for your fork or mirror, contact the maintainers through a private channel and include enough detail to reproduce and assess the issue.

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

*Last updated: 2026-06-05*
