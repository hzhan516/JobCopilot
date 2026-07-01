# =============================================================================
# Python AI 服务 Dockerfile (优化版)
# 框架: FastAPI + Uvicorn
# =============================================================================

# -----------------------------------------------------------------------------
# 阶段 1: 构建阶段 (Builder) - 包含所有编译所需的重型工具
# -----------------------------------------------------------------------------
FROM docker.io/library/python:3.11-slim AS builder

WORKDIR /build

# 安装编译所需的依赖 (gcc, libpq-dev 等编译依赖不需要进入最终镜像)
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# 创建虚拟环境，避免污染系统 Python 环境
RUN python -m venv /opt/venv
# 将虚拟环境路径加入环境变量
ENV PATH="/opt/venv/bin:$PATH"

COPY requirements.txt .
# 升级 pip 并安装依赖 (利用轮子缓存)
RUN pip install --no-cache-dir --upgrade pip wheel setuptools && \
    pip install --no-cache-dir -r requirements.txt

# -----------------------------------------------------------------------------
# 阶段 2: 运行阶段 (Runtime) - 极其精简的最终运行镜像
# -----------------------------------------------------------------------------
FROM docker.io/library/python:3.11-slim

# 版本构建参数 / Version build argument
ARG APP_VERSION=dev
LABEL maintainer="JobCopilot Labs"
LABEL description="JobCopilot Python AI Service"
LABEL org.opencontainers.image.version="${APP_VERSION}"
ENV APP_VERSION=${APP_VERSION}

# 环境变量设置
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/opt/venv/bin:$PATH" \
    HF_HOME="/app/models_cache"

# 仅安装运行所需的轻量级依赖 (使用 libpq5 代替 libpq-dev)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    libgomp1 \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 从构建阶段复制已经编译好的虚拟环境
COPY --from=builder /opt/venv /opt/venv

# 复制业务代码
COPY app/ ./app/

# 创建必要目录并设置非 root 用户
RUN mkdir -p /app/uploads /app/model-artifacts /app/models_cache && \
    adduser --disabled-password --gecos '' appuser && \
    chown -R appuser:appuser /app /opt/venv

# 切换为非 root 用户运行
USER appuser

EXPOSE 8000

# 添加健康检查，帮助 Docker/K8s 判断服务是否真正就绪
# (请确保你的 FastAPI 代码里有一个 /health 路由，返回 {"status": "ok"})
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# 启动命令 (开发/单核模式)
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--proxy-headers"]
