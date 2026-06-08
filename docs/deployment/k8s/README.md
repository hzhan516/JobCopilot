# Kubernetes Deployment Guide

> [English](README.md) | [简体中文](../../i18n/zh-Hans-CN/deployment/k8s/README.md) | [繁體中文](../../i18n/zh-Hant-TW/deployment/k8s/README.md)

## Overview

This directory provides **production-grade** Kubernetes deployment configurations for the JobCopilot platform.

### Three deployment methods

| Method | Best For | File Location |
|--------|----------|---------------|
| **Helm Chart** | Production, multi-environment, versioned releases | [`helm/jobcopilot/`](./helm/jobcopilot/) |
| **ArgoCD GitOps** | Teams with GitOps workflows, audit trails | [`argocd/`](./argocd/) |
| **Plain YAML + Kustomize** | Quick tests, learning, CI without Helm | [`plain-yaml/`](./plain-yaml/) |

### Two infrastructure modes

| Mode | Description | When to Use |
|------|-------------|-------------|
| **Embedded** | PostgreSQL + RabbitMQ + Redis run as StatefulSets inside K8s | Development, testing, small clusters |
| **Managed** | Use cloud-managed database / MQ / cache (RDS, CloudAMQP, ElastiCache) | **Production recommended** |

Switch between modes via Helm values: `global.infraMode` (`embedded` or `managed`).

---

## Prerequisites

- **Kubernetes** 1.28+ cluster (managed or self-hosted)
- **kubectl** configured with cluster access
- **Helm** 3.14+ (for Helm deployments)
- **Ingress Controller** (nginx-ingress, Traefik, AWS ALB, etc.)
- **cert-manager** (optional, for automatic TLS)
- **StorageClass** (for embedded mode database PVCs)

---

## Quick Start

### 1. Prepare Secrets

```bash
# From .env file
./scripts/generate-secrets.sh .env jobcopilot | kubectl apply -f -

# Or create manually
kubectl create secret generic jobcopilot-secrets \
  --namespace=jobcopilot \
  --from-literal=JWT_SECRET=$(openssl rand -base64 48) \
  --from-literal=POSTGRES_PASSWORD=$(openssl rand -base64 24) \
  --from-literal=RABBITMQ_PASSWORD=$(openssl rand -base64 24) \
  --from-literal=REDIS_PASSWORD=$(openssl rand -base64 24) \
  --from-literal=MINIO_ACCESS_KEY=jobcopilot-minio \
  --from-literal=MINIO_SECRET_KEY=$(openssl rand -base64 32) \
  --from-literal=INTERNAL_API_KEY=$(openssl rand -base64 32) \
  --from-literal=GEMINI_API_KEY=your-gemini-key
```

### 2. Deploy with Helm (Recommended)

```bash
# Embedded mode (dev)
helm install jobcopilot ./helm/jobcopilot \
  --namespace jobcopilot \
  --create-namespace \
  -f ./helm/jobcopilot/values.yaml \
  -f ./helm/jobcopilot/values-minimal.yaml

# Managed mode (production)
helm install jobcopilot ./helm/jobcopilot \
  --namespace jobcopilot \
  --create-namespace \
  -f ./helm/jobcopilot/values.yaml \
  -f ./helm/jobcopilot/values-production.yaml
```

### 3. Verify

```bash
kubectl get pods -n jobcopilot
kubectl get ingress -n jobcopilot

# Check backend health
kubectl port-forward svc/jobcopilot-backend 8080:8080 -n jobcopilot
curl http://localhost:8080/api/actuator/health

# Check AI service health
kubectl port-forward svc/jobcopilot-ai-service 8000:8000 -n jobcopilot
curl http://localhost:8000/health
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Ingress Controller                              │
│                         (TLS termination, path routing)                      │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
        /api/*                             /* (SPA)
              │                               │
    ┌─────────▼─────────┐          ┌─────────▼─────────┐
    │     Backend       │          │     Frontend      │
    │  (Spring Boot)    │          │  (React + Nginx)  │
    │   replicas: 3     │          │   replicas: 3     │
    └─────────┬─────────┘          └───────────────────┘
              │
    ┌─────────┼─────────┐
    │         │         │
    ▼         ▼         ▼
┌───────┐ ┌───────┐ ┌─────────────┐
│PostgreSQL│ │RabbitMQ│ │    Redis     │
│(Stateful)│ │(Stateful)│ │  (Stateful)  │
└───────┘ └───────┘ └─────────────┘
    │         │         │
    │    ┌────┘         │
    │    │              │
    ▼    ▼              ▼
┌─────────────────────────────────────┐
│           AI Service                 │
│        (Python FastAPI)              │
│         replicas: 3                  │
└─────────────────────────────────────┘
```

### Network Isolation

Three-tier NetworkPolicy isolation:
- **Tier 1**: Ingress -> frontend only
- **Tier 2**: backend <-> ai-service <-> ai-worker <-> rabbitmq <-> redis <-> minio
- **Tier 3**: backend -> postgres only

---

## Storage Recommendations

### File Storage (backend <-> ai-service shared files)

Docker Compose uses a shared volume. In K8s, **do not use ReadWriteMany PVC** for production.

| Option | Setup | Recommendation |
|--------|-------|----------------|
| **S3 / Compatible** | `STORAGE_TYPE=s3` | **Production recommended** |
| **MinIO** | `STORAGE_TYPE=minio` | On-premise / private cloud |
| **Aliyun OSS** | `STORAGE_TYPE=oss` | Alibaba Cloud environments |
| **ReadWriteMany PVC** | `STORAGE_TYPE=local` | Last resort; requires NFS/CephFS CSI |

---

## Directory Structure

```
k8s/
├── README.md                          # This file
├── helm/
│   └── jobcopilot/
│       ├── Chart.yaml
│       ├── values.yaml                # Default (embedded, dev)
│       ├── values-production.yaml     # Production (managed middleware)
│       ├── values-minimal.yaml        # Minimal resources (kind/minikube)
│       └── templates/                 # All K8s resources
├── argocd/
│   ├── app-of-apps/
│   │   └── jobcopilot-root.yaml
│   └── applications/
│       ├── jobcopilot-dev.yaml
│       ├── jobcopilot-staging.yaml
│       └── jobcopilot-prod.yaml
├── plain-yaml/
│   ├── base/                          # Base Kustomize resources
│   │   ├── namespace.yaml
│   │   ├── configmap.yaml
│   │   ├── secrets-placeholder.yaml
│   │   ├── network-policies.yaml
│   │   ├── ingress.yaml
│   │   ├── postgres/
│   │   ├── rabbitmq/
│   │   ├── redis/
│   │   ├── minio/
│   │   ├── backend/
│   │   ├── ai-service/
│   │   ├── ai-worker/
│   │   └── frontend/
│   └── overlays/
│       ├── development/
│       └── production/
└── scripts/
    ├── generate-secrets.sh
    └── validate-manifests.sh
```

---

## Troubleshooting

### Pods stuck in Pending

```bash
# Check PVC binding
kubectl get pvc -n jobcopilot
kubectl describe pvc <name> -n jobcopilot

# Ensure StorageClass exists
kubectl get storageclass
```

### Backend cannot connect to PostgreSQL

```bash
# Verify NetworkPolicy allows backend -> postgres
kubectl get networkpolicies -n jobcopilot

# Check DNS resolution inside pod
kubectl exec -it deploy/jobcopilot-backend -n jobcopilot -- nslookup jobcopilot-postgres
```

### Ingress not working

```bash
# Verify Ingress Controller is installed
kubectl get pods -n ingress-nginx

# Check Ingress events
kubectl describe ingress jobcopilot -n jobcopilot
```

---

## Security Hardening

- All containers run as **non-root** user
- **readOnlyRootFilesystem** where possible
- **NetworkPolicies** enforce three-tier isolation
- Secrets managed via **existingSecret** or **External Secrets Operator**
- **PodDisruptionBudgets** protect against voluntary disruptions
- **Resource quotas** prevent noisy-neighbor issues

---

## Contributing

When adding new environment variables:
1. Add non-sensitive vars to `templates/configmap.yaml`
2. Add sensitive vars to `templates/secrets.yaml` and `scripts/generate-secrets.sh`
3. Update `values.yaml` with defaults
4. Document in this README

---

## License

Same as the main project. See [../../../LICENSE](../../../LICENSE).
