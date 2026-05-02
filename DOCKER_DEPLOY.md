<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](DOCKER_DEPLOY.md) | [简体中文](docs/i18n/zh-Hans-CN/DOCKER_DEPLOY.md) | [繁體中文](docs/i18n/zh-Hant-TW/DOCKER_DEPLOY.md)

# Docker/Podman Deployment Guide

## Requirements

- **Docker**: 20.10+ or **Podman**: 4.0+
- **Docker Compose**: 2.0+ or **podman-compose**: 1.0+
- Memory: At least 4GB available memory
- Disk: At least 10GB available space

## Quick Start

### 1. Configure Environment Variables

```bash
# Copy environment variable template
cp .env.example .env

# Edit .env file and fill in necessary configurations
vim .env
```

Required configurations:

- `OPENAI_API_KEY`: Your OpenAI API key
- `JWT_SECRET`: JWT signing secret (must be changed in production)

### 2. Start Services

#### Using Docker

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop and remove volumes (use with caution)
docker-compose down -v
```

#### Using Podman

```bash
# Method 1: Using podman-compose
podman-compose up -d

# Method 2: Using native podman compose (Podman 3.0+)
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

# Health checks
curl http://localhost:8080/api/actuator/health
curl http://localhost:8000/health
curl http://localhost/health
```

## Service Access URLs

| Service             | URL                                  | Description             |
|---------------------|--------------------------------------|-------------------------|
| Frontend UI         | http://localhost                     | Job seeker interface    |
| Backend API         | http://localhost:8080/api            | REST API                |
| AI Service          | http://localhost:8000                | FastAPI docs            |
| RabbitMQ Management | http://localhost:15672               | guest/guest             |
| H2 Console          | http://localhost:8080/api/h2-console | Development environment |

## Common Commands

### View Logs

```bash
# All service logs
docker-compose logs -f

# Specific service logs
docker-compose logs -f backend
docker-compose logs -f ai-service
docker-compose logs -f postgres
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

# Rebuild only specific service
docker-compose up -d --build backend
```

### Enter Containers

```bash
# Enter backend container
docker-compose exec backend sh

# Enter database container
docker-compose exec postgres psql -U resume_user -d resume_assistant
```

## Data Persistence

Data is persisted through Docker volumes:

- `postgres-data`: PostgreSQL database data
- `rabbitmq-data`: RabbitMQ message queue data
- `shared-storage`: Uploaded resume files (shared between backend and AI service)

```bash
# View volumes
docker volume ls

# Backup data (PostgreSQL)
docker-compose exec postgres pg_dump -U resume_user resume_assistant > backup.sql

# Restore data
docker-compose exec -T postgres psql -U resume_user resume_assistant < backup.sql
```

## Troubleshooting

### Port Conflicts

If port conflict errors occur during startup, modify the port mappings in `docker-compose.yml`:

```yaml
ports:
  - "8081:8080"  # Change host port to 8081
```

### Out of Memory

If containers restart frequently, it may be due to insufficient memory:

```bash
# View container resource usage
docker stats

# Increase Docker memory limit (Docker Desktop)
```

### Clean Build Cache

```bash
# Clean unused images, containers, volumes
docker system prune -a --volumes

# Rebuild
docker-compose up -d --build --force-recreate
```

## Production Deployment

1. Modify `.env` file:
   ```
   SPRING_PROFILES_ACTIVE=prod
   JWT_SECRET=<strong-secret>
   POSTGRES_PASSWORD=<strong-password>
   RABBITMQ_PASS=<strong-password>
   ```

2. Use production environment configuration:
   ```bash
   docker-compose -f docker-compose.yml.example -f docker-compose.prod.yml up -d
   ```

3. Configure reverse proxy (Nginx/Traefik)

4. Enable HTTPS (Let's Encrypt)

## Notes

1. **Podman Users**:
    - Ensure `podman-compose` is installed
    - Or use `podman compose` (native support in Podman 3.0+)
    - If permission issues occur, check SELinux settings

2. **Windows Users**:
    - Use WSL2 to run Docker
    - File sharing performance may be slower

3. **Mac Users**:
    - Docker Desktop default memory is 2GB, recommend increasing to 4GB+
