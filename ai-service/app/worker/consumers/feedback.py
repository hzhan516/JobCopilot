import json
import logging
import asyncio
from app.infrastructure.redis.client import RedisBuffer

logger = logging.getLogger(__name__)

def handle_feedback_message(ch, method, properties, body: bytes):
    try:
        data = json.loads(body.decode("utf-8"))
        logger.info(f"Received feedback: {data.get('jobId')} - {data.get('feedbackType')}")
        
        label = 0
        fb_type = data.get("feedbackType")
        if fb_type in ("APPLY", "CLICK", "EXPLICIT_THUMBS_UP"):
            label = 1
            
        context_str = data.get("context", "{}")
        features = {}
        if context_str:
            try:
                features = json.loads(context_str)
            except:
                pass
        
        sample = {
            "type": "feedback",
            "user_id": data.get("userId"),
            "job_id": data.get("jobId"),
            "label": label,
            "features": features
        }
        
        redis_buffer = RedisBuffer()
        asyncio.run(redis_buffer.append(sample))
        
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as e:
        logger.error(f"Error handling feedback message: {e}")
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
