"""
Resume Assistant - Python AI service entry point.
"""

import os
import threading

from fastapi import FastAPI

from app.mq.consumer import create_connection, setup_all_queues, start_all_consumers

from app.schemas import (
    JobMatchRequest,
    JobMatchResponse,
    SuitabilityRequest,
    SuitabilityResponse,
)

from app.services.job_matching_service import find_job_matches
from app.services.suitability_service import evaluate_suitability_with_vertex


app = FastAPI(
    title="Resume Assistant AI Service",
    description="AI service for scraping, parsing, and job processing.",
    version="1.0.0",
)

_mq_started = False
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
    return {
        "status": "healthy",
        "vertex_ai_project_configured": bool(os.getenv("GOOGLE_CLOUD_PROJECT", "ser594-ai-service")),
        "vertex_ai_location": os.getenv("VERTEX_AI_LOCATION", "us-central1"),
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
    try:
        print("Starting RabbitMQ consumers...")
        connection = create_connection()
        channel = connection.channel()
        setup_all_queues(channel)
        print("RabbitMQ consumers are ready.")
        start_all_consumers(channel)
    except Exception as exc:
        print(f"RabbitMQ consumer startup failed: {exc}", flush=True)
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


@app.on_event("startup")
async def startup_event() -> None:
    _start_mq_consumer_once()


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

