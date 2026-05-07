# =============================================================================
# Java后端服务 Dockerfile
# 构建阶段 + 运行阶段（多阶段构建）
# =============================================================================

# -----------------------------------------------------------------------------
# 阶段 1: 构建阶段
# -----------------------------------------------------------------------------
FROM docker.io/eclipse-temurin:21-jdk-alpine AS builder

# 安装必要的工具
RUN apk add --no-cache curl

# 设置工作目录
WORKDIR /build

# 复制 Maven 配置文件（利用缓存层）
COPY pom.xml .
COPY types/pom.xml types/
COPY domain/pom.xml domain/
COPY api/pom.xml api/
COPY infrastructure/pom.xml infrastructure/
COPY trigger/pom.xml trigger/
COPY app/pom.xml app/

# 下载依赖（利用 Docker 缓存）
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -B || \
    (apk add --no-cache maven && \
    mvn dependency:go-offline -B)

# 复制源代码
COPY types/src types/src
COPY domain/src domain/src
COPY api/src api/src
COPY infrastructure/src infrastructure/src
COPY trigger/src trigger/src
COPY app/src app/src

# 构建项目（跳过测试，加快构建）
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# -----------------------------------------------------------------------------
# 阶段 2: 运行阶段
# -----------------------------------------------------------------------------
FROM docker.io/eclipse-temurin:21-jre-alpine

# 标签信息
LABEL maintainer="SER594 Team"
LABEL description="智能求职助手 - Java后端服务"

# 安装必要的工具
# curl 用于健康检查 / curl for health checks
# libreoffice + pandoc + weasyprint 用于文档格式转换 / for document format conversion
RUN apk add --no-cache \
    curl \
    libreoffice \
    pandoc \
    python3 \
    py3-pip \
    cairo \
    pango \
    gdk-pixbuf \
    font-noto-cjk \
    && pip3 install --break-system-packages weasyprint 2>/dev/null \
    || pip3 install weasyprint \
    && apk add --no-cache su-exec

# 创建非 root 用户
RUN addgroup -S resume && adduser -S resume -G resume

# 设置工作目录
WORKDIR /app

# 创建上传目录与日志目录（resume 用户需可写）
# Create upload and log directories (resume user must be able to write)
RUN mkdir -p /app/uploads /app/backend/logs && chown -R resume:resume /app

# 为 resume 用户创建 home 目录（LibreOffice 需要可写的 $HOME）
# Create home directory for resume user (LibreOffice needs a writable $HOME)
RUN mkdir -p /home/resume && chown resume:resume /home/resume
ENV HOME=/home/resume

# 从构建阶段复制 JAR 文件
COPY --from=builder /build/app/target/app-*.jar app.jar

# 复制 entrypoint 脚本并消除可能存在的 Windows CRLF 回车符
COPY entrypoint.sh /entrypoint.sh
RUN sed -i 's/\r$//' /entrypoint.sh && chmod +x /entrypoint.sh

# 暴露端口
EXPOSE 8080

# 使用 entrypoint 脚本启动：先以 root 修复 volume 权限，再切换到 resume 用户
ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-jar", "/app/app.jar"]
