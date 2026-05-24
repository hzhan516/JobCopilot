<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../README.md) | [简体中文](README.md) | [繁體中文](../zh-Hant-TW/README.md)

# 智能求职助手 (JobCopilot)

**智能求职助手**是一个 AI 驱动的平台，面向应届毕业生和转行者。它解析上传的简历，通过语义向量匹配评估与就业市场数据的契合度，并提供交互式 AI 助手迭代改进简历内容。安全的文件管理、异步 AI 处理和个性化推荐帮助用户节省时间、提高面试率。

**部署地址：** [待定 - 将在实现完成后更新]

## 团队成员

- **Guixing Jia** (@GuixingJia) - 项目经理 (PM)，Python AI 服务 & 前端开发
- **Hansheng Zhang** (@hzhan516) - Java 后端 & 数据库架构负责人
- **Mu-Hsi Yu** (@mhsiy) - 前端 & UX 设计负责人，Python AI 服务

## 功能特性

- **身份认证**：邮箱/密码注册（支持可选邮件验证码验证）与 Google OAuth 2.0 登录，带滑块 CAPTCHA 人机验证保护
- **简历管理**：上传、解析和管理多种格式的简历
- **AI智能解析**：使用 LiteLLM 兼容模型从简历和职位中提取结构化信息
- **职位匹配**：基于简历内容和向量相似度的智能职位推荐
- **申请追踪**：追踪求职申请状态并管理求职流程
- **AI对话**：交互式聊天助手，提供求职建议和简历优化指导
- **国际化**：支持英文、简体中文和繁体中文界面
- **向量搜索**：基于 PostgreSQL pgvector 扩展的语义搜索
- **增量式职位训练闭环**：用户评分行为通过增量学习反馈到 AI 基线模型，无需全量重训练即可持续提升匹配准确度

## 系统架构

本项目采用微服务架构，包含以下组件：

```text
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│     前端    │──────▶│     后端    │◀─────▶│   AI API   │
│   (React)   │      │ (Spring    │      │ (FastAPI)  │
│             │      │   Boot)     │      │            │
└─────────────┘      └──────┬──────┘      └──────▲──────┘
                            │      ┌─────────┐   │
                            │◀────▶│  Redis  │   │
                            │      │  :6379  │◀──┘
                            │      └─────────┘   │
                            ▼                      │
                     ┌─────────────┐               │
                     │  PostgreSQL │               │
                     │  + pgvector │───────────────┘
                     └─────────────┘      (消息队列)
                            ▲
                            │
                     ┌─────────────┐      ┌─────────────┐
                     │   RabbitMQ  │─────▶│  AI Worker  │
                     └─────────────┘      │ (LightGBM)  │
                                          └──────┬──────┘
                                                 │
                                          ┌──────▼──────┐
                                          │    MinIO    │
                                          │ (模型注册表)│
                                          └─────────────┘
```

| 服务   | 技术栈                       | 端口           | 说明            |
|------|---------------------------|--------------|---------------|
| 前端   | React 19 + Vite 7         | `${FRONTEND_HOST_PORT:-80}` -> 8080 | Nginx 托管的 Web 界面与反向代理 |
| 后端   | Java 21 + Spring Boot 3.5 | 8080 internal | REST API、业务逻辑及滑块 CAPTCHA 人机验证 |
| AI API | Python 3 + FastAPI + LiteLLM | 8000 internal | 无状态 API：AI 处理、嵌入生成、排序、对话 |
| AI Worker | Python 3 + LightGBM | 无 | 有状态后台工作节点：消费 `ai.queue.feedback` 进行增量模型训练 |
| 模型注册表 | MinIO | 9000 internal | 存储训练好的 LightGBM 模型 |
| 数据库  | PostgreSQL 15 + pgvector  | 5432 internal | 业务数据和向量存储     |
| 消息队列 | RabbitMQ 3                | 5672 internal | 异步消息处理        |
| 缓存     | Redis 7                   | 6379         | 分布式状态、锁、Pub/Sub |

## 项目结构

```text
.
├── frontend/              # React前端应用
│   ├── src/              # 源代码
│   ├── package.json      # Node.js依赖
│   └── Dockerfile        # 前端Docker镜像
├── backend/              # Java Spring Boot后端
│   ├── app/              # 应用入口
│   ├── api/              # API层（DTO、外观接口）
│   ├── domain/           # 领域层（业务逻辑）
│   ├── infrastructure/   # 基础设施层（数据库、缓存、消息队列）
│   ├── trigger/          # 触发器层（控制器、定时任务、监听器）
│   └── types/            # 共享类型和常量
├── ai-service/           # Python AI服务
│   ├── app/              # AI服务源代码 (DDD架构)
│   │   ├── api/          # API层 (FastAPI路由)
│   │   ├── domain/       # 领域层 (业务逻辑)
│   │   ├── infrastructure/ # 基础设施层 (MinIO, Redis, RabbitMQ)
│   │   ├── worker/       # Worker层 (LightGBM训练逻辑)
│   │   ├── main.py       # AI API 启动入口
│   │   └── worker_main.py # AI Worker 启动入口
│   ├── requirements.txt  # Python依赖
│   └── Dockerfile        # AI服务Docker镜像
├── docs/                 # 项目文档
├── eval/                 # AI评估脚本
├── tests/                # 测试脚本
├── docker-compose.yml    # Docker Compose配置
└── .env.example          # 环境变量模板
```

## 后端架构

后端采用**六边形架构 / 领域驱动设计（DDD）**，包含以下分层模块：

| 模块               | 说明               | 依赖               |
|------------------|------------------|------------------|
| `types`          | 基础类型、枚举、常量       | 无                |
| `domain`         | 领域实体、服务、仓储接口     | `types`          |
| `api`            | DTO、外观接口         | `domain`、`types` |
| `infrastructure` | 数据库、缓存、消息队列实现    | `domain`、`api`   |
| `trigger`        | 控制器、定时任务、事件监听器   | `domain`、`api`   |
| `app`            | Spring Boot启动和配置 | 所有模块             |

## 快速开始

### 环境要求

- Docker 20.10+ 和 Docker Compose 2.0+
- 或带有 podman-compose 的 Podman
- 一个用于本地 AI 功能的 LiteLLM 兼容模型服务密钥，例如 Gemini、OpenAI、Anthropic 或 Groq
- Google Cloud / Vertex AI 是可选项，本地开发不需要强制配置

### 1. 克隆仓库

```bash
git clone <仓库地址>
cd JobCopilot
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，填入必要的配置
```

必需的环境变量：

| 变量                     | 必需 | 说明 |
|--------------------------|------|------|
| `JWT_SECRET`             | 是   | JWT token 生成密钥（至少 32 个字符） |
| `GEMINI_API_KEY`         | 可选 | 当 `LLM_*_MODEL` 使用 `gemini/` 前缀时使用的 Gemini API 密钥 |
| `OPENAI_API_KEY`         | 可选 | 当 `LLM_*_MODEL` 使用 `openai/` 前缀时使用的 OpenAI API 密钥 |
| `ANTHROPIC_API_KEY`      | 可选 | 当 `LLM_*_MODEL` 使用 `anthropic/` 前缀时使用的 Anthropic API 密钥 |
| `GROQ_API_KEY`           | 可选 | 当 `LLM_*_MODEL` 使用 `groq/` 前缀时使用的 Groq API 密钥 |
| `LLM_TEXT_MODEL`         | 是   | LiteLLM 文本模型名称，例如 `gemini/gemini-2.5-flash` |
| `LLM_VISION_MODEL`       | 是   | LiteLLM 视觉模型名称 |
| `LLM_EMBEDDING_MODEL`    | 是   | LiteLLM 嵌入模型名称 |
| `LLM_EMBEDDING_MODEL_DIMENSION` | 是   | 嵌入模型输出维度（必须与所选模型一致） |
| `SPRING_PROFILES_ACTIVE` | 否   | Spring profile：`dev`（默认）或 `prod` |
| `LOG_LEVEL`              | 否   | AI service 日志级别：`INFO`（默认）或 `DEBUG` |
| `CAPTCHA_ENABLED`        | 否   | 是否启用滑块 CAPTCHA。默认：`true` |
| `CAPTCHA_TOLERANCE`      | 否   | CAPTCHA 拖动容差（像素）。默认：`8` |
| `CAPTCHA_TOKEN_EXPIRY`   | 否   | CAPTCHA token 过期时间（秒）。默认：`300` |
| `CAPTCHA_TRACK_WIDTH`    | 否   | CAPTCHA 滑轨宽度（像素）。默认：`300` |
| `REDIS_HOST`             | 否   | Redis 主机名。默认：`redis`（Docker）或 `localhost` |
| `REDIS_PORT`             | 否   | Redis 端口。默认：`6379` |
| `REDIS_PASSWORD`         | 否   | Redis 认证密码。开发环境可留空 |
| `CAPTCHA_MAX_ATTEMPTS`   | 否   | 每 IP 最大 CAPTCHA 尝试次数。默认：`5` |

本地开发时，请将 `.env.example` 复制为 `.env`，并提供一个与所选 LiteLLM 模型前缀匹配的 API key。例如，默认 Gemini 模型使用 `GEMINI_API_KEY`。

只有在您主动将项目配置为使用 Vertex AI 模型时，才需要 Google Cloud ADC。
注：如果您在系统运行时修改了 .env 中的 LLM 提供商、模型或维度，您必须执行 "docker compose up -d" 以应用新的环境变量。单纯的重启服务将不会生效。


### 3. 启动所有服务

使用 Docker Compose：

```bash
docker-compose up -d
```

使用 Podman：

```bash
podman-compose up -d
# 或
podman compose up -d
```

### 4. 验证服务状态

| 服务     | 地址               | 说明                  |
|--------|------------------|---------------------|
| 前端界面   | http://localhost | 唯一访问入口 (包含 Web 与 API) |
| 系统健康   | http://localhost/health | 整体健康状态探测端点          |

*注：本项目采用三层网络隔离架构，仅对外暴露前端 80 端口，底层服务（后端、AI、RabbitMQ、Redis、数据库）均不可从宿主机直接访问。*

*注：要查找 AI 服务的 URL，请运行 `docker compose port ai-service 8000`。*

### 5. 停止服务

```bash
docker-compose down

# 删除数据卷（警告：所有数据库数据将丢失！）
docker-compose down -v
```

## 测试

本项目在所有模块中维护了严格的测试套件（代码覆盖率 `> 80%`），以确保系统可靠性。

### 后端测试 (Java)

运行 Spring Boot 后端的单元和集成测试：

```bash
cd backend
mvn test
```

### AI 服务测试 (Python)

运行 FastAPI 和 AI 业务逻辑的 pytest 测试：

```bash
cd ai-service
pip install -r requirements.txt
pytest
```

## AI 评估套件

为了评估 AI 组件（如提取模块的 F1 分数和推荐系统的 NDCG@5）与基准模型的对比表现：

```bash
cd eval
# 安装评估脚本依赖
pip install -r requirements.txt
# 运行评估流水线
python run_eval.py
```

*评估结果将自动导出至 `eval/results/` 目录。*

## 开发指南

### 前端开发

```bash
cd frontend
npm install
npm run dev
```

开发服务器将在 <http://localhost:5173> 启动。

### 后端开发

环境要求：

- JDK 21
- Maven 3.9+

```bash
cd backend

# 构建所有模块
mvn clean install

# 使用 dev 配置运行（默认）
mvn spring-boot:run -pl app

# 使用 prod 配置运行
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=prod
```

### AI 服务开发

环境要求：

- Python 3.11+
- pip 或 poetry
- 已在项目根目录 `.env` 文件中配置兼容 LiteLLM 的模型服务密钥

```bash
cd ai-service

# 创建虚拟环境
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 加载项目根目录环境变量
set -a
source ../.env
set +a

# 运行开发服务器
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

本地开发不需要 Google Cloud ADC，除非您主动将 LiteLLM 配置为使用 Vertex AI 模型。


## 部署上线

详细的部署说明请参考 [deployment/DOCKER_DEPLOY.md](deployment/DOCKER_DEPLOY.md)，内容包括：

- 生产环境部署检查清单
- 环境变量配置
- SSL/TLS 设置
- 监控与日志记录
- 数据备份策略

## 技术栈

### 前端

- React 19.2
- Vite 7
- React Router 7
- Axios

### 后端

- Java 21
- Spring Boot 3.5.7
- PostgreSQL 15 + pgvector
- RabbitMQ 3
- Maven 3.9+

### AI服务

- Python 3.11
- FastAPI 0.115
- LiteLLM 1.61.11 兼容的文本、视觉与嵌入模型
- 默认通过 Google AI Studio 使用 Gemini；Vertex AI 为可选项
- Uvicorn 0.32

### 运维

- Docker & Docker Compose
- Nginx
- Redis 7（分布式状态、锁、Pub/Sub）
- Flyway（数据库迁移）

## 贡献指南

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m '添加新功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

本项目为亚利桑那州立大学（JobCopilot Open Source, JobCopilot课程）学术目的开发。

## 致谢

- 亚利桑那州立大学
- JobCopilot 课程教学团队
