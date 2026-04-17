import json

import pika

from app.config import (
    AI_DIRECT_EXCHANGE,
    JOB_PARSE_REQUEST_QUEUE,
    JOB_PARSE_REQUEST_ROUTING_KEY,
    RESUME_PARSE_REQUEST_QUEUE,
    RESUME_PARSE_REQUEST_ROUTING_KEY,
    RABBITMQ_HOST,
    RABBITMQ_PASSWORD,
    RABBITMQ_PORT,
    RABBITMQ_USERNAME,
    VECTOR_GEN_REQUEST_QUEUE,
    VECTOR_GEN_REQUEST_ROUTING_KEY,
CONVERSATION_REQUEST_QUEUE,
CONVERSATION_REQUEST_ROUTING_KEY,
)
from app.mq.publisher import publish_ai_result
from app.schemas import (
    AiResultEvent,
    JobParseCommand,
    ResumeParseCommand,
    VectorGenCommand,
)
from app.services.job_orchestrator import process_job

from app.services.resume_orchestrator import process_resume

from app.services.vector_service import process_vector

def create_connection() -> pika.BlockingConnection:
    credentials = pika.PlainCredentials(RABBITMQ_USERNAME, RABBITMQ_PASSWORD)
    parameters = pika.ConnectionParameters(
        host=RABBITMQ_HOST,
        port=RABBITMQ_PORT,
        credentials=credentials,
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

    channel.queue_declare(queue=RESUME_PARSE_REQUEST_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=RESUME_PARSE_REQUEST_QUEUE,
        routing_key=RESUME_PARSE_REQUEST_ROUTING_KEY,
    )

    channel.queue_declare(queue=VECTOR_GEN_REQUEST_QUEUE, durable=True)
    channel.queue_bind(
        exchange=AI_DIRECT_EXCHANGE,
        queue=VECTOR_GEN_REQUEST_QUEUE,
        routing_key=VECTOR_GEN_REQUEST_ROUTING_KEY,
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
        result = build_failed_event(
            reference_id=command.job_id,
            event_type="JOB_PARSE",
            error_message=str(exc),
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
        result = build_failed_event(
            reference_id=command.resume_id,
            event_type="RESUME_PARSE",
            error_message=str(exc),
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
        result = build_failed_event(
            reference_id=command.reference_id,
            event_type="VECTOR_GEN",
            error_message=str(exc),
            event_entity_type=command.entity_type,
        )

    publish_ai_result(channel, result)



def job_message_callback(ch, method, properties, body) -> None:

    try:
        handle_job_message(ch, body)
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as exc:
        print(f"Failed to process job message: {exc}")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def resume_message_callback(ch, method, properties, body) -> None:
    try:
        handle_resume_message(ch, body)
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as exc:
        print(f"Failed to process resume message: {exc}")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)


def vector_message_callback(ch, method, properties, body) -> None:

    try:
        handle_vector_message(ch, body)
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as exc:
        print(f"Failed to process vector message: {exc}")
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

    channel.start_consuming()
