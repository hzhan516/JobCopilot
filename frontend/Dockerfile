# =============================================================================
# 前端服务 Dockerfile
# React + Nginx 多阶段构建
# =============================================================================

# -----------------------------------------------------------------------------
# 阶段 1: 构建 React 应用
# -----------------------------------------------------------------------------
FROM node:20-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制 package 文件（利用缓存层）
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production

# 复制源代码
COPY . .

# 构建生产版本
RUN npm run build

# -----------------------------------------------------------------------------
# 阶段 2: Nginx 服务
# -----------------------------------------------------------------------------
FROM nginx:1.25-alpine

# 标签信息
LABEL maintainer="SER594 Team"
LABEL description="智能求职助手 - 前端服务"

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:80 || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
