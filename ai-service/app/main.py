"""
Resume Assistant - Python AI service entry point.
"""

import os
import threading

from fastapi import FastAPI, HTTPException
from contextlib import asynccontextmanager

from app.mq.consumer import create_connection, setup_all_queues, start_all_consumers

from app.schemas import (
    JobMatchRequest,
    JobMatchResponse,
    SuitabilityRequest,
    SuitabilityResponse,
)

from app.services.job_matching_service import find_job_matches
from app.services.suitability_service import evaluate_suitability_with_vertex


@asynccontextmanager
async def lifespan(app: FastAPI):
    _start_mq_consumer_once()
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
        raise HTTPException(status_code=503, detail="RabbitMQ consumer not connected")
    return {
        "status": "healthy",
        "vertex_project_configured": bool(os.getenv("VERTEX_PROJECT", "ser594-ai-service")),
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
    
    max_retries = 10
    retry_delay = 5
    
    for attempt in range(max_retries):
        try:
            print(f"Starting RabbitMQ consumers (Attempt {attempt + 1}/{max_retries})...")
            connection = create_connection()
            channel = connection.channel()
            setup_all_queues(channel)
            print("RabbitMQ consumers are ready.")
            _mq_is_connected = True
            start_all_consumers(channel)
            break # Successfully connected and consuming
        except Exception as exc:
            _mq_is_connected = False
            print(f"RabbitMQ consumer startup failed: {exc}", flush=True)
            if attempt < max_retries - 1:
                print(f"Retrying in {retry_delay} seconds...")
                time.sleep(retry_delay)
            else:
                print("Max retries reached. Giving up on RabbitMQ connection.")
                raise


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
        print("RabbitMQ consumer thread started.")





@app.post("/api/v1/suitability", response_model=SuitabilityResponse)
async def evaluate_suitability(request: SuitabilityRequest) -> SuitabilityResponse:
    return evaluate_suitability_with_vertex(request)


@app.post("/api/v1/match", response_model=JobMatchResponse)
async def match_jobs(request: JobMatchRequest) -> JobMatchResponse:
    return find_job_matches(request)


if __name__ == "__main__":
    _start_mq_consumer_once()

    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)

