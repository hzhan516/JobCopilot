<!-- Language Switcher -->
> [English](DOCKER_DEPLOY.md) | [简体中文](../i18n/zh-Hans-CN/deployment/DOCKER_DEPLOY.md) | [繁體中文](../i18n/zh-Hant-TW/deployment/DOCKER_DEPLOY.md)

# Docker/Podman Deployment Guide

## Requirements

- **Docker**: 20.10+ or **Podman**: 4.0+
- **Docker Compose**: 2.0+ or **podman-compose**: 1.0+
- Memory: at least 4GB free RAM
- Disk: at least 10GB free space

## Quick Start

### 1. Configure Environment Variables

**Recommended: Use the Web Configurator**

Open `docs/deployment/env-setup.html` directly in your browser (no server needed):

1. Select your language (EN / 简体中文 / 繁體中文) from the top-right
2. Fill in the required fields (marked with a red asterisk)
3. Click **Generate** next to secret fields (e.g. `JWT_SECRET`, `INTERNAL_API_KEY`) for secure random values
4. Review the progress indicator at the top to ensure all required variables are set
5. Click **Download .env** and save it to the project root directory

**Alternative (CLI):**

```bash
# Copy the environment template
cp .env.example .env

# Edit .env and fill in required values
vim .env
```

Required variables:

- `JWT_SECRET`: JWT signing key (must be changed in production)
- A LiteLLM-compatible model service key, e.g. `GEMINI_API_KEY`, `OPENAI_API_KEY`, or `ANTHROPIC_API_KEY`
- `LLM_TEXT_MODEL`, `LLM_VISION_MODEL`, and `LLM_EMBEDDING_MODEL`: model names matching the selected service prefix
- `LLM_EMBEDDING_MODEL_DIMENSION`: embedding output dimension (must match the selected model, default 1536)
- `CAPTCHA_ENABLED`: Enable CAPTCHA verification for auth endpoints (`true`/`false`, default `true`)
- `CAPTCHA_TOLERANCE`: Slider tolerance in pixels (default `8`)
- `CAPTCHA_MAX_ATTEMPTS`: Maximum verification attempts per challenge (default `5`)
- `CAPTCHA_TOKEN_EXPIRY`: Token cache TTL in seconds (default `300`)

By default the project can use Gemini models via LiteLLM, so local development only needs `GEMINI_API_KEY` unless you choose another provider.

Google Cloud ADC is optional. It is only needed if you actively configure LiteLLM to use Vertex AI models.
Note: If you change the LLM provider, models, or dimensions in .env while the system is running, you must execute "docker compose up -d" to apply the new environment variables. A simple restart will not take effect.

### 2. Start Services

#### Using Docker

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop and remove data volumes (use with caution)
docker-compose down -v
```

#### Using Podman

```bash
# Method 1: podman-compose
podman-compose up -d

# Method 2: native podman compose (Podman 3.0+)
podman compose up -d

# View logs
podman-compose logs -f

# Stop services
podman-compose down
```

### 3. Verify Service Status

```bash
# View all container statuses
docker-compose ps
# or
podman-compose ps

# Health checks through the public Nginx entry point
curl http://localhost/health
curl http://localhost/api/actuator/health

# AI service is internal by default; check it from inside the container
docker compose exec ai-service python -c "import urllib.request; print(urllib.request.urlopen('http://localhost:8000/health').read().decode())"
```

## Service Endpoints

| Service          | Address                              | Description          |
|------------------|--------------------------------------|----------------------|
| Frontend UI      | http://localhost                     | Job seeker interface |
| Backend API      | http://localhost/api                 | REST API (Proxied)   |
| System Health    | http://localhost/health              | Global health check  |

## Service Responsibilities

### AI Service (Python FastAPI)

In addition to resume/job parsing, embedding generation, ranking, and conversation, the AI service now includes an **incremental model training loop**:

1. **Job Dataset Sync**: When a job is successfully parsed, the backend writes it to the `job_dataset` table (training corpus).
2. **Score Label Consumption**: When a user scores a job, the backend sends a score label message to the `ai.queue.feedback` queue.
3. **Feedback Buffering**: The AI worker converts feedback into labeled feature samples and stores them in a Redis buffer.
4. **LightGBM Retraining**: When the sample threshold (`MIN_SAMPLES_FOR_RETRAIN=10`) is reached, the worker combines buffered feedback with baseline features, trains a LightGBM ranker, and writes versioned artifacts such as `ranker_model_<version>.txt` plus `latest_meta.json` to MinIO.
5. **Hot Reload**: The model manager loads the latest model from MinIO and reloads it when the worker publishes a Redis `ai.model.reload` notification.

`POST /api/v1/admin/recompute-model` is kept for compatibility and returns a deprecation message; scheduled retraining is handled by `ai-worker`.

## Common Commands

### View Logs

```bash
# All service logs
docker-compose logs -f

# Specific service logs
docker-compose logs -f backend
docker-compose logs -f ai-service
docker-compose logs -f postgres
docker-compose logs -f redis
```

### Restart Services

```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart backend
```

### Rebuild Services

```bash
# Rebuild and start (after code updates)
docker-compose up -d --build

# Rebuild only a specific service
docker-compose up -d --build backend
```

### Enter a Container

```bash
# Enter backend container
docker-compose exec backend sh

# Enter database container
docker-compose exec postgres psql -U resume_user -d JobCopilot
```

## Data Persistence

Data is persisted via Docker volumes:

- `postgres-data`: PostgreSQL database data
- `rabbitmq-data`: RabbitMQ message queue data
- `redis-data`: Redis cache and state data
- `shared-storage`: Uploaded resume files (shared between backend and AI service)

```bash
# List volumes
docker volume ls

# Backup data (PostgreSQL)
docker-compose exec postgres pg_dump -U resume_user JobCopilot > backup.sql

# Restore data
docker-compose exec -T postgres psql -U resume_user JobCopilot < backup.sql
```

## Queue Parameter Changes & Reset

RabbitMQ queue arguments are immutable once a queue is created. If application code changes queue declarations (e.g., adding `x-dead-letter-exchange` for DLX/DLQ support), Spring AMQP will fail to re-declare the existing queue with a `406 PRECONDITION_FAILED` error.

### Development Environment

Delete old queues and let Spring AMQP re-declare them on next startup:

```bash
cd backend
./scripts/reset-rabbitmq-queues.sh
```

Then restart the backend container:

```bash
docker-compose up -d --build backend
```

### Production Environment

Plan a maintenance window:
1. Stop producers (backend) to prevent new messages.
2. Drain or back up messages in affected queues.
3. Delete the old queues via the RabbitMQ Management UI or CLI.
4. Restart the backend; Spring AMQP will declare queues with the new parameters.

Alternatively, if message loss is acceptable, use the same `reset-rabbitmq-queues.sh` script.

## Database Initialization vs. Migration

The development environment disables Flyway (`spring.flyway.enabled=false` in `application-dev.yml`). Instead, PostgreSQL initialization relies on `docker-entrypoint-initdb.d`, which **only runs on the first database initialization** (when the data directory is empty).

### New Environment

When starting with a fresh `postgres-data` volume, all `.sql` files in `backend/app/src/main/resources/db/migration/` are executed alphabetically by `docker-entrypoint-initdb.d`. New tables (e.g., `outbox_message`) are created automatically.

### Existing Environment

If the database has already been initialized, adding a new `.sql` file will **not** cause it to run automatically. You have two options:

1. **Manual SQL execution** (recommended for dev environments with data):
   ```bash
   docker exec -i JobCopilot-postgres \
     psql -U resume_user -d JobCopilot \
     < backend/app/src/main/resources/db/migration/init_outbox_message.sql
   ```

2. **Recreate the volume** (destroys all data):
   ```bash
   docker-compose down -v
   docker-compose up -d
   ```

> **Production Note**: Production uses `spring.flyway.enabled=true` (see `application-prod.yml`). Flyway applies pending migrations on startup automatically.

## Troubleshooting

### Port Conflicts

If you see port-conflict errors on startup, change the port mapping in `docker-compose.yml`:

```yaml
ports:
  - "8081:8080"  # Map host port to 8081
```

### Out of Memory

If containers restart frequently, it may be due to insufficient memory:

```bash
# View container resource usage
docker stats

# Increase Docker memory limit (Docker Desktop)
```

### 406 PRECONDITION_FAILED (RabbitMQ)

**Symptom**: Backend logs show `channel.close(reply-code=406, reply-text=PRECONDITION_FAILED - inequivalent arg 'x-dead-letter-exchange'...)`.

**Cause**: RabbitMQ already has a queue with the same name but different arguments. Queue arguments are immutable.

**Fix**: Run the queue reset script and restart the backend:
```bash
cd backend && ./scripts/reset-rabbitmq-queues.sh
cd .. && docker-compose up -d --build backend
```

Or completely reset RabbitMQ by recreating its volume:
```bash
docker-compose down -v
docker-compose up -d
```

### "Relation does not exist" (PostgreSQL)

**Symptom**: Backend logs show `ERROR: relation "outbox_message" does not exist`.

**Cause**: The `outbox_message` table was added after the database was already initialized. `docker-entrypoint-initdb.d` only runs on the first initialization.

**Fix**: Manually execute the initialization SQL:
```bash
docker exec -i JobCopilot-postgres \
  psql -U resume_user -d JobCopilot \
  < backend/app/src/main/resources/db/migration/init_outbox_message.sql
```

Or recreate the database volume:
```bash
docker-compose down -v
docker-compose up -d
```

### `FATAL: database "JobCopilot" does not exist` (PostgreSQL)

**Symptom**: Backend or postgres healthcheck fails with `database "JobCopilot" does not exist` or `role "resume_user" does not exist`.

**Cause**: The `postgres-data` volume was already initialized (e.g., with default `postgres` credentials or from a previous project). PostgreSQL `docker-entrypoint-initdb.d` and `POSTGRES_USER`/`POSTGRES_DB` environment variables only apply on the **first** initialization when the data directory is empty.

**Fix**: Delete the volume and re-initialize:
```bash
docker-compose down
docker volume rm <project_name>_postgres-data
docker-compose up -d
```

### Clean Build Cache

```bash
# Clean unused images, containers, and volumes
docker system prune -a --volumes

# Rebuild
docker-compose up -d --build --force-recreate
```

### CAPTCHA Rate Limit Triggered

**Symptom**: `429 Too Many Requests` when requesting a CAPTCHA challenge.

**Cause**: The same IP has exceeded 20 CAPTCHA requests per minute.

**Fix**: Wait 1 minute for the rate-limit cache to expire, or set `CAPTCHA_ENABLED=false` in `.env` to disable CAPTCHA for local testing.

## Production Deployment

1. Edit `.env`:
   ```
   SPRING_PROFILES_ACTIVE=prod
   JWT_SECRET=<strong-secret>
   POSTGRES_PASSWORD=<strong-password>
   RABBITMQ_PASS=<strong-password>
   ```

2. Use production configuration:
   ```bash
   docker compose up -d --build
   ```

3. Configure reverse proxy (Nginx/Traefik)

4. Enable HTTPS (Let's Encrypt)

## Notes

1. **Podman users**:
   - Ensure `podman-compose` is installed
   - Or use `podman compose` (native support in Podman 3.0+)
   - If you encounter permission issues, check SELinux settings

2. **Windows users**:
   - Use WSL2 to run Docker
   - File sharing performance may be slower

3. **Mac users**:
   - Docker Desktop defaults to 2GB memory; increase to 4GB+
