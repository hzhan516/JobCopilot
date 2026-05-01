# =============================================================================
# Frontend Dockerfile (React + Vite + Nginx)
# Multi-stage build: compile static assets, then serve with Nginx
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build static assets
# -----------------------------------------------------------------------------
FROM node:20-alpine AS builder

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
FROM nginx:1.27-alpine

LABEL maintainer="SER594 Team"
LABEL description="Resume Assistant Frontend"

# Replace default site config with SPA + API reverse proxy config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled static files
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

# Container-level health probe for compose/orchestrators
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD wget -q -O /dev/null http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]

