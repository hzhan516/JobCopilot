[中文文档](./README.zh.md) | [English Document](../README.md)

# 智能求职助手

一个由AI驱动的智能求职助手，帮助求职者优化简历、匹配合适的职位并追踪申请进度。

## 功能特性

- **简历管理**：上传、解析和管理多种格式的简历
- **AI智能解析**：使用OpenAI从简历中提取结构化信息
- **职位匹配**：基于简历内容和向量相似度的智能职位推荐
- **申请追踪**：追踪求职申请状态并管理求职流程
- **AI对话**：交互式聊天助手，提供求职建议和简历优化指导
- **向量搜索**：基于PostgreSQL pgvector扩展的语义搜索

## 系统架构

本项目采用微服务架构，包含以下组件：

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│     前端    │──────▶│     后端    │◀─────▶│    AI      │
│   (React)   │      │ (Spring    │      │ (FastAPI)  │
│             │      │   Boot)     │      │            │
└─────────────┘      └──────┬──────┘      └──────▲──────┘
                            │                      │
                            ▼                      │
                     ┌─────────────┐               │
                     │  PostgreSQL │               │
                     │  + pgvector │───────────────┘
                     └─────────────┘      (消息队列)
                            ▲
                            │
                     ┌─────────────┐
                     │   RabbitMQ  │
                     └─────────────┘
```

| 服务 | 技术栈 | 端口 | 说明 |
|---------|------------|------|-------------|
| 前端 | React 18 + Vite | 80 | Nginx托管的Web界面 |
| 后端 | Java 21 + Spring Boot 3.5 | 8080 | REST API和业务逻辑 |
| AI服务 | Python 3 + FastAPI | 8000 | AI处理和OpenAI集成 |
| 数据库 | PostgreSQL 15 + pgvector | 5432 | 业务数据和向量存储 |
| 消息队列 | RabbitMQ 3 | 5672 / 15672 | 异步消息处理 |

## 项目结构

```
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
│   ├── app/              # FastAPI应用
│   ├── requirements.txt  # Python依赖
│   └── Dockerfile        # AI服务Docker镜像
├── docs/                 # 文档
├── docker-compose.yml    # Docker Compose配置
└── .env.example          # 环境变量模板
```

## 后端架构

后端采用**六边形架构 / 领域驱动设计（DDD）**，包含以下分层模块：

| 模块 | 说明 | 依赖 |
|--------|-------------|--------------|
| `types` | 基础类型、枚举、常量 | 无 |
| `domain` | 领域实体、服务、仓储接口 | `types` |
| `api` | DTO、外观接口 | `domain`、`types` |
| `infrastructure` | 数据库、缓存、消息队列实现 | `domain`、`api` |
| `trigger` | 控制器、定时任务、事件监听器 | `domain`、`api` |
| `app` | Spring Boot启动和配置 | 所有模块 |

## 快速开始

### 环境要求

- Docker 20.10+ 和 Docker Compose 2.0+
- 或 Podman 和 podman-compose
- OpenAI API 密钥

### 1. 克隆仓库

```bash
git clone <仓库地址>
cd resume-assistant
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑.env文件，填入你的OpenAI API密钥
```

必需的环境变量：

| 变量 | 必需 | 说明 |
|----------|----------|-------------|
| `OPENAI_API_KEY` | 是 | 你的OpenAI API密钥 |
| `JWT_SECRET` | 是 | JWT令牌生成的密钥（至少32个字符） |
| `SPRING_PROFILES_ACTIVE` | 否 | Spring配置文件：`dev`（默认）或`prod` |
| `LOG_LEVEL` | 否 | AI服务日志级别：`INFO`（默认）或`DEBUG` |

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

### 4. 验证服务

| 服务 | 地址 | 说明 |
|---------|-----|-------------|
| 前端 | http://localhost | Web应用 |
| 后端API | http://localhost:8080/api | REST API端点 |
| 后端健康检查 | http://localhost:8080/actuator/health | 健康检查 |
| AI服务 | http://localhost:8000 | FastAPI文档 |
| RabbitMQ管理 | http://localhost:15672 | 消息队列管理界面（guest/guest） |

### 5. 停止服务

```bash
docker-compose down

# 删除数据卷（注意：数据将丢失）
docker-compose down -v
```

## 开发指南

### 前端开发

```bash
cd frontend
npm install
npm run dev
```

开发服务器将在 http://localhost:5173 启动

### 后端开发

环境要求：
- JDK 21
- Maven 3.9+

```bash
cd backend

# 构建所有模块
mvn clean install

# 使用dev配置运行（默认）
mvn spring-boot:run -pl app

# 使用prod配置运行
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=prod
```

### AI服务开发

环境要求：
- Python 3.11+
- pip 或 poetry

```bash
cd ai-service

# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 运行开发服务器
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## 部署

详细部署说明请参考 [DOCKER_DEPLOY.md](../DOCKER_DEPLOY.md)，包括：

- 生产部署检查清单
- 环境配置
- SSL/TLS设置
- 监控和日志
- 备份策略

## 技术栈

### 前端
- React 18.2
- Vite 5.0
- React Router 6
- Axios

### 后端
- Java 21
- Spring Boot 3.5.7
- PostgreSQL 15 + pgvector
- RabbitMQ 3
- Maven 3.9+

### AI服务
- Python 3.11
- FastAPI
- OpenAI API
- Uvicorn

### 运维
- Docker & Docker Compose
- Nginx
- Flyway（数据库迁移）

## 贡献指南

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m '添加新功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

本项目为亚利桑那州立大学（SER594课程）学术目的开发。

## 致谢

- 亚利桑那州立大学
- SER594课程团队
