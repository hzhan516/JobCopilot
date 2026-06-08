<!-- Language Switcher -->
> [English](README.md) | [简体中文](docs/i18n/zh-Hans-CN/README.md) | [繁體中文](docs/i18n/zh-Hant-TW/README.md)

# JobCopilot

**JobCopilot** is an AI-powered platform for new graduates and career changers. It parses uploaded resumes, evaluates them against job market data using semantic vector matching, and provides an interactive AI copilot to iteratively improve resume content. Secure document management, asynchronous AI processing, and personalized recommendations save users time and improve interview rates.

**Deployment:** The system is verified through Docker Compose. Copy `.env.example` to `.env`, configure the required values, then run `docker compose --env-file .env up -d --build`. The frontend is available at `http://localhost` by default, or `http://localhost:${FRONTEND_HOST_PORT}` if a custom port is configured.

## Features

- **Authentication**: Email/password registration (with optional email verification) plus Google OAuth 2.0 login, protected by slider CAPTCHA bot protection
- **Resume Management**: Upload, parse, version, and export resumes in multiple formats
- **AI-Powered Parsing**: Extract structured information from resumes and job posts using LiteLLM-compatible models
- **Job Matching**: Intelligent job recommendations based on resume content and vector similarity
- **Incremental Job Training Loop**: User scoring behavior feeds back into the AI baseline model via incremental learning, improving match accuracy over time without full retraining
- **Application Tracking**: Track job application status and manage your job search pipeline
- **AI Conversation**: Interactive chat assistant for job search advice and resume optimization
- **Internationalization**: English, Simplified Chinese, and Traditional Chinese UI support
- **Vector Search**: Semantic search powered by PostgreSQL pgvector extension

## Architecture

JobCopilot uses a containerized service architecture. The frontend container is the only host-facing entry point by default; backend, AI, data, cache, queue, and model-registry services communicate over Docker networks.

```text
Browser
  |
  | HTTP :${FRONTEND_HOST_PORT:-80}
  v
Frontend container
  React static app + Nginx reverse proxy
  |
  | HTTP /api, /health
  v
Backend container
  Spring Boot API, auth, domain workflows, vector persistence
  |-- JDBC --------> PostgreSQL + pgvector
  |-- AMQP --------> RabbitMQ --------> AI worker container
  |-- HTTP --------> AI API container
  |-- Redis -------> Redis
  `-- Local files -> shared upload volume

AI API / AI worker
  |-- LiteLLM-compatible provider for parsing, embeddings, ranking, chat
  |-- Backend internal API for vector upserts and baseline features
  |-- Redis feedback buffer, locks, and model reload Pub/Sub
  `-- MinIO model registry for LightGBM artifacts
```

| Component | Technology | Exposure | Responsibility |
|-----------|------------|----------|----------------|
| Frontend / Gateway | React 19, Vite 7, Nginx | Host `${FRONTEND_HOST_PORT:-80}` -> container `8080` | Serve UI, proxy `/api` and `/health` to backend |
| Backend | Java 21, Spring Boot 3.5, DDD modules | Internal `8080`; direct host port disabled by default | REST API, authentication, resume/job/application workflows, vector persistence |
| AI API | Python 3.11, FastAPI, LiteLLM | Internal `8000`; direct host port disabled by default | Synchronous AI endpoints, embeddings, parsing, ranking, chat support |
| AI Worker | Python 3.11, RabbitMQ consumers, LightGBM | Internal worker process | Asynchronous parsing, ranking jobs, feedback ingestion, incremental model training |
| PostgreSQL | PostgreSQL 15 with pgvector | Internal `5432` on `db-network` | Business data and vector storage |
| RabbitMQ | RabbitMQ 3 management image | Internal `5672`; management port disabled by default | Durable async task transport between backend and AI services |
| Redis | Redis 7 | Internal `6379` | CAPTCHA state, distributed locks, feedback buffer, model reload Pub/Sub |
| MinIO | S3-compatible object storage | Internal `9000` | LightGBM model registry for AI worker artifacts |

## Project Structure

```text
.
|-- backend/                   # Java / Spring Boot backend
|   |-- api/                   # API DTOs, commands, queries, and facades
|   |-- app/                   # Application services, schedulers, and startup wiring
|   |-- domain/                # Domain entities, value objects, ports, and rules
|   |-- infrastructure/        # Persistence, storage, messaging, security, integrations
|   |-- trigger/               # REST controllers, WebSocket endpoints, MQ listeners
|   |-- types/                 # Shared types and constants
|   |-- scripts/               # Backend maintenance scripts
|   |-- Dockerfile             # Backend container image
|   `-- pom.xml                # Maven multi-module build
|-- frontend/                  # React / Vite / TypeScript frontend
|   |-- src/                   # UI source code
|   |   |-- components/        # Reusable UI components
|   |   |-- pages/             # Route-level pages
|   |   |-- services/          # API clients and service wrappers
|   |   |-- store/             # Client state management
|   |   |-- hooks/             # Shared React hooks
|   |   |-- i18n/              # Runtime i18n setup
|   |   `-- locales/           # Translation resources
|   |-- package.json           # Node.js dependencies and scripts
|   `-- Dockerfile             # Frontend container image
|-- ai-service/                # Python / FastAPI AI service and worker
|   |-- app/
|   |   |-- api/               # FastAPI endpoints
|   |   |-- domain/            # AI domain logic and model abstractions
|   |   |-- infrastructure/    # External integrations
|   |   |-- mq/                # Messaging integration
|   |   |-- services/          # AI application services
|   |   `-- worker/            # Background worker entry points
|   |-- tests/                 # Pytest test suite
|   |-- requirements.txt       # Python dependencies
|   `-- Dockerfile             # AI service container image
|-- docs/                      # ADRs, API docs, architecture, deployment, i18n
|-- eval/                      # AI evaluation scripts, datasets, and results
|-- middleware/                # Custom infrastructure images, such as PostgreSQL
|-- scripts/                   # Repository-level automation helpers
|-- .github/                   # CI, issue templates, PR template, CODEOWNERS
|-- docker-compose.yml         # Local Docker Compose stack
|-- .env.example               # Environment variable template
`-- empty-vertex.json          # Placeholder credentials for non-Vertex local runs
```

## Backend Architecture

The backend adopts **Hexagonal Architecture / Domain-Driven Design (DDD)** with the following layered modules:

| Module           | Description                              | Dependencies      |
|------------------|------------------------------------------|-------------------|
| `types`          | Basic types, enums, constants            | None              |
| `domain`         | Domain entities, services, repositories  | `types`           |
| `api`            | DTOs, facade interfaces                  | `domain`, `types` |
| `infrastructure` | DB, cache, messaging implementations     | `domain`, `api`   |
| `trigger`        | Controllers, schedulers, event listeners | `domain`, `api`   |
| `app`            | Spring Boot startup and configuration    | All modules       |

## Quick Start

### Prerequisites

- Docker 20.10+ and Docker Compose 2.0+
- Or Podman with podman-compose
- One LiteLLM-compatible AI provider key for local AI features, such as Gemini, OpenAI, or Anthropic
- Google Cloud / Vertex AI is optional and is not required for local development

### 1. Clone the Repository

```bash
git clone <repository-url>
cd JobCopilot
```

### 2. Configure Environment Variables

**Recommended: Use the Web Configurator**

Open `docs/deployment/env-setup.html` directly in your browser (no server needed):
1. Select your language and fill in the required fields (marked with *)
2. Click **Generate** next to secret fields for secure random values
3. Click **Download .env** and save it to the project root

**Alternative (CLI):**
```bash
cp .env.example .env
# Edit .env and fill in the required values
```

Key environment variables:

| Variable                 | Required | Description |
|--------------------------|----------|-------------|
| `JWT_SECRET`             | Yes      | Secret key for JWT token generation (min 32 chars) |
| `VITE_GOOGLE_CLIENT_ID`  | Yes      | Google OAuth 2.0 Client ID used by the frontend login flow |
| `INTERNAL_API_KEY`       | Recommended | Shared key for backend-to-AI service calls |
| `GEMINI_API_KEY`         | Conditional | Gemini API key used when `LLM_*_MODEL` uses the `gemini/` prefix |
| `OPENAI_API_KEY`         | Conditional | OpenAI API key used when `LLM_*_MODEL` uses the `openai/` prefix |
| `ANTHROPIC_API_KEY`      | Conditional | Anthropic API key used when `LLM_*_MODEL` uses the `anthropic/` prefix |
| `LLM_TEXT_MODEL`         | No       | LiteLLM text model name; defaults to a Gemini model in Compose |
| `LLM_VISION_MODEL`       | No       | LiteLLM vision model name |
| `LLM_EMBEDDING_MODEL`           | No       | LiteLLM embedding model name |
| `LLM_EMBEDDING_MODEL_DIMENSION` | No       | Embedding output dimension (must match the model) |
| `SPRING_PROFILES_ACTIVE`        | No       | Spring profile: `dev` (default) or `prod` |
| `LOG_LEVEL`              | No       | AI service log level: `INFO` (default) or `DEBUG` |
| `CAPTCHA_ENABLED`        | No       | Enable slider CAPTCHA. Default: `true` |
| `CAPTCHA_TOLERANCE`      | No       | CAPTCHA drag tolerance in pixels. Default: `8` |
| `CAPTCHA_TOKEN_EXPIRY`   | No       | CAPTCHA token expiry in seconds. Default: `300` |
| `CAPTCHA_TRACK_WIDTH`    | No       | CAPTCHA track width in pixels. Default: `300` |
| `REDIS_HOST`             | No       | Redis hostname. Default: `redis` (Docker) or `localhost` |
| `REDIS_PORT`             | No       | Redis port. Default: `6379` |
| `REDIS_PASSWORD`         | Yes      | Redis AUTH password used by Compose; change the local default before deployment |
| `MINIO_ENDPOINT`         | No       | Internal MinIO endpoint for the AI model registry |
| `MINIO_ACCESS_KEY`       | Yes      | MinIO access key for the AI model registry |
| `MINIO_SECRET_KEY`       | Yes      | MinIO secret key for the AI model registry |
| `MINIO_MODEL_BUCKET`     | No       | MinIO bucket used for trained model artifacts |
| `CAPTCHA_MAX_ATTEMPTS`   | No       | Maximum CAPTCHA attempts per IP. Default: `5` |

For local development, copy `.env.example` to `.env` and provide one API key that matches the LiteLLM model prefix you choose. For example, the default Gemini models use `GEMINI_API_KEY`.

Google Cloud ADC is only needed if you intentionally configure the project to use Vertex AI-based models.
Note: The provided `docker-compose.yml` runs backend resume storage in local-file mode by default. MinIO, S3, and OSS settings in `.env.example` are for customized deployments.
Note: If you change the LLM provider, models, dimensions, or frontend build-time variables in `.env` while the system is running, run `docker compose --env-file .env up -d --build` to rebuild/recreate the affected containers. A simple restart will not apply all changes.


### 3. Start Core Services

Using Docker Compose:
```bash
docker compose --env-file .env up -d --build
```

If your environment still uses the legacy Compose CLI, use:
```bash
docker-compose --env-file .env up -d --build
```

Using Podman:
```bash
podman-compose up -d
# or
podman compose up -d
```

For local development, `.env` is loaded by Docker Compose and by the local AI service process. The AI service uses LiteLLM, so provide an API key that matches the configured model provider, such as `GEMINI_API_KEY` for the default Gemini models.

If you run the AI service locally instead of in Docker, source the root `.env` before starting Uvicorn so RabbitMQ and LLM settings match the Compose environment.


### 4. Verify Services

| Service             | URL                                   | Description                    |
|---------------------|---------------------------------------|--------------------------------|
| Frontend UI         | http://localhost                      | Main entry point (React App)   |
| Backend API         | http://localhost/api                  | REST endpoints through Nginx   |
| System Health       | http://localhost/health               | Global health check            |

*Note: In the three-tier network architecture, only the configured Frontend port is exposed to the host. Backend, AI, RabbitMQ, Redis, and DB services are safely isolated by Docker networks.*

*Note: If `FRONTEND_HOST_PORT` is changed in `.env`, replace `http://localhost` with `http://localhost:${FRONTEND_HOST_PORT}`.*

### 5. Stop Services

```bash
docker compose --env-file .env down

# To remove volumes (WARNING: data will be lost)
docker compose --env-file .env down -v
```

## Testing

The project includes backend JUnit tests, frontend Vitest tests, and AI-service pytest tests covering API, domain, persistence, authentication, UI utilities, AI service, and message queue logic.

### Backend Tests (Java)

Run unit and integration tests for the Spring Boot backend:

```bash
cd backend
mvn test
```

Some backend integration tests use Testcontainers and require a working Docker environment.

### Frontend Tests (TypeScript)

Run linting, unit tests, coverage, and production build checks:

```bash
cd frontend
npm install
npm run lint
npm run test:run
npm run test:coverage
npm run build
```

### AI Service Tests (Python)

Run pytest for the FastAPI and AI logic:

```bash
cd ai-service
pip install -r requirements.txt
pytest
```

## AI Evaluation Suite

To evaluate the AI components against fixed benchmark cases:

```bash
cd eval
# Install evaluation dependencies
pip install -r requirements.txt
# Load the root .env first so LiteLLM provider settings are available
# Run the current evaluation pipeline:
# resume parsing, job parsing, and single-job suitability scoring
python run_eval.py

# Optional: also run the legacy job-ranking NDCG@5 evaluation
python run_eval.py --include-legacy-ranking
```

*Evaluation results will be exported to `eval/results/`.*

## Development

### Frontend Development

```bash
cd frontend
npm install
npm run dev
```

The development server will start at <http://localhost:5173>

### Backend Development

Requirements:

- JDK 21
- Maven 3.9+

```bash
cd backend

# Build all modules
mvn clean install

# Run with dev profile (default)
mvn spring-boot:run -pl app

# Run with prod profile
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=prod
```

### AI Service Development

Requirements:

- Python 3.11+
- pip or poetry
- A LiteLLM-compatible provider key configured in the root `.env` file

```bash
cd ai-service

# Create virtual environment
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Load root environment variables
set -a
source ../.env
set +a

# Run development server
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```
Google Cloud ADC is not required for local development unless you intentionally configure LiteLLM to use a Vertex AI-based model.


## Deployment

See [docs/deployment/DOCKER_DEPLOY.md](docs/deployment/DOCKER_DEPLOY.md) for detailed deployment instructions including:

- Production deployment checklist
- Environment configuration
- SSL/TLS setup
- Monitoring and logging
- Backup strategies

## Technology Stack

### Frontend

- React 19
- Vite 7
- TypeScript 5.9
- React Router 7
- Axios
- Zustand

### Backend

- Java 21
- Spring Boot 3.5.7
- PostgreSQL 15 + pgvector
- RabbitMQ 3
- Maven 3.9+

### AI Service

- Python 3.11
- FastAPI 0.115
- LiteLLM 1.61.11-compatible text, vision, and embedding models
- Gemini via Google AI Studio by default; Vertex AI is optional
- Uvicorn 0.32

### DevOps

- Docker & Docker Compose
- Nginx
- Redis 7 (distributed state, locks, Pub/Sub)
- Flyway (database migration)

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for details.

## Acknowledgments

Thanks to the open-source projects, contributors, and users that make JobCopilot possible.
