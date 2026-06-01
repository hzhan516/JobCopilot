from unittest.mock import MagicMock, patch

from app.worker.consumers.rabbitmq_setup import QUEUE_ARGUMENTS, create_worker_connection, setup_feedback_queue
from app.config import (
    AI_DIRECT_EXCHANGE,
    AI_DLX_EXCHANGE,
    AI_DLQ_QUEUE,
    AI_DLQ_ROUTING_KEY,
    FEEDBACK_REQUEST_QUEUE,
    FEEDBACK_REQUEST_ROUTING_KEY,
    RABBITMQ_HOST,
    RABBITMQ_PORT,
)


@patch("app.worker.consumers.rabbitmq_setup.pika.BlockingConnection")
@patch("app.worker.consumers.rabbitmq_setup.pika.ConnectionParameters")
@patch("app.worker.consumers.rabbitmq_setup.pika.PlainCredentials")
def test_create_worker_connection(mock_creds, mock_params, mock_conn):
    """Should create connection with correct RabbitMQ credentials and parameters / 应使用正确的 RabbitMQ 凭据和参数创建连接"""
    mock_creds.return_value = "creds_obj"
    mock_params.return_value = "params_obj"
    mock_conn.return_value = MagicMock()

    conn = create_worker_connection()

    mock_creds.assert_called_once()
    creds_call = mock_creds.call_args
    assert creds_call.args[0] == "guest"
    assert creds_call.args[1] == "guest"

    mock_params.assert_called_once()
    params_kwargs = mock_params.call_args.kwargs
    assert params_kwargs["host"] == RABBITMQ_HOST
    assert params_kwargs["port"] == RABBITMQ_PORT
    assert params_kwargs["credentials"] == "creds_obj"
    assert params_kwargs["heartbeat"] == 60
    assert params_kwargs["blocked_connection_timeout"] == 300

    mock_conn.assert_called_once_with("params_obj")
    assert conn is not None


def test_setup_feedback_queue():
    """Should declare exchange, queue and bind them / 应声明交换器、队列并绑定"""
    mock_channel = MagicMock()

    setup_feedback_queue(mock_channel)

    assert mock_channel.exchange_declare.call_count == 2
    mock_channel.exchange_declare.assert_any_call(
        exchange=AI_DIRECT_EXCHANGE,
        exchange_type="direct",
        durable=True,
    )
    mock_channel.exchange_declare.assert_any_call(
        exchange=AI_DLX_EXCHANGE,
        exchange_type="direct",
        durable=True,
    )

    assert mock_channel.queue_declare.call_count == 2
    mock_channel.queue_declare.assert_any_call(queue=AI_DLQ_QUEUE, durable=True)
    mock_channel.queue_declare.assert_any_call(
        queue=FEEDBACK_REQUEST_QUEUE,
        durable=True,
        arguments=QUEUE_ARGUMENTS,
    )

    assert mock_channel.queue_bind.call_count == 2
    mock_channel.queue_bind.assert_any_call(
        exchange=AI_DLX_EXCHANGE,
        queue=AI_DLQ_QUEUE,
        routing_key=AI_DLQ_ROUTING_KEY,
    )
    mock_channel.queue_bind.assert_any_call(
        exchange=AI_DIRECT_EXCHANGE,
        queue=FEEDBACK_REQUEST_QUEUE,
        routing_key=FEEDBACK_REQUEST_ROUTING_KEY,
    )
