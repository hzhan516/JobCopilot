import asyncio
import logging
import threading
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from app.worker.scheduler.trainer import IncrementalTrainer
from app.worker.consumers.rabbitmq_setup import create_worker_connection, setup_feedback_queue
from app.worker.consumers.feedback import handle_feedback_message
from app.config import FEEDBACK_REQUEST_QUEUE

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

import time

def start_mq_consumer():
    retry_delay = 5
    attempt = 0
    while True:
        attempt += 1
        try:
            logger.info("Starting RabbitMQ consumers for Worker (Attempt %d)...", attempt)
            connection = create_worker_connection()
            channel = connection.channel()
            setup_feedback_queue(channel)
            channel.basic_qos(prefetch_count=1)
            channel.basic_consume(
                queue=FEEDBACK_REQUEST_QUEUE,
                on_message_callback=handle_feedback_message,
                auto_ack=False,
            )
            logger.info("RabbitMQ consumer for ai.queue.feedback is ready.")
            channel.start_consuming()
            break
        except Exception as e:
            logger.warning("RabbitMQ worker consumer startup failed (Attempt %d): %s. Retrying in %d seconds...", attempt, e, retry_delay)
            time.sleep(retry_delay)

async def run_worker():
    logger.info("Starting AI Worker...")
    
    mq_thread = threading.Thread(target=start_mq_consumer, daemon=True)
    mq_thread.start()
    
    trainer = IncrementalTrainer()
    
    scheduler = AsyncIOScheduler()
    scheduler.add_job(
        trainer.try_retrain,
        "cron",
        hour=2, minute=0,
        id="daily_model_retrain",
        replace_existing=True,
    )
    scheduler.start()
    logger.info("APScheduler started: daily retrain at 02:00 UTC")
    
    await trainer.try_retrain()

    try:
        while True:
            await asyncio.sleep(3600)
    except (KeyboardInterrupt, SystemExit):
        scheduler.shutdown(wait=False)

if __name__ == "__main__":
    asyncio.run(run_worker())
