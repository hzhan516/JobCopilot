# Resume Assistant — Docker Compose 部署架构

> [English](../../../deployment/architecture.md) | [繁體中文](../../../i18n/zh-Hant-TW/deployment/architecture.md)

## 1. 概述

Resume Assistant（智能求职助手）是一个以**三层 Docker 网络架构**部署的 AI 驱动求职平台：面向公网的反向代理层、内部应用层以及隔离的数据库层。

## 2. 网络架构

```
                         Internet
                            |
                            v
                    +---------------+
                    |  Nginx : 80   |  <-- 唯一公网入口
                    |  (frontend)   |
                    +---------------+
                            |
            +---------------+---------------+
            |                               |
            v                               v
   +------------------+           +------------------+
   |  backend : 8080  |           |  ai-service:8000 |
   |  (Spring Boot)   |<--------->|  (FastAPI)       |
   +------------------+           +------------------+
            |                               |
            |      +------------------+     |
            |      | rabbitmq : 5672  |<----+
            |      | (Message Queue)  |     |
            |      +------------------+     |
            |      +------------------+     |
            +----->|  redis   : 6379  |<----+
                   |  (缓存与锁)      |
                   +------------------+
            |
            v
   +------------------+
   | postgres : 5432  |
   | (PostgreSQL +    |
   |  pgvector)       |
   +------------------+
```

### 网络分层

| 网络 | 服务 | 外部暴露 | 用途 |
|------|------|----------|------|
| **公网（Public）** | `frontend`（Nginx） | 仅端口 `80` | 所有 HTTP/HTTPS 流量的单一入口 |
| **内网（Internal）** | `backend`、`ai-service`、`rabbitmq`、`redis` | 无（仅 Docker DNS） | 服务间通过容器名通信 |
| **数据库网（Database）** | `postgres` | 无（仅 Docker DNS） | 隔离的持久化数据存储 |

> **开发模板说明**：当前 `docker-compose.yml` 模板为方便本地调试，额外映射了宿主机端口（`8080`、`8000`、`5432`、`5672`、`15672`）。在**生产部署**中，仅应将 `frontend:80` 暴露给宿主机。

## 3. 服务清单

### 3.1 前端（Nginx + React）

| 属性 | 值 |
|------|-----|
| **网络** | `resume-network`（公网 + 内网） |
| **宿主机端口** | `80`（生产目标；模板使用 `8081:80` 用于开发） |
| **职责** | 静态单页应用托管与所有 API 流量的反向代理。 |
| **安全说明** | Nginx 将 `/api/*` 代理至 `backend:8080`。环境变量 `VITE_API_BASE_URL` **必须为空或相对路径**（例如 `/api`）。若设为绝对 URL（如 `http://backend:8080`），浏览器将直接连接后端，绕过 Nginx，破坏单入口安全模型。 |

### 3.2 后端（Spring Boot）

| 属性 | 值 |
|------|-----|
| **网络** | `resume-network`（公网 + 内网 + 数据库网） |
| **宿主机端口** | 生产环境无（模板暴露 `8080:8080` 用于开发） |
| **职责** | REST API 网关、JWT 身份验证、CAPTCHA 验证、业务逻辑编排、RabbitMQ 生产者。 |
| **安全说明** | 唯一跨越三层网络的服务。通过 Docker DNS 与 PostgreSQL（`postgres:5432`）和 RabbitMQ（`rabbitmq:5672`）通信。所有发往 `ai-service` 的出站 REST 请求均携带 `X-Internal-API-Key` 请求头。 |

### 3.3 AI 服务（FastAPI）

| 属性 | 值 |
|------|-----|
| **网络** | `resume-network`（仅内网） |
| **宿主机端口** | 生产环境无（模板暴露 `8000:8000` 用于开发） |
| **职责** | 大语言模型（LLM）推理、嵌入向量生成、简历/职位解析、职位排序。 |
| **安全说明** | REST 端点 `/api/v1/ai/embeddings` 受 `X-Internal-API-Key` 中间件保护。MQ 消费者监听四个队列：`ai.queue.job.parse`、`ai.queue.resume.parse`、`ai.queue.conversation`、`ai.queue.job.rank`。不访问数据库。 |

### 3.4 PostgreSQL（含 pgvector）

| 属性 | 值 |
|------|-----|
| **网络** | `resume-network`（仅数据库网） |
| **宿主机端口** | 生产环境无（模板暴露 `5432:5432` 用于开发） |
| **职责** | 业务数据与嵌入向量的统一存储。 |
| **安全说明** | 使用 `pgvector` 扩展进行相似度检索。仅能从 Docker 网络内的 `backend` 访问。即使具备网络隔离，强密码 `POSTGRES_PASSWORD` 仍是纵深防御的必备措施。 |

### 3.5 RabbitMQ（Management）

| 属性 | 值 |
|------|-----|
| **网络** | `resume-network`（仅内网） |
| **宿主机端口** | 生产环境无（模板暴露 `5672:5672` 和 `15672:15672` 用于开发） |
| **职责** | 后端与 AI 服务之间的异步消息代理（Outbox 模式，消息队列）。 |
| **安全说明** | 通过环境变量覆盖默认的 `guest/guest` 凭据。管理面板（`:15672`）绝不应暴露于公网；通过 SSH 隧道访问：`ssh -L 15672:localhost:15672 <host>`。消息大小限制设为 10 MB（`max_message_size 10485760`），以容纳向量和简历摘要。 |

### 3.6 Redis（缓存与锁）

| 属性 | 值 |
|------|-----|
| **网络** | `resume-network`（仅内网） |
| **宿主机端口** | 生产环境无 |
| **职责** | 分布式状态存储：CAPTCHA 挑战/token、验证码、对话流桥接、增量模型统计、去重集合、分布式锁（ShedLock）。 |
| **安全说明** | 无外部访问。开发环境密码认证可选（`REDIS_PASSWORD` 可为空），生产环境建议启用。数据通过 `redis-data` 命名卷持久化。 |

## 4. 纵深防御（Defense in Depth）

本部署实现了五层独立的安全层。攻破一层不会自动导致下一层失守。

### 第一层：网络隔离

仅 `frontend` 服务（Nginx，端口 `80`）面向公网暴露。其他所有服务通过 Docker 内部 DNS（`<service-name>`）通信。对宿主机的外部端口扫描只能发现端口 `80`。

### 第二层：应用层 API 密钥

`INTERNAL_API_KEY` 环境变量在后端与 AI 服务之间共享。后端发往 AI 服务的每个 REST 请求均携带 `X-Internal-API-Key` 请求头。AI 服务中间件对缺失或不匹配的密钥返回 HTTP 401。

> **安全模型**：即使攻击者获得内网 Docker 网络的访问权限，没有密钥也无法调用 LLM 嵌入端点。

### 第三层：JWT 身份验证

所有面向用户的 API 调用（注册、登录、简历上传、职位匹配）均携带签名 JWT，置于 `Authorization: Bearer <token>` 请求头中。签名密钥（`JWT_SECRET`）仅后端知晓。密钥轮换或变更将强制所有用户重新登录。

### 第四层：RabbitMQ 凭据

AMQP 连接需要用户名和密码。默认的 `guest/guest` 通过环境变量覆盖。即使某个容器被攻破，访问消息代理仍需要独立的凭据。

### 第五层：人机验证（CAPTCHA）

所有认证端点（注册、登录）均要求有效的 CAPTCHA 挑战-响应。后端维护前缀隔离的 Redis 缓存（String 存储挑战/token，Sorted Set 存储 IP 速率限制滑动窗口）用于存储挑战和一次性 token，并实施基于 IP 的速率限制（每分钟 20 次请求）。即使攻击者绕过网络隔离并持有有效凭据，也无法在不解决 CAPTCHA 挑战的情况下以编程方式完成认证。

## 5. 快速开始

```bash
# 1. 复制 Compose 模板
cp docker-compose.yml.example docker-compose.yml

# 2. 复制环境变量模板
cp .env.example .env

# 3. 编辑 .env，替换所有 [replace-me] 占位符
vim .env

# 4. 生成强 JWT 密钥（48 字节 = 64 个 base64 字符）
openssl rand -base64 48
# 将输出粘贴到 .env 的 JWT_SECRET 中

# 5. 生成内部 API 密钥（32 字节 = 44 个 base64 字符）
openssl rand -base64 32
# 将输出粘贴到 .env 的 INTERNAL_API_KEY 中
#（后端和 AI 服务必须使用完全相同的值）

# 6. 启动所有服务
docker compose up -d

# 7. 验证容器健康状态
docker compose ps

# 8. 检查前端健康端点
curl -f http://localhost/health
```

第 8 步的预期输出：`HTTP 200 OK` 及简短的健康状态正文。

## 6. 故障排查

### 端口 80 已被占用

**现象**：`docker compose up` 失败，报错 `bind: address already in use`。

**解决**：修改 `docker-compose.yml` 中的前端宿主机端口：

```yaml
ports:
  - "8081:80"   # 或宿主机上任意空闲端口
```

然后通过 `http://localhost:8081` 访问应用。

### RabbitMQ 管理面板无法访问

**现象**：浏览器无法连接 `http://localhost:15672`。

**解决**：生产环境中，管理面板**故意**不对外暴露。请使用 SSH 隧道：

```bash
ssh -L 15672:localhost:15672 user@your-server
# 然后在本地浏览器打开 http://localhost:15672
```

### AI 服务返回 401 Unauthorized

**现象**：后端日志显示调用嵌入端点时返回 `401 Unauthorized: invalid or missing internal API key`。

**解决**：确保 `INTERNAL_API_KEY` 在后端和 AI 服务的环境中设置为**完全相同的值**。验证方式：

```bash
docker compose exec backend env | grep INTERNAL_API_KEY
docker compose exec ai-service env | grep INTERNAL_API_KEY
```

### 数据库连接被拒绝

**现象**：从宿主机执行 `psql` 时报错 `could not connect to server: Connection refused`。

**解决**：PostgreSQL 位于隔离的内网 Docker 网络中，生产环境**不**向宿主机暴露端口 `5432`。这是预期行为。如需访问数据库，请进入容器：

```bash
docker compose exec postgres psql -U resume_user -d resume_assistant
```
