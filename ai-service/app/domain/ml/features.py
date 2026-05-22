import re
from typing import Any

FEATURE_COLUMNS = [
    "semantic_match",
    "skill_overlap_ratio",
    "experience_overlap_ratio",
    "title_keyword_overlap",
    "query_title_similarity",
    "years_of_experience_diff",
    "location_match",
    "salary_range_overlap",
]

def tokenize_text(text: str) -> set[str]:
    if not text:
        return set()
    tokens = re.findall(r"[a-zA-Z]+", str(text).lower())
    return {token for token in tokens if len(token) >= 3}

def normalize_items(items: list[str]) -> set[str]:
    normalized: set[str] = set()
    for item in items:
        cleaned = str(item).strip().lower()
        if cleaned:
            normalized.add(cleaned)
    return normalized

def extract_features(job_details: dict[str, Any], query: str, resume_text: str) -> dict[str, float]:
    title = str(job_details.get("title", "")).strip()
    description = str(job_details.get("description", "")).strip()
    semantic_match = float(job_details.get("semanticMatch", 0.0))

    query_text = " ".join(part for part in [query, resume_text] if part).strip()
    query_tokens = tokenize_text(query_text)
    title_tokens = tokenize_text(title)
    description_tokens = tokenize_text(description)

    if query_tokens:
        skill_overlap_ratio = len(query_tokens & title_tokens) / len(query_tokens)
        experience_overlap_ratio = len(query_tokens & description_tokens) / len(query_tokens)
    else:
        skill_overlap_ratio = 0.0
        experience_overlap_ratio = 0.0

    return {
        "semantic_match": semantic_match,
        "skill_overlap_ratio": skill_overlap_ratio,
        "experience_overlap_ratio": experience_overlap_ratio,
        "title_keyword_overlap": float(len(query_tokens & title_tokens)),
        "query_title_similarity": 0.0,
        "years_of_experience_diff": 0.0,
        "location_match": 0.0,
        "salary_range_overlap": 0.0,
    }
