import json
import logging

import requests
from tenacity import retry, stop_after_attempt, wait_exponential

from app.config import (
    BACKEND_QUERY_TIMEOUT,
    BACKEND_SERVICE_URL,
    INTERNAL_API_KEY,
    LLM_EMBEDDING_MODEL,
    BACKEND_BATCH_UPSERT_TIMEOUT,
)

logger = logging.getLogger(__name__)

JOB_VECTOR_BATCH_URL = f"{BACKEND_SERVICE_URL.rstrip('/')}/api/v1/job-vectors/batch"
RESUME_VECTOR_BATCH_URL = f"{BACKEND_SERVICE_URL.rstrip('/')}/api/v1/resume-vectors/batch"

# Exponential backoff for transient backend failures (network blips, short outages).
# 指数退避重试策略：应对后端瞬时网络抖动或短暂不可用，避免请求风暴。
RETRY_STRATEGY = retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=10),
)


@RETRY_STRATEGY
def batch_upsert_job_vectors(items: list[dict]) -> dict:
    """Batch upsert job embeddings to the backend vector store.
    批量写入职位向量：后端可能因并发或 GC 出现瞬时失败，因此配置重试与超时。"""
    if not items:
        return {"total": 0, "success": 0, "failed": 0, "skipped": 0, "failedJobIds": []}

    payload = {"items": items}
    try:
        headers = {"X-Internal-API-Key": INTERNAL_API_KEY} if INTERNAL_API_KEY else {}
        response = requests.post(
            JOB_VECTOR_BATCH_URL,
            json=payload,
            headers=headers,
            timeout=BACKEND_BATCH_UPSERT_TIMEOUT,
        )
        response.raise_for_status()
        body = response.json()
        if isinstance(body, dict) and "data" in body:
            return body["data"]
        return body
    except Exception:
        logger.exception("Failed to batch upsert job vectors to backend, url=%s", JOB_VECTOR_BATCH_URL)
        return {
            "total": len(items),
            "success": 0,
            "failed": len(items),
            "skipped": 0,
            "failedJobIds": [item.get("jobId") for item in items],
        }


@RETRY_STRATEGY
def batch_upsert_resume_vectors(items: list[dict]) -> dict:
    """Batch upsert resume embeddings to the backend vector store.
    批量写入简历向量：与职位向量使用相同的重试策略，保证双写一致性。"""
    if not items:
        return {"total": 0, "success": 0, "failed": 0, "skipped": 0, "failedResumeVersionIds": []}

    payload = {"items": items}
    try:
        headers = {"X-Internal-API-Key": INTERNAL_API_KEY} if INTERNAL_API_KEY else {}
        response = requests.post(
            RESUME_VECTOR_BATCH_URL,
            json=payload,
            headers=headers,
            timeout=BACKEND_BATCH_UPSERT_TIMEOUT,
        )
        response.raise_for_status()
        body = response.json()
        if isinstance(body, dict) and "data" in body:
            return body["data"]
        return body
    except Exception:
        logger.exception("Failed to batch upsert resume vectors to backend, url=%s", RESUME_VECTOR_BATCH_URL)
        return {
            "total": len(items),
            "success": 0,
            "failed": len(items),
            "skipped": 0,
            "failedResumeVersionIds": [item.get("resumeVersionId") for item in items],
        }


def _build_job_vector_item(
    reference_id: str,
    embedding: list[float],
    title: str = "",
    description: str = "",
    requirements: list[str] | None = None,
    raw_content: str = "",
    source_file: str = "",
    model_version: str = "",
) -> dict:
    return {
        "jobId": reference_id,
        "embedding": embedding,
        "title": title,
        "description": description,
        "requirements": requirements or [],
        "rawContent": raw_content,
        "sourceFile": source_file,
        "modelVersion": model_version or LLM_EMBEDDING_MODEL,
    }


def _build_resume_vector_item(
    reference_id: str,
    embedding: list[float],
) -> dict:
    return {
        "resumeVersionId": reference_id,
        "embedding": embedding,
    }


