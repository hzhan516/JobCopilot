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
| `gemini/gemini-embedding-001` | `768` |
| `openai/text-embedding-ada-002` | `1536` |
| `openai/text-embedding-3-small` | `1536` |
| `openai/text-embedding-3-large` | `3072` |
| `sentence-transformers/all-MiniLM-L6-v2` | `384` |

> **Note**: The default `1536` in `.env.example` corresponds to OpenAI Ada-002. If you use Gemini embedding, change this to `768`.

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
| **Default** | *(empty)* |
| **Valid values** | Absolute filesystem path, e.g. `/home/user/service-account.json` |
| **Security notes** | **CRITICAL**: This **must** be an absolute path. Relative paths or plain filenames (e.g. `vertex.json`) are interpreted by Docker/Podman as named volume references, which will silently create an empty volume instead of mounting your credentials file. The file is mounted read-only (`:ro`) to prevent accidental modification. Store this key in a secrets manager and rotate it regularly. |
| **Common mistakes** | <ul><li>Using a relative path like `./vertex.json` or `~/keys/gcp.json`.</li><li>Using a path inside the project directory that is `.gitignore`d but forgetting to copy the file to the deployment host.</li><li>Setting this when using Gemini AI Studio (`gemini/` prefix) — ADC credentials are not needed for AI Studio.</li></ul> |

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
