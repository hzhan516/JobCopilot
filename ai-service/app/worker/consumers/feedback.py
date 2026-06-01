import json
import logging
import asyncio
from pydantic import ValidationError
from app.schemas import FeedbackCommand
from app.infrastructure.redis.client import RedisBuffer

logger = logging.getLogger(__name__)

def handle_feedback_message(ch, method, properties, body: bytes):
    try:
        command = FeedbackCommand.model_validate(json.loads(body.decode("utf-8")))
    except (json.JSONDecodeError, UnicodeDecodeError, ValidationError) as e:
        logger.warning("Invalid feedback message, rejecting without requeue: %s", e)
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        return

    logger.info("Received feedback: %s - %s", command.job_id, command.feedback_type)

    label = 0
    if command.feedback_type in ("APPLY", "CLICK", "EXPLICIT_THUMBS_UP"):
        label = 1

    features = {}
    if command.context:
        try:
            features = json.loads(command.context)
        except json.JSONDecodeError:
            logger.warning("Feedback context is not valid JSON: match_id=%s", command.match_id)

    sample = {
        "type": "feedback",
        "user_id": command.user_id,
        "job_id": command.job_id,
        "label": label,
        "features": features,
    }

    try:
        redis_buffer = RedisBuffer()
        asyncio.run(redis_buffer.append(sample))
    except Exception as e:
        logger.error("Temporary failure while buffering feedback, requeueing: %s", e)
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
        return

    ch.basic_ack(delivery_tag=method.delivery_tag)
