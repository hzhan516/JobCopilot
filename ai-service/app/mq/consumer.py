import json
import logging
from concurrent.futures import ThreadPoolExecutor

# RabbitMQ consumers for AI workflow requests: declare queues, parse commands, handle messages,
# and dispatch results or failures back to the appropriate result queues.

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
    VECTOR_GEN_REQUEST_QUEUE,
    VECTOR_GEN_REQUEST_ROUTING_KEY,
    VECTOR_GEN_RESULT_QUEUE,
    VECTOR_GEN_RESULT_ROUTING_KEY,
)

from app.mq.publisher import publish_ai_result, publish_job_rank_result
from app.schemas import (
    AiResultEvent,
    ConversationRequestCommand,
    JobRankCommand,
    JobParseCommand,
    ResumeParseCommand,
    VectorGenCommand,
)
from app.services.conversation_service import process_conversation
from app.services.job_rank_service import rank_jobs
from app.services.job_orchestrator import process_job
from app.services.resume_orchestrator import process_resume
from app.services.vector_service import process_vector

logger = logging.getLogger(__name__)
_executor = ThreadPoolExecutor(max_workers=10, thread_name_prefix="ai-worker-")

JOB_PARSE_FAILED_MESSAGE = "AI service failed while parsing the job posting. Please try again."
RESUME_PARSE_FAILED_MESSAGE = "AI service failed while parsing the resume. Please try again."
VECTOR_GEN_FAILED_MESSAGE = "AI service failed while generating embeddings. Please try again."
CONVERSATION_FAILED_MESSAGE = "AI service failed while generating the chat response. Please try again."
JOB_RANK_FAILED_MESSAGE = "AI service failed while ranking jobs. Please try again."

QUEUE_ARGUMENTS = {
    "x-dead-letter-exchange": AI_DLX_EXCHANGE,
    "x-dead-letter-routing-key": AI_DLQ_ROUTING_KEY,
}


# Declare a durable queue with shared DLQ configuration.
def declare_queue(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    queue: str,
) -> None:
    channel.queue_declare(queue=queue, durable=True, arguments=QUEUE_ARGUMENTS)


# Create a blocking RabbitMQ connection using configured credentials.
def create_connection() -> pika.BlockingConnection:
    credentials = pika.PlainCredentials(RABBITMQ_USERNAME, RABBITMQ_PASSWORD)
    parameters = pika.ConnectionParameters(
        host=RABBITMQ_HOST,
        port=RABBITMQ_PORT,
        credentials=credentials,
        heartbeat=60,
        blocked_connection_timeout=300
    )
    return pika.BlockingConnection(parameters)


# Declare exchanges, queues, and bindings for all AI workflow routes.
def setup_all_queues(channel: pika.adapters.blocking_connection.BlockingChannel) -> None:
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

    declare_queue(channel, VECTOR_GEN_REQUEST_QUEUE)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=VECTOR_GEN_REQUEST_QUEUE,
        routing_key=VECTOR_GEN_REQUEST_ROUTING_KEY,
    )
    declare_queue(channel, VECTOR_GEN_RESULT_QUEUE)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=VECTOR_GEN_RESULT_QUEUE,
        routing_key=VECTOR_GEN_RESULT_ROUTING_KEY,
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


# Parse a job-parse command payload from the message body.
def parse_job_command(body: bytes) -> JobParseCommand:
    payload = json.loads(body.decode("utf-8"))
    return JobParseCommand.model_validate(payload)


# Parse a resume-parse command payload from the message body.
def parse_resume_command(body: bytes) -> ResumeParseCommand:
    payload = json.loads(body.decode("utf-8"))
    return ResumeParseCommand.model_validate(payload)


# Parse a vector-generation command payload from the message body.
def parse_vector_command(body: bytes) -> VectorGenCommand:
    payload = json.loads(body.decode("utf-8"))
    return VectorGenCommand.model_validate(payload)


# Parse a conversation request command payload from the message body.
def parse_conversation_command(body: bytes) -> ConversationRequestCommand:
    payload = json.loads(body.decode("utf-8"))
    return ConversationRequestCommand.model_validate(payload)


# Parse a job-ranking command payload from the message body.
def parse_job_rank_command(body: bytes) -> JobRankCommand:
    payload = json.loads(body.decode("utf-8"))
    return JobRankCommand.model_validate(payload)


# Build a standardized failure event for AI results.
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


# Handle a job-parse request and publish the result or failure.
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


# Handle a resume-parse request and publish the result or failure.
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


# Handle a vector-generation request and publish the result or failure.
def handle_vector_message(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    body: bytes,
) -> None:
    command = parse_vector_command(body)
    try:
        result = process_vector(command)
    except Exception as exc:
        logger.exception("Vector processing failed: reference_id=%s", command.reference_id)
        result = build_failed_event(
            reference_id=command.reference_id,
            event_type="VECTOR_GEN",
            error_message=VECTOR_GEN_FAILED_MESSAGE,
            event_entity_type=command.entity_type,
        )

    publish_ai_result(channel, result)


# Handle a conversation request and publish the result or failure.
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


# Handle a job-ranking request and publish the result or failure.
def handle_job_rank_message(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    body: bytes,
) -> None:
    command = parse_job_rank_command(body)

    try:
        result = rank_jobs(command)
        publish_job_rank_result(channel, result.model_dump(by_alias=True))
    except Exception as exc:
        logger.exception("Job rank processing failed: match_id=%s", command.match_id)
        publish_job_rank_result(
            channel,
            {
                "matchId": command.match_id,
                "status": "FAILED",
                "rankTimeMs": 0,
                "rankedResults": [],
                "errorMessage": JOB_RANK_FAILED_MESSAGE,
            },
        )


# Wrap MQ handlers in a worker pool and ACK/NACK from the RabbitMQ thread.
# 使用线程池处理耗时任务，并通过 RabbitMQ 线程安全回调确认或拒绝消息。
def _async_handler(wrapped_handler, log_raw_payload: bool = False):
    def wrapper(ch, method, properties, body) -> None:
        delivery_tag = method.delivery_tag

        if log_raw_payload:
            logger.info(
                "Received raw conversation MQ message: delivery_tag=%s, body=%s",
                delivery_tag,
                body.decode("utf-8", errors="replace")[:1000],
            )

        def task() -> None:
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

        _executor.submit(task)

    return wrapper


# Start all consumer subscriptions and begin consuming.
def start_all_consumers(channel: pika.adapters.blocking_connection.BlockingChannel) -> None:
    channel.basic_qos(prefetch_count=10)

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
        queue=VECTOR_GEN_REQUEST_QUEUE,
        on_message_callback=_async_handler(handle_vector_message),
        auto_ack=False,
    )
    channel.basic_consume(
        queue=CONVERSATION_REQUEST_QUEUE,
        on_message_callback=_async_handler(handle_conversation_message, log_raw_payload=True),
        auto_ack=False,
    )
    channel.basic_consume(
        queue=JOB_RANK_REQUEST_QUEUE,
        on_message_callback=_async_handler(handle_job_rank_message),
        auto_ack=False,
    )
    channel.start_consuming()
