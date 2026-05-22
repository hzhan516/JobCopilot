# JobCopilot Helm Chart

> [English](README.md) | [简体中文](../../../../i18n/zh-Hans-CN/deployment/k8s/helm/README.md) | [繁體中文](../../../../i18n/zh-Hant-TW/deployment/k8s/helm/README.md)

## Overview

This Helm chart deploys the complete JobCopilot platform on Kubernetes.

## Installation

### Prerequisites

```bash
# Add bitnami repo if you need dependent charts (optional)
# helm repo add bitnami https://charts.bitnami.com/bitnami
# helm repo update
```

### Quick Install

```bash
# Create namespace first
kubectl create namespace JobCopilot

# Install with defaults (embedded middleware, dev profile)
helm install JobCopilot . \
  --namespace JobCopilot

# Install for production (managed middleware)
helm install JobCopilot . \
  --namespace JobCopilot \
  -f values.yaml \
  -f values-production.yaml

# Install for minimal resources (kind/minikube)
helm install JobCopilot . \
  --namespace JobCopilot \
  -f values.yaml \
  -f values-minimal.yaml
```

### Upgrade

```bash
helm upgrade JobCopilot . \
  --namespace JobCopilot \
  -f values.yaml \
  -f values-production.yaml
```

### Uninstall

```bash
helm uninstall JobCopilot --namespace JobCopilot
# Note: PVCs are NOT deleted by default to prevent data loss
kubectl delete pvc -n JobCopilot -l app.kubernetes.io/name=JobCopilot
```

---

## Configuration

### Key Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `global.infraMode` | Middleware mode: `embedded` or `managed` | `embedded` |
| `secrets.existingSecret` | Use pre-created Secret instead of Helm-managed | `""` |
| `ingress.host` | Ingress domain | `JobCopilot.local` |
| `ingress.tls.enabled` | Enable TLS | `false` |
| `backend.replicaCount` | Backend replicas | `1` |
| `backend.springProfilesActive` | Spring profile | `dev` |
| `backend.storageType` | File storage: `local`, `s3`, `minio`, `oss` | `local` |
| `aiService.replicaCount` | AI service replicas | `1` |
| `aiService.llmEmbeddingDimension` | Embedding vector dimension | `1536` |
| `postgres.enabled` | Deploy PostgreSQL StatefulSet | `true` |
| `rabbitmq.enabled` | Deploy RabbitMQ StatefulSet | `true` |
| `redis.enabled` | Deploy Redis StatefulSet | `true` |

### Full Values Reference

See [`values.yaml`](./values.yaml) for all configurable options.

---

## Secret Management

### Option 1: Helm-managed Secret (dev only)

Set secrets directly in values:
```yaml
secrets:
  jwtSecret: "your-secret"
  postgresPassword: "your-password"
```

### Option 2: Pre-created Secret (production recommended)

```bash
# Create Secret manually
kubectl create secret generic JobCopilot-secrets \
  --namespace=JobCopilot \
  --from-literal=JWT_SECRET=... \
  --from-literal=POSTGRES_PASSWORD=...

# Reference in values
helm install JobCopilot . \
  --set secrets.existingSecret=JobCopilot-secrets
```

### Option 3: External Secrets Operator

```yaml
secrets:
  existingSecret: "JobCopilot-eso-secret"
```

Configure ESO to sync from AWS Secrets Manager, Azure Key Vault, or GCP Secret Manager.

---

## PostgreSQL Initialization

When `global.infraMode=embedded`, the chart mounts `init.sql` and `init-db.sh` into the PostgreSQL pod. The init script:

1. Creates all tables (users, resumes, jobs, conversations, etc.)
2. Enables `pgvector` extension
3. Substitutes `vector(1536)` with the configured dimension
4. Creates HNSW index if dimension <= 2000

To provide `init.sql`:
```bash
helm install JobCopilot . \
  --set-file postgres.initSql=../../../backend/app/src/main/resources/db/init.sql
```

For production with `managed` mode, Flyway handles migrations automatically.

---

## GCP Vertex AI Credentials

To mount a GCP service account key:

```bash
# Base64-encode the key file
export GCP_CREDS_B64=$(base64 -w 0 /path/to/gcp-service-account.json)

helm install JobCopilot . \
  --set secrets.gcpCredentialsJson="$GCP_CREDS_B64"
```

The chart will create a separate Secret and mount it as a read-only volume at `/app/credentials/gcp-service-account.json`.

---

## Validating the Chart

```bash
# Lint
helm lint .

# Render templates without installing
helm template JobCopilot . \
  -f values.yaml \
  -f values-production.yaml > rendered.yaml

# Use the validation script
../../scripts/validate-manifests.sh
```

---

## Customizing Images

```yaml
global:
  imageRegistry: "ghcr.io/your-org/"

backend:
  image:
    repository: JobCopilot-backend
    tag: "v1.2.3"

aiService:
  image:
    repository: JobCopilot-ai-service
    tag: "v1.2.3"

frontend:
  image:
    repository: JobCopilot-frontend
    tag: "v1.2.3"
```

---

## High Availability

For production HA:

1. Use **managed middleware** (`global.infraMode=managed`) -- RDS/Cloud SQL, Amazon MQ/MemoryStore
2. Enable **HPA** (`backend.autoscaling.enabled=true`, `aiService.autoscaling.enabled=true`)
3. Enable **PDB** (`pdb.enabled=true`)
4. Run **multiple replicas** for all stateless services
5. Use **anti-affinity** rules to spread pods across nodes (add to values if needed)

The chart does NOT build HA clusters for PostgreSQL/RabbitMQ/Redis inside K8s.
For embedded HA, consider:
- PostgreSQL: [CloudNativePG](https://cloudnative-pg.io/)
- RabbitMQ: [RabbitMQ Cluster Operator](https://www.rabbitmq.com/kubernetes/operator/operator-overview)
- Redis: [Redis Operator](https://github.com/OT-CONTAINER-KIT/redis-operator)
