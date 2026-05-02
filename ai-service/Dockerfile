# =============================================================================
# Python AI服务 Dockerfile
# 基于FastAPI + Uvicorn
# =============================================================================

FROM docker.io/library/python:3.11-slim

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

# 创建非 root 用户并修改目录权限
RUN adduser --disabled-password --gecos '' appuser && \
    chown -R appuser:appuser /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8000

# 启动命令（Uvicorn单进程模式，适合容器）
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
