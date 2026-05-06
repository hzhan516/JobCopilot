"""
Resume Assistant - Python AI service entry point.
"""

import logging
import os
import threading

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager

from app.config import LOG_LEVEL, LLM_EMBEDDING_MODEL, LLM_EMBEDDING_MODEL_DIMENSION
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
from app.services.backend_client import sync_existing_job_embeddings


logging.basicConfig(
    level=getattr(logging, LOG_LEVEL.upper(), logging.INFO),
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)

logger = logging.getLogger(__name__)

def _start_sync_thread() -> None:
    """启动后台线程同步已有 embedding 数据到后端 / Start background thread to sync existing embeddings."""
    def _run_sync() -> None:
        try:
            sync_existing_job_embeddings()
        except Exception:
            logger.exception("Background embedding sync failed")

    sync_thread = threading.Thread(target=_run_sync, name="embedding-sync-thread", daemon=True)
    sync_thread.start()
    logger.info("Embedding sync background thread started.")


@asynccontextmanager
async def lifespan(app: FastAPI):
    _start_mq_consumer_once()
    _start_sync_thread()
    yield

app = FastAPI(
    title="Resume Assistant AI Service",
    description="AI service for scraping, parsing, and job processing.",
    version="1.0.0",
    lifespan=lifespan,
)

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
    global _mq_is_connected
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
            start_all_consumers(channel)
            break # Successfully connected and consuming
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
