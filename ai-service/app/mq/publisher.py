import json
import logging
from typing import Any

import pika
from pydantic import BaseModel

from app.config import (
    AI_DIRECT_EXCHANGE,
    JOB_RANK_RESULT_ROUTING_KEY,
    JOB_PARSE_RESULT_ROUTING_KEY,
    RESUME_PARSE_RESULT_ROUTING_KEY,
    CONVERSATION_RESULT_ROUTING_KEY,
)

from app.schemas import AiResultEvent, JobRankResultData, JobRankResultItem

logger = logging.getLogger(__name__)


def get_result_routing_key(event_type: str) -> str:
    """Map AI event types to backend result queue routing keys.
    将 AI 事件类型映射到后端结果队列的路由键，确保不同类型处理结果进入正确队列。"""
    if event_type == "JOB_PARSE":
        return JOB_PARSE_RESULT_ROUTING_KEY
    if event_type == "RESUME_PARSE":
        return RESUME_PARSE_RESULT_ROUTING_KEY
    if event_type == "CONVERSATION_REPLY":
        return CONVERSATION_RESULT_ROUTING_KEY
    if event_type == "JOB_RANK":
        return JOB_RANK_RESULT_ROUTING_KEY
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


def publish_json_payload(
    channel: pika.adapters.blocking_connection.BlockingChannel,
    routing_key: str,
    payload: BaseModel | dict[str, Any],
) -> None:
    if isinstance(payload, BaseModel):
        body = payload.model_dump(by_alias=True)
    else:
        body = payload
    message_body = json.dumps(body, ensure_ascii=False)

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
    match_id: str,
    status: str,
    rank_time_ms: int,
    ranked_results: list[JobRankResultItem | dict[str, Any]],
    error_message: str | None = None,
) -> None:
    """Publish a job-ranking result wrapped in the unified AiResultEvent schema.
    将职位精排结果以统一的 AiResultEvent 格式发布到 MQ，降低后端消费端的解析复杂度。"""
    event = AiResultEvent(
        referenceId=match_id,
        type="JOB_RANK",
        status=status,
        data=JobRankResultData(
            rankTimeMs=rank_time_ms,
            rankedResults=[
                item.model_dump(by_alias=True)
                if isinstance(item, JobRankResultItem)
                else JobRankResultItem.model_validate(item).model_dump(by_alias=True)
                for item in ranked_results
            ],
        ),
        errorMessage=error_message,
        eventType=None,
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
