<!-- 语言切换器 -->
> [English](../../../README.md) | [简体中文](README.md) | [繁體中文](../zh-Hant-TW/README.md)

# JobCopilot

**JobCopilot** 是一个面向应届毕业生和职业转型者的 AI 驱动平台。它能够解析上传的简历，利用语义向量匹配将其与就业市场数据进行比对评估，并提供交互式 AI 助手，帮助用户迭代优化简历内容。安全的文档管理、异步 AI 处理以及个性化推荐，帮助用户节省时间并提高面试成功率。

**部署：** 系统已通过 Docker Compose 验证。将 `.env.example` 复制为 `.env`，配置所需值后运行 `docker compose --env-file .env up -d --build`。前端默认可通过 `http://localhost` 访问；如果配置了自定义端口，则通过 `http://localhost:${FRONTEND_HOST_PORT}` 访问。

## 功能特性

- **身份认证**：邮箱/密码注册（支持可选的邮箱验证）以及 Google OAuth 2.0 登录，受滑块 CAPTCHA 反爬虫保护
- **简历管理**：上传、解析、版本管理和以多种格式导出简历
- **AI 智能解析**：使用 LiteLLM 兼容模型从简历和职位描述中提取结构化信息
- **职位匹配**：基于简历内容与向量相似度的智能职位推荐
- **增量式职位训练闭环**：用户评分行为通过增量学习反馈至 AI 基线模型，无需全量重训练即可持续提升匹配准确度
- **申请追踪**：追踪求职申请状态并管理求职流程
- **AI 对话**：交互式聊天助手，提供求职建议和简历优化指导
- **国际化**：支持英文、简体中文和繁体中文界面
- **向量搜索**：基于 PostgreSQL pgvector 扩展的语义搜索

## 系统架构

JobCopilot 采用容器化服务架构。默认情况下，只有前端容器面向主机暴露端口；后端、AI、数据、缓存、消息队列和模型注册表服务都通过 Docker 网络通信。

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

| 组件 | 技术 | 暴露方式 | 职责 |
|------|------|----------|------|
| 前端 / 网关 | React 19、Vite 7、Nginx | 主机 `${FRONTEND_HOST_PORT:-80}` -> 容器 `8080` | 提供 UI，代理 `/api` 和 `/health` 到后端 |
| 后端 | Java 21、Spring Boot 3.5、DDD 模块 | 内部 `8080`；默认不直接暴露到主机 | REST API、认证、简历/职位/申请工作流、向量持久化 |
| AI API | Python 3.11、FastAPI、LiteLLM | 内部 `8000`；默认不直接暴露到主机 | 同步 AI 端点、嵌入、解析、排序、对话支持 |
| AI Worker | Python 3.11、RabbitMQ 消费者、LightGBM | 内部工作进程 | 异步解析、职位排序任务、反馈采集、增量模型训练 |
| PostgreSQL | PostgreSQL 15 + pgvector | `db-network` 内部 `5432` | 业务数据和向量存储 |
| RabbitMQ | RabbitMQ 3 management 镜像 | 内部 `5672`；管理端口默认关闭 | 后端与 AI 服务之间的持久化异步任务传输 |
| Redis | Redis 7 | 内部 `6379` | CAPTCHA 状态、分布式锁、反馈缓冲、模型重载 Pub/Sub |
| MinIO | S3 兼容对象存储 | 内部 `9000` | AI Worker 的 LightGBM 模型产物注册表 |

## 项目结构

```text
.
|-- backend/                   # Java / Spring Boot 后端
|   |-- api/                   # API DTO、命令、查询和外观接口
|   |-- app/                   # 应用服务、调度器和启动装配
|   |-- domain/                # 领域实体、值对象、端口和业务规则
|   |-- infrastructure/        # 持久化、存储、消息、安全和外部集成
|   |-- trigger/               # REST 控制器、WebSocket 端点、MQ 监听器
|   |-- types/                 # 共享类型和常量
|   |-- scripts/               # 后端维护脚本
|   |-- Dockerfile             # 后端容器镜像
|   `-- pom.xml                # Maven 多模块构建
|-- frontend/                  # React / Vite / TypeScript 前端
|   |-- src/                   # UI 源代码
|   |   |-- components/        # 可复用 UI 组件
|   |   |-- pages/             # 路由页面
|   |   |-- services/          # API 客户端和服务封装
|   |   |-- store/             # 客户端状态管理
|   |   |-- hooks/             # 共享 React Hooks
|   |   |-- i18n/              # 运行时国际化设置
|   |   `-- locales/           # 翻译资源
|   |-- package.json           # Node.js 依赖和脚本
|   `-- Dockerfile             # 前端容器镜像
|-- ai-service/                # Python / FastAPI AI 服务和 Worker
|   |-- app/
|   |   |-- api/               # FastAPI 端点
|   |   |-- domain/            # AI 领域逻辑和模型抽象
|   |   |-- infrastructure/    # 外部集成
|   |   |-- mq/                # 消息集成
|   |   |-- services/          # AI 应用服务
|   |   `-- worker/            # 后台 Worker 入口
|   |-- tests/                 # Pytest 测试套件
|   |-- requirements.txt       # Python 依赖
|   `-- Dockerfile             # AI 服务容器镜像
|-- docs/                      # ADR、API 文档、架构、部署和国际化
|-- eval/                      # AI 评估脚本、数据集和结果
|-- middleware/                # 自定义基础设施镜像，例如 PostgreSQL
|-- scripts/                   # 仓库级自动化辅助脚本
|-- .github/                   # CI、Issue 模板、PR 模板、CODEOWNERS
|-- docker-compose.yml         # 本地 Docker Compose 栈
|-- .env.example               # 环境变量模板
`-- empty-vertex.json          # 非 Vertex 本地运行的占位凭据文件
```

## 后端架构

后端采用**六边形架构 / 领域驱动设计（DDD）**，包含以下分层模块：

| 模块            | 说明                           | 依赖              |
|-----------------|--------------------------------|-------------------|
| `types`         | 基础类型、枚举、常量           | 无                |
| `domain`        | 领域实体、服务、仓储接口         | `types`           |
| `api`           | DTO、外观接口                   | `domain`、`types` |
| `infrastructure`| 数据库、缓存、消息队列实现       | `domain`、`api`   |
| `trigger`       | 控制器、调度器、事件监听器       | `domain`、`api`   |
| `app`           | Spring Boot 启动和配置           | 所有模块          |

## 快速开始

### 前置要求

- Docker 20.10+ 和 Docker Compose 2.0+
- 或带 podman-compose 的 Podman
- 一个 LiteLLM 兼容的 AI 服务商密钥用于本地 AI 功能，例如 Gemini、OpenAI 或 Anthropic
- Google Cloud / Vertex AI 为可选项，本地开发不需要强制配置

### 1. 克隆仓库

```bash
git clone <仓库地址>
cd JobCopilot
```

### 2. 配置环境变量

**推荐：使用 Web 配置器**

直接在浏览器中打开 `docs/deployment/env-setup.html`（无需服务器）：
1. 选择语言并填写必填字段（带 * 标记）
2. 点击密钥字段旁的**生成**按钮以生成安全的随机值
3. 点击**下载 .env**并保存到项目根目录

**替代方案（命令行）：**
```bash
cp .env.example .env
# 编辑 .env 并填写所需值
```

关键环境变量：

| 变量                         | 必需      | 说明                                                              |
|------------------------------|-----------|-------------------------------------------------------------------|
| `JWT_SECRET`                 | 是        | JWT 令牌生成的密钥（至少 32 个字符）                              |
| `VITE_GOOGLE_CLIENT_ID`      | 是        | 前端登录流程使用的 Google OAuth 2.0 客户端 ID                      |
| `INTERNAL_API_KEY`           | 推荐      | 后端调用 AI 服务的共享密钥                                        |
| `GEMINI_API_KEY`             | 有条件    | 当 `LLM_*_MODEL` 使用 `gemini/` 前缀时使用的 Gemini API 密钥       |
| `OPENAI_API_KEY`             | 有条件    | 当 `LLM_*_MODEL` 使用 `openai/` 前缀时使用的 OpenAI API 密钥     |
| `ANTHROPIC_API_KEY`          | 有条件    | 当 `LLM_*_MODEL` 使用 `anthropic/` 前缀时使用的 Anthropic API 密钥 |
| `LLM_TEXT_MODEL`             | 否        | LiteLLM 文本模型名称；Compose 中默认为 Gemini 模型               |
| `LLM_VISION_MODEL`           | 否        | LiteLLM 视觉模型名称                                             |
| `LLM_EMBEDDING_MODEL`        | 否        | LiteLLM 嵌入模型名称                                             |
| `LLM_EMBEDDING_MODEL_DIMENSION` | 否     | 嵌入输出维度（必须与模型匹配）                                    |
| `SPRING_PROFILES_ACTIVE`     | 否        | Spring 配置文件：`dev`（默认）或 `prod`                          |
| `LOG_LEVEL`                  | 否        | AI 服务日志级别：`INFO`（默认）或 `DEBUG`                        |
| `CAPTCHA_ENABLED`            | 否        | 启用滑块 CAPTCHA。默认：`true`                                   |
| `CAPTCHA_TOLERANCE`          | 否        | CAPTCHA 拖动容差（像素）。默认：`8`                              |
| `CAPTCHA_TOKEN_EXPIRY`       | 否        | CAPTCHA 令牌过期时间（秒）。默认：`300`                          |
| `CAPTCHA_TRACK_WIDTH`        | 否        | CAPTCHA 轨道宽度（像素）。默认：`300`                            |
| `REDIS_HOST`                 | 否        | Redis 主机名。默认：`redis`（Docker）或 `localhost`               |
| `REDIS_PORT`                 | 否        | Redis 端口。默认：`6379`                                         |
| `REDIS_PASSWORD`             | 是        | Compose 使用的 Redis 认证密码；部署前请更改本地默认值              |
| `MINIO_ENDPOINT`             | 否        | AI 模型注册表使用的内部 MinIO 端点                                 |
| `MINIO_ACCESS_KEY`           | 是        | AI 模型注册表使用的 MinIO 访问密钥                                 |
| `MINIO_SECRET_KEY`           | 是        | AI 模型注册表使用的 MinIO 密钥                                     |
| `MINIO_MODEL_BUCKET`         | 否        | 用于保存训练模型产物的 MinIO 存储桶                                |
| `CAPTCHA_MAX_ATTEMPTS`       | 否        | 每 IP 最大 CAPTCHA 尝试次数。默认：`5`                           |

本地开发时，将 `.env.example` 复制为 `.env`，并提供与您选择的 LiteLLM 模型前缀匹配的 API 密钥。例如，默认 Gemini 模型使用 `GEMINI_API_KEY`。

仅在您有意将项目配置为使用 Vertex AI 模型时才需要 Google Cloud ADC。
注意：提供的 `docker-compose.yml` 默认在后端以本地文件模式运行简历存储。`.env.example` 中的 MinIO、S3 和 OSS 设置用于自定义部署。
注意：如果在系统运行时更改 `.env` 中的 LLM 提供商、模型、维度或前端构建时变量，请运行 `docker compose --env-file .env up -d --build` 以重建/重新创建受影响的容器。简单重启不会应用所有更改。


### 3. 启动核心服务

使用 Docker Compose：
```bash
docker compose --env-file .env up -d --build
```

如果环境仍使用旧版 Compose CLI，请使用：
```bash
docker-compose --env-file .env up -d --build
```

使用 Podman：
```bash
podman-compose up -d
# 或
podman compose up -d
```

本地开发时，`.env` 由 Docker Compose 和本地 AI 服务进程加载。AI 服务使用 LiteLLM，因此请提供与配置的模型提供商匹配的 API 密钥，例如默认 Gemini 模型的 `GEMINI_API_KEY`。

如果您在 Docker 外本地运行 AI 服务，请在启动 Uvicorn 前加载根目录 `.env`，以便 RabbitMQ 和 LLM 设置与 Compose 环境匹配。


### 4. 验证服务

| 服务        | 地址                        | 说明                          |
|-------------|-----------------------------|-------------------------------|
| 前端界面    | http://localhost            | 主入口（React 应用）            |
| 后端 API    | http://localhost/api        | 通过 Nginx 的 REST 端点       |
| 系统健康    | http://localhost/health     | 全局健康检查                   |

*注意：在三层网络架构中，仅将配置的前端端口暴露给主机。后端、AI、RabbitMQ、Redis 和数据库服务通过 Docker 网络安全隔离。*

*注意：如果在 `.env` 中更改了 `FRONTEND_HOST_PORT`，请将 `http://localhost` 替换为 `http://localhost:${FRONTEND_HOST_PORT}`。*

### 5. 停止服务

```bash
docker compose --env-file .env down

# 删除数据卷（警告：数据将丢失）
docker compose --env-file .env down -v
```

## 测试

本项目包含后端 JUnit 测试、前端 Vitest 测试和 AI 服务 pytest 测试，覆盖 API、领域、持久化、认证、UI 工具、AI 服务和消息队列逻辑。

### 后端测试（Java）

运行 Spring Boot 后端的单元和集成测试：

```bash
cd backend
mvn test
```

部分后端集成测试使用 Testcontainers，需要可用的 Docker 环境。

### 前端测试（TypeScript）

运行 lint、单元测试、覆盖率检查和生产构建检查：

```bash
cd frontend
npm install
npm run lint
npm run test:run
npm run test:coverage
npm run build
```

### AI 服务测试（Python）

运行 FastAPI 和 AI 逻辑的 pytest：

```bash
cd ai-service
pip install -r requirements.txt
pytest
```

## AI 评估套件

针对固定基准用例评估 AI 组件：

```bash
cd eval
# 安装评估依赖
pip install -r requirements.txt
# 先加载根目录 .env，以便 LiteLLM 提供商设置可用
# 运行当前评估流水线：
# 简历解析、职位解析和单职位适合度评分
python run_eval.py

# 可选：同时运行旧版职位排序 NDCG@5 评估
python run_eval.py --include-legacy-ranking
```

*评估结果将导出至 `eval/results/`。*

## 开发指南

### 前端开发

```bash
cd frontend
npm install
npm run dev
```

开发服务器将在 <http://localhost:5173> 启动。

### 后端开发

前置要求：

- JDK 21
- Maven 3.9+

```bash
cd backend

# 构建所有模块
mvn clean install

# 使用 dev 配置文件运行（默认）
mvn spring-boot:run -pl app

# 使用 prod 配置文件运行
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=prod
```

### AI 服务开发

前置要求：

- Python 3.11+
- pip 或 poetry
- 在项目根目录 `.env` 文件中配置的 LiteLLM 兼容提供商密钥

```bash
cd ai-service

# 创建虚拟环境
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 加载根目录环境变量
set -a
source ../.env
set +a

# 运行开发服务器
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

除非您有意将 LiteLLM 配置为使用 Vertex AI 模型，否则本地开发不需要 Google Cloud ADC。


## 部署

详细部署说明请参见 [docs/deployment/DOCKER_DEPLOY.md](docs/deployment/DOCKER_DEPLOY.md)，内容包括：

- 生产环境部署检查清单
- 环境配置
- SSL/TLS 设置
- 监控和日志
- 备份策略

## 技术栈

### 前端

- React 19
- Vite 7
- TypeScript 5.9
- React Router 7
- Axios
- Zustand

### 后端

- Java 21
- Spring Boot 3.5.7
- PostgreSQL 15 + pgvector
- RabbitMQ 3
- Maven 3.9+

### AI 服务

- Python 3.11
- FastAPI 0.115
- LiteLLM 1.61.11 兼容的文本、视觉和嵌入模型
- 默认通过 Google AI Studio 使用 Gemini；Vertex AI 为可选项
- Uvicorn 0.32

### 运维

- Docker & Docker Compose
- Nginx
- Redis 7（分布式状态、锁、Pub/Sub）
- Flyway（数据库迁移）

## 贡献指南

1. Fork 本仓库
2. 创建功能分支（`git checkout -b feature/amazing-feature`）
3. 提交更改（`git commit -m '添加新功能'`）
4. 推送到分支（`git push origin feature/amazing-feature`）
5. 创建 Pull Request

## 许可证

本项目基于 MIT 许可证发布。详情请参见 [LICENSE](../../../LICENSE)。

## 致谢

感谢支持 JobCopilot 的开源项目、贡献者和使用者。
