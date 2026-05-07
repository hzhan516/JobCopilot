#!/usr/bin/env bash
# =============================================================================
# RabbitMQ Queue Reset Script / RabbitMQ 队列重置脚本 / RabbitMQ 佇列重置腳本
# =============================================================================
# Purpose: Delete all business queues so Spring AMQP can re-declare them with
#          updated arguments (e.g., x-dead-letter-exchange).
#          删除所有业务队列，使 Spring AMQP 能以更新后的参数重新声明队列。
#          刪除所有業務佇列，使 Spring AMQP 能以更新後的參數重新宣告佇列。
#
# WARNING: This will delete any un-consumed messages in the queues.
#          这将删除队列中所有未消费的消息。
#          這將刪除佇列中所有未消費的訊息。
#
# Usage:
#   ./reset-rabbitmq-queues.sh [RABBITMQ_HOST] [RABBITMQ_PORT] [USER] [PASS]
#
# Default credentials: guest / guest @ localhost:15672
# =============================================================================

set -euo pipefail

RABBITMQ_HOST="${1:-localhost}"
RABBITMQ_PORT="${2:-15672}"
RABBITMQ_USER="${3:-guest}"
RABBITMQ_PASS="${4:-guest}"

API_BASE="http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api"

echo "============================================================"
echo "RabbitMQ Queue Reset Tool"
echo "Target: ${RABBITMQ_HOST}:${RABBITMQ_PORT}"
echo "============================================================"
echo ""
echo "The following 10 business queues will be DELETED:"
echo "  1. ai.queue.job.parse"
echo "  2. backend.queue.job.parse"
echo "  3. ai.queue.resume.parse"
echo "  4. backend.queue.resume.parse"
echo "  5. ai.queue.vector.gen"
echo "  6. backend.queue.vector.gen"
echo "  7. ai.queue.conversation"
echo "  8. backend.queue.conversation"
echo "  9. ai.queue.job.rank"
echo " 10. backend.queue.job.rank"
echo ""
echo "WARNING: Un-consumed messages will be LOST."
echo "警告：队列中未消费的消息将会丢失。"
echo "警告：佇列中未消費的訊息將會遺失。"
echo ""
read -r -p "Are you sure? Type 'yes' to continue: " confirm

if [[ "$confirm" != "yes" ]]; then
    echo "Aborted."
    exit 1
fi

echo ""
echo "Deleting queues..."

QUEUES=(
    "ai.queue.job.parse"
    "backend.queue.job.parse"
    "ai.queue.resume.parse"
    "backend.queue.resume.parse"
    "ai.queue.vector.gen"
    "backend.queue.vector.gen"
    "ai.queue.conversation"
    "backend.queue.conversation"
    "ai.queue.job.rank"
    "backend.queue.job.rank"
)

for queue in "${QUEUES[@]}"; do
    url="${API_BASE}/queues/%2f/${queue}"
    http_code=$(curl -s -o /dev/null -w "%{http_code}" -u "${RABBITMQ_USER}:${RABBITMQ_PASS}" "${url}" || true)

    if [[ "$http_code" == "200" ]]; then
        curl -s -o /dev/null -w "%{http_code}" -u "${RABBITMQ_USER}:${RABBITMQ_PASS}" -X DELETE "${url}"
        echo "  [DELETED] ${queue}"
    elif [[ "$http_code" == "404" ]]; then
        echo "  [NOT FOUND] ${queue} (skipped)"
    else
        echo "  [ERROR] ${queue} (HTTP ${http_code})"
    fi
done

echo ""
echo "============================================================"
echo "Reset complete. Please restart the backend container:"
echo "重置完成。请重启后端容器："
echo "重置完成。請重新啟動後端容器："
echo ""
echo "  docker-compose up -d --build backend"
echo "============================================================"
