# =============================================================================
# Python AI服务 Dockerfile
# 基于FastAPI + Uvicorn
# =============================================================================

FROM python:3.11-slim

# 标签信息
LABEL maintainer="SER594 Team"
LABEL description="智能求职助手 - Python AI服务"

# 设置环境变量
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libpq-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 复制依赖文件（利用缓存层）
COPY requirements.txt .

# 安装 Python 依赖
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY app/ ./app/

# 创建上传目录（与后端共享）
RUN mkdir -p /app/uploads

# 暴露端口
EXPOSE 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# 启动命令（Uvicorn单进程模式，适合容器）
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
