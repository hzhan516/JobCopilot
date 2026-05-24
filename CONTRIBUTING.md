# Contributing to JobCopilot ResumeAssistant

> **Languages:** English (current) | [简体中文](docs/i18n/zh-CN/CONTRIBUTING.md) | [繁體中文](docs/i18n/zh-TW/CONTRIBUTING.md)

First off, thank you for considering contributing to JobCopilot! This project is built on Hexagonal Architecture (Ports & Adapters) and serves as an AI-powered resume and job matching assistant. We are transitioning from a learning project to an open-source-ready product, and your contributions are vital.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Environment](#development-environment)
- [Branching Strategy](#branching-strategy)
- [Commit Convention](#commit-convention)
- [Code Style](#code-style)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Architecture Compliance](#architecture-compliance)
- [Documentation](#documentation)
- [Release Process](#release-process)
- [Community](#community)

---

## Getting Started

1. **Fork** the repository on GitHub.
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/ser594_Team6-ResumeAssistant.git
   cd ser594_Team6-ResumeAssistant
   ```
3. **Set up upstream** remote:
   ```bash
   git remote add upstream https://github.com/original-owner/ser594_Team6-ResumeAssistant.git
   ```
4. **Create a branch** following our [branch naming convention](#branching-strategy).

---

## Development Environment

### Prerequisites

| Component | Required Version |
|-----------|----------------|
| Java | 21 (LTS) |
| Maven | 3.9+ |
| Node.js | 20+ (for frontend) |
| Python | 3.11+ (for ai-service) |
| Docker & Docker Compose | Latest stable |
| PostgreSQL | 15+ (with pgvector extension) |
| MinIO | Latest (for object storage) |
| RabbitMQ | 3.12+ (for message queue) |

### Quick Start

```bash
# 1. Start infrastructure services
docker compose -f docker-compose.yml.example up -d postgres minio rabbitmq

# 2. Configure environment
cp .env.example .env
# Edit .env with your local settings

# 3. Build the backend
cd backend && mvn clean install -DskipTests

# 4. Start backend services
cd backend/trigger && mvn spring-boot:run

# 5. Start frontend (in another terminal)
cd frontend && npm install && npm run dev

# 6. Start AI service (in another terminal)
cd ai-service && pip install -r requirements.txt && uvicorn app.main:app --reload
```

---

## Branching Strategy

We use **GitHub Flow** — simple, lightweight, and optimized for continuous delivery.

```
main (protected)
  ↑
feat/PROJ-123-user-authentication
  ↑
fix/PROJ-456-login-timeout
  ↑
hotfix/PROJ-789-payment-crash
```

### Branch Naming Convention

```
{type}/{ticket-id}-{short-description}
```

| Type | Purpose |
|------|---------|
| `feat` | New feature |
| `fix` | Bug fix |
| `hotfix` | Production emergency |
| `chore` | Maintenance, dependencies |
| `docs` | Documentation only |
| `refactor` | Code restructuring, no behavior change |
| `test` | Test additions or fixes |
| `perf` | Performance improvements |
| `ci` | CI/CD configuration |
| `style` | Code formatting only |

**Rules:**
- All lowercase, hyphens for spaces
- Maximum 50 characters after the type prefix
- Always include ticket/issue number when available
- Branches are automatically deleted after merge

### Branch Lifetime Targets

| Branch Type | Target Lifetime | Maximum Lifetime |
|-------------|----------------|------------------|
| Feature | 1-3 days | 5 days |
| Bug fix | <1 day | 2 days |
| Hotfix | <4 hours | 1 day |

**Rule:** If a branch exceeds its maximum lifetime, it must be split into smaller PRs.

---

## Commit Convention

We enforce **Conventional Commits**. Every commit message must follow this format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type Reference

| Type | When to Use | Example |
|------|-------------|---------|
| `feat` | New feature | `feat(api): add resume upload endpoint` |
| `fix` | Bug fix | `fix(db): resolve N+1 query in job listing` |
| `perf` | Performance improvement | `perf(vector): optimize cosine similarity calculation` |
| `refactor` | No behavior change | `refactor(job): extract ScreenshotValidator from ApplicationService` |
| `docs` | Documentation | `docs(deploy): add Docker Desktop troubleshooting` |
| `test` | Tests only | `test(auth): add SSO edge cases` |
| `chore` | Build/tooling | `chore(deps): bump Spring Boot to 3.5.8` |
| `ci` | CI/CD changes | `ci: add JaCoCo coverage threshold check` |
| `style` | Formatting only | `style: apply Spotless formatting` |
| `revert` | Revert previous commit | `revert: feat(api): add resume upload endpoint` |

### Scope Reference

| Scope | Area |
|-------|------|
| `api` | API layer (DTOs, Facades) |
| `app` | Application layer (ApplicationServices) |
| `domain` | Domain layer (Entities, ValueObjects, Ports) |
| `infra` | Infrastructure layer (Adapters, Config) |
| `trigger` | Trigger layer (Controllers, EventListeners) |
| `db` | Database (migrations, schema) |
| `deploy` | Deployment configuration |
| `docs` | Documentation |
| `ci` | CI/CD pipeline |
| `frontend` | Frontend application |
| `ai` | AI service (Python) |

### Quality Rules

1. **Atomic commits** — one logical change per commit
2. **Imperative mood** — "add feature" not "added feature"
3. **Subject ≤ 72 characters** — must fit in `git log --oneline`
4. **Body wraps at 72 characters** — readable in terminal
5. **Reference issues** — `Fixes #123` or `Refs PROJ-456`
6. **No WIP commits on main** — squash or interactive rebase first
7. **Breaking changes must be explicit:**
   ```
   feat(api)!: change auth header format
   
   BREAKING CHANGE: Authorization header now requires "Bearer " prefix.
   Migration: Update all API clients to include "Bearer " before token.
   ```

### Commit Message Template

We provide a `.gitmessage` template. Configure it:

```bash
git config commit.template .gitmessage
```

---

## Code Style

### Java (Backend)

- **Formatter:** Spotless Maven plugin (Google Java Format)
- **Run:** `cd backend && mvn spotless:apply`
- **Check:** `cd backend && mvn spotless:check`

### TypeScript (Frontend)

- **Formatter:** Prettier
- **Linter:** ESLint
- **Run:** `cd frontend && npm run lint:fix`

### Python (AI Service)

- **Formatter:** Black
- **Linter:** Ruff
- **Run:** `cd ai-service && ruff check . && black .`

### Architecture Rules (Mandatory)

Our backend follows **Hexagonal Architecture (Ports & Adapters)**. Violations will be rejected in code review.

| Layer | Can Depend On | Must NOT Depend On |
|-------|-------------|-------------------|
| `trigger` | `app`, `api` | `domain`, `infrastructure` |
| `app` | `domain`, `api` | `infrastructure`, `trigger` |
| `domain` | `types` only | `app`, `infrastructure`, `trigger`, `api` |
| `infrastructure` | `domain`, `app`, `api` | — |
| `api` | `types` only | `app`, `domain`, `infrastructure`, `trigger` |
| `types` | Nothing (pure shared types) | Any layer |

**Key Constraints:**
- ApplicationService must not exceed **150 lines** — split into DomainServices or use Cases
- No `RestTemplate` / `HttpClient` / `JdbcTemplate` in `app` or `domain`
- No Spring annotations (`@Component`, `@Service`, `@Autowired`) in `domain`
- No `Map<String, Object>` for external API responses — use strict DTOs
- Domain entities must have **behavior methods**, not just getters/setters

We use **ArchUnit** tests to enforce these rules automatically. Run:
```bash
cd backend && mvn test -pl app -Dtest="*ArchitectureTest*"
```

---

## Testing Requirements

### Minimum Coverage (Enforced by CI)

| Layer | Minimum Coverage |
|-------|-----------------|
| `domain` | 80% |
| `app` | 70% |
| `infrastructure` | 50% |
| `trigger` | 60% |

### Test Types Required

1. **Unit Tests** — JUnit 5 + Mockito + AssertJ
   ```bash
   cd backend && mvn test
   ```

2. **Architecture Tests** — ArchUnit
   ```bash
   cd backend && mvn test -Dtest="*ArchitectureTest*"
   ```

3. **Integration Tests** — Spring Boot Test + Testcontainers
   ```bash
   cd backend && mvn verify -Pintegration-test
   ```

### Writing Good Tests

- Test **behavior**, not implementation
- Use **Given-When-Then** structure in test names:
  ```java
  @Test
  void shouldRejectScreenshotLargerThan5MB() { ... }
  ```
- Mock external dependencies; test domain logic with real objects
- Integration tests must clean up database state (`@Transactional` or `@Sql`)

---

## Pull Request Process

### Before Creating a PR

1. Rebase on latest `main`:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```
2. Run the full check suite:
   ```bash
   cd backend && mvn clean verify
   cd frontend && npm run lint && npm run test
   cd ai-service && ruff check . && pytest
   ```
3. Ensure your branch follows naming convention
4. Squash WIP commits: `git rebase -i upstream/main`

### PR Template

Our PR template is auto-populated when you open a PR. Fill in all sections:

- **What** — One sentence description
- **Why** — Link to issue or explain the problem
- **How** — Technical approach and key decisions
- **Testing** — Checklist of tests performed
- **Screenshots** — For UI changes
- **Checklist** — Self-review checklist

### PR Size Guidelines

| Size | Lines Changed | Expected Review Time |
|------|--------------|---------------------|
| XS | <10 | 5 min |
| S | 10-50 | 15 min |
| M | 50-200 | 30 min |
| L | 200-500 | 60 min |

**Rule:** PRs >500 lines have a 40% higher defect rate. Split aggressively.

### Review Process

| Change Type | Minimum Approvals | Required Reviewers |
|-------------|------------------|-------------------|
| Feature | 2 | 1 domain expert |
| Bug fix | 1 | Any team member |
| Hotfix | 1 | On-call + lead |
| Refactor | 2 | Original author if available |
| Docs only | 1 | Any |
| Dependency update | 1 | Security-aware reviewer |
| Database migration | 2 | DBA/senior + 1 dev |

### Review Comment Taxonomy

Prefix your review comments to clarify intent:

| Prefix | Meaning | Blocks Merge? |
|--------|---------|--------------|
| `blocking:` | Must fix before merge | Yes |
| `suggestion:` | Consider this improvement | No |
| `nit:` | Style/formatting preference | No |
| `question:` | Need clarification | Maybe |
| `praise:` | Great work | No |
| `thought:` | Long-term consideration | No |

---

## Architecture Compliance

### Hexagonal Architecture Checklist

Before submitting a PR, verify:

- [ ] ApplicationServices only orchestrate, contain no business logic
- [ ] Domain entities have behavior methods (not just getters/setters)
- [ ] No framework code (`RestTemplate`, `JPA`, `RabbitTemplate`) in `domain` or `app`
- [ ] All external dependencies go through Port interfaces
- [ ] DTOs live in `api` layer, never leak into `domain`
- [ ] ValueObjects are immutable and validated at construction
- [ ] No `@Transactional` around HTTP calls or external service invocations

### Common Violations to Avoid

❌ **God ApplicationService** (>150 lines) → Extract DomainService or UseCase  
❌ `Map<String, Object>` for external API responses → Define strict DTOs  
❌ `@Transactional` around `RestTemplate` calls → Move HTTP call outside transaction  
❌ Business logic in Controller → Move to ApplicationService or DomainService  
❌ Spring annotations in Domain layer → Use plain Java + constructor injection at app layer  
❌ Repository interfaces returning framework types → Return domain types only  

---

## Documentation

### What Requires Documentation

All user-facing or contributor-facing documentation must be provided in **three languages**:
- **English** (primary, root directory)
- **简体中文** (`docs/i18n/zh-CN/`)
- **繁體中文** (`docs/i18n/zh-TW/`)

| Document | Location | Requires i18n? |
|----------|----------|---------------|
| README.md | `/` | Yes |
| CONTRIBUTING.md | `/` | Yes |
| CODE_OF_CONDUCT.md | `/` | Yes |
| CHANGELOG.md | `/` | Yes |
| Deployment docs | `docs/deployment/` | Yes |
| ADRs | `docs/adr/` | Yes |
| API docs | Auto-generated (OpenAPI) | No |
| LICENSE | `/` | No |

### Architecture Decision Records (ADRs)

For any significant architectural decision, create an ADR in `docs/adr/`:

```
docs/adr/
├── 001-hexagonal-architecture.md
├── 002-postgresql-pgvector.md
├── 003-rabbitmq-message-queue.md
└── 004-minio-object-storage.md
```

ADR template:
```markdown
# ADR-XXX: Title

## Status
Proposed / Accepted / Deprecated / Superseded

## Context
What problem are we solving?

## Decision
What did we decide?

## Consequences
Positive and negative outcomes.
```

---

## Release Process

We follow **Semantic Versioning (SemVer)** and use **release-please** for automated releases.

### Version Bump Rules

| Change Type | Version Bump | Example |
|-------------|-------------|---------|
| Breaking API change | MAJOR | Remove endpoint, change response shape |
| New feature (backward compatible) | MINOR | Add endpoint, new optional field |
| Bug fix | PATCH | Fix calculation error, typo |

### Automated Release Pipeline

1. Merge PRs to `main` with conventional commits
2. `release-please` creates a release PR with changelog + version bump
3. Human reviews and merges the release PR
4. Git tag `vX.Y.Z` is created automatically
5. GitHub Release is published with auto-generated notes
6. Docker image `ghcr.io/jobcopilot/resumeassistant:vX.Y.Z` is built and pushed

### Manual Release (Emergency Only)

```bash
# Only for hotfixes when automation is broken
git tag -a v1.2.1 -m "Hotfix: resolve connection pool starvation"
git push upstream v1.2.1
```

---

## Community

### Communication Channels

- **Issues:** Bug reports, feature requests, questions
- **Discussions:** Architecture proposals, general questions
- **Security:** For security vulnerabilities, email **security@jobcopilot.dev** (DO NOT open public issues)

### Getting Help

1. Search existing issues and discussions
2. Check the documentation in `docs/`
3. Ask in Discussions with the `question` label
4. For bugs, use the Bug Report issue template

### Recognition

Contributors will be recognized in:
- `CHANGELOG.md` for each release
- `CONTRIBUTORS.md` (updated quarterly)
- Release notes on GitHub

---

## Attribution

This contribution guide is adapted from:
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow)
- [Semantic Versioning](https://semver.org/)
- [Contributor Covenant](https://www.contributor-covenant.org/)

Thank you for making JobCopilot better! 🚀
