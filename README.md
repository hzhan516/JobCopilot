[中文文档](./docs/README.zh.md) | [English Document](./README.md)

# Resume Assistant

The **Resume Assistant** is an AI-powered platform designed to streamline the job hunting process for new graduates and career changers. It automatically parses user-uploaded resumes, evaluates them against job market data using semantic vector matching, and provides an interactive AI copilot to iteratively optimize resume content. By combining secure document management, asynchronous AI processing, and personalized recommendations, the system saves users hours of manual tailoring while increasing their interview chances.

**Deployment URL:** [TBD - Will be updated upon implementation completion]

## Team Roster

- **Guixing Jia** (@GuixingJia) - Project Manager, Python AI Service & Frontend
- **Hansheng Zhang** (@hzhan516) - Java Backend & Database Lead
- **Mu-Hsi Yu** (@muhsiyu) - Frontend & UX Lead, Python AI Service

## Features

- **Resume Management**: Upload, parse, and manage your resumes in multiple formats
- **AI-Powered Parsing**: Extract structured information from resumes using OpenAI
- **Job Matching**: Intelligent job recommendations based on resume content and vector similarity
- **Application Tracking**: Track job application status and manage your job search pipeline
- **AI Conversation**: Interactive chat assistant for job search advice and resume optimization
- **Vector Search**: Semantic search powered by PostgreSQL pgvector extension

## Architecture

This project adopts a microservices architecture with the following components:

```text
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Frontend  │──────▶│   Backend   │◀─────▶│    AI       │
│   (React)   │      │  (Spring    │      │  (FastAPI)  │
│             │      │   Boot)     │      │             │
└─────────────┘      └──────┬──────┘      └──────▲──────┘
                            │                      │
                            ▼                      │
                     ┌─────────────┐               │
                     │  PostgreSQL │               │
                     │  + pgvector │───────────────┘
                     └─────────────┘     (Message Queue)
                            ▲
                            │
                     ┌─────────────┐
                     │   RabbitMQ  │
                     └─────────────┘
```

| Service | Technology | Port | Description |
|---------|------------|------|-------------|
| Frontend | React 18 + Vite | 80 | Web user interface served by Nginx |
| Backend | Java 21 + Spring Boot 3.5 | 8080 | REST API and business logic |
| AI Service | Python 3 + FastAPI | 8000 | AI processing and OpenAI integration |
| Database | PostgreSQL 15 + pgvector | 5432 | Business data and vector storage |
| Message Queue | RabbitMQ 3 | 5672 / 15672 | Async message processing |

## Project Structure

```text
.
├── frontend/              # React frontend application
│   ├── src/              # Source code
│   ├── package.json      # Node.js dependencies
│   └── Dockerfile        # Frontend Docker image
├── backend/              # Java Spring Boot backend
│   ├── app/              # Application entry point
│   ├── api/              # API layer (DTOs, Facades)
│   ├── domain/           # Domain layer (business logic)
│   ├── infrastructure/   # Infrastructure (DB, cache, messaging)
│   ├── trigger/          # Controllers, schedulers, listeners
│   └── types/            # Shared types and constants
├── ai-service/           # Python AI service
│   ├── app/              # FastAPI application
│   ├── requirements.txt  # Python dependencies
│   └── Dockerfile        # AI service Docker image
├── docs/                 # Documentation
├── eval/                 # Evaluation scripts
├── tests/                # Test scripts
├── docker-compose.yml    # Docker Compose configuration
└── .env.example          # Environment variables template
```

## Backend Architecture

The backend adopts **Hexagonal Architecture / Domain-Driven Design (DDD)** with the following layered modules:

| Module | Description | Dependencies |
|--------|-------------|--------------|
| `types` | Basic types, enums, constants | None |
| `domain` | Domain entities, services, repositories | `types` |
| `api` | DTOs, facade interfaces | `domain`, `types` |
| `infrastructure` | DB, cache, messaging implementations | `domain`, `api` |
| `trigger` | Controllers, schedulers, event listeners | `domain`, `api` |
| `app` | Spring Boot startup and configuration | All modules |

## Quick Start

### Prerequisites

- Docker 20.10+ and Docker Compose 2.0+
- Or Podman with podman-compose
- OpenAI API key or access to a compatible LLM API

### 1. Clone the Repository

```bash
git clone <repository-url>
cd resume-assistant
```

### 2. Configure Environment Variables

```bash
cp .env.example .env
# Edit .env and fill in your OpenAI API key
```

Required environment variables:

| Variable | Required | Description |
|----------|----------|-------------|
| `OPENAI_API_KEY` | Yes | Your OpenAI API key |
| `JWT_SECRET` | Yes | Secret key for JWT token generation (min 32 chars) |
| `SPRING_PROFILES_ACTIVE` | No | Spring profile: `dev` (default) or `prod` |
| `LOG_LEVEL` | No | AI service log level: `INFO` (default) or `DEBUG` |

### 3. Start All Services

Using Docker Compose:

```bash
docker-compose up -d
```

Using Podman:

```bash
podman-compose up -d
# or
podman compose up -d
```

### 4. Verify Services

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | <http://localhost> | Web application |
| Backend API | <http://localhost:8080/api> | REST API endpoints |
| Backend Health | <http://localhost:8080/actuator/health> | Health check |
| AI Service | <http://localhost:8000> | FastAPI documentation |
| RabbitMQ Management | <http://localhost:15672> | Message queue UI (guest/guest) |

### 5. Stop Services

```bash
docker-compose down

# To remove volumes (WARNING: data will be lost)
docker-compose down -v
```

## Testing

The project maintains a rigorous test suite (`> 80%` coverage) across all modules to ensure system reliability.

### Backend Tests (Java)

Run unit and integration tests for the Spring Boot backend:

```bash
cd backend
mvn test
```

### AI Service Tests (Python)

Run pytest for the FastAPI and AI logic:

```bash
cd ai-service
pip install -r requirements.txt
pytest
```

## AI Evaluation Suite

To evaluate the AI components (Extraction F1 Score and Recommendation NDCG@5) against baselines:

```bash
cd eval
# Install evaluation dependencies
pip install -r requirements.txt
# Run the evaluation pipeline
python run_eval.py
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

```bash
cd ai-service

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run development server
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Deployment

See [DOCKER_DEPLOY.md](./DOCKER_DEPLOY.md) for detailed deployment instructions including:

- Production deployment checklist
- Environment configuration
- SSL/TLS setup
- Monitoring and logging
- Backup strategies

## Technology Stack

### Frontend

- React 18.2
- Vite 5.0
- React Router 6
- Axios

### Backend

- Java 21
- Spring Boot 3.5.7
- PostgreSQL 15 + pgvector
- RabbitMQ 3
- Maven 3.9+

### AI Service

- Python 3.11
- FastAPI
- OpenAI API
- Uvicorn

### DevOps

- Docker & Docker Compose
- Nginx
- Flyway (database migration)

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is developed for academic purposes at Arizona State University (SER594 course).

## Acknowledgments

- Arizona State University
- SER594 Course Team
