<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../deployment/DOCKER_DEPLOY.md) | [简体中文](DOCKER_DEPLOY.md) | [繁體中文](../zh-Hant-TW/deployment/DOCKER_DEPLOY.md)

# Docker/Podman 部署指南

## 环境要求

- **Docker**: 20.10+ 或 **Podman**: 4.0+
- **Docker Compose**: 2.0+ 或 **podman-compose**: 1.0+
- 内存：至少 4GB 可用内存
- 磁盘：至少 10GB 可用空间

## 快速开始

### 1. 配置环境变量

**推荐方式：使用网页配置工具**

直接用浏览器打开 `docs/deployment/env-setup.html`（无需启动服务器）：

1. 在页面右上角选择语言（EN / 简体中文 / 繁體中文）
2. 填写必填项（标记为红色星号 *）
3. 对密钥类变量（如 `JWT_SECRET`、`INTERNAL_API_KEY`）点击**生成**按钮以获取安全随机值
4. 查看顶部进度条，确保所有必填变量已完成
5. 点击**下载 .env**，保存到项目根目录

**备选方式（命令行）：**

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
- `LLM_EMBEDDING_MODEL_DIMENSION`: 嵌入模型输出维度（必须与所选模型一致，默认 1536）
- `CAPTCHA_ENABLED`: 是否启用 CAPTCHA 验证（`true`/`false`，默认 `true`）
- `CAPTCHA_TOLERANCE`: 滑动容差像素（默认 `8`）
- `CAPTCHA_MAX_ATTEMPTS`: 每个挑战最大验证次数（默认 `5`）
- `CAPTCHA_TOKEN_EXPIRY`: Token 缓存 TTL，单位秒（默认 `300`）

默认情况下，项目可以通过 LiteLLM 使用 Gemini 模型，因此本地开发只需要配置 `GEMINI_API_KEY`，除非您选择其他模型服务。

Google Cloud ADC 是可选项。只有在您主动将 LiteLLM 配置为使用 Vertex AI 模型时才需要。
注：如果您在系统运行时修改了 .env 中的 LLM 提供商、模型或维度，您必须执行 "docker compose up -d" 以应用新的环境变量。单纯的重启服务将不会生效。

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

# 通过公开 Nginx 入口进行健康检查
curl http://localhost/health
curl http://localhost/api/actuator/health

# AI 服务默认仅在 Docker 内网暴露；从容器内部检查
docker compose exec ai-service python -c "import urllib.request; print(urllib.request.urlopen('http://localhost:8000/health').read().decode())"
```

## 服务访问地址

| 服务     | 地址               | 说明                      |
|--------|------------------|-------------------------|
| 前端界面   | http://localhost | 统一入口 (包含 Web 与 API 代理) |
| 系统健康   | http://localhost/health | 整体服务状态                |

## 服务职责

### AI 服务（Python FastAPI）

除简历/职位解析、嵌入生成、排序和对话外，AI 服务现在还包含**增量模型训练闭环**：

1. **职位数据集同步**：职位成功解析后，后端将其写入 `job_dataset` 表（训练语料库）。
2. **评分标签消费**：用户对职位进行评分时，后端将评分标签消息发送到 `ai.queue.feedback` 队列。
3. **反馈缓冲**：AI worker 将反馈转换为带标签的特征样本，并写入 Redis 缓冲区。
4. **LightGBM 重新训练**：当样本达到阈值（`MIN_SAMPLES_FOR_RETRAIN=10`）时，worker 将缓冲反馈与 baseline features 合并，训练 LightGBM ranker，并将 `ranker_model_<version>.txt` 和 `latest_meta.json` 等版本化 artifact 写入 MinIO。
5. **热加载**：model manager 从 MinIO 加载最新模型，并在 worker 发布 Redis `ai.model.reload` 通知时重新加载。

`POST /api/v1/admin/recompute-model` 仅保留兼容性并返回弃用提示；定时重新训练由 `ai-worker` 负责。

## 常用命令

### 查看日志

```bash
# 所有服务日志
docker-compose logs -f

# 特定服务日志
docker-compose logs -f backend
docker-compose logs -f ai-service
docker-compose logs -f postgres
docker-compose logs -f redis
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
- `redis-data`: Redis 缓存与状态数据
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

### CAPTCHA 速率限制触发

**现象**：请求 CAPTCHA 挑战时返回 `429 Too Many Requests`。

**原因**：同一 IP 在 1 分钟内超过 20 次 CAPTCHA 请求。

**解决**：等待 1 分钟让速率限制缓存过期，或在 `.env` 中设置 `CAPTCHA_ENABLED=false` 以在本地测试时禁用 CAPTCHA。

### 406 PRECONDITION_FAILED（RabbitMQ）

**现象**：后端日志出现 `channel.close(reply-code=406, reply-text=PRECONDITION_FAILED - inequivalent arg 'x-dead-letter-exchange'...)`。

**原因**：RabbitMQ 中已存在同名队列，但参数不同。队列参数不可变。

**解决**：运行队列重置脚本并重启后端：
```bash
cd backend && ./scripts/reset-rabbitmq-queues.sh
cd .. && docker-compose up -d --build backend
```

或彻底重建 RabbitMQ（删除卷）：
```bash
docker-compose down -v
docker-compose up -d
```

### "Relation does not exist"（PostgreSQL）

**现象**：后端日志出现 `ERROR: relation "outbox_message" does not exist`。

**原因**：`outbox_message` 表在数据库初始化完成后才添加。`docker-entrypoint-initdb.d` 仅在首次初始化时执行。

**解决**：手动执行初始化 SQL：
```bash
docker exec -i resume-assistant-postgres \
  psql -U resume_user -d resume_assistant \
  < backend/app/src/main/resources/db/migration/init_outbox_message.sql
```

或重建数据库卷：
```bash
docker-compose down -v
docker-compose up -d
```

### `FATAL: database "resume_assistant" does not exist`（PostgreSQL）

**现象**：后端或 PostgreSQL 健康检查报错 `database "resume_assistant" does not exist` 或 `role "resume_user" does not exist`。

**原因**：`postgres-data` 数据卷已被初始化过（例如使用默认的 `postgres` 凭据或来自之前的项目）。PostgreSQL 的 `docker-entrypoint-initdb.d` 以及 `POSTGRES_USER`/`POSTGRES_DB` 环境变量仅在数据目录为空时的**首次**初始化生效。

**解决**：删除数据卷并重新初始化：
```bash
docker-compose down
docker volume rm <project_name>_postgres-data
docker-compose up -d
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

## 队列参数变更与重置

RabbitMQ 队列一旦创建，其参数不可变。如果应用代码变更了队列声明（例如添加 `x-dead-letter-exchange` 以支持 DLX/DLQ），Spring AMQP 在尝试重新声明已有队列时会报错 `406 PRECONDITION_FAILED`。

### 开发环境

删除旧队列，让 Spring AMQP 在下次启动时自动重新声明：

```bash
cd backend
./scripts/reset-rabbitmq-queues.sh
```

然后重启后端容器：

```bash
docker-compose up -d --build backend
```

### 生产环境

建议安排在维护窗口执行：
1. 停止生产者（backend），防止新消息进入。
2. 排空或备份受影响队列中的消息。
3. 通过 RabbitMQ Management UI 或 CLI 删除旧队列。
4. 重启后端服务，Spring AMQP 将使用新参数声明队列。

如果消息丢失可接受，也可以直接使用 `reset-rabbitmq-queues.sh` 脚本。

## 数据库初始化与迁移

开发环境禁用了 Flyway（`application-dev.yml` 中 `spring.flyway.enabled=false`）。数据库初始化依赖于 `docker-entrypoint-initdb.d`，该机制**仅在数据库首次初始化**（数据目录为空）时执行一次。

### 全新环境

使用全新的 `postgres-data` 卷启动时，`docker-entrypoint-initdb.d` 会按字母顺序执行 `backend/app/src/main/resources/db/migration/` 目录下的所有 `.sql` 文件。新增的数据表（如 `outbox_message`）会自动创建。

### 已有环境

如果数据库已经初始化过，新增 `.sql` 文件**不会**自动执行。有两种处理方式：

1. **手动执行 SQL**（推荐用于有数据的开发环境）：
   ```bash
   docker exec -i resume-assistant-postgres \
     psql -U resume_user -d resume_assistant \
     < backend/app/src/main/resources/db/migration/init_outbox_message.sql
   ```

2. **重建数据卷**（会清空所有数据）：
   ```bash
   docker-compose down -v
   docker-compose up -d
   ```

> **生产环境说明**：生产环境使用 `spring.flyway.enabled=true`（见 `application-prod.yml`）。Flyway 会在启动时自动应用待执行的迁移脚本，因此新增表会自动处理。

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
