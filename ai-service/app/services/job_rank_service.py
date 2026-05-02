import time
import litellm

from app.config import LLM_MODEL
from app.schemas import JobRankCommand, JobRankResultPayload, JobRankResultItem, MatchFactors
from app.services.vector_service import generate_embedding


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
    left_norm = sum(a * a for a in left) ** 0.5
    right_norm = sum(b * b for b in right) ** 0.5

    if left_norm == 0.0 or right_norm == 0.0:
        return 0.0

    return dot / (left_norm * right_norm)


def _build_job_text(job_id: str, command: JobRankCommand) -> str:
    details = command.job_details.get(job_id, {})
    if not isinstance(details, dict):
        details = {}

    return " ".join(
        [
            str(details.get("title", "")),
            str(details.get("company", "")),
            str(details.get("description", "")),
        ]
    ).strip()


def _rank_single_job(
    job_id: str,
    command: JobRankCommand,
    candidate_embedding: list[float] | None,
) -> JobRankResultItem:
    details = command.job_details.get(job_id, {})
    if not isinstance(details, dict):
        details = {}

    title = str(details.get("title", "")).strip()
    company = str(details.get("company", "")).strip()
    description = str(details.get("description", "")).strip()

    query_text = " ".join(part for part in [command.query, command.resume_text] if part).strip()
    query_tokens = _tokenize(query_text)
    title_tokens = _tokenize(title)
    description_tokens = _tokenize(description)

    skill_match = 0.0 if not query_tokens else len(query_tokens & title_tokens) / len(query_tokens)
    experience_match = 0.0 if not query_tokens else len(query_tokens & description_tokens) / len(query_tokens)

    semantic_match = 0.0
    if candidate_embedding is not None:
        try:
            job_embedding = generate_embedding(_build_job_text(job_id, command)[:8000])
        except Exception:
            job_embedding = None

        if job_embedding is not None:
            semantic_match = _clip_score((_cosine_similarity(candidate_embedding, job_embedding) + 1.0) / 2.0)

    match_score = _clip_score((skill_match * 0.35) + (experience_match * 0.25) + (semantic_match * 0.40))

    short_description = description.replace("\n", " ")
    if len(short_description) > 280:
        short_description = short_description[:277].rstrip() + "..."

    return JobRankResultItem(
        jobId=job_id,
        title=title,
        company=company,
        matchScore=round(match_score, 4),
        matchFactors=MatchFactors(
            skillMatch=round(_clip_score(max(skill_match, semantic_match)), 4),
            experienceMatch=round(_clip_score(max(experience_match, semantic_match)), 4),
            locationMatch=0.0,
        ),
        description=short_description,
        matchReason=None,
    )


def _generate_match_reason(command: JobRankCommand, job: JobRankResultItem) -> str | None:
    if not command.resume_text:
        return None

    # Safe truncation to prevent token overflow
    resume_snippet = command.resume_text[:3000]
    
    details = command.job_details.get(job.job_id, {})
    if not isinstance(details, dict):
        details = {}
        
    job_desc = str(details.get("description", ""))[:2000]
    
    prompt = f"""
You are an expert career advisor.
Briefly explain in 1-2 sentences why this job is a good fit for the candidate.
Focus on matching skills and experience.

Candidate Resume:
{resume_snippet}

Job Title: {job.title}
Job Company: {job.company}
Job Description:
{job_desc}
"""

    try:
        response = litellm.completion(
            model=LLM_MODEL,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=150
        )
        return response.choices[0].message.content.strip()
    except Exception as e:
        print(f"Failed to generate match reason for job {job.job_id}: {e}")
        return None

def rank_jobs(command: JobRankCommand) -> JobRankResultPayload:
    rank_start = time.perf_counter()
    query_text = " ".join(part for part in [command.query, command.resume_text] if part).strip()

    try:
        candidate_embedding = generate_embedding(query_text[:8000]) if query_text else None
    except Exception:
        candidate_embedding = None

    ranked_results = [
        _rank_single_job(job_id, command, candidate_embedding)
        for job_id in command.recalled_job_ids
    ]
    ranked_results.sort(key=lambda item: item.match_score, reverse=True)

    # RAG Generation: Generate match reasons for top 3 candidates
    for i in range(min(3, len(ranked_results))):
        reason = _generate_match_reason(command, ranked_results[i])
        if reason:
            ranked_results[i].match_reason = reason

    rank_time_ms = int((time.perf_counter() - rank_start) * 1000)

    return JobRankResultPayload(
        matchId=command.match_id,
        status="COMPLETED",
        rankTimeMs=rank_time_ms,
        rankedResults=ranked_results,
        errorMessage=None,
    )
