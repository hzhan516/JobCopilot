# =============================================================================
# JobCopilot - PostgreSQL with pgvector extension
# 基于 Bitnami PostgreSQL 镜像，额外安装 pgvector 扩展
# =============================================================================
FROM public.ecr.aws/bitnami/postgresql:15

USER root

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates gnupg && \
    \
    curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc | gpg --dearmor -o /etc/apt/trusted.gpg.d/postgresql.gpg && \
    echo "deb http://apt.postgresql.org/pub/repos/apt bookworm-pgdg main" > /etc/apt/sources.list.d/pgdg.list && \
    apt-get update && \
    \
    apt-get install -y --no-install-recommends postgresql-15-pgvector && \
    \
    cp /usr/lib/postgresql/15/lib/vector.so /opt/bitnami/postgresql/lib/ && \
    cp /usr/share/postgresql/15/extension/vector* /opt/bitnami/postgresql/share/extension/ && \
    sed -i 's/^#\?trusted = false/trusted = true/' /opt/bitnami/postgresql/share/extension/vector.control && \
    grep -q '^trusted = true' /opt/bitnami/postgresql/share/extension/vector.control || echo 'trusted = true' >> /opt/bitnami/postgresql/share/extension/vector.control && \
    \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

USER 1001
