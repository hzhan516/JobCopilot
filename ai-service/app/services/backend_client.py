import json
import logging
from pathlib import Path

import requests
from tenacity import retry, stop_after_attempt, wait_exponential

from app.config import (
    BACKEND_BATCH_SIZE,
    BACKEND_BATCH_UPSERT_TIMEOUT,
    BACKEND_SERVICE_URL,
    LLM_EMBEDDING_MODEL,
)
from app.services.vector_service import generate_embedding

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
        response = requests.post(
            JOB_VECTOR_BATCH_URL,
            json=payload,
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
        response = requests.post(
            RESUME_VECTOR_BATCH_URL,
            json=payload,
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


def sync_existing_job_embeddings(
    data_file: Path | str | None = None,
    batch_size: int = BACKEND_BATCH_SIZE,
) -> dict:
    """Startup sync: read offline data-pipeline output, generate embeddings, and batch-write to backend.
    启动时同步离线数据：读取数据管道产出的归一化职位数据，生成 embedding 后批量写入后端，
    使向量库在系统启动后即具备可检索数据，避免冷启动空窗期。"""
    if data_file is None:
        project_root = Path(__file__).resolve().parent.parent.parent
        data_file = project_root / "data_pipeline" / "output" / "normalized_jobs_sample.jsonl"
    else:
        data_file = Path(data_file)

    if not data_file.is_file():
        logger.info("Data file not found: %s, skipping startup sync.", data_file)
        return {"total": 0, "success": 0, "failed": 0, "skipped": 0}

    total = 0
    success = 0
    failed = 0
    skipped = 0
    batch = []

    logger.info("Starting startup sync from %s", data_file)

    with data_file.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                record = json.loads(line)
            except json.JSONDecodeError:
                logger.warning("Invalid JSON line, skipping")
                continue

            job_id = record.get("job_id")
            if not job_id:
                continue

            text = record.get("description") or record.get("title") or ""
            if not text:
                logger.warning("No text for job_id %s, skipping embedding generation", job_id)
                continue

            try:
                embedding = generate_embedding(text)
            except Exception:
                logger.exception("Embedding generation failed for job_id=%s", job_id)
                failed += 1
                continue

            item = _build_job_vector_item(
                reference_id=job_id,
                embedding=embedding,
                title=record.get("title", ""),
                description=record.get("description", ""),
                requirements=record.get("requirements", []),
                raw_content=record.get("description", ""),
                source_file=str(data_file),
                model_version=LLM_EMBEDDING_MODEL,
            )
            batch.append(item)
            total += 1

            if len(batch) >= batch_size:
                result = batch_upsert_job_vectors(batch)
                success += result.get("success", 0)
                failed += result.get("failed", 0)
                skipped += result.get("skipped", 0)
                batch.clear()

    if batch:
        result = batch_upsert_job_vectors(batch)
        success += result.get("success", 0)
        failed += result.get("failed", 0)
        skipped += result.get("skipped", 0)

    logger.info(
        "Startup sync completed. Total processed: %d, Success: %d, Skipped: %d, Failed: %d",
        total, success, skipped, failed,
    )
    return {"total": total, "success": success, "failed": failed, "skipped": skipped}
