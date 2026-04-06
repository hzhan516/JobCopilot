# =============================================================================
# Java后端服务 Dockerfile
# 构建阶段 + 运行阶段（多阶段构建）
# =============================================================================

# -----------------------------------------------------------------------------
# 阶段 1: 构建阶段
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

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
FROM eclipse-temurin:21-jre-alpine

# 标签信息
LABEL maintainer="SER594 Team"
LABEL description="智能求职助手 - Java后端服务"

# 安装必要的工具（curl用于健康检查）
RUN apk add --no-cache curl

# 创建非 root 用户
RUN addgroup -S resume && adduser -S resume -G resume

# 设置工作目录
WORKDIR /app

# 创建上传目录
RUN mkdir -p /app/uploads && chown -R resume:resume /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /build/app/target/app-*.jar app.jar

# 切换到非 root 用户
USER resume

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
