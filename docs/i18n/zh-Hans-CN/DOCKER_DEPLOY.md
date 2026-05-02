<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../DOCKER_DEPLOY.md) | [简体中文](DOCKER_DEPLOY.md) | [繁體中文](../zh-Hant-TW/DOCKER_DEPLOY.md)

# Docker/Podman 部署指南

## 环境要求

- **Docker**: 20.10+ 或 **Podman**: 4.0+
- **Docker Compose**: 2.0+ 或 **podman-compose**: 1.0+
- 内存：至少 4GB 可用内存
- 磁盘：至少 10GB 可用空间

## 快速开始

### 1. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，填入必要的配置
vim .env
```

必需配置项：

- `JWT_SECRET`: JWT 签名密钥（生产环境必须修改）
- 一个兼容 LiteLLM 的模型服务密钥，例如 `GEMINI_API_KEY`、`OPENAI_API_KEY`、`ANTHROPIC_API_KEY` 或 `GROQ_API_KEY`
- `LLM_TEXT_MODEL`、`LLM_VISION_MODEL` 和 `LLM_EMBEDDING_MODEL`: 与所选模型服务前缀匹配的模型名称

默认情况下，项目可以通过 LiteLLM 使用 Gemini 模型，因此本地开发只需要配置 `GEMINI_API_KEY`，除非您选择其他模型服务。

Google Cloud ADC 是可选项。只有在您主动将 LiteLLM 配置为使用 Vertex AI 模型时才需要。

### 2. 启动服务

#### 使用 Docker

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 停止并删除数据卷（谨慎使用）
docker-compose down -v
```

#### 使用 Podman

```bash
# 方法 1: 使用 podman-compose
podman-compose up -d

# 方法 2: 使用 podman 原生 compose（Podman 3.0+）
podman compose up -d

# 查看日志
podman-compose logs -f

# 停止服务
podman-compose down
```

### 3. 验证服务状态

```bash
# 查看所有容器状态
docker-compose ps
# 或
podman-compose ps

# 健康检查
curl http://localhost:8080/api/actuator/health
curl http://localhost:8000/health
curl http://localhost/health
```

## 服务访问地址

| 服务          | 地址                                   | 说明          |
|-------------|--------------------------------------|-------------|
| 前端界面        | http://localhost                     | 求职者界面       |
| 后端 API      | http://localhost:8080/api            | REST API    |
| AI 服务       | http://localhost:8000                | FastAPI 文档  |
| RabbitMQ 管理 | http://localhost:15672               | guest/guest |
| H2 控制台      | http://localhost:8080/api/h2-console | 开发环境        |

## 常用命令

### 查看日志

```bash
# 所有服务日志
docker-compose logs -f

# 特定服务日志
docker-compose logs -f backend
docker-compose logs -f ai-service
docker-compose logs -f postgres
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart backend
```

### 重建服务

```bash
# 重新构建并启动（代码更新后）
docker-compose up -d --build

# 仅重建特定服务
docker-compose up -d --build backend
```

### 进入容器

```bash
# 进入后端容器
docker-compose exec backend sh

# 进入数据库容器
docker-compose exec postgres psql -U resume_user -d resume_assistant
```

## 数据持久化

数据通过 Docker 卷持久化存储：

- `postgres-data`: PostgreSQL 数据库数据
- `rabbitmq-data`: RabbitMQ 消息队列数据
- `shared-storage`: 上传的简历文件（后端和 AI 服务共享）

```bash
# 查看卷
docker volume ls

# 备份数据（PostgreSQL）
docker-compose exec postgres pg_dump -U resume_user resume_assistant > backup.sql

# 恢复数据
docker-compose exec -T postgres psql -U resume_user resume_assistant < backup.sql
```

## 故障排查

### 端口冲突

如果启动时报端口冲突错误，修改 `docker-compose.yml` 中的端口映射：

```yaml
ports:
  - "8081:8080"  # 将主机端口改为 8081
```

### 内存不足

如果容器频繁重启，可能是内存不足：

```bash
# 查看容器资源使用
docker stats

# 增加 Docker 内存限制（Docker Desktop）
```

### 清理构建缓存

```bash
# 清理未使用的镜像、容器、卷
docker system prune -a --volumes

# 重新构建
docker-compose up -d --build --force-recreate
```

## 生产环境部署

1. 修改 `.env` 文件：
   ```
   SPRING_PROFILES_ACTIVE=prod
   JWT_SECRET=<强密钥>
   POSTGRES_PASSWORD=<强密码>
   RABBITMQ_PASS=<强密码>
   ```

2. 使用生产环境配置：
   ```bash
   docker-compose -f docker-compose.yml.example -f docker-compose.prod.yml up -d
   ```

3. 配置反向代理（Nginx/Traefik）

4. 启用 HTTPS（Let's Encrypt）

## 注意事项

1. **Podman 用户**：
    - 确保安装 `podman-compose`
    - 或使用 `podman compose`（Podman 3.0+ 原生支持）
    - 如果遇到权限问题，检查 SELinux 设置

2. **Windows 用户**：
    - 使用 WSL2 运行 Docker
    - 文件共享性能可能较慢

3. **Mac 用户**：
    - Docker Desktop 内存默认 2GB，建议增加到 4GB+
