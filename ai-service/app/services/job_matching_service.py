import json
import math
import time
from pathlib import Path

from app.schemas import JobMatchRequest, JobMatchResponse, MatchFactors, MatchItem
from app.services.vector_service import generate_embedding


JOBS_DATASET_FILE = Path(__file__).resolve().parents[2] / "data_pipeline" / "output" / "normalized_jobs_sample.jsonl"
_JOB_ROWS_CACHE: list[dict] | None = None
_JOB_EMBEDDING_CACHE: dict[str, list[float]] = {}


def _load_jobs() -> list[dict]:
    global _JOB_ROWS_CACHE

    if _JOB_ROWS_CACHE is not None:
        return _JOB_ROWS_CACHE

    rows: list[dict] = []
    if not JOBS_DATASET_FILE.exists():
        _JOB_ROWS_CACHE = rows
        return rows

    with JOBS_DATASET_FILE.open("r", encoding="utf-8") as file:
        for line in file:
            line = line.strip()
            if not line:
                continue

            payload = json.loads(line)
            if payload.get("job_id") and payload.get("title"):
                rows.append(payload)

    _JOB_ROWS_CACHE = rows
    return rows


def _tokenize(text: str) -> set[str]:
    token = []
    tokens: set[str] = set()

    for char in text.lower():
        if char.isalnum():
            token.append(char)
        else:
            if len(token) >= 2:
                tokens.add("".join(token))
            token = []

    if len(token) >= 2:
        tokens.add("".join(token))

    return tokens


def _clip_score(value: float) -> float:
    return max(0.0, min(1.0, value))


def _cosine_similarity(left: list[float], right: list[float]) -> float:
    if not left or not right or len(left) != len(right):
        return 0.0

    dot = sum(a * b for a, b in zip(left, right))
    left_norm = math.sqrt(sum(a * a for a in left))
    right_norm = math.sqrt(sum(b * b for b in right))

    if left_norm == 0.0 or right_norm == 0.0:
        return 0.0

    return dot / (left_norm * right_norm)


def _job_text(job: dict) -> str:
    requirements = " ".join(job.get("requirements", []))
    return " ".join(
        [
            str(job.get("title", "")),
            str(job.get("company", "")),
            str(job.get("description", "")),
            requirements,
        ]
    ).strip()


def _get_job_embedding(job: dict) -> list[float] | None:
    job_id = str(job.get("job_id", "")).strip()
    if not job_id:
        return None

    cached = _JOB_EMBEDDING_CACHE.get(job_id)
    if cached is not None:
        return cached

    try:
        embedding = generate_embedding(_job_text(job)[:8000])
    except Exception:
        return None

    _JOB_EMBEDDING_CACHE[job_id] = embedding
    return embedding


def _score_job(query: str, query_embedding: list[float] | None, job: dict, filters: dict[str, str]) -> MatchItem:
    query_tokens = _tokenize(query)
    title_tokens = _tokenize(str(job.get("title", "")))
    requirements_tokens = {
        token
        for requirement in job.get("requirements", [])
        for token in _tokenize(str(requirement))
    }
    description_tokens = _tokenize(str(job.get("description", "")))

    skill_source = title_tokens | requirements_tokens
    skill_match = 0.0 if not query_tokens else len(query_tokens & skill_source) / len(query_tokens)
    experience_match = 0.0 if not query_tokens else len(query_tokens & description_tokens) / len(query_tokens)

    filter_values = " ".join(value for value in filters.values() if value).strip()
    if not filter_values:
        location_match = 1.0
    else:
        filter_tokens = _tokenize(filter_values)
        location_match = 1.0 if filter_tokens & description_tokens else 0.0

    semantic_match = 0.0
    if query_embedding is not None:
        job_embedding = _get_job_embedding(job)
        if job_embedding is not None:
            semantic_match = _clip_score((_cosine_similarity(query_embedding, job_embedding) + 1.0) / 2.0)

    match_score = _clip_score(
        (skill_match * 0.4) +
        (experience_match * 0.2) +
        (location_match * 0.1) +
        (semantic_match * 0.3)
    )

    description = str(job.get("description", "")).strip().replace("\n", " ")
    if len(description) > 280:
        description = description[:277].rstrip() + "..."

    return MatchItem(
        jobId=str(job.get("job_id", "")).strip(),
        title=str(job.get("title", "")).strip(),
        company=str(job.get("company", "")).strip(),
        matchScore=round(match_score, 4),
        matchFactors=MatchFactors(
            skillMatch=round(_clip_score(skill_match), 4),
            experienceMatch=round(_clip_score(max(experience_match, semantic_match)), 4),
            locationMatch=round(_clip_score(location_match), 4),
        ),
        description=description,
    )


def find_job_matches(request: JobMatchRequest) -> JobMatchResponse:
    query = request.query.strip()
    if not query:
        return JobMatchResponse(matches=[], total=0, recallTime=0, rankTime=0)

    jobs = _load_jobs()

    recall_start = time.perf_counter()
    candidate_jobs = jobs
    if request.filters:
        filter_tokens = {
            token
            for value in request.filters.values()
            for token in _tokenize(value)
        }
        if filter_tokens:
            filtered = []
            for job in jobs:
                haystack = _tokenize(_job_text(job))
                if filter_tokens & haystack:
                    filtered.append(job)
            candidate_jobs = filtered or jobs

    recall_time = int((time.perf_counter() - recall_start) * 1000)

    rank_start = time.perf_counter()
    try:
        query_embedding = generate_embedding(query)
    except Exception:
        query_embedding = None

    ranked_matches = [
        _score_job(query, query_embedding, job, request.filters)
        for job in candidate_jobs
    ]
    ranked_matches.sort(key=lambda item: item.match_score, reverse=True)
    top_k = max(1, request.top_k)
    matches = ranked_matches[:top_k]
    rank_time = int((time.perf_counter() - rank_start) * 1000)

    return JobMatchResponse(
        matches=matches,
        total=len(matches),
        recallTime=recall_time,
        rankTime=rank_time,
    )
