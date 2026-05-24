import logging
import time
from typing import Any

import requests

from app.config import BACKEND_QUERY_TIMEOUT, BACKEND_SERVICE_URL, INTERNAL_API_KEY
from app.schemas import JobMatchRequest, JobMatchResponse, MatchFactors, MatchItem, VectorSearchResult
from app.services.vector_service import generate_embedding

logger = logging.getLogger(__name__)


def _clip_score(value: float) -> float:
    return max(0.0, min(1.0, value))


def _truncate_description(description: str) -> str:
    normalized = description.strip().replace("\n", " ")
    if len(normalized) > 280:
        return normalized[:280].rstrip() + "..."
    return normalized


def _extract_search_results(payload: Any) -> list[VectorSearchResult]:
    if isinstance(payload, list):
        return [VectorSearchResult.model_validate(item) for item in payload if isinstance(item, dict)]

    if isinstance(payload, dict) and isinstance(payload.get("data"), list):
        return [VectorSearchResult.model_validate(item) for item in payload["data"] if isinstance(item, dict)]

    return []


def _to_float(value: Any, default: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def find_job_matches(request: JobMatchRequest) -> JobMatchResponse:
    """Embed the user query and delegate vector search to the backend to avoid in-memory caches.
    生成查询向量后调用后端向量检索：避免 AI 服务常驻缓存导致内存膨胀，同时利用后端 pgvector 的索引加速。"""
    query = request.query.strip()
    if not query:
        return JobMatchResponse(matches=[], total=0, recallTime=0, rankTime=0)

    rank_start = time.perf_counter()

    try:
        query_embedding = generate_embedding(query)
    except Exception:
        logger.exception("Query embedding failed")
        query_embedding = None

    try:
        headers = {"X-Internal-API-Key": INTERNAL_API_KEY} if INTERNAL_API_KEY else {}
        response = requests.post(
            f"{BACKEND_SERVICE_URL.rstrip('/')}/api/v1/jobs/vector-search",
            json={
                "queryText": query,
                "queryEmbedding": query_embedding,
                "limit": request.top_k,
                "filters": request.filters,
            },
            headers=headers,
            timeout=BACKEND_QUERY_TIMEOUT,
        )
        response.raise_for_status()
        search_results = _extract_search_results(response.json())
    except Exception:
        logger.exception("Backend vector-search failed")
        return JobMatchResponse(matches=[], total=0, recallTime=0, rankTime=0)

    rank_time = int((time.perf_counter() - rank_start) * 1000)

    matches = []
    for result in search_results:
        matches.append(
            MatchItem(
                jobId=result.job_id,
                title=result.title,
                company=result.company,
                matchScore=round(_clip_score(result.similarity), 4),
                matchFactors=MatchFactors(
                    skillMatch=round(_clip_score(result.match_factors.skill_match), 4),
                    experienceMatch=round(_clip_score(result.match_factors.experience_match), 4),
                    locationMatch=round(_clip_score(result.match_factors.location_match), 4),
                ),
                description=_truncate_description(result.description),
            )
        )

    return JobMatchResponse(
        matches=matches,
        total=len(matches),
        recallTime=0,
        rankTime=rank_time,
    )
