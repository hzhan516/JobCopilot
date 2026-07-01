"""AI service admin endpoints — basic monitoring + advanced model/queue management."""

import asyncio
import json
import logging
import os
import time
from datetime import datetime, timezone
from typing import Any

import pika
from fastapi import APIRouter, Depends, Header, HTTPException
from pydantic import BaseModel

from app.api.model_manager import get_model_manager, model_manager
from app.config import (
    CONVERSATION_REQUEST_QUEUE,
    ENV,
    FEEDBACK_REQUEST_QUEUE,
    INTERNAL_API_KEY,
    JOB_PARSE_REQUEST_QUEUE,
    JOB_RANK_REQUEST_QUEUE,
    RABBITMQ_HOST,
    RABBITMQ_PASSWORD,
    RABBITMQ_PORT,
    RABBITMQ_USERNAME,
    RESUME_PARSE_REQUEST_QUEUE,
)
from app.infrastructure.minio.client import get_minio_client
from app.infrastructure.redis.client import get_redis_client
from app.worker.scheduler.trainer import get_trainer
from app import main as app_main


class ConfigReloadRequest(BaseModel):
    key: str
    value: str


class ModelRollbackRequest(BaseModel):
    version: str


logger = logging.getLogger(__name__)
router = APIRouter(prefix="/admin", tags=["admin"])

START_TIME = time.time()

# ─── Security ───


async def verify_internal_key(x_internal_api_key: str | None = Header(None)) -> bool:
    """Validate the internal API key for admin write endpoints.

    In development the middleware already skips auth; this dependency provides
    defence-in-depth and makes the contract explicit for each endpoint.
    """
    if ENV != "dev":
        if not x_internal_api_key or x_internal_api_key != INTERNAL_API_KEY:
            raise HTTPException(
                status_code=401,
                detail="Unauthorized: invalid or missing internal API key",
            )
    return True


# ─── Basic (read-only) ───


@router.get("/status")
async def admin_status():
    meta = model_manager._model_meta
    return {
        "service": "jobcopilot-ai-service",
        "version": app_main.APP_VERSION,
        "uptime_seconds": time.time() - START_TIME,
        "model_version": meta.get("version") if meta else None,
        "model_trained_at": meta.get("trained_at") if meta else None,
        "mq_connected": app_main._mq_is_connected,
    }


@router.get("/queue-stats")
async def queue_stats(_=Depends(verify_internal_key)):
    """Return real message counts for the AI service RabbitMQ queues."""
    queues = [
        JOB_PARSE_REQUEST_QUEUE,
        RESUME_PARSE_REQUEST_QUEUE,
        CONVERSATION_REQUEST_QUEUE,
        JOB_RANK_REQUEST_QUEUE,
        FEEDBACK_REQUEST_QUEUE,
    ]
    stats: dict[str, dict[str, Any]] = {}

    def _connect():
        return pika.BlockingConnection(
            pika.ConnectionParameters(
                host=RABBITMQ_HOST,
                port=RABBITMQ_PORT,
                credentials=pika.PlainCredentials(RABBITMQ_USERNAME, RABBITMQ_PASSWORD),
                connection_attempts=1,
                socket_timeout=3,
            )
        )

    try:
        connection = await asyncio.get_event_loop().run_in_executor(None, _connect)
        channel = await asyncio.get_event_loop().run_in_executor(
            None, connection.channel
        )
        for queue in queues:
            try:
                result = await asyncio.get_event_loop().run_in_executor(
                    None,
                    lambda q=queue: channel.queue_declare(queue=q, passive=True),
                )
                stats[queue] = {
                    "depth": result.method.message_count,
                    "consumers": result.method.consumer_count,
                }
            except Exception:
                stats[queue] = {"depth": "unavailable", "consumers": 0}
        await asyncio.get_event_loop().run_in_executor(None, connection.close)
    except Exception as exc:
        for queue in queues:
            stats[queue] = {
                "depth": "connection_error",
                "consumers": 0,
                "error": str(exc),
            }

    return {"queues": stats}


@router.get("/model/info")
async def model_info():
    meta = model_manager._model_meta
    if not meta:
        return {"loaded": False, "message": "No model loaded"}
    return {
        "loaded": True,
        "version": meta.get("version"),
        "trained_at": meta.get("trained_at"),
        "metrics": meta.get("metrics", {}),
        "filename": meta.get("model_filename"),
    }


@router.get("/model/history")
async def model_history(_=Depends(verify_internal_key)):
    """List all historical model versions stored in MinIO."""
    minio_client = get_minio_client()

    try:
        objects = minio_client.list_objects(prefix="ranker_model_")
        versions = []
        for obj in objects:
            key = obj.get("Key")
            if not key:
                continue
            last_modified = obj.get("LastModified")
            versions.append(
                {
                    "key": key,
                    "size": obj.get("Size"),
                    "last_modified": last_modified.isoformat() if last_modified else None,
                    "version": key.replace("ranker_model_", "").replace(".txt", ""),
                }
            )
        versions.sort(key=lambda v: v["last_modified"] or "", reverse=True)
        return {"versions": versions}
    except Exception as exc:
        return {"versions": [], "error": str(exc)}


# ─── Advanced (write, require internal key) ───


@router.post("/model/retrain")
async def manual_retrain(_=Depends(verify_internal_key)):
    """Manually trigger incremental model training."""
    trainer = get_trainer()

    try:
        result = await trainer.try_retrain()
        if result is None:
            return {
                "status": "skipped",
                "message": "No new feedback data or lock unavailable",
            }
        return {
            "status": "completed",
            "new_version": result.get("version"),
            "metrics": result.get("metrics"),
            "samples_used": result.get("sample_count"),
        }
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Retrain failed: {exc}") from exc


@router.post("/model/rollback")
async def rollback_model(request: ModelRollbackRequest, _=Depends(verify_internal_key)):
    """Roll the production model back to a previous version stored in MinIO."""
    model_manager = get_model_manager()
    minio_client = get_minio_client()
    redis_client = get_redis_client()

    target_version = request.version
    target_key: str | None = None

    # Accept either a full object key or a version identifier.
    if target_version.endswith(".txt") and minio_client.object_exists(target_version):
        target_key = target_version
    else:
        objects = minio_client.list_objects(prefix="ranker_model_")
        for obj in objects:
            key = obj.get("Key")
            if key and target_version in key:
                target_key = key
                break

    if not target_key:
        raise HTTPException(
            status_code=404,
            detail=f"Model version {target_version} not found in registry",
        )

    try:
        minio_client.update_latest_meta(
            {
                "version": target_version,
                "object_key": target_key,
                "rolled_back_at": datetime.now(timezone.utc).isoformat(),
            }
        )

        payload = {"version": target_version, "object_key": target_key}
        await redis_client.publish("ai.model.reload", json.dumps(payload))
        await model_manager.load_latest()

        return {
            "status": "rolled_back",
            "version": target_version,
            "message": f"Model rolled back to {target_version} and broadcast to all instances",
        }
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=500, detail=f"Rollback failed: {exc}"
        ) from exc


@router.post("/queue/purge/{queue_name}")
async def purge_queue(queue_name: str, _=Depends(verify_internal_key)):
    """Purge all messages from an allowed AI service queue."""
    allowed_queues = {
        JOB_PARSE_REQUEST_QUEUE,
        RESUME_PARSE_REQUEST_QUEUE,
        CONVERSATION_REQUEST_QUEUE,
        JOB_RANK_REQUEST_QUEUE,
        FEEDBACK_REQUEST_QUEUE,
    }
    if queue_name not in allowed_queues:
        raise HTTPException(status_code=400, detail=f"Unknown queue: {queue_name}")

    def _connect():
        return pika.BlockingConnection(
            pika.ConnectionParameters(
                host=RABBITMQ_HOST,
                port=RABBITMQ_PORT,
                credentials=pika.PlainCredentials(RABBITMQ_USERNAME, RABBITMQ_PASSWORD),
            )
        )

    try:
        connection = await asyncio.get_event_loop().run_in_executor(None, _connect)
        channel = await asyncio.get_event_loop().run_in_executor(
            None, connection.channel
        )
        result = await asyncio.get_event_loop().run_in_executor(
            None, lambda: channel.queue_purge(queue=queue_name)
        )
        purged_count = result.method.message_count
        await asyncio.get_event_loop().run_in_executor(None, connection.close)
        return {
            "status": "purged",
            "queue": queue_name,
            "messages_removed": purged_count,
        }
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Purge failed: {exc}") from exc


@router.post("/queue/retry-dlq/{queue_name}")
async def retry_dlq(queue_name: str, _=Depends(verify_internal_key)):
    """Move messages from the dead-letter queue back to the original queue."""
    dlq_name = f"{queue_name}.dlq"
    retried = 0

    def _connect():
        return pika.BlockingConnection(
            pika.ConnectionParameters(
                host=RABBITMQ_HOST,
                port=RABBITMQ_PORT,
                credentials=pika.PlainCredentials(RABBITMQ_USERNAME, RABBITMQ_PASSWORD),
            )
        )

    try:
        connection = await asyncio.get_event_loop().run_in_executor(None, _connect)
        channel = await asyncio.get_event_loop().run_in_executor(
            None, connection.channel
        )

        while True:
            method, properties, body = await asyncio.get_event_loop().run_in_executor(
                None,
                lambda: channel.basic_get(queue=dlq_name, auto_ack=False),
            )
            if method is None:
                break
            await asyncio.get_event_loop().run_in_executor(
                None,
                lambda: channel.basic_publish(
                    exchange="", routing_key=queue_name, body=body
                ),
            )
            await asyncio.get_event_loop().run_in_executor(
                None,
                lambda: channel.basic_ack(delivery_tag=method.delivery_tag),
            )
            retried += 1

        await asyncio.get_event_loop().run_in_executor(None, connection.close)
        return {
            "status": "completed",
            "queue": queue_name,
            "messages_retried": retried,
        }
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"DLQ retry failed: {exc}") from exc


@router.post("/cache/flush")
async def flush_cache(_=Depends(verify_internal_key)):
    """Flush AI-service-owned Redis keys without touching backend session/config data."""
    redis = get_redis_client()

    try:
        ai_prefixes = ["ai:feedback:", "ai:lock:", "ai:cache:"]
        deleted = 0
        for prefix in ai_prefixes:
            keys = await redis.keys(f"{prefix}*")
            if keys:
                deleted += await redis.delete(*keys)

        return {"status": "flushed", "keys_deleted": deleted}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Cache flush failed: {exc}") from exc


# ─── Config hot-reload ───


@router.post("/config/reload")
async def reload_config(req: ConfigReloadRequest):
    """Receive config change from backend, hot-reload if applicable."""
    logger.info("Config reload: %s = %s", req.key, req.value)
    if req.key.startswith("llm.") or req.key.startswith("log.") or req.key.startswith("ai."):
        if req.key == "log.aiServiceLevel":
            logging.getLogger().setLevel(req.value.upper())
        elif req.key == "ai.textModel":
            os.environ["LLM_TEXT_MODEL"] = req.value
        elif req.key == "ai.visionModel":
            os.environ["LLM_VISION_MODEL"] = req.value
        elif req.key == "ai.embeddingModel":
            os.environ["LLM_EMBEDDING_MODEL"] = req.value
        # ponytail: LLM client reinitialization deferred
        return {"status": "applied", "key": req.key}
    return {"status": "ignored", "key": req.key, "reason": "not a hot-reloadable key"}
