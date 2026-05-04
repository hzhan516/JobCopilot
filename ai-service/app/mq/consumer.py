import json
import logging
import traceback

import pika

from app.config import (
    AI_DIRECT_EXCHANGE,
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


def setup_all_queues(channel: pika.adapters.blocking_connection.BlockingChannel) -> None:
    channel.exchange_declare(
        exchange=AI_DIRECT_EXCHANGE,
        exchange_type="direct",
        durable=True,
    )

    channel.queue_declare(queue=JOB_PARSE_REQUEST_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=JOB_PARSE_REQUEST_QUEUE,
        routing_key=JOB_PARSE_REQUEST_ROUTING_KEY,
    )
    channel.queue_declare(queue=JOB_PARSE_RESULT_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=JOB_PARSE_RESULT_QUEUE,
        routing_key=JOB_PARSE_RESULT_ROUTING_KEY,
    )

    channel.queue_declare(queue=RESUME_PARSE_REQUEST_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=RESUME_PARSE_REQUEST_QUEUE,
        routing_key=RESUME_PARSE_REQUEST_ROUTING_KEY,
    )
    channel.queue_declare(queue=RESUME_PARSE_RESULT_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=RESUME_PARSE_RESULT_QUEUE,
        routing_key=RESUME_PARSE_RESULT_ROUTING_KEY,
    )

    channel.queue_declare(queue=VECTOR_GEN_REQUEST_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=VECTOR_GEN_REQUEST_QUEUE,
        routing_key=VECTOR_GEN_REQUEST_ROUTING_KEY,
    )
    channel.queue_declare(queue=VECTOR_GEN_RESULT_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=VECTOR_GEN_RESULT_QUEUE,
        routing_key=VECTOR_GEN_RESULT_ROUTING_KEY,
    )

    channel.queue_declare(queue=CONVERSATION_REQUEST_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=CONVERSATION_REQUEST_QUEUE,
        routing_key=CONVERSATION_REQUEST_ROUTING_KEY,
    )
    channel.queue_declare(queue=CONVERSATION_RESULT_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=CONVERSATION_RESULT_QUEUE,
        routing_key=CONVERSATION_RESULT_ROUTING_KEY,
    )

    channel.queue_declare(queue=JOB_RANK_REQUEST_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=JOB_RANK_REQUEST_QUEUE,
        routing_key=JOB_RANK_REQUEST_ROUTING_KEY,
    )
    channel.queue_declare(queue=JOB_RANK_RESULT_QUEUE, durable=True)
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


def parse_vector_command(body: bytes) -> VectorGenCommand:
    payload = json.loads(body.decode("utf-8"))
    return VectorGenCommand.model_validate(payload)


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
            error_message=str(exc) + "\n\n" + traceback.format_exc(),
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
            error_message=str(exc) + "\n\n" + traceback.format_exc(),
            event_entity_type=None,
        )

    publish_ai_result(channel, result)


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
            error_message=str(exc) + "\n\n" + traceback.format_exc(),
            event_entity_type=command.entity_type,
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
            error_message=str(exc) + "\n\n" + traceback.format_exc(),
            event_entity_type=None,
        )

    publish_ai_result(channel, result)


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
                "errorMessage": str(exc) + "\n\n" + traceback.format_exc(),
            },
        )


def job_message_callback(ch, method, properties, body) -> None:
    try:
        handle_job_message(ch, body)
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception:
        logger.exception("Failed to process job message")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def resume_message_callback(ch, method, properties, body) -> None:
    try:
        handle_resume_message(ch, body)
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception:
        logger.exception("Failed to process resume message")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def vector_message_callback(ch, method, properties, body) -> None:
    try:
        handle_vector_message(ch, body)
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception:
        logger.exception("Failed to process vector message")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def conversation_message_callback(ch, method, properties, body) -> None:
    logger.info(
        "Received raw conversation MQ message: delivery_tag=%s, body=%s",
        method.delivery_tag,
        body.decode("utf-8", errors="replace")[:1000],
    )
    try:
        handle_conversation_message(ch, body)
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception:
        logger.exception("Failed to process conversation message")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def job_rank_message_callback(ch, method, properties, body) -> None:
    try:
        handle_job_rank_message(ch, body)
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception:
        logger.exception("Failed to process job rank message")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def start_all_consumers(channel: pika.adapters.blocking_connection.BlockingChannel) -> None:
    channel.basic_qos(prefetch_count=1)

    channel.basic_consume(
        queue=JOB_PARSE_REQUEST_QUEUE,
        on_message_callback=job_message_callback,
    )
    channel.basic_consume(
        queue=RESUME_PARSE_REQUEST_QUEUE,
        on_message_callback=resume_message_callback,
    )
    channel.basic_consume(
        queue=VECTOR_GEN_REQUEST_QUEUE,
        on_message_callback=vector_message_callback,
    )
    channel.basic_consume(
        queue=CONVERSATION_REQUEST_QUEUE,
        on_message_callback=conversation_message_callback,
    )
    channel.basic_consume(
        queue=JOB_RANK_REQUEST_QUEUE,
        on_message_callback=job_rank_message_callback,
    )
    channel.start_consuming()
