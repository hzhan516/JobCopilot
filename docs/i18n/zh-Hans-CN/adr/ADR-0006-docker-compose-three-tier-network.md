<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../adr/ADR-0006-docker-compose-three-tier-network.md) | [简体中文](ADR-0006-docker-compose-three-tier-network.md) | [繁體中文](../../zh-Hant-TW/adr/ADR-0006-docker-compose-three-tier-network.md)

# ADR-0006: Docker Compose 三层网络架构

| 属性 | 内容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | 后端架构团队 |
| **Affected Files** | `docker-compose.yml`, `.env.example`, `docs/deployment/architecture.md`, `frontend/Dockerfile` |

---

## 1. Context / 背景

ResumeAssistant 面向三种部署角色：

| 角色 | 部署模式 | 网络需求 |
|------|----------|----------|
| **非技术用户** | 本地可安装包（VirtualBox / VMware 虚拟机） | 最小攻击面；一键启动 |
| **技术专业人员** | 本地 Docker Compose 栈 | 无需 K8s 复杂度的纵深防御 |
| **企业用户** | Kubernetes / 云原生分发 | 网络策略、服务网格兼容 |

三种角色共享一个共同要求：**默认情况下，应用不得将内部服务（数据库、消息队列、AI 推理）暴露给宿主机或互联网。**

### 1.1 采用 ADR 前的反模式

早期原型将宿主机端口直接映射到每个服务：

```yaml
# ❌ 反模式：每个服务都暴露到宿主机
services:
  backend:
    ports: ["8080:8080"]   # 宿主机可直接访问 Spring Boot
  postgres:
    ports: ["5432:5432"]   # 宿主机可直接访问 PostgreSQL
  rabbitmq:
    ports: ["5672:5672"]   # 宿主机可直接访问 AMQP
```

这违反了最小权限原则：
- 被入侵的宿主机进程可以直接连接数据库。
- 开发者会意外地将前端 `VITE_API_BASE_URL` 指向 `http://localhost:8080`，绕过反向代理，使后端暴露于 CORS 和直接攻击。
- 该架构无法作为本地可安装包安全发布，因为用户的笔记本电脑实际上变成了公网可达的服务器。

### 1.2 候选架构

| 方案 | 说明 | 评估 |
|------|------|------|
| **A. 单一 Docker 网络（bridge）** | 所有容器共享一个网络；服务名全局解析 | 简单，但无横向移动保护 |
| **B. 三层 Docker 网络（bridge）** | `public`、`internal`、`db` — 显式分段，后端作为网关 | 纵深防御，易于理解，可映射到 K8s NetworkPolicies |
| **C. Docker Compose + host 网络模式** | 容器共享宿主机网络命名空间 | 最快，但破坏所有隔离；立即拒绝 |
| **D. 本地部署完整 K8s** | Minikube / k3s + NetworkPolicies | 对非技术用户过重；学习曲线陡峭 |

---

## 2. Decision / 决策

**采用方案 B：三层 Docker bridge 网络架构，显式定义服务放置位置，后端作为唯一的跨网关机。**

### 2.1 分层定义

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                  互联网（INTERNET）                          │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────┐      │
│   │  宿主机（开发者笔记本 / 虚拟机 / 云实例）                          │      │
│   │                                                                 │      │
│   │   端口 80（或 FRONTEND_HOST_PORT）──► ┌─────────────┐          │      │
│   │                                       │   Nginx     │          │      │
│   │                                       │  (frontend) │          │      │
│   │                                       └──────┬──────┘          │      │
│   │                                              │                │      │
│   │   ┌──────────────────────────────────────────┘                │      │
│   │   │         public-network（bridge, /16）                       │      │
│   │   │                                                           │      │
│   │   │    ┌──────────────┐          ┌──────────────┐             │      │
│   │   └───►│   backend    │◄─────────►│   backend    │◄────────────┘      │
│   │        │  :8080       │          │  :8080       │                    │
│   │        │  (Spring Boot)│         │  (Spring Boot)│                    │
│   │        │              │          │              │                    │
│   │        │  同时接入：   │          │  同时接入：   │                    │
│   │        │  internal    │          │  db          │                    │
│   │        └──────┬───────┘          └──────┬───────┘                    │
│   │               │                         │                            │
│   │   ┌───────────┴─────────────────────────┘                            │
│   │   │         internal-network（bridge, /16）                          │
│   │   │                                                                │
│   │   │   ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐ │
│   │   │   │ai-api  │  │rabbitmq│  │ redis  │  │ai-worker│  │ minio  │ │
│   │   │   │:8000   │  │:5672   │  │:6379   │  │ (训练)  │  │:9000   │ │
│   │   │   └────────┘  └────────┘  └────────┘  └────────┘  └────────┘ │
│   │   └────────────────────────────────────────────────────────────────┘
│   │
│   │   ┌────────────────────────────────────────────────────────────────┐
│   │   │         db-network（bridge, /16）                                │
│   │   │                                                              │
│   │   │                    ┌──────────────┐                            │
│   │   │                    │  postgres    │                            │
│   │   │                    │  :5432       │                            │
│   │   │                    │ + pgvector   │                            │
│   │   │                    └──────────────┘                            │
│   │   └────────────────────────────────────────────────────────────────┘
│   └─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 服务与网络映射

| 服务 | public-network | internal-network | db-network | 宿主机端口 | 角色 |
|------|---------------|------------------|------------|------------|------|
| **frontend** (Nginx) | ✅ | ❌ | ❌ | `80:8080` | 唯一 HTTP 入口；反向代理 `/api/*` 到后端 |
| **backend** (Spring Boot) | ✅ | ✅ | ✅ | 无 | 网关；跨三层 |
| **ai-api** (FastAPI) | ❌ | ✅ | ❌ | 无 | LLM 推理、向量化、解析 |
| **ai-worker** (LightGBM) | ❌ | ✅ | ❌ | 无 | 增量模型训练 |
| **rabbitmq** | ❌ | ✅ | ❌ | 无 | 异步消息代理（Outbox 模式） |
| **redis** | ❌ | ✅ | ❌ | 无 | 缓存、分布式锁（ShedLock）、反馈缓冲 |
| **minio** | ❌ | ✅ | ❌ | 无 | 模型制品仓库 |
| **postgres** | ❌ | ❌ | ✅ | 无 | 业务数据 + 向量嵌入（pgvector） |

### 2.3 网关原则

**后端**是唯一接入全部三个网络的容器。这是有意为之：

1. **流量控制**：所有外部 HTTP 请求通过 `frontend:80` → `backend:8080` 进入。后端决定是查询 PostgreSQL、发布 RabbitMQ 消息，还是调用 AI 服务。
2. **密钥集中化**：只有后端需要 PostgreSQL 凭据、RabbitMQ 凭据和用于 AI 服务认证的 `INTERNAL_API_KEY`。其他层永远不会看到跨层密钥。
3. **可观测性**：单个请求可以追踪 `Nginx → backend → (db | mq | ai-api)`，无需跨越网络边界跳转。

### 2.4 Docker Compose 实现

```yaml
# docker-compose.yml — 网络配置节选
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
    # 无宿主机端口 — 仅通过 public-network 可达

  ai-api:
    networks:
      - internal-network

  postgres:
    networks:
      - db-network
    # 无宿主机端口 — 仅通过 db-network 可达
```

### 2.5 开发与生产的端口策略

```yaml
services:
  postgres:
    # --- 仅开发环境使用 — 生产环境必须移除 ---
    # ports:
    #   - "5432:5432"
    networks:
      - db-network
```

所有直接宿主机端口映射（后端 `8080`、postgres `5432`、rabbitmq `5672`/`15672`、ai-api `8000`）**默认注释掉**。取消注释会在文件头部打印 `SECURITY WARNING`，且必须在发布前恢复。

---

## 3. Consequences / 后果

### 3.1 Positive / 正面

| 收益 | 说明 |
|------|------|
| **纵深防御** | 即使前端 Nginx 被入侵，攻击者也无法到达 PostgreSQL，因为 `public-network` 到 `db-network` 之间没有网络路径。 |
| **与 K8s 形态一致** | Docker Compose 栈的结构与带 NetworkPolicies 的 K8s 部署镜像对应：`public-network` ≈ ingress 暴露的命名空间，`internal-network` ≈ 集群内部命名空间，`db-network` ≈ 受限命名空间。迁移到 K8s 无需重构服务通信。 |
| **单一入口** | 每个 HTTP 请求都流经 `frontend:80`。CORS、速率限制、TLS 终止和 WAF 规则只需在 Nginx 中实现一次。 |
| **密钥隔离** | PostgreSQL 密码仅 `backend` 和 `postgres` 容器知晓。AI 服务永远不会看到它。 |
| **本地安装包安全性** | 作为 VirtualBox/VMware 虚拟机发布时，虚拟机的单一转发端口（`80→80`）仅暴露 Nginx。宿主机 OS 不会意外绑定到 `5432` 并与开发者本地的 PostgreSQL 冲突。 |

### 3.2 Negative / 负面

| 成本 | 说明 |
|------|------|
| **后端复杂度增加** | 后端必须管理三个网络接口并正确路由流量。配置错误（例如忘记接入 `db-network`）会导致连接失败，比扁平网络更难调试。 |
| **开发摩擦** | 想要直接用 pgAdmin 或 Postman 连接后端的开发者必须取消注释宿主机端口，并记得在提交前恢复。 |
| **多宿主 DNS 怪异行为** | 接入多个网络的容器无法预测地将自身主机名解析到哪个网络。后端必须使用显式服务名（`postgres`、`rabbitmq`）而非 `localhost`。 |
| **无内置加密** | Docker bridge 网络提供 Layer-2 隔离，但不提供加密。后端与 postgres 之间的流量在链路上是明文。对于多主机部署，需要 overlay 网络加 TLS 或服务网格。 |

### 3.3 Risks / 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| 开发者意外提交取消注释了开发端口的 `docker-compose.yml` | **Git 忽略 + CI 检查**：`docker-compose.yml` 被 gitignored（提交的是示例文件）。CI 运行 `docker compose config`，若检测到除 `frontend:80` 外的任何宿主机端口则构建失败。 |
| 前端 `VITE_API_BASE_URL` 设为绝对 URL 绕过 Nginx | **构建时断言**：前端 Dockerfile 检查 `VITE_API_BASE_URL`；若以 `http` 开头则构建失败。文档明确警告不要使用绝对 URL。 |
| 后端容器逃逸危及所有层 | **运行时加固**：容器以非 root 运行（`USER 1000:1000`），只读根文件系统（`read_only: true`），并丢弃所有 capabilities（`cap_drop: [ALL]`）。 |
| Docker bridge MTU 不匹配导致云虚拟机静默丢包 | **显式 MTU**：每个网络声明 `com.docker.network.driver.mtu: 1500` 以匹配标准以太网；云覆盖层 MTU 问题（例如 AWS VPC 9001 Jumbo Frames）在宿主机层面处理。 |

---

## 4. Compliance / 合规验证

- **CI 端口扫描**：每个 PR 运行 `ci/check-compose-ports.sh`，解析 `docker-compose.yml` 并断言只有 `frontend` 暴露宿主机端口。
- **安全审查**：每季度审计 `docker-compose.yml` 和 `.env.example`，确保没有新服务在未修订 ADR 的情况下引入宿主机端口暴露。
- **渗透测试**：年度外部渗透测试包含网络分段验证 — 确认从 `public-network` 中的容器运行 `nmap` 无法到达 `postgres:5432` 或 `rabbitmq:5672`。
- **文档同步**：每当服务到网络的映射发生变化时，必须重新生成 `docs/deployment/architecture.md` ASCII 架构图。

---

## 5. Related / 相关决策

- ADR-0001 — 六边形架构（后端作为单一网关，与 Ports & Adapters 边界控制一致）
- ADR-0002 — PostgreSQL + pgvector（`db-network` 隔离）
- ADR-0003 — RabbitMQ + Outbox（`internal-network` 隔离）
- ADR-0004 — Redis 缓存与锁（`internal-network` 隔离）
- ADR-0005 — Embedding 服务抽象（AI 服务位于 `internal-network`，仅通过后端可达）

---

## 6. Notes / 备注

> 三层模型是 **Purdue 工业控制系统安全模型** 在云原生应用栈中的简化版本：
> - 第一层（Public）≈ DMZ / 企业区
> - 第二层（Internal）≈ 制造区
> - 第三层（Database）≈ 安全区 / Level 3.5
>
> 对于企业 Kubernetes 部署，每个 Docker 网络映射到一个命名空间 + NetworkPolicy：
> | Docker 网络 | Kubernetes 等价物 |
> |-------------|---------------------|
> | `public-network` | `ingress-nginx` 命名空间，允许 ingress |
> | `internal-network` | `jobcopilot-app` 命名空间，禁止 ingress，允许 egress 到 db 命名空间 |
> | `db-network` | `jobcopilot-data` 命名空间，仅允许来自 `jobcopilot-app` 命名空间的 ingress |
>
> 后端的多网络接入映射到 Kubernetes 的 **Sidecar** 或 **Ambassador** 模式，其中网关容器跨越信任边界。

---

*End of ADR-0006*
