import pika
from app.config import (
    RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USERNAME, RABBITMQ_PASSWORD,
    AI_DIRECT_EXCHANGE, AI_DLX_EXCHANGE, AI_DLQ_QUEUE, AI_DLQ_ROUTING_KEY,
    FEEDBACK_REQUEST_QUEUE, FEEDBACK_REQUEST_ROUTING_KEY
)

QUEUE_ARGUMENTS = {
    "x-dead-letter-exchange": AI_DLX_EXCHANGE,
    "x-dead-letter-routing-key": AI_DLQ_ROUTING_KEY,
}

def create_worker_connection():
    credentials = pika.PlainCredentials(RABBITMQ_USERNAME, RABBITMQ_PASSWORD)
    parameters = pika.ConnectionParameters(
        host=RABBITMQ_HOST,
        port=RABBITMQ_PORT,
        credentials=credentials,
        heartbeat=60,
        blocked_connection_timeout=300
    )
    return pika.BlockingConnection(parameters)

def setup_feedback_queue(channel):
    channel.exchange_declare(exchange=AI_DIRECT_EXCHANGE, exchange_type="direct", durable=True)
    channel.exchange_declare(exchange=AI_DLX_EXCHANGE, exchange_type="direct", durable=True)
    channel.queue_declare(queue=AI_DLQ_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DLX_EXCHANGE,
        queue=AI_DLQ_QUEUE,
        routing_key=AI_DLQ_ROUTING_KEY,
    )
    channel.queue_declare(queue=FEEDBACK_REQUEST_QUEUE, durable=True, arguments=QUEUE_ARGUMENTS)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=FEEDBACK_REQUEST_QUEUE,
        routing_key=FEEDBACK_REQUEST_ROUTING_KEY,
    )
