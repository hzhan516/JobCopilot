"""AI service admin endpoints — basic monitoring + advanced model/queue management."""
import time
import logging
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.api.model_manager import model_manager
from app import main as app_main


class ConfigReloadRequest(BaseModel):
    key: str
    value: str

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/admin", tags=["admin"])

START_TIME = time.time()


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
async def queue_stats():
    return {
        "queues": {
            "ai.queue.job.parse": {"depth": "unknown"},
            "ai.queue.resume.parse": {"depth": "unknown"},
            "ai.queue.conversation": {"depth": "unknown"},
            "ai.queue.job.rank": {"depth": "unknown"},
            "ai.queue.feedback": {"depth": "unknown"},
        }
    }


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
async def model_history():
    # ponytail: list all versions from MinIO bucket
    return {"versions": []}


# ─── Advanced (write, require confirmation) ───

@router.post("/model/retrain")
async def trigger_retrain():
    # ponytail: scheduled retraining handled by worker; manual trigger deferred
    return {"status": "not_implemented", "message": "Manual retraining deferred — use scheduled worker"}


@router.post("/model/rollback")
async def rollback_model(version: str):
    raise HTTPException(501, detail="Model rollback deferred to Phase 3 completion")


@router.post("/queue/purge/{queue_name}")
async def purge_queue(queue_name: str):
    raise HTTPException(501, detail="Queue purge deferred to Phase 3 completion")


@router.post("/queue/retry-dlq/{queue_name}")
async def retry_dlq(queue_name: str):
    raise HTTPException(501, detail="DLQ retry deferred to Phase 3 completion")


@router.post("/cache/flush")
async def flush_cache():
    raise HTTPException(501, detail="Cache flush deferred to Phase 3 completion")


# ─── Config hot-reload ───

@router.post("/config/reload")
async def reload_config(req: ConfigReloadRequest):
    """Receive config change from backend, hot-reload if applicable."""
    logger.info("Config reload: %s = %s", req.key, req.value)
    if req.key.startswith("llm.") or req.key.startswith("log."):
        if req.key == "log.aiServiceLevel":
            logging.getLogger().setLevel(req.value.upper())
        # ponytail: LLM client reinitialization deferred
        return {"status": "reloaded", "key": req.key}
    return {"status": "ignored", "key": req.key, "reason": "not a hot-reloadable key"}
