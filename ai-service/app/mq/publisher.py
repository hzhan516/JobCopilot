import json

import pika

from app.config import (
    AI_DIRECT_EXCHANGE,
    JOB_RANK_RESULT_ROUTING_KEY,
    JOB_PARSE_RESULT_ROUTING_KEY,
    RESUME_PARSE_RESULT_ROUTING_KEY,
    VECTOR_GEN_RESULT_ROUTING_KEY,
    CONVERSATION_RESULT_ROUTING_KEY,
)

from app.schemas import AiResultEvent


def get_result_routing_key(event_type: str) -> str:
    if event_type == "JOB_PARSE":
        return JOB_PARSE_RESULT_ROUTING_KEY
    if event_type == "RESUME_PARSE":
        return RESUME_PARSE_RESULT_ROUTING_KEY
    if event_type == "VECTOR_GEN":
        return VECTOR_GEN_RESULT_ROUTING_KEY
    if event_type == "CONVERSATION_REPLY":
        return CONVERSATION_RESULT_ROUTING_KEY
    raise ValueError(f"Unsupported event type: {event_type}")

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
    print(
        f"Published AI result: type={event.type}, status={event.status}, routing_key={routing_key}",
        flush=True,
    )


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


def publish_job_rank_result(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    payload: dict,
) -> None:
    publish_json_payload(
        channel=channel,
        routing_key=JOB_RANK_RESULT_ROUTING_KEY,
        payload=payload,
    )
