# Dependency Management Policy

> [English](dependency-management.md) | [简体中文](../i18n/zh-Hans-CN/dependency-management.md) | [繁體中文](../i18n/zh-Hant-TW/dependency-management.md)

This document defines how JobCopilot manages automated dependency updates from Dependabot and how the team should handle CVE fixes, major version bumps, and routine upgrades.

---

## Policy at a glance

| Update type | Source | Review required | Auto-merge | Notes |
|---|---|---|---|---|
| **Patch** (`X.Y.Z → X.Y.Z+1`) | Dependabot | No | Yes | Security and bug fixes with low risk. |
| **Minor** (`X.Y → X.Y+1`) | Dependabot | 1 person | No | Must check release notes for behavior changes. |
| **Major** (`X → X+1`) | Dependabot | N/A | N/A | **Automatically closed.** Open a regular PR with a compatibility assessment if truly needed. |
| **CVE emergency fix** | Manual PR | 2 persons | No | May require major bump; must include impact analysis and regression plan. |

---

## Why we block major version upgrades from Dependabot

Major version upgrades can introduce:

- Breaking API changes that break compile-time or runtime behavior.
- Transitive dependency conflicts (especially in the Maven and npm ecosystems).
- Unplanned migration work (e.g., Spring Boot major upgrades, React major upgrades).
- Extra regression testing effort across backend, frontend, and AI service.

To keep `main` stable and predictable, **Dependabot is configured to ignore `semver-major` updates** and an automated workflow closes any major Dependabot PR that slips through.

If a major upgrade is genuinely required (e.g., to fix a CVE with no patched minor version), open a normal pull request that includes:

1. A clear rationale (CVE ID, performance need, or EOL timeline).
2. A compatibility checklist based on the upstream changelog.
3. Evidence that existing tests pass.
4. Approval from **two** code owners.

---

## Dependabot configuration

File: `.github/dependabot.yml`

- **Ecosystems**: Maven (backend), npm (frontend), pip (ai-service), GitHub Actions.
- **Schedule**: spread across the week to avoid a Monday PR flood.
  - Maven: Monday
  - npm: Tuesday
  - pip: Wednesday
  - GitHub Actions: Thursday
- **Open PR limit**: 10 total across all ecosystems.
- **Ignored updates**: `version-update:semver-major`.
- **Rebase strategy**: `auto` to reduce manual rebase work.

---

## Automated workflows

### `block-major-upgrades.yml`

Runs on every Dependabot pull request. Detects major version bumps in the PR title/body, posts an explanatory comment, and closes the PR.

### `dependabot-auto-merge.yml`

Runs on Dependabot PRs after the `CI` workflow succeeds. Enables GitHub auto-merge (squash) **only** for patch-level updates (`semver-patch`). Minor updates still require a human review.

### `dependency-check-nightly.yml`

Runs OWASP dependency-check every Sunday at 02:00 UTC. It is intentionally **not** part of the per-PR CI to avoid blocking merges with transient CVE false-positives. Reports are uploaded as artifacts.

---

## CI behavior for dependency PRs

- Regular CI runs on every Dependabot PR to ensure tests, lint, and type checks still pass.
- **Docker build test** is skipped for Dependabot PRs because dependency manifest changes do not affect image build logic, and the regular CI already validates service builds.
- **Qodana** is skipped for Dependabot PRs because lockfile changes do not warrant static analysis.
- **OWASP dependency-check** is not run per-PR; it runs nightly instead.

---

## Handling CVEs and security exceptions

1. Check the [nightly OWASP report](https://github.com/hzhan516/JobCopilot/actions/workflows/dependency-check-nightly.yml) or GitHub Security Advisories.
2. If a CVE has a patched **patch/minor** version, wait for Dependabot to open the PR or manually bump it in a small PR.
3. If the only fix is a **major** version bump:
   - Open a manual PR with a compatibility assessment.
   - Add the CVE ID and severity to the PR description.
   - Add a suppression entry to `backend/owasp-suppressions.xml` **only** if the CVE is a confirmed false-positive.
   - Obtain two approvals before merging.

### Adding an OWASP suppression

File: `backend/owasp-suppressions.xml`

Only add suppressions when you have evidence that the CVE does not affect the project's usage of the dependency. Include:

- The CVE ID.
- A short justification.
- A link to the upstream issue or release note.

```xml
<suppress>
  <notes>
    CVE-20XX-XXXXX is a false positive for our usage because the vulnerable
    endpoint is not exposed. See: https://...
  </notes>
  <cve>CVE-20XX-XXXXX</cve>
</suppress>
```

---

## Responsibilities

| Role | Responsibility |
|---|---|
| **Dependabot** | Open patch/minor PRs; ignore major. |
| `block-major-upgrades.yml` | Close major Dependabot PRs automatically. |
| `dependabot-auto-merge.yml` | Merge patch PRs after CI passes. |
| **Code owners** | Review minor PRs within 3 business days. |
| **Security lead** | Review nightly OWASP reports and drive CVE exceptions. |

---

## Success metrics

- Open Dependabot PRs stay below **5** at any time.
- Patch PRs merge within **24 hours** without human intervention.
- Minor PRs merge within **3 business days**.
- Zero major Dependabot PRs remain open longer than 1 hour.
- No unplanned major version upgrades enter `main`.
