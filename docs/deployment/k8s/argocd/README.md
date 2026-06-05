# ArgoCD GitOps Deployment

> [English](README.md) | [简体中文](../../../i18n/zh-Hans-CN/deployment/k8s/argocd/README.md) | [繁體中文](../../../i18n/zh-Hant-TW/deployment/k8s/argocd/README.md)

## Overview

This directory provides ArgoCD `Application` manifests for managing the JobCopilot platform through GitOps.

## Architecture

```
┌─────────────────────────────────────────┐
│  ArgoCD Root Application                │
│  (app-of-apps/JobCopilot-root)   │
└─────────────┬───────────────────────────┘
              │ manages
    ┌─────────┼─────────┐
    ▼         ▼         ▼
┌───────┐ ┌───────┐ ┌───────┐
│  dev  │ │staging│ │  prod │
└───────┘ └───────┘ └───────┘
```

## Prerequisites

- ArgoCD 2.10+ installed on your cluster
- Git repository containing this project
- kubectl access to the ArgoCD namespace (default: `argocd`)

## Setup

### 1. Update Repository URLs

Edit all files under `applications/` and `app-of-apps/` to point to your actual Git repository:

```yaml
source:
  repoURL: https://github.com/your-org/JobCopilot.git
  targetRevision: HEAD  # or branch name
```

### 2. Deploy the Root Application

```bash
kubectl apply -f app-of-apps/JobCopilot-root.yaml
```

This creates the root Application which automatically manages the dev, staging, and prod Applications.

### 3. Access ArgoCD UI

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
# Open https://localhost:8080 in browser
```

## Environment Configuration

| Environment | Namespace | Sync Policy | Auto-Sync | Replicas |
|-------------|-----------|-------------|-----------|----------|
| **dev** | `JobCopilot-dev` | Automatic | Yes | 1 (minimal) |
| **staging** | `JobCopilot-staging` | Automatic | Yes | 2 (production-like) |
| **prod** | `JobCopilot-prod` | Manual | No | 3 (full production) |

### Dev

- Uses `values-minimal.yaml`
- Automatic sync and prune
- Single-node resources
- Embedded middleware

### Staging

- Uses `values-production.yaml`
- Automatic sync
- Multi-replica but reduced resources
- Can use managed or embedded middleware

### Prod

- Uses `values-production.yaml`
- **Manual sync** -- requires operator approval in ArgoCD UI
- Full replica counts and resource limits
- Managed middleware recommended
- `ignoreDifferences` configured for HPA-driven replica changes

## Adding a New Environment

1. Copy `applications/JobCopilot-dev.yaml`
2. Rename to `JobCopilot-<env>.yaml`
3. Update:
   - `metadata.name`
   - `destination.namespace`
   - `source.helm.valueFiles`
   - `source.helm.parameters` (host, replicas, etc.)
   - `syncPolicy` as needed
4. Add the new file to Git
5. ArgoCD root app will auto-sync it

## Secret Management with ArgoCD

Never store plain secrets in Git. Options:

1. **Pre-create Secrets** in target namespaces
   ```bash
   kubectl create secret generic JobCopilot-secrets \
     --namespace=JobCopilot-prod \
     --from-literal=JWT_SECRET=...
   ```
   Then set `secrets.existingSecret` in values.

2. **External Secrets Operator** + ArgoCD
   - Install ESO in cluster
   - Create `ExternalSecret` resources (can be stored in Git)
   - ESO syncs from cloud secret stores

3. **Sealed Secrets** (Bitnami)
   - Encrypt secrets with `kubeseal`
   - Store encrypted `SealedSecret` in Git
   - Controller decrypts to regular Secret

## Syncing Strategies

### Automatic Sync

```yaml
syncPolicy:
  automated:
    prune: true      # Remove resources not in Git
    selfHeal: true   # Revert manual changes
```

### Manual Sync

```yaml
syncPolicy:
  automated:
    prune: false
    selfHeal: false
```

Use ArgoCD UI or CLI to sync:
```bash
argocd app sync JobCopilot-prod
```

## Monitoring

View sync status:
```bash
argocd app list
argocd app get JobCopilot-prod
```

Check for drift:
```bash
argocd app diff JobCopilot-prod
```
