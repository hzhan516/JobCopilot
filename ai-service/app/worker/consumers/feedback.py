import json
import logging
import asyncio
from app.schemas import FeedbackCommand
from app.infrastructure.redis.client import RedisBuffer

logger = logging.getLogger(__name__)

def handle_feedback_message(ch, method, properties, body: bytes):
    try:
        command = FeedbackCommand.model_validate(json.loads(body.decode("utf-8")))
        logger.info(f"Received feedback: {command.job_id} - {command.feedback_type}")
        
        label = 0
        if command.feedback_type in ("APPLY", "CLICK", "EXPLICIT_THUMBS_UP"):
            label = 1
            
        features = {}
        if command.context:
            try:
                features = json.loads(command.context)
            except:
                pass
        
        sample = {
            "type": "feedback",
            "user_id": command.user_id,
            "job_id": command.job_id,
            "label": label,
            "features": features
        }
        
        redis_buffer = RedisBuffer()
        asyncio.run(redis_buffer.append(sample))
        
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as e:
        logger.error(f"Error handling feedback message: {e}")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
