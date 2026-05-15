# =============================================================================
# Frontend Dockerfile (React + Vite + Nginx)
# Multi-stage build: compile static assets, then serve with Nginx
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build static assets
# -----------------------------------------------------------------------------
FROM docker.io/library/node:20-alpine AS builder

WORKDIR /app

# Install dependencies first to maximize layer caching
COPY package.json package-lock.json ./
RUN npm ci

# Accept build-time environment variables
ARG VITE_GOOGLE_CLIENT_ID
ARG VITE_API_BASE_URL

# Build frontend
COPY . .
RUN npm run build

# -----------------------------------------------------------------------------
# Stage 2: Serve with Nginx
# -----------------------------------------------------------------------------
FROM docker.io/library/nginx:1.27-alpine

LABEL maintainer="SER594 Team"
LABEL description="Resume Assistant Frontend"

# Install curl for Docker healthcheck (nginx:alpine does not include wget or curl by default)
RUN apk add --no-cache curl

# Prepare directories and nginx main config for non-root execution
# 为 non-root 运行准备目录与 nginx 主配置
RUN mkdir -p /var/cache/nginx /var/run /tmp \
    && chown -R nginx:nginx /var/cache/nginx /var/run /tmp /var/log/nginx /usr/share/nginx/html /etc/nginx/conf.d \
    && touch /var/run/nginx.pid \
    && chown nginx:nginx /var/run/nginx.pid \
    && sed -i 's|pid /var/run/nginx.pid;|pid /tmp/nginx.pid;|' /etc/nginx/nginx.conf \
    && sed -i 's|error_log .*|error_log /dev/stderr notice;|' /etc/nginx/nginx.conf \
    && sed -i 's|access_log .*|access_log /dev/stdout main;|' /etc/nginx/nginx.conf

# Replace default site config with SPA + API reverse proxy config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled static files
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 8080

USER nginx

CMD ["nginx", "-g", "daemon off;"]

