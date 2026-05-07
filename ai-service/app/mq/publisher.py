import json
import logging

# RabbitMQ publishers for AI workflow results and helper utilities for routing.

import pika

from app.config import (
    AI_DIRECT_EXCHANGE,
    JOB_RANK_RESULT_ROUTING_KEY,
    JOB_PARSE_RESULT_ROUTING_KEY,
    RESUME_PARSE_RESULT_ROUTING_KEY,
    CONVERSATION_RESULT_ROUTING_KEY,
)

from app.schemas import AiResultEvent

logger = logging.getLogger(__name__)


# Map AI event types to the correct result routing key.
def get_result_routing_key(event_type: str) -> str:
    if event_type == "JOB_PARSE":
        return JOB_PARSE_RESULT_ROUTING_KEY
    if event_type == "RESUME_PARSE":
        return RESUME_PARSE_RESULT_ROUTING_KEY
    if event_type == "CONVERSATION_REPLY":
        return CONVERSATION_RESULT_ROUTING_KEY
    raise ValueError(f"Unsupported event type: {event_type}")


# Publish a structured AI result event to the direct exchange.
def publish_ai_result(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    event: AiResultEvent,
) -> None:
    routing_key = get_result_routing_key(event.type)

    message_body = json.dumps(
        event.model_dump(by_alias=True),
        ensure_ascii=False,
    )

    channel.basic_publish(
        exchange=AI_DIRECT_EXCHANGE,
        routing_key=routing_key,
        body=message_body.encode("utf-8"),
        properties=pika.BasicProperties(
            content_type="application/json",
            delivery_mode=2,
        ),
    )
    if event.status == "FAILED":
        logger.error(
            "Published AI result: type=%s, status=%s, routing_key=%s, error=%s",
            event.type,
            event.status,
            routing_key,
            event.error_message,
        )
    else:
        logger.info(
            "Published AI result: type=%s, status=%s, routing_key=%s",
            event.type,
            event.status,
            routing_key,
        )


# Publish a JSON payload to a specific routing key.
def publish_json_payload(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    routing_key: str,
    payload: dict,
) -> None:
    message_body = json.dumps(payload, ensure_ascii=False)

    channel.basic_publish(
        exchange=AI_DIRECT_EXCHANGE,
        routing_key=routing_key,
        body=message_body.encode("utf-8"),
        properties=pika.BasicProperties(
            content_type="application/json",
            delivery_mode=2,
        ),
    )


# Publish a job ranking result as a standardized AiResultEvent.
# 将职位精排结果以统一的 AiResultEvent 格式发布到 MQ。
def publish_job_rank_result(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    match_id: str,
    status: str,
    rank_time_ms: int,
    ranked_results: list[dict],
    error_message: str | None = None,
) -> None:
    event = AiResultEvent(
        referenceId=match_id,
        type="JOB_RANK",
        status=status,
        data={
            "rankTimeMs": rank_time_ms,
            "rankedResults": ranked_results,
        } if status == "COMPLETED" else None,
        errorMessage=error_message,
    )

    routing_key = JOB_RANK_RESULT_ROUTING_KEY
    message_body = json.dumps(
        event.model_dump(by_alias=True),
        ensure_ascii=False,
    )

    channel.basic_publish(
        exchange=AI_DIRECT_EXCHANGE,
        routing_key=routing_key,
        body=message_body.encode("utf-8"),
        properties=pika.BasicProperties(
            content_type="application/json",
            delivery_mode=2,
        ),
    )

    if event.status == "FAILED":
        logger.error(
            "Published AI result: type=%s, status=%s, routing_key=%s, error=%s",
            event.type,
            event.status,
            routing_key,
            event.error_message,
        )
    else:
        logger.info(
            "Published AI result: type=%s, status=%s, routing_key=%s",
            event.type,
            event.status,
            routing_key,
        )
