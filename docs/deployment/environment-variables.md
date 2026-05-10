# Resume Assistant — Environment Variables Reference

> [简体中文](../i18n/zh-Hans-CN/deployment/environment-variables.md) | [繁體中文](../i18n/zh-Hant-TW/deployment/environment-variables.md)

This document describes every environment variable used by the Resume Assistant stack. Variables are organized by functional area, matching the comment blocks in `.env.example`.

> **Quick tip**: Run `cp .env.example .env` before editing. Never commit `.env` to version control.

---

## Table of Contents

- [Docker Compose Configuration](#docker-compose-configuration)
- [A. Database / PostgreSQL](#a-database--postgresql)
- [B. Message Queue / RabbitMQ](#b-message-queue--rabbitmq)
- [C. Authentication / JWT](#c-authentication--jwt)
- [D. Frontend / Web App](#d-frontend--web-app)
- [E. Spring Boot / Backend](#e-spring-boot--backend)
- [K. JWT Token Lifetime](#k-jwt-token-lifetime)
- [L. AI Service Connection](#l-ai-service-connection)
- [M. File Storage](#m-file-storage)
- [N. Backend Logging](#n-backend-logging)
- [O. Email Verification Configuration](#o-email-verification-configuration)
- [P. CAPTCHA Configuration](#p-captcha-configuration)
- [F. AI Provider Keys](#f-ai-provider-keys)
- [G. Model Parameters](#g-model-parameters)
- [H. AI Service Logging](#h-ai-service-logging)
- [I. Vertex AI Settings](#i-vertex-ai-settings)
- [J. Internal API Key](#j-internal-api-key)

---

## Docker Compose Configuration

The following values are **not** defined in `.env.example` but are configurable via `docker-compose.yml`. They affect container naming and host port binding.

### `COMPOSE_PROJECT_NAME`

| Field | Value |
|-------|-------|
| **Purpose** | Prefix for container names, volumes, and networks. Enables multiple independent instances on the same Docker host. |
| **Default** | Directory name of the project root (e.g. `ser594_ai_prject`) |
| **Valid values** | Any lowercase alphanumeric string with hyphens/underscores |
| **Security notes** | Using distinct project names prevents accidental cross-contamination between dev, staging, and production stacks. |
| **Common mistakes** | Using the same project name for two clones of the repo causes port and volume conflicts. |

### `FRONTEND_HOST_PORT`

| Field | Value |
|-------|-------|
| **Purpose** | The host port mapped to the Nginx container's port `80`. |
| **Default** | `8081` (in `docker-compose.yml.example`) |
| **Valid values** | Any free TCP port on the host (`80`, `8080`, `8081`, `3000`, etc.) |
| **Security notes** | In production, this should be `80` (or `443` behind an external TLS terminator). Do not expose backend/AI/database ports to the host. |
| **Common mistakes** | Setting this to `80` on macOS/Linux without `sudo` fails because ports < 1024 require root privileges. Use `8081` for local development. |

### `STORAGE_TYPE`

| Field | Value |
|-------|-------|
| **Purpose** | Determines where uploaded resume files are persisted. |
| **Default** | `local` (hardcoded in `docker-compose.yml`) |
| **Valid values** | `local` (Docker volume), `minio` (self-hosted S3-compatible), `s3`, `oss` |
| **Security notes** | `local` stores files in a named Docker volume (`shared-storage`). For multi-host deployments, switch to `minio` or `s3` so all replicas share the same object store. |
| **Common mistakes** | Setting `STORAGE_TYPE=minio` without configuring `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, and `MINIO_SECRET_KEY` causes file upload failures at runtime. |

---

## A. Database / PostgreSQL

### `POSTGRES_DB`

| Field | Value |
|-------|-------|
| **Purpose** | Name of the default database created on first container startup. |
| **Default** | `resume_assistant` |
| **Valid values** | Any valid PostgreSQL identifier |
| **Security notes** | Database name is not a secret, but changing it from the default makes automated scanning slightly harder. |
| **Common mistakes** | Renaming this after the volume has been initialized has no effect. `docker-entrypoint-initdb.d` only runs on the first start when the data directory is empty. |

### `POSTGRES_USER`

| Field | Value |
|-------|-------|
| **Purpose** | Superuser name for the PostgreSQL instance. |
| **Default** | `resume_user` |
| **Valid values** | Any valid PostgreSQL username |
| **Security notes** | Avoid `postgres` as the username. Use a dedicated application account with limited privileges in production. |
| **Common mistakes** | Same as `POSTGRES_DB` — changing after initialization requires recreating the volume. |

### `POSTGRES_PASSWORD`

| Field | Value |
|-------|-------|
| **Purpose** | Password for `POSTGRES_USER`. |
| **Default** | `resume_pass` |
| **Valid values** | Any string; recommended length ≥ 16 characters |
| **Security notes** | Even though PostgreSQL is on an isolated Docker network, a strong password is a mandatory layer of defense-in-depth. If a container is compromised and gains network access, weak credentials allow trivial database access. |
| **Common mistakes** | Keeping the default `resume_pass` in any non-local environment. |

### `POSTGRES_HOST`

| Field | Value |
|-------|-------|
| **Purpose** | Hostname used by the backend to connect to PostgreSQL. |
| **Default** | `postgres` (Docker service name) |
| **Valid values** | Docker service name, container IP, or external hostname |
| **Security notes** | Using the Docker service name (`postgres`) ensures traffic never leaves the internal bridge network. |
| **Common mistakes** | Setting this to `localhost` inside a container — containers do not share the host's loopback interface. |

### `POSTGRES_PORT`

| Field | Value |
|-------|-------|
| **Purpose** | PostgreSQL listener port. |
| **Default** | `5432` |
| **Valid values** | `1024–65535` |
| **Security notes** | Non-standard ports provide minimal security benefit (security through obscurity). Network isolation is the primary defense. |
| **Common mistakes** | Changing this without updating the `ports` mapping in `docker-compose.yml`. |

---

## B. Message Queue / RabbitMQ

### `RABBITMQ_HOST`

| Field | Value |
|-------|-------|
| **Purpose** | Hostname used by backend and AI service to connect to RabbitMQ. |
| **Default** | `rabbitmq` (Docker service name) |
| **Valid values** | Docker service name or external hostname |
| **Security notes** | Internal Docker DNS resolution keeps AMQP traffic off the host network. |
| **Common mistakes** | Using `localhost` from inside a container. |

### `RABBITMQ_PORT`

| Field | Value |
|-------|-------|
| **Purpose** | AMQP protocol port. |
| **Default** | `5672` |
| **Valid values** | `5672` (standard), `5671` (TLS) |
| **Security notes** | Enable TLS (`amqps://`) in production by switching to port `5671` and mounting TLS certificates. |
| **Common mistakes** | Confusing this with the Management UI port (`15672`). |

### `RABBITMQ_USERNAME`

| Field | Value |
|-------|-------|
| **Purpose** | AMQP authentication username. |
| **Default** | `guest` |
| **Valid values** | Any string |
| **Security notes** | The `guest` account is disabled for remote connections by default in RabbitMQ. Always override this in production. |
| **Common mistakes** | Using `guest` in production deployments. |

### `RABBITMQ_PASSWORD`

| Field | Value |
|-------|-------|
| **Purpose** | AMQP authentication password. |
| **Default** | `guest` |
| **Valid values** | Any string; recommended length ≥ 16 characters |
| **Security notes** | This is the fourth layer of defense-in-depth. Even if an attacker breaches the network, they still need valid MQ credentials to publish or consume messages. |
| **Common mistakes** | Reusing the same password as `POSTGRES_PASSWORD` or `JWT_SECRET`. Rotate independently. |

---

## C. Authentication / JWT

### `JWT_SECRET`

| Field | Value |
|-------|-------|
| **Purpose** | Symmetric signing key for JSON Web Tokens (JWT). Used to sign and verify all user authentication tokens. |
| **Default** | `change-this-to-a-long-random-secret-at-least-32-characters` |
| **Valid values** | Base64 or plain text string; **minimum 32 bytes (256 bits)** |
| **Security notes** | This is the most critical secret in the entire stack. Anyone who knows this value can forge authentication tokens and impersonate any user, including admins. Store it in a secrets manager (Docker secrets, HashiCorp Vault, AWS Secrets Manager) in production. |
| **Common mistakes** | <ul><li>Using a short human-readable string like `my-secret` or `password123`.</li><li>Checking `.env` into Git.</li><li>Using the same secret across dev/staging/prod.</li><li>Rotating the secret without planning — **all active users will be forced to re-login immediately** because their existing tokens will fail validation.</li></ul> |

**Recommended generation:**

```bash
# 48 bytes = 64 base64 characters
openssl rand -base64 48
```

---

## D. Frontend / Web App

### `VITE_GOOGLE_CLIENT_ID`

| Field | Value |
|-------|-------|
| **Purpose** | Google OAuth 2.0 Client ID for "Sign in with Google" on the frontend. |
| **Default** | `your-google-oauth-client-id.apps.googleusercontent.com` |
| **Valid values** | A valid OAuth 2.0 Client ID from Google Cloud Console |
| **Security notes** | This is a public client ID (not a secret). It is embedded in the compiled JavaScript bundle. Restrict the authorized JavaScript origins in Google Cloud Console to your production domain. |
| **Common mistakes** | <ul><li>Using a Web Client ID without adding `http://localhost` to authorized origins during local development.</li><li>Confusing this with the Client Secret (which is NOT needed for OAuth 2.0 implicit/authorization-code flow with PKCE).</li></ul> |

### `VITE_API_BASE_URL`

| Field | Value |
|-------|-------|
| **Purpose** | Base URL prefix for all frontend API calls. |
| **Default** | *(empty)* |
| **Valid values** | Empty string, `/api`, or a relative path starting with `/` |
| **Security notes** | **CRITICAL**: This must be a relative path or empty. When the frontend runs behind Nginx, API calls are automatically prefixed and proxied to the backend. If you set an absolute URL (e.g. `http://backend:8080`), the browser will attempt to connect directly to the backend, bypassing Nginx, exposing the backend port, and breaking CORS and the single-entry-point security model. |
| **Common mistakes** | <ul><li>Setting `VITE_API_BASE_URL=http://localhost:8080/api` for local development — this leaks the backend address to users and fails in production.</li><li>Setting it to an internal Docker hostname like `http://backend:8080` — browsers cannot resolve Docker service names.</li></ul> |

---

## E. Spring Boot / Backend

### `SPRING_PROFILES_ACTIVE`

| Field | Value |
|-------|-------|
| **Purpose** | Activates a Spring Boot configuration profile. |
| **Default** | `dev` |
| **Valid values** | `dev`, `prod`, `test` |
| **Security notes** | The `dev` profile disables Flyway, uses a 24-hour JWT expiry, and enables verbose error messages. **Never use `dev` in production.** The `prod` profile enables Flyway validation, sets 1-hour JWT expiry, and hides stack traces from API responses. |
| **Common mistakes** | Deploying with `SPRING_PROFILES_ACTIVE=dev` in production. |

### `SPRING_APPLICATION_NAME`

| Field | Value |
|-------|-------|
| **Purpose** | Application name displayed in Spring Boot Actuator, metrics, and logs. |
| **Default** | `resume-assistant-backend` |
| **Valid values** | Any valid Spring Boot application name |
| **Security notes** | Not a secret. Used for observability and service discovery. |
| **Common mistakes** | Using spaces or special characters that break URL-safe identifiers. |

### `SPRING_FLYWAY_ENABLED`

| Field | Value |
|-------|-------|
| **Purpose** | Whether to run Flyway database migrations on startup. |
| **Default** | `false` (dev) / `true` (prod) |
| **Valid values** | `true`, `false` |
| **Security notes** | Disabling Flyway in production prevents schema updates, which may cause runtime errors when the code expects newer tables or columns. Only disable if you manage schema changes manually. |
| **Common mistakes** | <ul><li>Setting `false` in production and forgetting to apply migrations manually.</li><li>Changing this after the database has been initialized without understanding the impact on existing data.</li></ul> |

---

## F. AI Provider Keys

You only need to configure **one** provider. The choice is determined by the prefix in `LLM_TEXT_MODEL`, `LLM_VISION_MODEL`, and `LLM_EMBEDDING_MODEL`.

### `GEMINI_API_KEY`

| Field | Value |
|-------|-------|
| **Purpose** | API key for Google AI Studio (Gemini models via LiteLLM). |
| **Default** | `[replace-with-your-gemini-api-key]` |
| **Valid values** | A valid Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey) |
| **Security notes** | Gemini offers a generous free tier. No credit card or GCP billing is required for the free tier. Treat this key as a secret — anyone with it can consume your quota. |
| **Common mistakes** | <ul><li>Pasting the key with surrounding whitespace or quotes into `.env`.</li><li>Using a GCP Service Account key instead of an AI Studio API key.</li></ul> |

### `OPENAI_API_KEY`

| Field | Value |
|-------|-------|
| **Purpose** | API key for OpenAI models (GPT-4o, etc.) via LiteLLM. |
| **Default** | *(empty)* |
| **Valid values** | A valid OpenAI API key starting with `sk-` |
| **Security notes** | OpenAI is a paid service. Set spending limits and monitor usage dashboards to avoid unexpected bills. |
| **Common mistakes** | Setting this **and** `GEMINI_API_KEY` at the same time. LiteLLM will use whichever provider matches the model prefix, but having multiple keys increases the attack surface. |

### `ANTHROPIC_API_KEY`

| Field | Value |
|-------|-------|
| **Purpose** | API key for Anthropic Claude models via LiteLLM. |
| **Default** | *(empty)* |
| **Valid values** | A valid Anthropic API key |
| **Security notes** | Claude models are generally more expensive per token than Gemini Flash. Evaluate cost before switching. |
| **Common mistakes** | Forgetting to update the model prefix from `gemini/` to `anthropic/` when switching providers. |

### `GROQ_API_KEY`

| Field | Value |
|-------|-------|
| **Purpose** | API key for Groq (fast inference) models via LiteLLM. |
| **Default** | *(empty)* |
| **Valid values** | A valid Groq API key |
| **Security notes** | Groq offers very low latency but limited model selection. |
| **Common mistakes** | Expecting embedding support from Groq — most Groq models are text-generation only. |

### `OLLAMA_API_BASE`

| Field | Value |
|-------|-------|
| **Purpose** | Base URL for a local Ollama instance (self-hosted open-source models). |
| **Default** | `http://localhost:11434` |
| **Valid values** | Any HTTP URL reachable from the AI service container |
| **Security notes** | `localhost:11434` from inside the AI service container refers to the container itself, not the Docker host. To reach the host, use `http://host.docker.internal:11434` (Docker Desktop) or the host's LAN IP. |
| **Common mistakes** | <ul><li>Leaving the default `localhost:11434` and wondering why the AI service cannot connect.</li><li>Not pulling the model (`ollama pull llama3`) before starting the stack.</li></ul> |

---

## G. Model Parameters

### `LLM_TEXT_MODEL`

| Field | Value |
|-------|-------|
| **Purpose** | Model identifier for general text generation (job parsing, resume optimization, conversation). |
| **Default** | `gemini/gemini-2.5-flash` |
| **Valid values** | Any LiteLLM-supported model string with provider prefix, e.g. `gemini/gemini-2.5-flash`, `openai/gpt-4o-mini`, `vertex_ai/gemini-2.5-flash` |
| **Security notes** | Model names are not secrets, but switching to a more expensive model (e.g. `gpt-4o`) without budgeting can cause unexpected costs. |
| **Common mistakes** | Using a model prefix that does not match the configured API key (e.g. `openai/` prefix with `GEMINI_API_KEY` set). |

### `LLM_VISION_MODEL`

| Field | Value |
|-------|-------|
| **Purpose** | Model identifier for vision/multi-modal tasks (resume image parsing). |
| **Default** | `gemini/gemini-2.5-flash` |
| **Valid values** | Any vision-capable LiteLLM-supported model |
| **Security notes** | Vision models are typically more expensive per token. Ensure the selected model supports image input. |
| **Common mistakes** | Selecting a text-only model (e.g. `text-embedding-ada-002`) for vision tasks. |

### `LLM_EMBEDDING_MODEL`

| Field | Value |
|-------|-------|
| **Purpose** | Model identifier for generating text embeddings (vector search). |
| **Default** | `gemini/gemini-embedding-001` |
| **Valid values** | Any LiteLLM-supported embedding model |
| **Security notes** | Embedding model output dimension must match `LLM_EMBEDDING_MODEL_DIMENSION`. |
| **Common mistakes** | Switching the embedding model without updating the dimension, causing vector insertion failures in PostgreSQL. |

### `LLM_EMBEDDING_MODEL_DIMENSION`

| Field | Value |
|-------|-------|
| **Purpose** | Output dimension of the selected embedding model. Must match the actual vector size produced by the model. |
| **Default** | `1536` |
| **Valid values** | Positive integer matching the model's output dimension |
| **Security notes** | Incorrect dimensions cause runtime SQL errors when inserting vectors into the `pgvector` column, but do not create a security vulnerability. |
| **Common mistakes** | <ul><li>Changing `LLM_EMBEDDING_MODEL` to `openai/text-embedding-3-large` (3072 dimensions) but leaving `LLM_EMBEDDING_MODEL_DIMENSION=1536`.</li><li>Forgetting to recreate the PostgreSQL volume after changing dimensions — existing vector columns retain the old dimension and will reject new inserts.</li></ul> |

**Dimension reference table:**

| Model | Dimension |
|-------|-----------|
| `gemini/gemini-embedding-001` | `1536` |
| `openai/text-embedding-ada-002` | `1536` |
| `openai/text-embedding-3-small` | `1536` |
| `openai/text-embedding-3-large` | `3072` |

> **Note**: The default `1536` in `.env.example` matches the current default `gemini/gemini-embedding-001` configuration and the listed OpenAI 1536-dimension embedding models.

### `LLM_TEMPERATURE`

| Field | Value |
|-------|-------|
| **Purpose** | Controls the randomness (creativity) of the LLM output. |
| **Default** | `0.1` |
| **Valid values** | `0.0` to `2.0` |
| **Security notes** | Lower values produce more deterministic output, which is safer for structured JSON extraction (resume parsing, job matching). Higher values increase hallucination risk. |
| **Common mistakes** | Setting `1.0` or higher for structured extraction tasks, causing invalid JSON responses that break downstream parsers. |

### `LLM_REQUEST_TIMEOUT_SECONDS`

| Field | Value |
|-------|-------|
| **Purpose** | Maximum wait time for each LiteLLM request (text, vision, embedding). |
| **Default** | `60` |
| **Valid values** | Positive integer (seconds) |
| **Security notes** | Long timeouts can exhaust worker threads under heavy load. Short timeouts improve resilience but may cause unnecessary retries for slow models. |
| **Common mistakes** | Setting this too low (< 10s) for large resume files or batch embedding jobs. |

### `LLM_MAX_TOKENS`

| Field | Value |
|-------|-------|
| **Purpose** | Maximum number of output tokens the LLM is allowed to generate for a single request. |
| **Default** | `8192` |
| **Valid values** | Positive integer |
| **Security notes** | Higher values increase cost per request and may increase latency. However, setting this too low causes the model to truncate structured JSON responses (e.g. conversation replies, resume optimization), leading to `JSONDecodeError` downstream. |
| **Common mistakes** | <ul><li>Leaving the value at a provider-specific default (e.g. 1024 or 2048) without realizing it truncates long outputs.</li><li>Setting an extremely high value (> 32k) with a model that does not support it, causing provider errors.</li><li>Not correlating `json.decoder.JSONDecodeError` logs in the AI service with insufficient `max_tokens`.</li></ul> |

### `BACKEND_SERVICE_URL`

| Field | Value |
|-------|-------|
| **Purpose** | URL the AI service uses to call backend APIs (e.g. vector upsert). |
| **Default** | `http://backend:8080` |
| **Valid values** | Any HTTP URL reachable from the AI service container |
| **Security notes** | Uses the Docker service name `backend` so traffic stays inside the internal network. |
| **Common mistakes** | Using `http://localhost:8080` from inside the AI service container. |

### `BACKEND_QUERY_TIMEOUT`

| Field | Value |
|-------|-------|
| **Purpose** | Timeout in seconds for backend API calls from the AI service. |
| **Default** | `5` |
| **Valid values** | Positive integer (seconds) |
| **Security notes** | Short timeouts prevent the AI service from hanging if the backend is overloaded. |
| **Common mistakes** | Setting this too low for batch vector upserts, causing partial data insertion. |

### Incremental Model Training Parameters

The following parameters control the incremental job training loop. They are currently **hard-coded** in `ai-service/app/services/incremental_model_service.py` and are not configurable via environment variables.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `FEATURE_COUNT_CAP` | `5000` | Soft cap for per-feature per-class sample count. When exceeded, old statistics decay to make room for new feedback. |
| `MIN_SAMPLES_TO_RECOMPUTE` | `10` | Minimum number of new samples required to trigger automatic model weight recomputation. |

> **Future work**: These parameters may be exposed as environment variables in a future release for finer operational control.

---

## H. AI Service Logging

### `LOG_LEVEL`

| Field | Value |
|-------|-------|
| **Purpose** | Controls the verbosity of the AI service logger (structlog). |
| **Default** | `INFO` |
| **Valid values** | `DEBUG`, `INFO`, `WARNING`, `ERROR`, `CRITICAL` |
| **Security notes** | `DEBUG` may log request payloads containing resume content or user data. Never use `DEBUG` in production without log sanitization and retention policies. |
| **Common mistakes** | Leaving `DEBUG` on in production and accidentally logging PII (personally identifiable information) to persistent log files. |

---

## I. Vertex AI Settings

### `VERTEX_PROJECT_ID`

| Field | Value |
|-------|-------|
| **Purpose** | Google Cloud project ID for Vertex AI (only needed if using `vertex_ai/` model prefix). |
| **Default** | `ser594-ai-service` |
| **Valid values** | Any valid Google Cloud project ID |
| **Security notes** | Not a secret. This identifies the billing project. |
| **Common mistakes** | Setting this when using Gemini via AI Studio (`gemini/` prefix) — it is ignored in that mode. |

### `VERTEX_LOCATION`

| Field | Value |
|-------|-------|
| **Purpose** | Vertex AI region/location for model inference. |
| **Default** | `global` |
| **Valid values** | `global`, `us-central1`, `europe-west4`, `asia-northeast1`, etc. |
| **Security notes** | Choose a region close to your deployment to minimize latency and comply with data residency requirements. |
| **Common mistakes** | Using a region where the selected model is not available, causing `404 Model not found` errors. |

### `VERTEX_CREDENTIALS`

| Field | Value |
|-------|-------|
| **Purpose** | Absolute host path to the Google Cloud Service Account JSON key file. Mounted as a read-only volume into the AI service container. |
| **Default / template value** | `use-your-gcp-service-account-json-key-file-if-using-vertex-ai` in `.env.example`; set it to an absolute path only when using `vertex_ai/`, or clear it when using the default `gemini/` AI Studio models. |
| **Valid values** | Empty value for non-Vertex local runs, or an absolute filesystem path such as `/home/user/service-account.json` |
| **Security notes** | **CRITICAL**: This **must** be an absolute path. Relative paths or plain filenames (e.g. `vertex.json`) are interpreted by Docker/Podman as named volume references, which will silently create an empty volume instead of mounting your credentials file. The file is mounted read-only (`:ro`) to prevent accidental modification. Store this key in a secrets manager and rotate it regularly. |
| **Common mistakes** | <ul><li>Leaving the placeholder value unchanged after copying `.env.example` to `.env`.</li><li>Using a relative path like `./vertex.json` or `~/keys/gcp.json`.</li><li>Using a path inside the project directory that is `.gitignore`d but forgetting to copy the file to the deployment host.</li><li>Setting this when using Gemini AI Studio (`gemini/` prefix) — ADC credentials are not needed for AI Studio.</li></ul> |

---

## J. Internal API Key

### `INTERNAL_API_KEY`

| Field | Value |
|-------|-------|
| **Purpose** | Shared secret between the backend and AI service REST endpoints. Protects the embedding generation endpoint from unauthorized internal network access. |
| **Default** | *(empty)* |
| **Valid values** | Any string; recommended length ≥ 32 characters |
| **Security notes** | This is the **application-layer defense-in-depth** key. Even if an attacker compromises a container and gains access to the internal Docker network, they cannot invoke the LLM embedding endpoint without this key. The backend automatically attaches it to every outbound REST request via `InternalApiKeyInterceptor`. The AI service middleware rejects requests with HTTP 401 if the header is missing or incorrect. |
| **Common mistakes** | <ul><li>Setting different values for `backend` and `ai-service` in `docker-compose.yml` — both services must share the **exact same value**.</li><li>Using a short, guessable string.</li><li>Forgetting to set it in production because it works fine when empty (both sides skip validation).</li></ul> |

**Recommended generation:**

```bash
# 32 bytes = 44 base64 characters
openssl rand -base64 32
```

If left empty, both the backend interceptor and the AI service middleware skip the check. This is convenient for local development but **must not** be used in production.

## K. JWT Token Lifetime

### `JWT_ACCESS_EXPIRATION`

| Field | Value |
|-------|-------|
| **Purpose** | Access token expiration time in milliseconds. Controls how long a user stays logged in before re-authenticating. |
| **Default** | `86400000` (24 hours in dev) / `3600000` (1 hour in prod) |
| **Valid values** | Positive integer (milliseconds) |
| **Security notes** | Shorter values improve security by limiting the window of opportunity for stolen tokens. Longer values improve user experience but increase risk. In production, 1 hour (`3600000`) is recommended. |
| **Common mistakes** | <ul><li>Setting an extremely long value (e.g. `31536000000` = 1 year) for convenience, which defeats the purpose of token rotation.</li><li>Forgetting that the prod profile defaults to 1 hour even if you set a different dev value.</li></ul> |

### `JWT_REFRESH_EXPIRATION`

| Field | Value |
|-------|-------|
| **Purpose** | Refresh token expiration time in milliseconds. Controls how long a refresh token remains valid for obtaining new access tokens. |
| **Default** | `604800000` (7 days) |
| **Valid values** | Positive integer (milliseconds); must be greater than `JWT_ACCESS_EXPIRATION` |
| **Security notes** | Refresh tokens are long-lived by design. If a refresh token is compromised, an attacker can maintain access for the entire expiration period. Consider shorter values (e.g. 1–3 days) for high-security deployments. |
| **Common mistakes** | Setting `JWT_REFRESH_EXPIRATION` shorter than `JWT_ACCESS_EXPIRATION`, which makes it impossible to refresh tokens before they expire. |

---

## L. AI Service Connection

### `AI_SERVICE_BASE_URL`

| Field | Value |
|-------|-------|
| **Purpose** | Base URL of the AI service REST API as seen from the backend. The backend calls this endpoint for vector generation and job matching. |
| **Default** | `http://localhost:8000` (non-Docker) / `http://ai-service:8000` (Docker) |
| **Valid values** | Any HTTP URL reachable from the backend container or process |
| **Security notes** | Uses Docker service name resolution inside the container network. Never expose this endpoint to the public internet without authentication. |
| **Common mistakes** | <ul><li>Setting `http://localhost:8000` when running inside Docker — containers cannot resolve `localhost` to the host.</li><li>Trying to override this via `.env` when `docker-compose.yml` hardcodes the value in the `backend` service `environment` block. Remove the hardcoded line in `docker-compose.yml` first.</li></ul> |

---

## M. File Storage

### `STORAGE_TYPE`

| Field | Value |
|-------|-------|
| **Purpose** | Determines which storage backend the backend uses for uploaded resume files. |
| **Default** | `minio` (Spring Boot default) / `local` (Docker Compose hardcoded) |
| **Valid values** | `minio`, `local`, `s3`, `oss` |
| **Security notes** | `local` stores files in a Docker volume or local filesystem. For production with multiple backend replicas, use `minio` or `s3` so all instances share the same object store. |
| **Common mistakes** | <ul><li>Setting `STORAGE_TYPE=minio` without configuring the MinIO endpoint and credentials.</li><li>Using `local` in a multi-replica deployment, causing files uploaded to one replica to be invisible to others.</li></ul> |

### MinIO Settings (`STORAGE_TYPE=minio`)

### `MINIO_ENDPOINT`

| Field | Value |
|-------|-------|
| **Purpose** | URL of the MinIO server. |
| **Default** | `http://localhost:9000` (dev) / `http://minio:9000` (Docker) |
| **Valid values** | Any HTTP(S) URL |
| **Security notes** | Use HTTPS in production. Verify the TLS certificate or configure the backend trust store. |
| **Common mistakes** | Using `http://localhost:9000` inside Docker instead of the service name. |

### `MINIO_ACCESS_KEY`

| Field | Value |
|-------|-------|
| **Purpose** | MinIO access key (username). |
| **Default** | `minioadmin` |
| **Valid values** | Any string |
| **Security notes** | Rotate from the default `minioadmin` immediately in production. |
| **Common mistakes** | Reusing the same credentials across dev and production MinIO instances. |

### `MINIO_SECRET_KEY`

| Field | Value |
|-------|-------|
| **Purpose** | MinIO secret key (password). |
| **Default** | `minioadmin` |
| **Valid values** | Any string; recommended length ≥ 16 |
| **Security notes** | Store in a secrets manager. Leaked keys grant full read/write access to all buckets. |
| **Common mistakes** | Committing keys to Git or sharing them in chat messages. |

### `MINIO_BUCKET_NAME`

| Field | Value |
|-------|-------|
| **Purpose** | Name of the MinIO bucket for resume files. |
| **Default** | `resumes` |
| **Valid values** | Any valid S3 bucket name |
| **Security notes** | Bucket names are globally unique in S3, but only locally unique in MinIO. |
| **Common mistakes** | Using uppercase letters or underscores, which are invalid in DNS-compatible S3 bucket names. |

### `MINIO_CONNECT_TIMEOUT`

| Field | Value |
|-------|-------|
| **Purpose** | TCP connection timeout to MinIO in milliseconds. |
| **Default** | `5000` |
| **Valid values** | Positive integer (ms) |
| **Security notes** | Short timeouts prevent hanging on unreachable endpoints but may fail on slow networks. |
| **Common mistakes** | Setting this too low (< 1000 ms) on high-latency networks. |

### `MINIO_WRITE_TIMEOUT`

| Field | Value |
|-------|-------|
| **Purpose** | Socket write timeout to MinIO in milliseconds. |
| **Default** | `60000` |
| **Valid values** | Positive integer (ms) |
| **Security notes** | Large file uploads need sufficient timeout. |
| **Common mistakes** | Setting this too low for multi-MB resume PDF uploads. |

### `MINIO_READ_TIMEOUT`

| Field | Value |
|-------|-------|
| **Purpose** | Socket read timeout from MinIO in milliseconds. |
| **Default** | `30000` |
| **Valid values** | Positive integer (ms) |
| **Security notes** | Downloading large files requires adequate timeout. |
| **Common mistakes** | Same as write timeout — insufficient for large files. |

### Local Storage Settings (`STORAGE_TYPE=local`)

### `LOCAL_STORAGE_BASE_PATH`

| Field | Value |
|-------|-------|
| **Purpose** | Root directory for local file storage. |
| **Default** | `./uploads` (dev) / `/app/uploads` (Docker) |
| **Valid values** | Any absolute or relative filesystem path |
| **Security notes** | Ensure the directory is writable by the backend process user. In Docker, use a mounted volume for persistence. |
| **Common mistakes** | Using a relative path that resolves differently depending on the working directory. |

### `LOCAL_STORAGE_RESUME_PATH`

| Field | Value |
|-------|-------|
| **Purpose** | Subdirectory under `LOCAL_STORAGE_BASE_PATH` for resume files. |
| **Default** | `resumes` |
| **Valid values** | Any valid directory name |
| **Security notes** | No special requirements. |
| **Common mistakes** | Using path separators (e.g. `resumes/2024`) — this is handled by `LOCAL_STORAGE_DATE_SUBDIR`. |

### `LOCAL_STORAGE_DATE_SUBDIR`

| Field | Value |
|-------|-------|
| **Purpose** | Whether to create date-based subdirectories (e.g. `2025/05/06/`) for uploaded files. |
| **Default** | `true` |
| **Valid values** | `true`, `false` |
| **Security notes** | Date subdirectories make it harder to enumerate all files by scanning a single directory. |
| **Common mistakes** | Setting `false` and accumulating thousands of files in a single folder, degrading filesystem performance. |

### `LOCAL_STORAGE_URL_PREFIX`

| Field | Value |
|-------|-------|
| **Purpose** | URL prefix prepended to generated file access URLs. Useful when serving files through an Nginx reverse proxy. |
| **Default** | *(empty)* |
| **Valid values** | Any URL path prefix, e.g. `/files`, `https://cdn.example.com` |
| **Security notes** | If empty, the backend generates relative URLs. Setting an external CDN improves performance but requires proper access control. |
| **Common mistakes** | Adding a trailing slash that causes double slashes in URLs. |

### AWS S3 Settings (`STORAGE_TYPE=s3`)

### `AWS_S3_REGION`

| Field | Value |
|-------|-------|
| **Purpose** | AWS region for S3 operations. |
| **Default** | `us-east-1` |
| **Valid values** | Any valid AWS region code |
| **Security notes** | Choose a region close to your deployment to minimize latency and comply with data residency. |
| **Common mistakes** | Using a region where the bucket does not exist, causing `NoSuchBucket` errors. |

### `AWS_S3_ENDPOINT`

| Field | Value |
|-------|-------|
| **Purpose** | Custom endpoint for S3-compatible storage (MinIO, Ceph, Wasabi, etc.). |
| **Default** | *(empty)* |
| **Valid values** | Any HTTP(S) URL or empty |
| **Security notes** | Leave empty for official AWS S3. Required for third-party S3-compatible services. |
| **Common mistakes** | Setting this for genuine AWS S3, which overrides the official endpoint and causes signature errors. |

### `AWS_S3_ACCESS_KEY`

| Field | Value |
|-------|-------|
| **Purpose** | AWS IAM access key or S3-compatible access key. |
| **Default** | *(empty)* |
| **Valid values** | Any string |
| **Security notes** | Use IAM roles (instance profiles) instead of long-term access keys when running on AWS EC2/EKS. |
| **Common mistakes** | Using root account keys instead of scoped IAM user keys. |

### `AWS_S3_SECRET_KEY`

| Field | Value |
|-------|-------|
| **Purpose** | AWS IAM secret key or S3-compatible secret key. |
| **Default** | *(empty)* |
| **Valid values** | Any string |
| **Security notes** | Same as access key — prefer IAM roles. Never commit to version control. |
| **Common mistakes** | Pasting the key with surrounding whitespace or quotes. |

### `AWS_S3_BUCKET_NAME`

| Field | Value |
|-------|-------|
| **Purpose** | Name of the S3 bucket for resume files. |
| **Default** | *(empty)* |
| **Valid values** | Any valid S3 bucket name |
| **Security notes** | Ensure the bucket policy restricts public access. Enable server-side encryption. |
| **Common mistakes** | Using a bucket name that is already taken in the global S3 namespace. |

### `AWS_S3_PATH_STYLE`

| Field | Value |
|-------|-------|
| **Purpose** | Whether to use path-style bucket URLs (`s3.example.com/bucketname`) instead of virtual-hosted-style (`bucketname.s3.example.com`). |
| **Default** | `false` |
| **Valid values** | `true`, `false` |
| **Security notes** | Path-style is required for some third-party S3-compatible services (e.g. MinIO without DNS). AWS S3 recommends virtual-hosted-style. |
| **Common mistakes** | Leaving `false` when using MinIO with IP-based endpoints, causing DNS resolution failures. |

### `AWS_S3_CONNECTION_TIMEOUT`

| Field | Value |
|-------|-------|
| **Purpose** | S3 connection timeout in milliseconds. |
| **Default** | `5000` |
| **Valid values** | Positive integer (ms) |
| **Security notes** | Short timeouts improve resilience against unreachable endpoints. |
| **Common mistakes** | Setting too low for high-latency regions. |

### `AWS_S3_SOCKET_TIMEOUT`

| Field | Value |
|-------|-------|
| **Purpose** | S3 socket timeout in milliseconds. |
| **Default** | `50000` |
| **Valid values** | Positive integer (ms) |
| **Security notes** | Large file transfers need sufficient timeout. |
| **Common mistakes** | Same as MinIO read/write timeouts. |

### `AWS_S3_MAX_CONNECTIONS`

| Field | Value |
|-------|-------|
| **Purpose** | Maximum number of concurrent HTTP connections to S3. |
| **Default** | `50` |
| **Valid values** | Positive integer |
| **Security notes** | Higher values improve throughput for bulk uploads but consume more memory. |
| **Common mistakes** | Setting extremely high values (> 500) without monitoring, causing memory pressure. |

### `AWS_S3_ENCRYPTION_ENABLED`

| Field | Value |
|-------|-------|
| **Purpose** | Whether to enable server-side encryption for uploaded objects. |
| **Default** | `false` |
| **Valid values** | `true`, `false` |
| **Security notes** | Enable this in production to meet compliance requirements (GDPR, HIPAA, etc.). |
| **Common mistakes** | Enabling encryption without specifying a KMS key, which may use the default AWS-managed key with different permissions. |

### `AWS_S3_KMS_KEY_ID`

| Field | Value |
|-------|-------|
| **Purpose** | AWS KMS key ID or ARN for server-side encryption. Only used when `AWS_S3_ENCRYPTION_ENABLED=true`. |
| **Default** | *(empty)* |
| **Valid values** | KMS key ID, key ARN, or alias ARN |
| **Security notes** | Using a customer-managed key (CMK) provides audit trails and fine-grained access control. |
| **Common mistakes** | Specifying a key from a different region, causing cross-region KMS calls and higher latency. |

### Aliyun OSS Settings (`STORAGE_TYPE=oss`)

### `ALIYUN_OSS_ENDPOINT`

| Field | Value |
|-------|-------|
| **Purpose** | OSS endpoint URL. |
| **Default** | *(empty)* |
| **Valid values** | Any valid OSS endpoint, e.g. `https://oss-cn-hangzhou.aliyuncs.com` |
| **Security notes** | Use internal endpoints (`oss-cn-hangzhou-internal.aliyuncs.com`) when both backend and OSS are in the same Alibaba Cloud region to avoid public network charges. |
| **Common mistakes** | Using a public endpoint when an internal one is available, incurring unnecessary egress fees. |

### `ALIYUN_OSS_ACCESS_KEY_ID`

| Field | Value |
|-------|-------|
| **Purpose** | Aliyun AccessKey ID. |
| **Default** | *(empty)* |
| **Valid values** | Any valid Aliyun AccessKey ID |
| **Security notes** | Prefer RAM roles (instance roles) over long-term AccessKeys when running on Alibaba Cloud ECS. |
| **Common mistakes** | Using the root account AccessKey instead of a RAM user key with limited permissions. |

### `ALIYUN_OSS_ACCESS_KEY_SECRET`

| Field | Value |
|-------|-------|
| **Purpose** | Aliyun AccessKey secret. |
| **Default** | *(empty)* |
| **Valid values** | Any string |
| **Security notes** | Same as AccessKey ID — prefer RAM roles. |
| **Common mistakes** | Committing secrets to version control. |

### `ALIYUN_OSS_BUCKET_NAME`

| Field | Value |
|-------|-------|
| **Purpose** | Name of the OSS bucket for resume files. |
| **Default** | *(empty)* |
| **Valid values** | Any valid OSS bucket name |
| **Security notes** | Ensure the bucket ACL and policy restrict public access. |
| **Common mistakes** | Using a bucket in a different region from the endpoint, causing cross-region latency. |

### `ALIYUN_OSS_CDN_DOMAIN`

| Field | Value |
|-------|-------|
| **Purpose** | Custom CDN domain for generating public file URLs. |
| **Default** | *(empty)* |
| **Valid values** | Any valid domain, e.g. `https://cdn.example.com` |
| **Security notes** | CDN domains should have proper access control and HTTPS. |
| **Common mistakes** | Forgetting to configure CDN origin pull settings after setting the domain. |

### `ALIYUN_OSS_CONNECTION_TIMEOUT`

| Field | Value |
|-------|-------|
| **Purpose** | OSS connection timeout in milliseconds. |
| **Default** | `5000` |
| **Valid values** | Positive integer (ms) |
| **Security notes** | Same as S3 connection timeout. |
| **Common mistakes** | Same as S3. |

### `ALIYUN_OSS_SOCKET_TIMEOUT`

| Field | Value |
|-------|-------|
| **Purpose** | OSS socket timeout in milliseconds. |
| **Default** | `50000` |
| **Valid values** | Positive integer (ms) |
| **Security notes** | Same as S3 socket timeout. |
| **Common mistakes** | Same as S3. |

### `ALIYUN_OSS_MAX_CONNECTIONS`

| Field | Value |
|-------|-------|
| **Purpose** | Maximum concurrent connections to OSS. |
| **Default** | `50` |
| **Valid values** | Positive integer |
| **Security notes** | Same as S3 max connections. |
| **Common mistakes** | Same as S3. |

### `ALIYUN_OSS_DOWNLOAD_BUFFER`

| Field | Value |
|-------|-------|
| **Purpose** | Buffer size for OSS downloads in kilobytes. |
| **Default** | `64` |
| **Valid values** | Positive integer (KB) |
| **Security notes** | Larger buffers improve download speed but increase memory usage. |
| **Common mistakes** | Setting extremely large values (> 1024 KB) for small files, wasting memory. |

### `ALIYUN_OSS_UPLOAD_PART_SIZE`

| Field | Value |
|-------|-------|
| **Purpose** | Part size for multipart uploads in megabytes. |
| **Default** | `5` |
| **Valid values** | Positive integer (MB) |
| **Security notes** | Multipart uploads are required for files > 5 GB. Smaller parts improve resume capability but increase API call overhead. |
| **Common mistakes** | Setting part size below the OSS minimum (1 MB) or above the maximum (5 GB). |

### `ALIYUN_OSS_SSE`

| Field | Value |
|-------|-------|
| **Purpose** | Server-side encryption algorithm for OSS objects. |
| **Default** | *(empty)* |
| **Valid values** | `AES256`, `KMS`, `SM4`, or empty |
| **Security notes** | Enable encryption for compliance. `AES256` is the simplest option. `KMS` provides audit trails. |
| **Common mistakes** | Setting `KMS` without configuring the KMS key ID. |

### `ALIYUN_OSS_KMS_KEY_ID`

| Field | Value |
|-------|-------|
| **Purpose** | KMS key ID for OSS encryption. Only used when `ALIYUN_OSS_SSE=KMS`. |
| **Default** | *(empty)* |
| **Valid values** | Any valid Aliyun KMS key ID |
| **Security notes** | Using a customer-managed key provides better access control than the default service key. |
| **Common mistakes** | Specifying a key from a different region or without proper RAM permissions. |

---

## O. Email Verification Configuration

### `EMAIL_VERIFICATION_ENABLED`

| Field | Value |
|-------|-------|
| **Purpose** | Master switch for email verification during user registration. When enabled, new users must enter a 6-digit code sent to their email before registration succeeds. |
| **Default** | `false` |
| **Valid values** | `true`, `false` |
| **Security notes** | Even if SMTP credentials are configured, verification is only enforced when this flag is `true`. This ensures backward compatibility and safe gradual rollouts. |
| **Common mistakes** | Setting to `true` without configuring `SMTP_HOST`, `SMTP_USERNAME`, and `SMTP_PASSWORD` causes all verification requests to fail at runtime. |

### `EMAIL_FROM`

| Field | Value |
|-------|-------|
| **Purpose** | Sender address displayed in verification emails. |
| **Default** | `noreply@resume-assistant.local` |
| **Valid values** | Any valid email address |
| **Security notes** | Some SMTP providers require the `From` address to be verified or registered in their console. Using an unverified address may cause emails to be rejected or land in spam. |
| **Common mistakes** | Using a personal Gmail address without enabling "App Passwords" or without configuring SPF/DKIM for the domain. |

### `SMTP_HOST`

| Field | Value |
|-------|-------|
| **Purpose** | Hostname of the SMTP relay server used to send verification emails. |
| **Default** | *(empty)* |
| **Valid values** | Any reachable SMTP hostname (e.g., `smtp.gmail.com`, `smtp.office365.com`, `smtp.mailgun.org`) |
| **Security notes** | Prefer SMTP providers that enforce TLS (port 587 with STARTTLS). Avoid plain-text SMTP on port 25. |
| **Common mistakes** | Using `localhost` inside a container; containers do not share the host's loopback. Use the Docker host-gateway address if running a local relay: `host.docker.internal`. |

### `SMTP_PORT`

| Field | Value |
|-------|-------|
| **Purpose** | TCP port for the SMTP relay. |
| **Default** | `587` |
| **Valid values** | `25`, `465` (SSL), `587` (STARTTLS), `2525` (alternative) |
| **Security notes** | Port 587 with STARTTLS is the modern standard. Port 465 is legacy SSL. Port 25 is often blocked by cloud providers. |
| **Common mistakes** | Using port `465` with STARTTLS or port `587` with implicit SSL; mismatched encryption and port causes connection hangs. |

### `SMTP_USERNAME`

| Field | Value |
|-------|-------|
| **Purpose** | Username for SMTP authentication. Often the full email address. |
| **Default** | *(empty)* |
| **Valid values** | Any string accepted by the SMTP server |
| **Security notes** | Treat this as sensitive configuration. Do not commit real credentials to version control. |
| **Common mistakes** | For Gmail, using the regular Google password instead of an App Password (which is required when 2FA is enabled). |

### `SMTP_PASSWORD`

| Field | Value |
|-------|-------|
| **Purpose** | Password or app-specific password for SMTP authentication. |
| **Default** | *(empty)* |
| **Valid values** | Any string |
| **Security notes** | Store this in `.env` only. Rotate periodically. If the credential is leaked, an attacker can send emails on your behalf. |
| **Common mistakes** | Committing `.env` to Git. Using a weak or reused password. Not rotating credentials after a team member departs. |

### `EMAIL_CODE_EXPIRY`

| Field | Value |
|-------|-------|
| **Purpose** | Lifetime of a verification code in seconds before it expires. |
| **Default** | `300` (5 minutes) |
| **Valid values** | Positive integer, recommended range `60–900` |
| **Security notes** | Shorter expiry windows reduce the brute-force window. Too short (< 60s) causes UX friction. |
| **Common mistakes** | Setting a very long expiry (e.g., 1 hour) makes brute-force attacks more feasible. |

### `EMAIL_RESEND_COOLDOWN`

| Field | Value |
|-------|-------|
| **Purpose** | Minimum seconds a user must wait before requesting another verification code for the same email. |
| **Default** | `60` (1 minute) |
| **Valid values** | Positive integer, recommended range `30–300` |
| **Security notes** | Prevents rapid resend abuse. Pair with the existing check that rejects codes for already-registered emails. |
| **Common mistakes** | Setting to `0` removes protection against accidental double-clicks and deliberate abuse. |

### `EMAIL_MAX_ATTEMPTS`

| Field | Value |
|-------|-------|
| **Purpose** | Maximum number of failed validation attempts before the verification code is invalidated. |
| **Default** | `3` |
| **Valid values** | Positive integer, recommended range `3–5` |
| **Security notes** | Limits brute-force guessing. After exceeding this threshold, the user must request a new code. |
| **Common mistakes** | Setting to a high value (e.g., 10) makes 6-digit numeric codes easier to brute-force within the 5-minute expiry window. |

## P. CAPTCHA Configuration

### `CAPTCHA_ENABLED`

| Field | Value |
|-------|-------|
| **Purpose** | Master switch for the slider CAPTCHA challenge. When enabled, registration and login endpoints require a completed CAPTCHA verification. |
| **Default** | `true` |
| **Valid values** | `true`, `false` |
| **Security notes** | Disabling CAPTCHA removes a layer of bot protection. Only set to `false` in trusted internal networks or automated test environments. |
| **Common mistakes** | Setting to `false` on public-facing instances without alternative bot mitigation (e.g., rate limiting or WAF rules). |

### `CAPTCHA_TOLERANCE`

| Field | Value |
|-------|-------|
| **Purpose** | Pixel tolerance when validating the slider position. A higher value makes the puzzle easier to solve. |
| **Default** | `8` |
| **Valid values** | Non-negative integer, recommended range `4–20` |
| **Security notes** | Excessive tolerance (e.g., `50`) effectively disables the challenge because any drop position is accepted. |
| **Common mistakes** | Setting to `0` requires pixel-perfect alignment, causing legitimate users to fail repeatedly and abandon registration. |

### `CAPTCHA_TOKEN_EXPIRY`

| Field | Value |
|-------|-------|
| **Purpose** | Lifetime of a captchaToken in seconds before it expires. |
| **Default** | `300` (5 minutes) |
| **Valid values** | Positive integer, recommended range `60–600` |
| **Security notes** | Shorter expiry windows reduce replay-attack windows. Too short (< 30 s) causes UX friction on slow networks. |
| **Common mistakes** | Setting a very long expiry (e.g., 1 hour) allows attackers to reuse a single solved challenge for multiple requests. |

### `CAPTCHA_TRACK_WIDTH`

| Field | Value |
|-------|-------|
| **Purpose** | Width of the slider track in pixels. Determines the difficulty range of the challenge. |
| **Default** | `300` |
| **Valid values** | Positive integer, recommended range `200–500` |
| **Security notes** | Narrow tracks (< 150 px) are easier for bots to brute-force because the search space is smaller. |
| **Common mistakes** | Setting a width that does not match the frontend CSS, causing visual misalignment and failed drops. |

### `CAPTCHA_MAX_ATTEMPTS`

| Field | Value |
|-------|-------|
| **Purpose** | Maximum number of validation attempts allowed for a single challenge before the captchaToken is invalidated. |
| **Default** | `5` |
| **Valid values** | Positive integer, recommended range `3–10` |
| **Security notes** | Limits brute-force guessing of the slider position. After exceeding this threshold, the user must request a new challenge. |
| **Common mistakes** | Setting to a high value (e.g., 50) defeats the purpose of the challenge by allowing unlimited guesses within the token expiry window. |

---

## N. Backend Logging

### `LOG_HIBERNATE_LEVEL`

| Field | Value |
|-------|-------|
| **Purpose** | Log level for Hibernate SQL statements and JDBC operations. |
| **Default** | `DEBUG` (dev) / `WARN` (prod) |
| **Valid values** | `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF` |
| **Security notes** | `DEBUG` and `TRACE` may log bind parameter values that contain personal data from resumes. Never use `DEBUG` in production without log sanitization. |
| **Common mistakes** | Leaving `DEBUG` on in production and accidentally logging PII (personally identifiable information) to persistent files. |

### `LOG_APP_LEVEL`

| Field | Value |
|-------|-------|
| **Purpose** | Log level for application code (`edu.asu.ser594.resumeassistant`). |
| **Default** | `DEBUG` (dev) / `INFO` (prod) |
| **Valid values** | `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF` |
| **Security notes** | `DEBUG` prints request details, service calls, and exception stack traces. Useful for troubleshooting but generates large log volumes. |
| **Common mistakes** | Setting `TRACE` without log rotation, causing disk space exhaustion within hours on busy servers. |

---
