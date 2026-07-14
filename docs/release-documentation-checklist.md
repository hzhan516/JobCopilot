# Release Documentation Synchronization Checklist

English documentation is authoritative. For every release-relevant change, verify the English source and both Chinese mirrors before marking documentation complete.

| Area | English source | Simplified Chinese | Traditional Chinese | July 2026 status |
| --- | --- | --- | --- | --- |
| Repository entry point | `README.md` | `docs/i18n/zh-Hans-CN/README.md` | `docs/i18n/zh-Hant-TW/README.md` | Reviewed |
| API and MQ contracts | `docs/api/` | `docs/i18n/zh-Hans-CN/api/` | `docs/i18n/zh-Hant-TW/api/` | AI-MQ compact contract synchronized |
| Deployment and environment | `docs/deployment/`, `.env.example` | `docs/i18n/zh-Hans-CN/deployment/` | `docs/i18n/zh-Hant-TW/deployment/` | Release-relevant pages reviewed |
| Architecture decisions | `docs/architecture/` | `docs/i18n/zh-Hans-CN/architecture/` | `docs/i18n/zh-Hant-TW/architecture/` | Reviewed for current topology |
| AI contributor guide | `.skill/jobcopilot-codebase/` | Not separately mirrored | Not separately mirrored | Authority notice and compact queues synchronized |
| Version and release | `VERSION`, `CHANGELOG.md`, release workflow | Same executable sources | Same executable sources | Automated drift check added |

Release evidence must include:

- `python scripts/check-markdown-links.py`
- `bash scripts/check-version-sync.sh`
- the exact commit under test;
- any intentionally deferred or not-executed documentation areas.

This checklist records synchronization obligations; it does not turn unverified statements into implementation guarantees.
