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

# Replace default site config with SPA + API reverse proxy config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled static files
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

