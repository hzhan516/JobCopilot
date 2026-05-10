"""
Resume Assistant - Python AI service entry point.
智能求职助手 —— AI 服务入口，负责任务调度、HTTP API 及 MQ 消费者生命周期管理。
"""

import logging
import os
import socket
import threading

from fastapi import APIRouter, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager

from app.config import LOG_LEVEL, LLM_EMBEDDING_MODEL, LLM_EMBEDDING_MODEL_DIMENSION
from app.infrastructure.redis_client import get_redis
from app.mq.consumer import create_connection, setup_all_queues, start_all_consumers

from app.schemas import (
    EmbeddingRequest,
    EmbeddingResponse,
    JobMatchRequest,
    JobMatchResponse,
    SuitabilityRequest,
    SuitabilityResponse,
)

from app.services.incremental_model_service import incremental_service
from app.services.job_matching_service import find_job_matches
from app.services.suitability_service import evaluate_suitability_with_vertex
from app.services.vector_service import generate_embedding
from app.services.backend_client import sync_existing_job_embeddings


logging.basicConfig(
    level=getattr(logging, LOG_LEVEL.upper(), logging.INFO),
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)

logger = logging.getLogger(__name__)

# MQ connection reference for graceful shutdown
_mq_connection = None
_mq_channel = None


def _start_sync_thread() -> None:
    """Launch a background thread to sync offline job embeddings into the backend vector store on startup.
    启动后台线程，在应用启动时将离线职位 embedding 同步到后端向量库，避免冷启动时向量检索为空。
    使用 Redis 分布式锁确保多实例下仅一个实例执行同步。"""
    def _run_sync() -> None:
        redis = get_redis()
        lock_key = "ra:ai:startup_sync_lock"
        lock_value = f"{os.getpid()}@{socket.gethostname()}"
        acquired = redis.set(lock_key, lock_value, nx=True, ex=600)
        if not acquired:
            logger.info("Startup sync already running on another instance, skipping.")
            return
        try:
            sync_existing_job_embeddings()
        except Exception:
            logger.exception("Background embedding sync failed")
        finally:
            lua = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
            try:
                redis.eval(lua, 1, lock_key, lock_value)
            except Exception:
                logger.exception("Failed to release startup sync lock")

    sync_thread = threading.Thread(target=_run_sync, name="embedding-sync-thread", daemon=True)
    sync_thread.start()
    logger.info("Embedding sync background thread started.")


@asynccontextmanager
async def lifespan(app: FastAPI):
    _start_mq_consumer_once()
    _start_sync_thread()
    yield
    # Shutdown / 优雅关闭
    logger.info("Shutting down MQ consumers...")
    global _mq_connection, _mq_channel
    if _mq_channel and _mq_channel.is_open:
        try:
            _mq_channel.stop_consuming()
        except Exception:
            pass
    if _mq_connection and _mq_connection.is_open:
        try:
            _mq_connection.close()
        except Exception:
            pass
    logger.info("MQ consumers shut down.")


app = FastAPI(
    title="Resume Assistant AI Service",
    description="AI service for scraping, parsing, and job processing.",
    version="1.0.0",
    lifespan=lifespan,
)

admin_router = APIRouter(prefix="/admin", tags=["admin"])


@admin_router.post("/recompute-model")
def recompute_model(
    x_internal_api_key: str | None = Header(None, alias="X-Internal-API-Key"),
):
    """强制触发增量模型权重重算 / Force incremental model weight recomputation."""
    if INTERNAL_API_KEY and x_internal_api_key != INTERNAL_API_KEY:
        raise HTTPException(status_code=401, detail="Unauthorized")
    result = incremental_service.recompute_weights()
    return {"status": "ok", "version": result["version"]}


app.include_router(admin_router, prefix="/api/v1")

# ---------------------------------------------------------------------------
# Internal API Key Middleware (Defense in Depth)
# 内部 API Key 中间件 —— 纵深防御，防止 AI 服务被外部直接访问。
# ---------------------------------------------------------------------------
INTERNAL_API_KEY = os.getenv("INTERNAL_API_KEY", "")

# Whitelist paths that must remain accessible without authentication (health, docs).
# 以下路径免鉴权，确保探针和文档端点始终可访问。
_SKIP_AUTH_PATHS = {"/health", "/", "/docs", "/openapi.json", "/redoc"}


@app.middleware("http")
async def internal_api_key_middleware(request: Request, call_next):
    """Validate X-Internal-API-Key header for all inbound HTTP requests.
    Skipped when INTERNAL_API_KEY is unset (local development) or path is whitelisted.
    对所有入站 HTTP 请求校验 X-Internal-API-Key header。
    当 INTERNAL_API_KEY 未设置（本地开发）或路径在白名单内时跳过检查。"""
    if INTERNAL_API_KEY and request.url.path not in _SKIP_AUTH_PATHS:
        provided = request.headers.get("X-Internal-API-Key", "")
        if provided != INTERNAL_API_KEY:
            logger.warning(
                "Unauthorized request: path=%s, remote=%s, header_present=%s",
                request.url.path,
                request.client.host if request.client else "unknown",
                bool(provided),
            )
            return JSONResponse(
                status_code=401,
                content={"detail": "Unauthorized: invalid or missing internal API key"},
            )
    return await call_next(request)


_mq_started = False
_mq_is_connected = False
_mq_lock = threading.Lock()


@app.get("/")
async def root():
    return {
        "service": "Resume Assistant AI",
        "version": "1.0.0",
        "status": "running",
    }


@app.get("/health")
async def health_check():
    if not _mq_is_connected:
        return JSONResponse(
            status_code=503,
            content={
                "status": "degraded",
                "mq_connected": False,
                "message": "Initializing or waiting for RabbitMQ connection..."
            }
        )
    return {
        "status": "healthy",
        "mq_connected": True,
        "vertex_project_configured": os.getenv("VERTEX_PROJECT_ID") is not None and os.getenv("VERTEX_PROJECT_ID") != "ser594-ai-service",
        "vertex_location": os.getenv("VERTEX_LOCATION", "global"),
    }


@app.get("/api/status")
async def status():
    return {
        "service": "AI Service",
        "features": [
            "resume_parse",
            "embedding_generation",
            "job_matching",
            "chat_processing",
        ],
        "ready": True,
    }


def initialize_mq() -> None:
    global _mq_is_connected, _mq_connection, _mq_channel
    import time
    
    retry_delay = 5
    
    logger = logging.getLogger(__name__)
    attempt = 0
    while True:
        attempt += 1
        try:
            logger.info("Starting RabbitMQ consumers (Attempt %d)...", attempt)
            connection = create_connection()
            channel = connection.channel()
            setup_all_queues(channel)
            logger.info("RabbitMQ consumers are ready.")
            _mq_is_connected = True
            _mq_connection = connection
            _mq_channel = channel
            start_all_consumers(channel)
            break
        except Exception as e:
            _mq_is_connected = False
            logger.warning("RabbitMQ consumer startup failed (Attempt %d): %s. Retrying in %d seconds...", attempt, e, retry_delay)
            time.sleep(retry_delay)


def _start_mq_consumer_once() -> None:
    global _mq_started

    with _mq_lock:
        if _mq_started:
            return

        consumer_thread = threading.Thread(
            target=initialize_mq,
            name="rabbitmq-consumer-thread",
            daemon=True,
        )
        consumer_thread.start()
        _mq_started = True
        logger = logging.getLogger(__name__)
        logger.info("RabbitMQ consumer thread started.")




@app.post("/api/v1/suitability", response_model=SuitabilityResponse)
async def evaluate_suitability(request: SuitabilityRequest) -> SuitabilityResponse:
    return evaluate_suitability_with_vertex(request)


@app.post("/api/v1/match", response_model=JobMatchResponse)
async def match_jobs(request: JobMatchRequest) -> JobMatchResponse:
    return find_job_matches(request)


@app.post("/api/v1/ai/embeddings", response_model=EmbeddingResponse)
async def batch_embeddings(request: EmbeddingRequest) -> EmbeddingResponse:
    if not request.texts:
        return EmbeddingResponse(
            embeddings=[],
            modelUsed=LLM_EMBEDDING_MODEL,
            count=0,
        )

    embeddings: list[list[float]] = []
    for index, text in enumerate(request.texts):
        try:
            embeddings.append(generate_embedding(text))
        except Exception:
            logger.exception("Embedding failed for input index=%d, length=%d", index, len(text))
            embeddings.append([0.0] * LLM_EMBEDDING_MODEL_DIMENSION)

    return EmbeddingResponse(
        embeddings=embeddings,
        modelUsed=request.model or LLM_EMBEDDING_MODEL,
        count=len(embeddings),
    )


if __name__ == "__main__":
    _start_mq_consumer_once()

    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
