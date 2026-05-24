# Branching Strategy & Commit Standards

This document defines the Git engineering standards for the JobCopilot ResumeAssistant project. It covers branching strategy, commit conventions, code review workflows, and release management.

> **Status:** Accepted  
> **Scope:** All repositories under the JobCopilot organization  
> **Language:** English (primary) | [简体中文](docs/i18n/zh-CN/BRANCHING_AND_COMMITS.md) | [繁體中文](docs/i18n/zh-TW/BRANCHING_AND_COMMITS.md)

---

## 1. Branching Strategy: GitHub Flow

We use **GitHub Flow** — a lightweight, trunk-based branching model optimized for continuous delivery.

```
main (protected, always deployable)
  ↑
feat/RES-123-resume-upload
  ↑
fix/RES-456-login-timeout
  ↑
hotfix/RES-789-connection-pool
```

### Why GitHub Flow?

| Factor | Our Situation | Fit |
|--------|--------------|-----|
| Team size | 3 core members | ✅ Perfect |
| Release cadence | Weekly to bi-weekly | ✅ Perfect |
| CI/CD maturity | Building up (CI exists, CD in progress) | ✅ Good |
| Feature flags | Not yet implemented | ⚠️ Acceptable |
| Complexity | Low — simple trunk + feature branches | ✅ Perfect |

### Alternative Strategies We Rejected

| Strategy | Why Not Used |
|----------|-------------|
| **GitFlow** | Too heavy for our cadence; `develop` branch adds overhead without value |
| **Trunk-Based** | Requires feature flags and >80% coverage — not ready yet |
| **Release Flow** | Overkill for a team < 10 |

### Branch Naming Convention

```
{type}/{ticket-id}-{short-description}
```

**Types:**

| Type | Purpose | Example |
|------|---------|---------|
| `feat` | New feature | `feat/RES-123-resume-upload` |
| `fix` | Bug fix | `fix/RES-456-login-timeout` |
| `hotfix` | Production emergency | `hotfix/RES-789-connection-pool` |
| `chore` | Maintenance, deps | `chore/RES-200-bump-spring-boot` |
| `docs` | Documentation only | `docs/RES-300-deployment-guide` |
| `refactor` | Code restructure, no behavior change | `refactor/RES-400-extract-validator` |
| `test` | Test additions | `test/RES-500-auth-edge-cases` |
| `perf` | Performance improvements | `perf/RES-600-vector-caching` |
| `ci` | CI/CD configuration | `ci/RES-700-add-jacoco` |
| `style` | Code formatting only | `style/RES-800-spotless-format` |

**Rules:**
- All lowercase, hyphens for spaces
- Maximum 50 characters after `type/`
- Always include ticket/issue number when available
- Branches deleted automatically after merge (via GitHub setting)

### Branch Lifetime Targets

| Branch Type | Target Lifetime | Max Lifetime | Action if Exceeded |
|-------------|----------------|--------------|-------------------|
| Feature | 1-3 days | 5 days | Must split into smaller PRs |
| Bug fix | <1 day | 2 days | Prioritize review |
| Hotfix | <4 hours | 1 day | Emergency review process |

**>500 line PRs have a 40% higher defect rate. Split aggressively.**

---

## 2. Commit Convention: Conventional Commits

Every commit must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Format

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
| `perf` | Performance improvement | `perf(vector): optimize cosine similarity` |
| `refactor` | No behavior change | `refactor(job): extract ScreenshotValidator` |
| `docs` | Documentation | `docs(deploy): add Docker Desktop troubleshooting` |
| `test` | Tests only | `test(auth): add SSO edge cases` |
| `chore` | Build/tooling | `chore(deps): bump Spring Boot to 3.5.8` |
| `ci` | CI/CD changes | `ci: add JaCoCo coverage threshold` |
| `style` | Formatting only | `style: apply Spotless formatting` |
| `revert` | Revert previous commit | `revert: feat(api): add resume upload` |

### Scope Reference

| Scope | Area | Typical Files |
|-------|------|--------------|
| `api` | API layer | `backend/api/src/...` |
| `app` | Application layer | `backend/app/src/...` |
| `domain` | Domain layer | `backend/domain/src/...` |
| `infra` | Infrastructure layer | `backend/infrastructure/src/...` |
| `trigger` | Trigger layer | `backend/trigger/src/...` |
| `db` | Database | `backend/**/db/`, migrations |
| `deploy` | Deployment | `docker-compose*.yml`, `.env.example` |
| `docs` | Documentation | `docs/`, `README.md` |
| `ci` | CI/CD pipeline | `.github/workflows/` |
| `frontend` | Frontend app | `frontend/` |
| `ai` | AI service | `ai-service/` |

### Quality Rules

1. **Atomic commits** — one logical change per commit
2. **Imperative mood** — "add feature" not "added feature"
3. **Subject ≤ 72 chars** — must fit in `git log --oneline`
4. **Body wraps at 72 chars** — readable in terminal
5. **Reference issues** — `Fixes #123` or `Refs RES-456`
6. **No WIP commits on main** — squash or interactive rebase first
7. **Breaking changes must be explicit:**
   ```
   feat(api)!: change auth header format
   
   BREAKING CHANGE: Authorization header now requires "Bearer " prefix.
   ```

### Commit Message Template

Configure the provided template:

```bash
git config commit.template .gitmessage
```

Contents of `.gitmessage`:
```
# <type>(<scope>): <subject>
#
# Why this change?
#
# What changed?
#
# Refs: RES-XXX
#
# Types: feat|fix|perf|refactor|docs|test|chore|ci|style|revert
# Breaking: add ! after type or BREAKING CHANGE: in footer
```

---

## 3. Code Review Workflow

### PR Template

All PRs use the template in `.github/PULL_REQUEST_TEMPLATE.md`. Required sections:

- **What** — One sentence
- **Why** — Link to issue or problem statement
- **How** — Technical approach, key decisions
- **Testing** — Checklist with mandatory checks
- **Checklist** — Self-review verification

### PR Size Guidelines

| Size | Lines Changed | Review Time | Defect Rate |
|------|--------------|-------------|-------------|
| XS | <10 | 5 min | ~0% |
| S | 10-50 | 15 min | ~5% |
| M | 50-200 | 30 min | ~15% |
| L | 200-500 | 60 min | ~25% |
| XL | >500 | 120+ min | ~40% |

**Rule: If your PR is >400 lines, it must be split.**

### Review SLAs

| Priority | First Review | Approval | Escalation |
|----------|-------------|----------|------------|
| Hotfix | 30 min | 1 hour | Page on-call |
| Critical | 2 hours | 4 hours | DM team lead |
| Normal | 4 hours | 24 hours | Daily standup |
| Low | 24 hours | 48 hours | Weekly review |

### Review Comment Taxonomy

Prefix all review comments:

| Prefix | Meaning | Blocks Merge? |
|--------|---------|--------------|
| `blocking:` | Must fix before merge | Yes |
| `suggestion:` | Consider this improvement | No |
| `nit:` | Style/formatting preference | No |
| `question:` | Need clarification | Maybe |
| `praise:` | Great work | No |
| `thought:` | Long-term consideration | No |

### Approval Rules

| Change Type | Min Approvals | Required Reviewers | Auto-merge? |
|-------------|--------------|-------------------|------------|
| Feature | 2 | 1 domain expert | No |
| Bug fix | 1 | Any team member | Optional |
| Hotfix | 1 | On-call + lead | After deploy |
| Refactor | 2 | Original author if available | No |
| Docs only | 1 | Any | Yes |
| Dependency update | 1 | Security-aware reviewer | Dependabot: yes |
| Config change | 2 | Ops + dev | No |
| Database migration | 2 | DBA/senior + 1 dev | No |

---

## 4. Branch Protection Rules

### `main` Branch

```yaml
required_reviews: 2
dismiss_stale_reviews: true
require_code_owner_reviews: true
require_signed_commits: false  # Enable when team is ready
require_linear_history: true    # No merge commits
require_status_checks:
  - "Backend Build & Test"
  - "Frontend Build & Test"
  - "AI Service Build & Test"
  - "Security Scan"
  - "Docker Build Test"
restrict_push: [release-bot]
allow_force_push: false
allow_deletions: false
require_conversation_resolution: true
```

### `develop` Branch (if used)

```yaml
required_reviews: 1
require_status_checks:
  - "Backend Build & Test"
  - "Frontend Build & Test"
```

---

## 5. CI/CD Integration

### Pre-merge CI Pipeline

Our CI runs on every PR and push to `main`/`develop`:

| Stage | Checks | Target Duration |
|-------|--------|----------------|
| Lint & Format | Spotless, ESLint, Ruff, Prettier | <30s |
| Type Check | TypeScript strict, mypy | <60s |
| Unit Tests | JUnit 5, pytest | <3 min |
| Integration Tests | Spring Boot + Testcontainers | <5 min |
| Security Scan | Trivy, OWASP dependency-check | <2 min |
| Build | Maven package, Vite build, Docker build | <3 min |

**Total target: <10 minutes**

### CI Rules

- All checks must pass before merge
- Flaky tests quarantined within 24h
- New code must not decrease coverage
- Security findings (HIGH/CRITICAL) block merge

---

## 6. Release Management

### Semantic Versioning (SemVer)

```
MAJOR.MINOR.PATCH[-prerelease]

Examples:
  1.0.0        → First stable release
  1.1.0        → New feature, backward compatible
  1.1.1        → Bug fix
  2.0.0        → Breaking change
```

### Version Bump Decision Matrix

| Change Type | Version Bump | Example |
|-------------|-------------|---------|
| Breaking API change | MAJOR | Remove endpoint, change response shape |
| New feature (backward compatible) | MINOR | Add endpoint, new optional field |
| Bug fix | PATCH | Fix calculation error, typo |
| Performance improvement | PATCH | Optimize query (same behavior) |
| Dependency update (compatible) | PATCH | Bump lodash minor |
| Dependency update (breaking) | MAJOR or MINOR | Evaluate downstream impact |

### Container-First Release

We publish **Docker images only** — no Maven Central, npm registry, or PyPI publishing.

**Artifact registry:** `ghcr.io/jobcopilot/resumeassistant`

| Component | Image Tag | Example |
|-----------|-----------|---------|
| Backend | `ghcr.io/jobcopilot/resumeassistant/backend:vX.Y.Z` | `backend:v1.2.0` |
| Frontend | `ghcr.io/jobcopilot/resumeassistant/frontend:vX.Y.Z` | `frontend:v1.2.0` |
| AI Service | `ghcr.io/jobcopilot/resumeassistant/ai-service:vX.Y.Z` | `ai-service:v1.2.0` |

**Tag strategy:**
- `vX.Y.Z` — exact release (immutable)
- `vX.Y` — minor line (latest patch)
- `vX` — major line (latest minor)
- `latest` — rolling, use only for dev/test

### Automated Release (release-please)

We use [release-please](https://github.com/googleapis/release-please) for zero-touch releases:

1. Merge PRs to `main` with conventional commits
2. `release-please` creates a release PR with:
   - Version bump in `package.json` / `pom.xml`
   - `CHANGELOG.md` update
3. Human reviews and merges the release PR
4. Git tag `vX.Y.Z` created automatically
5. GitHub Release published with auto-generated notes
6. Docker images built and pushed to `ghcr.io/jobcopilot/resumeassistant/*:vX.Y.Z`
7. **No package registry publishing** — containers are the only distribution artifact

### Hotfix Process

```
1. Create branch from latest release tag:
   git checkout -b hotfix/RES-XXX-description v1.2.0

2. Implement fix with test

3. PR with 'hotfix' label → expedited review (1 reviewer)

4. Merge to main AND cherry-pick to release branch (if exists)

5. Tag patch release immediately:
   git tag -a v1.2.1 -m "Hotfix: resolve ..."
   
   # Container images rebuilt automatically from tag via GitHub Actions

6. Deploy to production

7. Post-incident: add regression test to CI
```

**SLA: Fix deployed within 4 hours of identification**

---

## 7. Git Security

### Secrets Prevention

**Pre-commit hooks** (managed via lefthook or husky):
- gitleaks scan on staged files
- Detect-secrets baseline check

**CI scan:**
- Trivy filesystem scan on every PR
- truffleHog in CI pipeline

### Emergency Response (Secret Exposed)

1. **Revoke the credential IMMEDIATELY** — don't wait for history cleanup
2. Remove with `git filter-repo` or BFG Repo-Cleaner
3. Force push cleaned history
4. Contact GitHub support to clear caches
5. Audit credential usage logs
6. Rotate all potentially exposed credentials
7. Add pattern to pre-commit hooks

> **⚠️ Warning:** Even after removing from history, assume the secret is compromised. Anyone who cloned the repo may have it cached.

### Commit Signing (Recommended)

```bash
# SSH signing (simpler than GPG)
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true
```

---

## 8. Repository Hygiene

### .gitignore Checklist

**Always ignore:**
- `node_modules/`, `venv/`, `__pycache__/`
- `.env`, `.env.local`, `.env.*.local`
- `*.key`, `*.pem`, `*.p12`
- `.DS_Store`, `Thumbs.db`
- `*.log`, `logs/`
- `dist/`, `build/`, `out/`
- `coverage/`, `.nyc_output/`
- `.idea/`, `.vscode/` (except shared settings)

**Never ignore:**
- `.gitignore` itself
- Lockfiles (`package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`)
- `.env.example` (template without secrets)
- `docker-compose.yml`
- `Makefile`, `Taskfile`

### Stale Branch Cleanup

- Branches auto-delete after merge (GitHub setting)
- Monthly review of stale branches (>30 days old)
- `git branch -r --merged main | grep -v main | xargs -n1 git push --delete origin`

---

## 9. Metrics & Health Dashboard

### Weekly Tracking

| Metric | Good | Great | Target |
|--------|------|-------|--------|
| PR review time | <24h | <4h | <4h |
| PR merge time | <48h | <24h | <24h |
| CI pipeline | <15 min | <10 min | <10 min |
| CI pass rate | >90% | >95% | >95% |
| Branch lifetime | <5 days | <3 days | <3 days |
| Stale branches | <20 | <10 | 0 |
| Code review coverage | >80% | >95% | 100% |

### Repository Health Score

Track weekly at: `.github/health-dashboard.md`

---

## 10. Common Git Commands Cheat Sheet

```bash
# Start feature
git checkout -b feat/RES-123-description main

# Keep branch rebased
git fetch upstream
git rebase upstream/main

# Clean up before PR
git rebase -i upstream/main
# pick → keep
# squash → combine with previous
# fixup → combine, discard message
# reword → change message
# drop → remove

# Fix last commit message
git commit --amend

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Find lost commit
git reflog

# Recover deleted branch
git reflog | grep "checkout: moving"
git checkout -b recovered-branch <sha>

# Remove file from all history (BFG)
bfg --delete-files filename
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

---

## Attribution

This document is adapted from:
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow)
- [Semantic Versioning](https://semver.org/)
- [AfrexAI Git Engineering](https://github.com/afrexai/git-engineering)

---

*Last updated: 2026-05-25*