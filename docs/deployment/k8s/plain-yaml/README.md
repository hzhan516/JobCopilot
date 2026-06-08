# Plain YAML + Kustomize Deployment

> [English](README.md) | [简体中文](../../../i18n/zh-Hans-CN/deployment/k8s/plain-yaml/README.md) | [繁體中文](../../../i18n/zh-Hant-TW/deployment/k8s/plain-yaml/README.md)

## Overview

This directory provides raw Kubernetes YAML manifests with Kustomize overlays for environments that do not use Helm.

## Directory Structure

```
plain-yaml/
├── kustomization.yaml          # Base kustomization
├── base/                       # Base resources
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secrets-placeholder.yaml
│   ├── network-policies.yaml
│   ├── ingress.yaml
│   ├── postgres/
│   ├── rabbitmq/
│   ├── redis/
│   ├── minio/
│   ├── backend/
│   ├── ai-service/
│   ├── ai-worker/
│   └── frontend/
└── overlays/
    ├── development/
    └── production/
```

## Quick Start

### 1. Create Secrets

```bash
# Generate from .env
../scripts/generate-secrets.sh .env jobcopilot | kubectl apply -f -

# Or create manually
kubectl create secret generic jobcopilot-secrets \
  --namespace=jobcopilot \
  --from-literal=JWT_SECRET=your-secret \
  --from-literal=POSTGRES_PASSWORD=your-password \
  --from-literal=RABBITMQ_PASSWORD=your-rabbitmq-password \
  --from-literal=REDIS_PASSWORD=your-redis-password \
  --from-literal=MINIO_ACCESS_KEY=your-minio-access-key \
  --from-literal=MINIO_SECRET_KEY=your-minio-secret-key \
  --from-literal=INTERNAL_API_KEY=your-internal-api-key
```

### 2. Deploy Base

```bash
# Apply all base resources
kubectl apply -k .
```

### 3. Deploy with Overlay

```bash
# Development
kubectl apply -k overlays/development/

# Production
kubectl apply -k overlays/production/
```

## Customization

### Modifying Base Resources

Edit files under `base/` directly. All overlays inherit from base.

### Creating a New Overlay

```bash
mkdir overlays/my-env
cat > overlays/my-env/kustomization.yaml <<EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: jobcopilot-my-env

resources:
  - ../../base

namePrefix: my-env-

commonLabels:
  environment: my-env

patches:
  - target:
      kind: Ingress
      name: jobcopilot
    patch: |
      - op: replace
        path: /spec/rules/0/host
        value: my-env.jobcopilot.example.com
EOF
```

### Adding Resource Limits

Add a patch to your overlay:

```yaml
patches:
  - target:
      kind: Deployment
      name: jobcopilot-backend
    patch: |
      - op: add
        path: /spec/template/spec/containers/0/resources
        value:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
```

### Excluding Embedded Middleware

For managed middleware mode, remove StatefulSet resources from the kustomization:

```yaml
resources:
  - base/namespace.yaml
  - base/configmap.yaml
  # ... other resources ...
  # Do NOT include postgres/, rabbitmq/, redis/ StatefulSets
```

## Without Kustomize

You can apply individual files directly:

```bash
kubectl apply -f base/namespace.yaml
kubectl apply -f base/configmap.yaml
# ... apply each file in order ...
```

Recommended order:
1. Namespace, ConfigMap, Secret
2. NetworkPolicies
3. PostgreSQL, RabbitMQ, Redis (StatefulSets + Services)
4. Backend, AI Service, Frontend (Deployments + Services)
5. Ingress

## Validation

```bash
# Dry run
kubectl apply -k . --dry-run=client

# With server-side validation
kubectl apply -k . --dry-run=server
```
