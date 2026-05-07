import logging
import time
from typing import Any

import requests

from app.config import BACKEND_QUERY_TIMEOUT, BACKEND_SERVICE_URL
from app.schemas import JobMatchRequest, JobMatchResponse, MatchFactors, MatchItem
from app.services.vector_service import generate_embedding

logger = logging.getLogger(__name__)


def _clip_score(value: float) -> float:
    return max(0.0, min(1.0, value))


def _truncate_description(description: str) -> str:
    normalized = description.strip().replace("\n", " ")
    if len(normalized) > 280:
        return normalized[:280].rstrip() + "..."
    return normalized


def _extract_search_results(payload: Any) -> list[dict[str, Any]]:
    if isinstance(payload, list):
        return [item for item in payload if isinstance(item, dict)]

    if isinstance(payload, dict) and isinstance(payload.get("data"), list):
        return [item for item in payload["data"] if isinstance(item, dict)]

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
        response = requests.post(
            f"{BACKEND_SERVICE_URL.rstrip('/')}/api/v1/jobs/vector-search",
            json={
                "queryText": query,
                "queryEmbedding": query_embedding,
                "limit": request.top_k,
                "filters": request.filters,
            },
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
        match_factors = result.get("matchFactors") or {}
        similarity = _clip_score(_to_float(result.get("similarity")))

        matches.append(
            MatchItem(
                jobId=str(result.get("jobId", "")),
                title=str(result.get("title", "")),
                company=str(result.get("company", "")),
                matchScore=round(similarity, 4),
                matchFactors=MatchFactors(
                    skillMatch=round(_clip_score(_to_float(match_factors.get("skillMatch"))), 4),
                    experienceMatch=round(_clip_score(_to_float(match_factors.get("experienceMatch"))), 4),
                    locationMatch=round(_clip_score(_to_float(match_factors.get("locationMatch"))), 4),
                ),
                description=_truncate_description(str(result.get("description", ""))),
            )
        )

    return JobMatchResponse(
        matches=matches,
        total=len(matches),
        recallTime=0,
        rankTime=rank_time,
    )
