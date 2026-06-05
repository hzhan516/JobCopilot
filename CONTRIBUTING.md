# Contributing to ResumeAssistant

> **Language**: English | [简体中文](./docs/i18n/zh-Hans-CN/CONTRIBUTING.md) | [繁體中文](./docs/i18n/zh-Hant-TW/CONTRIBUTING.md)

Thank you for your interest in contributing to ResumeAssistant! This document provides guidelines and instructions to help you get started.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Branching Strategy](#branching-strategy)
- [Commit Convention](#commit-convention)
- [Pull Request Process](#pull-request-process)
- [Architecture Guidelines](#architecture-guidelines)
- [Testing Requirements](#testing-requirements)
- [Code Style](#code-style)
- [Documentation](#documentation)
- [Release Process](#release-process)
- [Community](#community)

---

## Code of Conduct

This project adheres to a standard of respectful, constructive collaboration. By participating, you agree to:

- Be respectful and inclusive in all interactions
- Accept constructive criticism gracefully
- Focus on what is best for the community and the project
- Show empathy towards other community members

---

## Getting Started

### Prerequisites

| Component | Minimum Version | Recommended |
|-----------|----------------|-------------|
| Java      | 21             | 21 (LTS)    |
| Maven     | 3.9.6          | 3.9.9       |
| Node.js   | 20.11.0        | 20.x LTS    |
| Python    | 3.11           | 3.11+       |
| Docker    | 24.0.0         | 27.x        |
| Docker Compose | 2.20.0    | 2.29+       |

> Verify your environment: `java -version`, `mvn -v`, `node -v`, `python3 --version`, `docker --version`

### Fork and Clone

```bash
# Fork the repository on GitHub, then clone your fork
git clone https://github.com/YOUR_USERNAME/ser594_Team6-ResumeAssistant.git
cd ser594_Team6-ResumeAssistant

# Add upstream remote
git remote add upstream https://github.com/hzhan516/ser594_Team6-ResumeAssistant.git
```

---

## Development Setup

### Option A: Docker Compose (Recommended for first-time contributors)

```bash
cp .env.example .env
# Edit .env with your configuration
docker compose -f docker-compose.yml.example up -d
```

### Option B: Local Development

```bash
# 1. Start infrastructure services
docker compose -f docker-compose.infra.yml up -d

# 2. Backend
cd backend
mvn clean install -DskipTests
mvn spring-boot:run -pl app

# 3. Frontend (new terminal)
cd frontend
npm install
npm run dev

# 4. AI Service (new terminal, optional)
cd ai-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

### Environment Variables

Copy `.env.example` to `.env` and configure:
- Database credentials (PostgreSQL + pgvector)
- MinIO / S3-compatible storage
- RabbitMQ connection
- OpenAI / Embedding API keys
- Google OAuth credentials

See `docs/deployment/` for detailed configuration reference.

---

## Project Structure

```
├── backend/           # Java / Spring Boot (DDD Hexagonal Architecture)
│   ├── app/           # Application layer (Services, Schedulers)
│   ├── domain/        # Domain layer (Entities, Value Objects, Ports)
│   ├── api/           # API layer (DTOs, Commands, Queries)
│   ├── infrastructure/# Adapters (DB, MQ, HTTP, File Storage)
│   ├── trigger/       # REST Controllers, WebSocket, Event Listeners
│   └── types/         # Shared types and constants
├── frontend/          # TypeScript / React / Vite / TailwindCSS
├── ai-service/        # Python / FastAPI (Embedding, LLM inference)
├── docs/              # Documentation (English + i18n)
└── .github/           # CI/CD, Templates, Dependabot
```

### Architecture Principles

- **Domain-Driven Design (DDD)**: Business logic lives in `domain/`; no framework dependencies
- **Hexagonal Architecture (Ports & Adapters)**: Domain defines ports; infrastructure implements adapters
- **Outbox Pattern**: All async messaging goes through database outbox table for reliability
- **CQRS**: Separate command and query responsibilities where beneficial

---

## Branching Strategy

We follow a simplified Git Flow model:

| Branch | Purpose | Protection |
|--------|---------|------------|
| `main` | Production-ready releases | Protected; requires PR + 1 review |
| `develop` | Integration branch for next release | Protected; requires PR |
| `feature/*` | New features | Merge to `develop` via PR |
| `fix/*` | Bug fixes | Merge to `develop` via PR |
| `hotfix/*` | Urgent production fixes | Merge to `main` + `develop` via PR |
| `sanitize-for-oss` | OSS preparation / cleanup | Long-running; periodic rebase |

### Workflow

```bash
# Start new feature
git checkout develop
git pull upstream develop
git checkout -b feature/your-feature-name

# Work, commit, push
git add .
git commit -m "feat(matching): add vector caching for recall"
git push origin feature/your-feature-name

# Open Pull Request against develop (or main for hotfixes)
```

---

## Commit Convention

We use [Conventional Commits](https://www.conventionalcommits.org/) with the following types:

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat(auth): add Google OAuth login` |
| `fix` | Bug fix | `fix(tx): resolve nested transaction in matching` |
| `docs` | Documentation only | `docs(deploy): add Kubernetes deployment guide` |
| `style` | Code style (formatting) | `style(frontend): apply prettier to components` |
| `refactor` | Code change neither fix nor feature | `refactor(domain): extract JobValidator from service` |
| `perf` | Performance improvement | `perf(embedding): batch vector generation` |
| `test` | Adding or correcting tests | `test(matching): add MatchTransactionService tests` |
| `chore` | Maintenance / dependencies | `chore(deps): bump Spring Boot to 3.5.7` |
| `ci` | CI/CD changes | `ci: add OWASP dependency check` |
| `build` | Build system changes | `build: configure maven-enforcer-plugin` |
| `revert` | Revert previous commit | `revert: rollback vector cache (regression)` |

### Scope Guidelines

Use component-level scopes: `auth`, `job`, `resume`, `matching`, `embedding`, `conversation`, `tracking`, `domain`, `infrastructure`, `frontend`, `ai`, `deploy`, `docs`.

### Breaking Changes

Prefix the description with `!` or add a `BREAKING CHANGE:` footer:

```
feat(api)!: remove deprecated v1 endpoints

BREAKING CHANGE: /api/v1/* endpoints removed. Migrate to /api/v2/*.
```

---

## Pull Request Process

### Before Submitting

1. **Update your branch**: `git pull upstream develop` (or `main`)
2. **Run quality checks locally**:
   ```bash
   # Backend
   cd backend && mvn clean verify

   # Frontend
   cd frontend && npm run lint && npm run test:run

   # AI Service
   cd ai-service && ruff check . && pytest
   ```
3. **Write or update tests** for new logic
4. **Update documentation** if user-facing behavior changes
5. **Review your diff** — keep changes focused and minimal

### PR Template

PRs must include:

- **What**: Clear description of changes
- **Why**: Motivation and context (link related issues: `Fixes #123`)
- **How**: Key implementation decisions
- **Testing**: How you verified the change
- **Checklist**: Confirm all items below

### Review Criteria

- [ ] Code follows DDD Hexagonal Architecture
- [ ] No framework dependencies in `domain/` module
- [ ] Tests added/updated with ≥60% coverage for changed lines
- [ ] No `@Transactional` on network I/O (HTTP, MQ, file)
- [ ] ESLint / Prettier passes (frontend)
- [ ] Maven build succeeds with tests
- [ ] Documentation updated if applicable
- [ ] Commit messages follow Conventional Commits

### Review Flow

1. Author opens PR against `develop`
2. CI checks must pass (build, test, lint, security scan)
3. At least one maintainer review required
4. Address review feedback with fixup commits
5. Maintainer squashes and merges

---

## Architecture Guidelines

### DDD Layer Rules

```
┌─────────────────────────────────────┐
│  trigger  (Controllers, WebSocket)   │  ◄── HTTP / WebSocket in
├─────────────────────────────────────┤
│  app      (Application Services)     │  ◄── Orchestration, tx boundaries
├─────────────────────────────────────┤
│  domain   (Entities, Ports, VO)    │  ◄── Pure business logic
├─────────────────────────────────────┤
│  infra    (Adapters, Repositories) │  ◄── DB, MQ, HTTP, Storage
└─────────────────────────────────────┘
```

| Rule | Enforcement |
|------|-------------|
| `domain/` has zero Spring / Hibernate imports (except `javax.validation`) | ArchUnit test: `noClasses().that().resideInAPackage("..domain..")..should().dependOnClassesThat().resideInAPackage("org.springframework..")` |
| Domain interfaces (Ports) are in `domain/**/port/` | Code review |
| Application Services hold `@Transactional`, not Domain Services | Checklist |
| Infrastructure adapters implement domain ports | Compile-time |

### Transaction Safety

- **Commands**: `@Transactional` (read-write)
- **Queries**: `@Transactional(readOnly = true)`
- **No network I/O in transactions**: HTTP calls, MQ sends, file uploads must happen outside `@Transactional`
- **Outbox Pattern**: All async messaging writes to `outbox` table first; scheduler relays

See `docs/transactional-strategy.md` for the complete policy.

---

## Testing Requirements

### Backend (Java)

| Test Type | Tool | Minimum Coverage |
|-----------|------|-----------------|
| Unit tests | JUnit 5 + Mockito | 60% instruction / line, 40% branch |
| Architecture tests | ArchUnit | 100% (must pass) |
| Integration tests | `@SpringBootTest` | Key flows only |

```bash
# Run all backend tests with coverage
cd backend && mvn clean verify

# Run specific module tests
cd backend/app && mvn test
```

### Frontend (TypeScript)

| Test Type | Tool | Requirement |
|-----------|------|-------------|
| Unit tests | Vitest + React Testing Library | All components with logic |
| E2E tests | Playwright (planned) | Critical user journeys |

```bash
cd frontend
npm run test:run        # Run once
npm run test:coverage   # With coverage report
```

### AI Service (Python)

| Test Type | Tool | Requirement |
|-----------|------|-------------|
| Unit tests | pytest | All service functions |
| Lint | ruff | Must pass |

```bash
cd ai-service
pytest --cov=app --cov-report=term-missing
```

---

## Code Style

### Java

- **Formatter**: Use Spring Boot default style (4 spaces, 120 char line)
- **Lombok**: Allowed in `app/` and `infra/`; **not allowed** in `domain/`
- **Imports**: No wildcard imports; static imports for `Assertions`, `Mockito`
- **Null safety**: Use `Optional<>`; avoid `null` returns from domain methods

### TypeScript / React

- **Formatter**: Prettier (config in `frontend/prettier.config.js`)
- **Linter**: ESLint with TypeScript, React Hooks, and React Refresh rules
- **Components**: Function components with hooks; no class components
- **Styling**: TailwindCSS + `class-variance-authority` for component variants

```bash
# Auto-fix frontend issues
cd frontend
npm run lint            # Check
npx prettier --write .  # Format
```

### Python

- **Formatter**: Black-compatible (via `ruff format`)
- **Linter**: ruff with isort, flake8, and pydocstyle rules
- **Type hints**: Required for all function signatures

```bash
cd ai-service
ruff check .            # Lint
ruff format .           # Format
```

---

## Documentation

- **User-facing changes**: Update `docs/` with English + Chinese (Simplified + Traditional) versions
- **Architecture decisions**: Add entry to `docs/adr/` (Architecture Decision Records)
- **API changes**: OpenAPI annotations update automatically; verify with `mvn spring-boot:run` + `/swagger-ui.html`
- **Deployment changes**: Update `docs/deployment/` and `.env.example`

---

## Release Process

1. **Version bump**: Update `version` in `backend/pom.xml` and `frontend/package.json`
2. **Changelog**: Update `CHANGELOG.md` with Conventional Commits summary
3. **Tag**: `git tag -a v1.x.x -m "Release v1.x.x"`
4. **Build**: CI builds Docker images and pushes to registry
5. **Deploy**: Update production environment with new images

---

## Community

- **Issues**: Use GitHub Issues for bug reports and feature requests
- **Discussions**: Use GitHub Discussions for questions and ideas
- **Security**: Report security vulnerabilities privately to maintainers

---

## Questions?

If anything is unclear, open a [GitHub Discussion](https://github.com/hzhan516/ser594_Team6-ResumeAssistant/discussions) or ask in an issue. We're here to help.

**Thank you for contributing! 🚀**
