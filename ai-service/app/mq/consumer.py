import json
import logging

import pika

from app.config import (
    AI_DIRECT_EXCHANGE,
    AI_DLX_EXCHANGE,
    AI_DLQ_QUEUE,
    AI_DLQ_ROUTING_KEY,
    CONVERSATION_REQUEST_QUEUE,
    CONVERSATION_REQUEST_ROUTING_KEY,
    CONVERSATION_RESULT_QUEUE,
    CONVERSATION_RESULT_ROUTING_KEY,
    JOB_RANK_REQUEST_QUEUE,
    JOB_RANK_REQUEST_ROUTING_KEY,
    JOB_RANK_RESULT_QUEUE,
    JOB_RANK_RESULT_ROUTING_KEY,
    JOB_PARSE_REQUEST_QUEUE,
    JOB_PARSE_REQUEST_ROUTING_KEY,
    JOB_PARSE_RESULT_QUEUE,
    JOB_PARSE_RESULT_ROUTING_KEY,
    RESUME_PARSE_REQUEST_QUEUE,
    RESUME_PARSE_REQUEST_ROUTING_KEY,
    RESUME_PARSE_RESULT_QUEUE,
    RESUME_PARSE_RESULT_ROUTING_KEY,
    RABBITMQ_HOST,
    RABBITMQ_PASSWORD,
    RABBITMQ_PORT,
    RABBITMQ_USERNAME,
)

from app.mq.publisher import publish_ai_result, publish_job_rank_result
from app.schemas import (
    AiResultEvent,
    ConversationRequestCommand,
    JobRankCommand,
    JobParseCommand,
    ResumeParseCommand,
)
from app.services.conversation_service import process_conversation
from app.services.job_rank_service import rank_jobs
from app.services.job_orchestrator import process_job
from app.services.resume_orchestrator import process_resume

logger = logging.getLogger(__name__)

JOB_PARSE_FAILED_MESSAGE = "AI service failed while parsing the job posting. Please try again."
RESUME_PARSE_FAILED_MESSAGE = "AI service failed while parsing the resume. Please try again."
CONVERSATION_FAILED_MESSAGE = "AI service failed while generating the chat response. Please try again."
JOB_RANK_FAILED_MESSAGE = "AI service failed while ranking jobs. Please try again."

# Dead-letter queue arguments: failed messages are routed to the DLX for later inspection.
# 死信队列参数：处理失败的消息转发到 DLX，便于后续人工排查与重试。
QUEUE_ARGUMENTS = {
    "x-dead-letter-exchange": AI_DLX_EXCHANGE,
    "x-dead-letter-routing-key": AI_DLQ_ROUTING_KEY,
}


def declare_queue(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    queue: str,
) -> None:
    channel.queue_declare(queue=queue, durable=True, arguments=QUEUE_ARGUMENTS)


def create_connection() -> pika.BlockingConnection:
    """Create a blocking RabbitMQ connection with configured credentials and heartbeat.
    建立阻塞式 RabbitMQ 连接：使用配置凭据并启用心跳，防止长时间空闲被服务端断开。"""
    credentials = pika.PlainCredentials(RABBITMQ_USERNAME, RABBITMQ_PASSWORD)
    parameters = pika.ConnectionParameters(
        host=RABBITMQ_HOST,
        port=RABBITMQ_PORT,
        credentials=credentials,
        heartbeat=60,
        blocked_connection_timeout=300
    )
    return pika.BlockingConnection(parameters)


def setup_all_queues(channel: pika.adapters.blocking_connection.BlockingChannel) -> None:
    """Declare exchanges, queues, and bindings for all AI workflow routes.
    声明全部交换器、队列及绑定关系：保证 MQ 拓扑在启动时即就绪，避免运行时动态创建导致的消息丢失。"""
    channel.exchange_declare(
        exchange=AI_DIRECT_EXCHANGE,
        exchange_type="direct",
        durable=True,
    )
    channel.exchange_declare(
        exchange=AI_DLX_EXCHANGE,
        exchange_type="direct",
        durable=True,
    )
    channel.queue_declare(queue=AI_DLQ_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DLX_EXCHANGE,
        queue=AI_DLQ_QUEUE,
        routing_key=AI_DLQ_ROUTING_KEY,
    )

    declare_queue(channel, JOB_PARSE_REQUEST_QUEUE)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=JOB_PARSE_REQUEST_QUEUE,
        routing_key=JOB_PARSE_REQUEST_ROUTING_KEY,
    )
    declare_queue(channel, JOB_PARSE_RESULT_QUEUE)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=JOB_PARSE_RESULT_QUEUE,
        routing_key=JOB_PARSE_RESULT_ROUTING_KEY,
    )

    declare_queue(channel, RESUME_PARSE_REQUEST_QUEUE)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=RESUME_PARSE_REQUEST_QUEUE,
        routing_key=RESUME_PARSE_REQUEST_ROUTING_KEY,
    )
    declare_queue(channel, RESUME_PARSE_RESULT_QUEUE)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=RESUME_PARSE_RESULT_QUEUE,
        routing_key=RESUME_PARSE_RESULT_ROUTING_KEY,
    )

    declare_queue(channel, CONVERSATION_REQUEST_QUEUE)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=CONVERSATION_REQUEST_QUEUE,
        routing_key=CONVERSATION_REQUEST_ROUTING_KEY,
    )
    declare_queue(channel, CONVERSATION_RESULT_QUEUE)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=CONVERSATION_RESULT_QUEUE,
        routing_key=CONVERSATION_RESULT_ROUTING_KEY,
    )

    declare_queue(channel, JOB_RANK_REQUEST_QUEUE)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=JOB_RANK_REQUEST_QUEUE,
        routing_key=JOB_RANK_REQUEST_ROUTING_KEY,
    )
    declare_queue(channel, JOB_RANK_RESULT_QUEUE)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=JOB_RANK_RESULT_QUEUE,
        routing_key=JOB_RANK_RESULT_ROUTING_KEY,
    )




def parse_job_command(body: bytes) -> JobParseCommand:
    payload = json.loads(body.decode("utf-8"))
    return JobParseCommand.model_validate(payload)


def parse_resume_command(body: bytes) -> ResumeParseCommand:
    payload = json.loads(body.decode("utf-8"))
    return ResumeParseCommand.model_validate(payload)


def parse_conversation_command(body: bytes) -> ConversationRequestCommand:
    payload = json.loads(body.decode("utf-8"))
    return ConversationRequestCommand.model_validate(payload)


def parse_job_rank_command(body: bytes) -> JobRankCommand:
    payload = json.loads(body.decode("utf-8"))
    return JobRankCommand.model_validate(payload)


def build_failed_event(
    reference_id: str,
    event_type: str,
    error_message: str,
    event_entity_type: str | None = None,
) -> AiResultEvent:
    return AiResultEvent(
        referenceId=reference_id,
        type=event_type,
        status="FAILED",
        data=None,
        errorMessage=error_message,
        eventType=event_entity_type,
    )


def handle_job_message(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    body: bytes,
) -> None:
    command = parse_job_command(body)

    try:
        result = process_job(command)
    except Exception as exc:
        logger.exception("Job processing failed: job_id=%s", command.job_id)
        result = build_failed_event(
            reference_id=command.job_id,
            event_type="JOB_PARSE",
            error_message=JOB_PARSE_FAILED_MESSAGE,
            event_entity_type="JOB",
        )

    publish_ai_result(channel, result)


def handle_resume_message(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    body: bytes,
) -> None:
    command = parse_resume_command(body)
    try:
        result = process_resume(command)
    except Exception as exc:
        logger.exception("Resume processing failed: resume_id=%s", command.resume_id)
        result = build_failed_event(
            reference_id=command.resume_id,
            event_type="RESUME_PARSE",
            error_message=RESUME_PARSE_FAILED_MESSAGE,
            event_entity_type=None,
        )

    publish_ai_result(channel, result)


def handle_conversation_message(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    body: bytes,
) -> None:
    command = parse_conversation_command(body)
    try:
        result = process_conversation(command)
    except Exception as exc:
        logger.exception("Conversation processing failed: conversation_id=%s", command.conversation_id)
        result = build_failed_event(
            reference_id=command.conversation_id,
            event_type="CONVERSATION_REPLY",
            error_message=CONVERSATION_FAILED_MESSAGE,
            event_entity_type=None,
        )

    publish_ai_result(channel, result)


import asyncio

def handle_job_rank_message(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    body: bytes,
) -> None:
    command = parse_job_rank_command(body)

    try:
        result = asyncio.run(rank_jobs(command))
        publish_job_rank_result(
            channel,
            match_id=command.match_id,
            status="COMPLETED",
            rank_time_ms=result.rank_time_ms,
            ranked_results=[item.model_dump(by_alias=True) for item in result.ranked_results],
        )
    except Exception as exc:
        logger.exception("Job rank processing failed: match_id=%s", command.match_id)
        publish_job_rank_result(
            channel,
            match_id=command.match_id,
            status="FAILED",
            rank_time_ms=0,
            ranked_results=[],
            error_message=JOB_RANK_FAILED_MESSAGE,
        )





def _async_handler(wrapped_handler, log_message_metadata: bool = False):
    """Wrap MQ message handlers with ACK/NACK logic via thread-safe RabbitMQ callbacks.
    包装 MQ 消息处理器：业务逻辑在当前线程同步执行，ACK/NACK 通过 thread-safe 回调提交，
    避免在消费者线程中直接操作 channel 引发并发冲突。"""
    def wrapper(ch, method, properties, body) -> None:
        delivery_tag = method.delivery_tag

        if log_message_metadata:
            logger.info(
                "Received conversation MQ message: delivery_tag=%s, payload_bytes=%d",
                delivery_tag,
                len(body),
            )

        try:
            wrapped_handler(ch, body)
            ch.connection.add_callback_threadsafe(
                lambda: ch.basic_ack(delivery_tag=delivery_tag)
            )
        except Exception:
            logger.exception("Handler failed, tag=%s", delivery_tag)
            ch.connection.add_callback_threadsafe(
                lambda: ch.basic_nack(delivery_tag=delivery_tag, requeue=False)
            )

    return wrapper


def start_all_consumers(channel: pika.adapters.blocking_connection.BlockingChannel) -> None:
    """Start consuming from all AI workflow queues with prefetch=1 for fair distribution.
    启动所有消费者：prefetch_count=1 保证消息在多个 worker 间公平分发，防止单个任务阻塞后续消息。"""
    channel.basic_qos(prefetch_count=1)

    channel.basic_consume(
        queue=JOB_PARSE_REQUEST_QUEUE,
        on_message_callback=_async_handler(handle_job_message),
        auto_ack=False,
    )
    channel.basic_consume(
        queue=RESUME_PARSE_REQUEST_QUEUE,
        on_message_callback=_async_handler(handle_resume_message),
        auto_ack=False,
    )
    channel.basic_consume(
        queue=CONVERSATION_REQUEST_QUEUE,
        on_message_callback=_async_handler(handle_conversation_message, log_message_metadata=True),
        auto_ack=False,
    )
    channel.basic_consume(
        queue=JOB_RANK_REQUEST_QUEUE,
        on_message_callback=_async_handler(handle_job_rank_message),
        auto_ack=False,
    )
    channel.start_consuming()
