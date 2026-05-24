# ADR-0006: Docker Compose Three-Tier Network Architecture

| Attribute | Value |
|-----------|-------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | Backend Architecture Team |
| **Affected Files** | `docker-compose.yml`, `.env.example`, `docs/deployment/architecture.md`, `frontend/Dockerfile` |

---

## 1. Context / Background

ResumeAssistant targets three deployment personas:

| Persona | Deployment Model | Networking Concern |
|---------|------------------|-------------------|
| **Non-technical user** | Local installable package (VirtualBox / VMware VM) | Minimal attack surface; single-click launch |
| **Technical professional** | Local Docker Compose stack | Defense-in-depth without K8s complexity |
| **Enterprise** | Kubernetes / cloud-native distribution | Network policies, service mesh compatibility |

All three personas share a common requirement: **the application must not expose internal services (database, message queue, AI inference) to the host or internet by default.**

### 1.1 Pre-ADR Anti-Pattern

Early prototypes mapped host ports directly to every service:

```yaml
# ❌ Anti-pattern: every service exposed to host
services:
  backend:
    ports: ["8080:8080"]   # Host can reach Spring Boot directly
  postgres:
    ports: ["5432:5432"]   # Host can reach PostgreSQL directly
  rabbitmq:
    ports: ["5672:5672"]   # Host can reach AMQP directly
```

This violates the principle of least privilege:
- A compromised host process can connect directly to the database.
- Developers accidentally point the frontend `VITE_API_BASE_URL` to `http://localhost:8080`, bypassing the reverse proxy and exposing the backend to CORS and direct attack.
- The architecture cannot be safely shipped as a local installable package because the user's laptop effectively becomes a publicly reachable server.

### 1.2 Candidate Architectures

| Approach | Description | Evaluation |
|----------|-------------|------------|
| **A. Single Docker network (bridge)** | All containers share one network; service names resolve globally | Simple, but zero lateral-movement protection |
| **B. Three-tier Docker networks (bridge)** | `public`, `internal`, `db` — explicit segmentation with backend as gateway | Defense-in-depth, easy to reason about, maps to K8s NetworkPolicies |
| **C. Docker Compose + host network mode** | Containers share the host network namespace | Fastest, but destroys all isolation; rejected immediately |
| **D. Full K8s for local deployment** | Minikube / k3s with NetworkPolicies | Overkill for non-technical users; steep learning curve |

---

## 2. Decision / Decision

**Adopt Option B: a three-tier Docker bridge network architecture with explicit service placement and the backend as the sole multi-homed gateway.**

### 2.1 Tier Definition

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                  INTERNET                                   │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────┐      │
│   │  HOST (developer laptop / VM / cloud instance)                  │      │
│   │                                                                 │      │
│   │   Port 80 (or FRONTEND_HOST_PORT) ──► ┌─────────────┐          │      │
│   │                                       │   Nginx     │          │      │
│   │                                       │  (frontend) │          │      │
│   │                                       └──────┬──────┘          │      │
│   │                                              │                │      │
│   │   ┌──────────────────────────────────────────┘                │      │
│   │   │         public-network (bridge, /16)                        │      │
│   │   │                                                           │      │
│   │   │    ┌──────────────┐          ┌──────────────┐             │      │
│   │   └───►│   backend    │◄─────────►│   backend    │◄────────────┘      │
│   │        │  :8080       │          │  :8080       │                    │
│   │        │  (Spring Boot)│         │  (Spring Boot)│                    │
│   │        │              │          │              │                    │
│   │        │  also on:    │          │  also on:    │                    │
│   │        │  internal    │          │  db          │                    │
│   │        └──────┬───────┘          └──────┬───────┘                    │
│   │               │                         │                            │
│   │   ┌───────────┴─────────────────────────┘                            │
│   │   │         internal-network (bridge, /16)                           │
│   │   │                                                                │
│   │   │   ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐ │
│   │   │   │ai-api  │  │rabbitmq│  │ redis  │  │ai-worker│  │ minio  │ │
│   │   │   │:8000   │  │:5672   │  │:6379   │  │ (train) │  │:9000   │ │
│   │   │   └────────┘  └────────┘  └────────┘  └────────┘  └────────┘ │
│   │   └────────────────────────────────────────────────────────────────┘
│   │
│   │   ┌────────────────────────────────────────────────────────────────┐
│   │   │         db-network (bridge, /16)                               │
│   │   │                                                              │
│   │   │                    ┌──────────────┐                            │
│   │   │                    │  postgres  │                            │
│   │   │                    │  :5432     │                            │
│   │   │                    │ + pgvector │                            │
│   │   │                    └──────────────┘                            │
│   │   └────────────────────────────────────────────────────────────────┘
│   └─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Service-to-Network Mapping

| Service | public-network | internal-network | db-network | Host Ports | Role |
|---------|---------------|------------------|------------|------------|------|
| **frontend** (Nginx) | ✅ | ❌ | ❌ | `80:8080` | Single HTTP entry point; reverse-proxies `/api/*` to backend |
| **backend** (Spring Boot) | ✅ | ✅ | ✅ | None | Gateway; spans all tiers |
| **ai-api** (FastAPI) | ❌ | ✅ | ❌ | None | LLM inference, embedding, parsing |
| **ai-worker** (LightGBM) | ❌ | ✅ | ❌ | None | Incremental model training |
| **rabbitmq** | ❌ | ✅ | ❌ | None | Async message broker (Outbox pattern) |
| **redis** | ❌ | ✅ | ❌ | None | Cache, distributed locks (ShedLock), feedback buffer |
| **minio** | ❌ | ✅ | ❌ | None | Model artifact registry |
| **postgres** | ❌ | ❌ | ✅ | None | Business data + vector embeddings (pgvector) |

### 2.3 The Gateway Principle

The **backend** is the only container attached to all three networks. This is intentional:

1. **Traffic control**: All external HTTP requests enter through `frontend:80` → `backend:8080`. The backend decides whether to query PostgreSQL, publish a RabbitMQ message, or call the AI service.
2. **Secret centralization**: Only the backend needs PostgreSQL credentials, RabbitMQ credentials, and the `INTERNAL_API_KEY` for AI service authentication. Other tiers never see cross-tier secrets.
3. **Observability**: A single request can be traced through `Nginx → backend → (db | mq | ai-api)` without jumping between network boundaries.

### 2.4 Docker Compose Implementation

```yaml
# docker-compose.yml — network excerpt
networks:
  public-network:
    driver: bridge
    driver_opts:
      com.docker.network.driver.mtu: 1500

  internal-network:
    driver: bridge
    driver_opts:
      com.docker.network.driver.mtu: 1500

  db-network:
    driver: bridge
    driver_opts:
      com.docker.network.driver.mtu: 1500

services:
  frontend:
    networks:
      - public-network
    ports:
      - "${FRONTEND_HOST_PORT:-80}:8080"

  backend:
    networks:
      - public-network
      - internal-network
      - db-network
    # No host ports — unreachable except via public-network

  ai-api:
    networks:
      - internal-network

  postgres:
    networks:
      - db-network
    # No host ports — unreachable except via db-network
```

### 2.5 Development vs Production Port Policy

```yaml
services:
  postgres:
    # --- DEVELOPMENT ONLY — REMOVE IN PRODUCTION ---
    # ports:
    #   - "5432:5432"
    networks:
      - db-network
```

All direct host port mappings (backend `8080`, postgres `5432`, rabbitmq `5672`/`15672`, ai-api `8000`) are **commented out** by default. Uncommenting them prints a `SECURITY WARNING` in the file header and must be reverted before shipping.

---

## 3. Consequences / Consequences

### 3.1 Positive / Positive

| Benefit | Explanation |
|---------|-------------|
| **Defense in Depth** | Even if the frontend Nginx is compromised, the attacker cannot reach PostgreSQL because there is no network path from `public-network` to `db-network`. |
| **Same shape as K8s** | The Docker Compose stack mirrors a K8s deployment with NetworkPolicies: `public-network` ≈ ingress-exposed namespace, `internal-network` ≈ cluster-internal namespace, `db-network` ≈ restricted namespace. Migrating to K8s does not require re-architecting service communication. |
| **Single entry point** | Every HTTP request flows through `frontend:80`. CORS, rate limiting, TLS termination, and WAF rules are implemented once in Nginx. |
| **Secret compartmentalization** | PostgreSQL password is known only by `backend` and `postgres` containers. The AI service never sees it. |
| **Local package safety** | When shipped as a VirtualBox/VMware VM, the VM's single forwarded port (`80→80`) exposes only Nginx. The host OS cannot accidentally bind to `5432` and collide with a developer's local PostgreSQL. |

### 3.2 Negative / Negative

| Cost | Explanation |
|------|-------------|
| **Backend complexity** | The backend must manage three network interfaces and route traffic correctly. Misconfiguration (e.g., forgetting to attach `db-network`) causes connection failures that are harder to debug than a flat network. |
| **Development friction** | Developers who want to connect pgAdmin or Postman directly to the backend must uncomment host ports and remember to revert before committing. |
| **Multi-homed DNS quirks** | A container on multiple networks resolves its own hostname to one of the attached networks unpredictably. The backend must use explicit service names (`postgres`, `rabbitmq`) rather than `localhost`. |
| **No built-in encryption** | Docker bridge networks provide Layer-2 isolation but not encryption. Traffic between backend and postgres is plaintext on the wire. For multi-host deployments, an overlay network with TLS or a service mesh is required. |

### 3.3 Risks / Risks & Mitigation

| Risk | Mitigation |
|------|------------|
| Developer accidentally commits `docker-compose.yml` with dev ports uncommented | **Git ignore + CI lint**: `docker-compose.yml` is gitignored (the example file is committed instead). CI runs `docker compose config` and fails if any host port other than `frontend:80` is detected. |
| Frontend `VITE_API_BASE_URL` set to absolute URL bypassing Nginx | **Build-time assertion**: The frontend Dockerfile checks `VITE_API_BASE_URL`; if it starts with `http`, the build fails. Documentation explicitly warns against absolute URLs. |
| Backend container escape compromises all tiers | **Runtime hardening**: Containers run as non-root (`USER 1000:1000` where possible), read-only root filesystem (`read_only: true`), and dropped capabilities (`cap_drop: [ALL]`). |
| Docker bridge MTU mismatch causes silent packet loss in cloud VMs | **Explicit MTU**: Each network declares `com.docker.network.driver.mtu: 1500` to match standard Ethernet; cloud overlay MTU issues (e.g., AWS VPC 9001 Jumbo Frames) are handled at the host level. |

---

## 4. Compliance / Compliance Verification

- **CI Port Scan**: Every PR runs `ci/check-compose-ports.sh` which parses `docker-compose.yml` and asserts that only `frontend` exposes host ports.
- **Security Review**: Quarterly audit of `docker-compose.yml` and `.env.example` to ensure no new service introduces host port exposure without ADR amendment.
- **Penetration Test**: Annual external pentest includes network segmentation validation — confirm that `nmap` from a container in `public-network` cannot reach `postgres:5432` or `rabbitmq:5672`.
- **Documentation Sync**: `docs/deployment/architecture.md` ASCII diagram must be regenerated whenever service-to-network mapping changes.

---

## 5. Related / Related Decisions

- ADR-0001 — Hexagonal Architecture (backend as the single gateway aligns with Ports & Adapters boundary control)
- ADR-0002 — PostgreSQL + pgvector (`db-network` isolation)
- ADR-0003 — RabbitMQ + Outbox (`internal-network` isolation)
- ADR-0004 — Redis Cache & Lock (`internal-network` isolation)
- ADR-0005 — Embedding Port Abstraction (AI service lives in `internal-network`, reachable only via backend)

---

## 6. Notes / Notes

> The three-tier model is a simplified version of the **Purdue Model for ICS Security** applied to a cloud-native application stack:
> - Tier 1 (Public) ≈ DMZ / Enterprise Zone
> - Tier 2 (Internal) ≈ Manufacturing Zone
> - Tier 3 (Database) ≈ Safety Zone / Level 3.5
>
> For enterprise Kubernetes deployment, each Docker network maps to a Namespace + NetworkPolicy:
> | Docker Network | Kubernetes Equivalent |
> |----------------|----------------------|
> | `public-network` | `ingress-nginx` namespace, ingress-allowed |
> | `internal-network` | `jobcopilot-app` namespace, ingress denied, egress to db namespace allowed |
> | `db-network` | `jobcopilot-data` namespace, ingress only from `jobcopilot-app` namespace |
>
> The backend's multi-network membership maps to a Kubernetes **Sidecar** or **Ambassador** pattern where the gateway container bridges trust boundaries.

---

*End of ADR-0006*
