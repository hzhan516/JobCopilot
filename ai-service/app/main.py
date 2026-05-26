"""
Resume Assistant - Python AI service entry point.
智能求职助手 —— AI 服务入口，负责任务调度、HTTP API 及 MQ 消费者生命周期管理。
"""

import logging
import os
import sys
import threading

from fastapi import APIRouter, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager

from app.config import LOG_LEVEL, LLM_EMBEDDING_MODEL, LLM_EMBEDDING_MODEL_DIMENSION, INTERNAL_API_KEY, ENV
from app.mq.consumer import create_connection, setup_all_queues, start_all_consumers

from app.schemas import (
    EmbeddingRequest,
    EmbeddingResponse,
    JobMatchRequest,
    JobMatchResponse,
    SuitabilityRequest,
    SuitabilityResponse,
)

from app.services.job_matching_service import find_job_matches
from app.services.suitability_service import evaluate_suitability_with_vertex
from app.services.vector_service import generate_embedding

logging.basicConfig(
    level=getattr(logging, LOG_LEVEL.upper(), logging.INFO),
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)

logger = logging.getLogger(__name__)

# MQ connection reference for graceful shutdown
_mq_connection = None
_mq_channel = None

import asyncio
from app.api.model_manager import model_manager

@asynccontextmanager
async def lifespan(app: FastAPI):
    _start_mq_consumer_once()
    await model_manager.load_latest()
    reload_task = asyncio.create_task(model_manager.watch_for_reloads())
    yield
    # Shutdown / 优雅关闭
    logger.info("Shutting down MQ consumers...")
    reload_task.cancel()
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
def recompute_model():
    raise HTTPException(status_code=410, detail="Deprecated. Use ai-worker scheduling.")


app.include_router(admin_router, prefix="/api/v1")

# ---------------------------------------------------------------------------
# Internal API Key Middleware (Defense in Depth)
# 内部 API Key 中间件 —— 纵深防御，防止 AI 服务被外部直接访问。
# ---------------------------------------------------------------------------

# Whitelist paths that must remain accessible without authentication (health, docs).
# 以下路径免鉴权，确保探针和文档端点始终可访问。
_SKIP_AUTH_PATHS = {"/health", "/", "/docs", "/openapi.json", "/redoc"}


@app.middleware("http")
async def internal_api_key_middleware(request: Request, call_next):
    """Validate X-Internal-API-Key header for all inbound HTTP requests.
    Skipped when ENV=dev (local development) or path is whitelisted.
    对所有入站 HTTP 请求校验 X-Internal-API-Key header。
    开发环境或白名单路径跳过检查。"""
    if ENV != "dev" and request.url.path not in _SKIP_AUTH_PATHS:
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
        "vertex_project_configured": os.getenv("VERTEX_PROJECT_ID") is not None and os.getenv("VERTEX_PROJECT_ID") != "jobcopilot-ai-service",
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
    MAX_MQ_RETRIES = 20

    logger = logging.getLogger(__name__)
    attempt = 0
    while attempt < MAX_MQ_RETRIES:
        attempt += 1
        try:
            logger.info("Starting RabbitMQ consumers (Attempt %d/%d)...", attempt, MAX_MQ_RETRIES)
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
            logger.warning("RabbitMQ consumer startup failed (Attempt %d/%d): %s. Retrying in %d seconds...", attempt, MAX_MQ_RETRIES, e, retry_delay)
            time.sleep(retry_delay)
    else:
        logger.error("Max MQ retries (%d) exceeded. RabbitMQ unavailable. Shutting down.", MAX_MQ_RETRIES)
        os._exit(1)


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
def evaluate_suitability(request: SuitabilityRequest) -> SuitabilityResponse:
    return evaluate_suitability_with_vertex(request)


@app.post("/api/v1/match", response_model=JobMatchResponse)
def match_jobs(request: JobMatchRequest) -> JobMatchResponse:
    return find_job_matches(request)


@app.post("/api/v1/ai/embeddings", response_model=EmbeddingResponse)
def batch_embeddings(request: EmbeddingRequest) -> EmbeddingResponse:
    # Runtime guard — schema validation already enforces limits, but defense in depth.
    # 运行时守卫：schema 校验已生效，此处为纵深防御。
    from app.config import EMBEDDING_MAX_BATCH_SIZE, EMBEDDING_MAX_TEXT_LENGTH
    if len(request.texts) > EMBEDDING_MAX_BATCH_SIZE:
        raise HTTPException(
            status_code=400,
            detail=f"Batch size exceeds maximum of {EMBEDDING_MAX_BATCH_SIZE}",
        )
    for i, text in enumerate(request.texts):
        if len(text) > EMBEDDING_MAX_TEXT_LENGTH:
            raise HTTPException(
                status_code=400,
                detail=f"Text at index {i} exceeds maximum length of {EMBEDDING_MAX_TEXT_LENGTH} characters",
            )

    if not request.texts:
        return EmbeddingResponse(
            embeddings=[],
            modelUsed=LLM_EMBEDDING_MODEL,
            count=0,
        )

    embeddings: list[list[float]] = []
    failed_indices: list[int] = []
    for index, text in enumerate(request.texts):
        try:
            embeddings.append(generate_embedding(text))
        except Exception:
            logger.exception("Embedding failed for input index=%d, length=%d", index, len(text))
            failed_indices.append(index)

    return EmbeddingResponse(
        embeddings=embeddings,
        modelUsed=request.model or LLM_EMBEDDING_MODEL,
        count=len(embeddings),
        failedIndices=failed_indices,
        errorCount=len(failed_indices),
    )


if __name__ == "__main__":
    _start_mq_consumer_once()

    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
