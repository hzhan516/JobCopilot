<!-- 语言切换 -->
> [English](../../../architecture/Architecture.md) | **简体中文** | [繁體中文](../../zh-Hant-TW/architecture/Architecture.md)

# JobCopilot 架构

## 文档信息

| 字段 | 内容 |
|------|------|
| 项目 | JobCopilot |
| 文档 | 系统架构 |
| 版本 | 2.0.0 |
| 状态 | 持续更新 |
| 最近更新 | 2026-06-05 |
| 读者 | 维护者、贡献者、部署人员 |

## 概览

JobCopilot 是一个容器化的 AI 求职平台，由 React 前端、Java Spring Boot 后端、Python FastAPI AI 服务、AI Worker、PostgreSQL + pgvector、RabbitMQ、Redis 和 MinIO 组成。

架构目标是保持清晰服务边界、异步处理耗时 AI 任务、通过 LiteLLM 支持可替换模型提供商，并通过 Docker Compose 提供默认只暴露前端网关的本地部署方式。

## 架构原则

| 原则 | 设计选择 |
|------|----------|
| 清晰职责边界 | 前端负责 UI，后端负责业务工作流和持久化，AI 服务负责模型调用和排序逻辑 |
| 领域优先后端 | 后端模块遵循 DDD 和六边形架构：`types`、`domain`、`api`、`infrastructure`、`trigger`、`app` |
| 耗时 AI 任务异步化 | RabbitMQ 承载解析、排序、对话和反馈任务 |
| 统一向量存储 | PostgreSQL 同时保存业务实体和 pgvector 嵌入向量 |
| AI 提供商中立 | 通过环境变量配置 LiteLLM 兼容模型名称和密钥 |
| 运行时分层隔离 | Docker 网络隔离 public、internal 和 database 层 |
| 易于本地复现 | `.env.example` 和 `docker-compose.yml` 提供可复现本地栈 |

## 运行拓扑

```text
Browser
  |
  | HTTP :${FRONTEND_HOST_PORT:-80}
  v
Frontend container
  React 静态应用 + Nginx 反向代理
  |
  | HTTP /api, /health
  v
Backend container
  Spring Boot API、认证、领域工作流、向量持久化
  |-- JDBC --------> PostgreSQL + pgvector
  |-- AMQP --------> RabbitMQ --------> AI worker container
  |-- HTTP --------> AI API container
  |-- Redis -------> Redis
  `-- Local files -> shared upload volume

AI API / AI worker
  |-- LiteLLM 兼容提供商：解析、嵌入、排序、对话
  |-- 后端内部 API：向量写入和基线特征读取
  |-- Redis：反馈缓冲、分布式锁、模型重载 Pub/Sub
  `-- MinIO：LightGBM 模型产物注册表
```

## 组件

| 组件 | 技术 | 网络暴露 | 职责 |
|------|------|----------|------|
| 前端 / 网关 | React 19、Vite 7、Nginx | 主机 `${FRONTEND_HOST_PORT:-80}` -> 容器 `8080` | 提供 UI，反向代理后端 API 和健康检查 |
| 后端 | Java 21、Spring Boot 3.5 | 内部 `8080`；可选开发端口映射 | REST API、认证、领域工作流、事务、持久化、MQ 发布/消费 |
| AI API | Python 3.11、FastAPI、LiteLLM | 内部 `8000`；可选开发端口映射 | 嵌入、解析、排序和对话相关同步端点 |
| AI Worker | Python 3.11、RabbitMQ 消费者、LightGBM | 内部工作进程 | 异步任务处理、反馈采集、增量模型训练 |
| PostgreSQL | PostgreSQL 15 + pgvector | `db-network` 内部 `5432` | 业务表和向量表 |
| RabbitMQ | RabbitMQ 3 management 镜像 | 内部 `5672`；管理 UI 默认关闭 | 持久化队列传输和 DLQ 支持 |
| Redis | Redis 7 | 内部 `6379` | CAPTCHA 状态、分布式锁、反馈缓冲、模型重载 Pub/Sub |
| MinIO | S3 兼容对象存储 | 内部 `9000`；控制台默认不暴露 | LightGBM 模型产物和元数据 |

## 后端架构

后端是 Maven 多模块 Spring Boot 系统。

| 模块 | 角色 | 依赖方向 |
|------|------|----------|
| `types` | 共享枚举、值类型、常量 | 不依赖其他项目模块 |
| `domain` | 实体、值对象、端口、领域服务 | 依赖 `types` |
| `api` | DTO、命令、查询、外观接口 | 依赖 `domain`、`types` |
| `infrastructure` | JPA、Redis、RabbitMQ、存储、外部服务适配器 | 实现 domain/API 端口 |
| `trigger` | REST 控制器、WebSocket 端点、事件/MQ 监听器 | 调用应用/API 接口 |
| `app` | Spring Boot 启动、配置、应用服务、调度器 | 装配所有模块 |

后端规则：

- 领域代码不依赖 Spring、持久化、HTTP 或消息中间件 API。
- 应用服务负责事务边界。
- 数据库事务内不执行网络 I/O。
- 异步副作用应通过 RabbitMQ 和适用的 outbox 风格可靠性机制处理。
- 向量维度由 `LLM_EMBEDDING_MODEL_DIMENSION` 控制，必须与嵌入模型匹配。

## AI 服务架构

Python 服务同时包含同步 API 和后台 Worker 逻辑。

| 区域 | 路径 | 职责 |
|------|------|------|
| API | `ai-service/app/main.py`、`app/api/` | FastAPI 端点、健康检查、模型管理器 |
| 领域 | `ai-service/app/domain/` | AI 领域抽象和共享逻辑 |
| 基础设施 | `ai-service/app/infrastructure/` | 后端内部 API 客户端、Redis 客户端、MinIO 客户端 |
| 服务 | `ai-service/app/services/` | LLM 调用、嵌入、解析、匹配、排序 |
| MQ | `ai-service/app/mq/`、`app/worker/consumers/` | RabbitMQ 消费者和消息处理 |
| Worker | `ai-service/app/worker_main.py`、`app/worker/` | 反馈处理和 LightGBM 重新训练 |

AI 设计说明：

- LiteLLM 是文本、视觉和嵌入模型的统一抽象。
- 嵌入输出长度会校验 `LLM_EMBEDDING_MODEL_DIMENSION`。
- AI Worker 在 Redis 中缓冲反馈样本后再训练。
- 训练后的 LightGBM 产物上传到 MinIO，并通过 Redis Pub/Sub 通知重载。
- AI 服务通过后端内部 API 写入向量和读取基线特征。

## 数据架构

| 存储 | 内容 | 说明 |
|------|------|------|
| PostgreSQL | 用户、简历、职位、对话、申请记录、outbox/任务状态 | 业务数据事实来源 |
| pgvector | 简历和职位嵌入向量 | 用于语义召回和相似度搜索 |
| Redis | CAPTCHA challenge/token/rate-limit 状态、AI 反馈缓冲、锁、模型重载事件 | Compose 中需要 `REDIS_PASSWORD` |
| RabbitMQ | AI 请求/响应队列、反馈队列、DLQ | 默认仅内部访问 |
| 本地上传卷 | 默认 Compose 配置下的上传简历文件 | 后端 Compose 配置使用 `STORAGE_TYPE=local` |
| MinIO | AI 模型产物和元数据 | 由 AI Worker/模型管理器使用；自定义部署中也可用于简历存储 |

## 主要流程

### 简历上传和解析

```text
User -> Frontend -> Backend
Backend -> PostgreSQL: 保存简历元数据和版本
Backend -> Local upload volume: 保存文件
Backend -> RabbitMQ: 发布解析请求
AI worker -> LiteLLM provider: 解析文档内容
AI worker -> Backend: 返回结构化数据和向量
Backend -> PostgreSQL/pgvector: 持久化解析数据和嵌入
Frontend -> Backend: 轮询或获取更新后的简历状态
```

### 职位匹配

```text
Frontend -> Backend: 请求匹配结果
Backend -> PostgreSQL/pgvector: 语义召回
Backend -> AI API or RabbitMQ: 排序/解释任务
AI service -> LiteLLM provider: 排序或解释匹配结果
Backend -> Frontend: 返回排序后的职位和匹配元数据
```

### 反馈与增量训练

```text
User scoring behavior -> Backend
Backend -> RabbitMQ: 发布反馈事件
AI worker -> Redis: 缓冲标注样本
AI worker -> Backend internal API: 获取基线特征
AI worker -> LightGBM: 满足阈值且获取锁后训练
AI worker -> MinIO: 上传模型产物和 latest 元数据
AI worker -> Redis Pub/Sub: 发布模型重载事件
AI API model manager -> MinIO: 加载最新模型
```

## 部署拓扑

Docker Compose 定义三层网络：

| 网络 | 成员 | 用途 |
|------|------|------|
| `public-network` | `frontend`、`backend` | 浏览器入口网关访问后端 |
| `internal-network` | `backend`、`ai-service`、`ai-worker`、`rabbitmq`、`redis`、`minio` | 内部服务通信 |
| `db-network` | `backend`、`postgres` | 数据库访问与其他服务隔离 |

默认只有 `frontend` 映射主机端口。后端、AI API、RabbitMQ 管理端、Redis、PostgreSQL 和 MinIO 都保持内部访问，除非显式开启开发用端口映射。

## 安全边界

- 密钥从 `.env` 加载，`.env.example` 记录本地所需值。
- 共享或类生产环境必须更换 `JWT_SECRET`、`INTERNAL_API_KEY`、数据库凭证、RabbitMQ 凭证、Redis 密码和 MinIO 密钥。
- 后端负责用户侧工作流的认证和授权。
- AI 服务应通过后端内部 API 访问业务数据，而不是直接访问数据库。
- Docker 网络隔离默认阻止主机直接访问数据平面服务。
- 上传文件和模型产物与应用代码分离存储。

## 扩展点

| 变更 | 预期扩展点 |
|------|------------|
| 增加存储后端 | 实现后端存储适配器并设置 `STORAGE_TYPE` |
| 增加 LLM 提供商 | 在 `.env` 中配置 LiteLLM 模型名和提供商密钥 |
| 增加 AI 任务 | 增加 RabbitMQ 消息契约、后端发布/监听逻辑、AI 消费者 |
| 增加领域工作流 | 增加领域模型/端口、应用服务、基础设施适配器、触发器端点 |
| 增加可观测性 | 在 Compose 或部署清单中扩展 metrics/logging 服务 |

## 相关文档

- [Docker 部署](../deployment/DOCKER_DEPLOY.md)
- [环境变量](../deployment/environment-variables.md)
- [事务策略](../../../transactional-strategy.md)
- [分支和提交规范](../../../BRANCHING_AND_COMMITS.md)
- [架构决策记录](../../../adr/)
