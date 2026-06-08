# JobCopilot — Docker Compose Deployment Architecture

> [简体中文](../i18n/zh-Hans-CN/deployment/architecture.md) | [繁體中文](../i18n/zh-Hant-TW/deployment/architecture.md)

## 1. Overview

JobCopilot is an AI-powered job-search platform deployed as a **three-tier Docker network architecture**: a public-facing reverse proxy tier, an internal application tier, and an isolated database tier.

## 2. Network Architecture

```
                         Internet
                            |
                            v
                    +---------------+
                    | Host:80 ->    |  <-- Only public entry point
                    | Nginx : 8080  |
                    |  (frontend)   |
                    +---------------+
                            |
            +---------------+---------------+
            |                               |
            v                               v
   +------------------+           +------------------+
   |  backend : 8080  |           |  ai-service   :8000  |
   |  (Spring Boot)   |<--------->|  (FastAPI)       |
   +------------------+           +------------------+
            |                               |
            |      +------------------+     |
            |      | rabbitmq : 5672  |<----+
            |      | (Message Queue)  |     |
            |      +------------------+     |
            |      +------------------+     |
            +----->|  redis   : 6379  |<----+
                   |  (Cache & Locks) |
                   +------------------+
            |               |
            v               v
   +------------------+   +------------------+
   | postgres : 5432  |   |  ai-worker       |
   | (PostgreSQL +    |   |  (LightGBM)      |
   |  pgvector)       |   +------------------+
   +------------------+           |
                                  v
                          +------------------+
                          |  minio : 9000    |
                          |  (Model Registry)|
                          +------------------+
```

### Network Segmentation

| Network | Services | External Exposure | Purpose |
|---------|----------|-------------------|---------|
| **Public** | `frontend` (Nginx), `backend` | Host `${FRONTEND_HOST_PORT:-80}` to `frontend:8080` | Single entry point for all HTTP/HTTPS traffic |
| **Internal** | `backend`, `ai-service`, `ai-worker`, `rabbitmq`, `redis`, `minio` | None (Docker DNS only) | Inter-service communication via container names |
| **Database** | `backend`, `postgres` | None (Docker DNS only) | Isolated persistent data storage |

> **Note on Development Template**: The current `docker-compose.yml` exposes only the frontend by default. Direct host ports for backend (`8080`), AI service (`8000`), PostgreSQL (`5432`), RabbitMQ (`5672`), and RabbitMQ Management (`15672`) are kept as commented development-only examples. In a **production deployment**, only the frontend host port should be exposed.

## 3. Service Inventory

### 3.1 Frontend (Nginx + React)

| Attribute | Value |
|-----------|-------|
| **Networks** | `public-network` |
| **Host ports** | `${FRONTEND_HOST_PORT:-80}:8080` |
| **Role** | Static SPA host and reverse proxy for all API traffic. |
| **Security notes** | Nginx proxies `/api/*` to `backend:8080`. The `VITE_API_BASE_URL` environment variable **must be empty or a relative path** (e.g. `/api`). If set to an absolute URL (e.g. `http://backend:8080`), the browser will attempt direct connections to the backend, bypassing Nginx and breaking the single-entry-point security model. |

### 3.2 Backend (Spring Boot)

| Attribute | Value |
|-----------|-------|
| **Networks** | `public-network`, `internal-network`, `db-network` |
| **Host ports** | None by default; `8080:8080` is a commented dev-only example |
| **Role** | REST API gateway, JWT authentication, CAPTCHA verification, business logic orchestration, and RabbitMQ producer. |
| **Security notes** | The only service that spans all three network tiers. Communicates with PostgreSQL via Docker DNS (`postgres:5432`) and with RabbitMQ (`rabbitmq:5672`). All outbound REST calls to `ai-service` include the `X-Internal-API-Key` header. |

### 3.3 AI Service (FastAPI)

| Attribute | Value |
|-----------|-------|
| **Networks** | `internal-network` |
| **Host ports** | None by default; `8000:8000` is a commented dev-only example |
| **Role** | LLM inference, embedding generation, resume/job parsing, job ranking, suitability scoring, and adaptive model artifact loading. |
| **Security notes** | REST endpoint `/api/v1/ai/embeddings` is protected by `X-Internal-API-Key` middleware. MQ consumers listen on four queues: `ai.queue.job.parse`, `ai.queue.resume.parse`, `ai.queue.conversation`, and `ai.queue.job.rank`. No database access. |

### 3.4 AI Worker (LightGBM)

| Attribute | Value |
|-----------|-------|
| **Networks** | `internal-network` |
| **Host ports** | None |
| **Role** | Background worker for incremental model training. Consumes feedback from `ai.queue.feedback` and saves trained models to MinIO. |
| **Security notes** | Strictly isolated from PostgreSQL. Only communicates with RabbitMQ, Redis, and MinIO. |

### 3.5 PostgreSQL (with pgvector)

| Attribute | Value |
|-----------|-------|
| **Networks** | `db-network` |
| **Host ports** | None by default; `5432:5432` is a commented dev-only example |
| **Role** | Unified storage for business data and vector embeddings. |
| **Security notes** | Uses the `pgvector` extension for similarity search. Accessible only from `backend` within the Docker network. Even with network isolation, a strong `POSTGRES_PASSWORD` is mandatory as a defense-in-depth measure. |

### 3.6 RabbitMQ (Management)

| Attribute | Value |
|-----------|-------|
| **Networks** | `internal-network` |
| **Host ports** | None by default; `5672:5672` and `15672:15672` are commented dev-only examples |
| **Role** | Async message broker between backend, AI Service, and AI Worker (Outbox pattern). |
| **Security notes** | Override the default `guest/guest` credentials via `RABBITMQ_USERNAME` and `RABBITMQ_PASSWORD`. The Management UI (`:15672`) should never be exposed to the public internet; access it via SSH tunnel: `ssh -L 15672:localhost:15672 <host>`. Message size limit is set to 10 MB (`max_message_size 10485760`) to accommodate vectors and resume summaries. |

### 3.7 Redis (Cache & Locks)

| Attribute | Value |
|-----------|-------|
| **Networks** | `internal-network` |
| **Host ports** | None in production |
| **Role** | Distributed state storage: CAPTCHA challenges/tokens, verification codes, conversation streaming bridges, incremental model statistics, deduplication sets, and distributed locks (ShedLock). |
| **Security notes** | No external access. Password auth is optional in dev (`REDIS_PASSWORD` may be empty) but recommended in production. Data persists via the `redis-data` named volume. |

### 3.8 MinIO (Model Registry)

| Attribute | Value |
|-----------|-------|
| **Networks** | `internal-network` |
| **Host ports** | None in production |
| **Role** | Object storage for trained LightGBM model artifacts. |
| **Security notes** | No external access. Used exclusively by `ai-worker` (write) and `ai-service` (read). |

## 4. Defense in Depth

The deployment implements five independent security layers. Breaching one does not automatically compromise the next.

### Layer 1: Network Isolation

Only the `frontend` service is exposed to the internet through `${FRONTEND_HOST_PORT:-80}` on the host, forwarding to Nginx on container port `8080`. All other services communicate through Docker's internal DNS (`<service-name>`). An external port scan of the host should only discover the configured frontend host port.

### Layer 2: Application-Layer API Key

The `INTERNAL_API_KEY` environment variable is shared between `backend` and `ai-service`. Every REST request from the backend to the AI service carries the `X-Internal-API-Key` header. The AI service middleware rejects requests with missing or mismatched keys with HTTP 401.

> **Security model**: Even if an attacker gains access to the internal Docker network, they cannot invoke the LLM embedding endpoint without the secret key.

### Layer 3: JWT Authentication

All user-facing API calls (registration, login, resume upload, job matching) carry a signed JWT in the `Authorization: Bearer <token>` header. The signing key (`JWT_SECRET`) is known only to the backend. Token rotation or secret changes force all users to re-authenticate.

### Layer 4: RabbitMQ Credentials

AMQP connections require a username and password. The default `guest/guest` is overridden via environment variables. Even if a container is compromised, accessing the message broker requires separate credentials.

### Layer 5: Human Verification (CAPTCHA)

All authentication endpoints (registration, login) require a valid CAPTCHA challenge-response. The backend maintains prefix-isolated Redis-backed caches (String for challenges/tokens, Sorted Set for IP rate-limit sliding windows) with IP-based rate limiting (20 requests/minute). Even if an attacker bypasses network isolation and possesses valid credentials, they cannot programmatically authenticate without solving the CAPTCHA challenge.

## 5. Quick Start

```bash
# 1. Copy the environment template
cp .env.example .env

# 2. Edit .env and replace all [replace-me] placeholders
vim .env

# 3. Generate a strong JWT secret (48 bytes = 64 base64 chars)
openssl rand -base64 48
# Paste the output into .env as JWT_SECRET

# 4. Generate an internal API key (32 bytes = 44 base64 chars)
openssl rand -base64 32
# Paste the output into .env as INTERNAL_API_KEY
# (both backend and ai-service must share the exact same value)

# 5. Start all services
docker compose up -d

# 6. Verify container health
docker compose ps

# 7. Check the frontend health endpoint
curl -f http://localhost/health
```

Expected output from step 8: `HTTP 200 OK` with a short health status body.

## 6. Troubleshooting

### Port 80 already in use

**Symptom**: `docker compose up` fails with `bind: address already in use`.

**Solution**: Change the frontend host port in `docker-compose.yml`:

```yaml
ports:
  - "8081:8080"   # or any free port on your host
```

Then access the app at `http://localhost:8081`.

### RabbitMQ Management UI unreachable

**Symptom**: Browser cannot connect to `http://localhost:15672`.

**Solution**: In production, the Management UI is intentionally **not** exposed externally. Use an SSH tunnel:

```bash
ssh -L 15672:localhost:15672 user@your-server
# Then open http://localhost:15672 in your local browser
```

### AI Service returns 401 Unauthorized

**Symptom**: Backend logs show `401 Unauthorized: invalid or missing internal API key` when calling the embedding endpoint.

**Solution**: Ensure `INTERNAL_API_KEY` is set to **exactly the same value** in both the `backend` and `ai-service` service environments. Verify with:

```bash
docker compose exec backend env | grep INTERNAL_API_KEY
docker compose exec ai-service env | grep INTERNAL_API_KEY
```

### Database connection refused

**Symptom**: `psql: could not connect to server: Connection refused` from the host.

**Solution**: PostgreSQL is isolated on the internal Docker network and does **not** expose port `5432` to the host in production. This is expected behavior. To access the database, enter the container:

```bash
docker compose exec postgres psql -U resume_user -d resume_assistant
```
