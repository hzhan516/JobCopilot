import json
import logging
import re
from pathlib import Path

# Suitability scoring utilities for resume and job matching.

from app.schemas import SuitabilityBreakdown, SuitabilityRequest, SuitabilityResponse
from app.services.llm_client import generate_json_from_text_prompt


BASELINE_MODEL_FILE = Path(__file__).resolve().parents[2] / "data_pipeline" / "output" / "baseline_model.json"
logger = logging.getLogger(__name__)

# Normalize a list of strings into a lowercase unique set.
def _normalize_items(items: list[str]) -> set[str]:
    normalized: set[str] = set()

    for item in items:
        cleaned = item.strip().lower()
        if cleaned:
            normalized.add(cleaned)

    return normalized


# Convert experience item count into a coarse score.
def _calculate_experience_score(experience_items: list[dict]) -> float:
    experience_count = len(experience_items)

    if experience_count == 0:
        return 0.2
    if experience_count == 1:
        return 0.4
    if experience_count == 2:
        return 0.6
    if experience_count == 3:
        return 0.8

    return 1.0


# Load the dataset model artifact if present.
def _load_dataset_model_artifact() -> dict | None:
    if not BASELINE_MODEL_FILE.exists():
        return None

    with BASELINE_MODEL_FILE.open("r", encoding="utf-8") as file:
        payload = json.load(file)

    return payload.get("model_artifact")


# Tokenize text into lowercase alphabetic tokens of length >= 3.
def _tokenize_text(text: str) -> set[str]:
    tokens = re.findall(r"[a-zA-Z]+", text.lower())
    return {token for token in tokens if len(token) >= 3}


# Build a combined text summary from experience items.
def _build_experience_text(experience_items: list[dict]) -> str:
    parts: list[str] = []

    for item in experience_items:
        if not isinstance(item, dict):
            continue

        for key in ("title", "summary", "company"):
            value = item.get(key)
            if value:
                parts.append(str(value))

    return " ".join(parts)


# Calculate a dataset-based suitability score if an artifact exists.
def _calculate_dataset_score(request: SuitabilityRequest) -> float | None:
    artifact = _load_dataset_model_artifact()
    if not artifact:
        return None

    resume_skills = _normalize_items(request.resume.skills)
    job_requirements = _normalize_items(request.job.requirements)

    if job_requirements:
        skill_overlap_ratio = len(resume_skills & job_requirements) / len(job_requirements)
    else:
        skill_overlap_ratio = 0.0

    title_tokens = _tokenize_text(request.job.title)
    title_keyword_overlap = len(resume_skills & title_tokens)

    resume_experience_text = _build_experience_text(request.resume.experience)
    job_text = " ".join([
        request.job.title,
        request.job.description,
    ])

    experience_description_overlap = len(
        _tokenize_text(resume_experience_text) & _tokenize_text(job_text)
    )

    raw_features = {
        "skill_overlap_ratio": skill_overlap_ratio,
        "title_keyword_overlap": float(title_keyword_overlap),
        "experience_description_overlap": float(experience_description_overlap),
    }

    feature_weights = artifact.get("feature_weights", {})
    normalization = artifact.get("normalization", {})
    bias = float(artifact.get("bias", 0.0))

    score = bias

    for key, value in raw_features.items():
        weight = float(feature_weights.get(key, 0.0))
        normalizer = float(normalization.get(key, 1.0)) or 1.0
        normalized_value = min(value / normalizer, 1.0)
        score += weight * normalized_value

    return round(_clamp_score(score), 2)


# Compute a baseline suitability score using heuristics only.
def evaluate_suitability_baseline(request: SuitabilityRequest) -> SuitabilityResponse:
    resume_skills = _normalize_items(request.resume.skills)
    job_requirements = _normalize_items(request.job.requirements)

    if not job_requirements:
        matched = set()
        skill_score = 0.5
    else:
        matched = resume_skills & job_requirements
        skill_score = len(matched) / len(job_requirements)

    experience_score = _calculate_experience_score(request.resume.experience)
    overall_score = round((skill_score * 0.7) + (experience_score * 0.3), 2)
    suitable = overall_score >= 0.6

    matched_list = sorted(matched)

    if matched_list:
        matched_preview = ", ".join(matched_list[:3])

        if suitable:
            summary = f"The resume matches key requirements such as {matched_preview}."
        else:
            summary = f"The resume shows some alignment through {matched_preview}, but coverage is still limited."
    else:
        summary = "The resume does not yet show clear overlap with the listed job requirements."

    return SuitabilityResponse(
        suitable=suitable,
        summary=summary,
        breakdown=SuitabilityBreakdown(
            skillScore=round(skill_score, 2),
            experienceScore=round(experience_score, 2),
            overallScore=overall_score,
        ),
        vertexScore=overall_score,
        datasetScore=None,
        finalScore=overall_score,
    )

# Clamp a score to the [0, 1] range.
def _clamp_score(value: float) -> float:
    return max(0.0, min(1.0, value))


# Build the LLM prompt used for Vertex suitability scoring.
def _build_vertex_suitability_prompt(request: SuitabilityRequest) -> str:
    return f"""
You are evaluating how suitable a candidate resume is for a job posting.

Return valid JSON only.
Do not include markdown fences.
Do not include explanations outside JSON.

Score everything between 0.0 and 1.0.

Return exactly one JSON object with this shape:
{{
  "suitable": true,
  "summary": "string",
  "skillScore": 0.0,
  "experienceScore": 0.0,
  "overallScore": 0.0
}}

Rules:
- suitable: true if the candidate is generally a good fit
- summary: 1-2 concise sentences explaining the decision
- skillScore: how well the resume skills match the job requirements
- experienceScore: how well the resume experience matches the role
- overallScore: overall suitability
- do not invent facts not supported by the input

Resume:
{json.dumps(request.resume.model_dump(), ensure_ascii=False, indent=2)}

Job:
{json.dumps(request.job.model_dump(), ensure_ascii=False, indent=2)}
""".strip()


# Evaluate suitability using Vertex AI and combine with baseline/dataset scores.
def evaluate_suitability_with_vertex(request: SuitabilityRequest) -> SuitabilityResponse:
    baseline_response = evaluate_suitability_baseline(request)
    prompt = _build_vertex_suitability_prompt(request)

    try:
        result = generate_json_from_text_prompt(prompt)

        vertex_suitable = bool(result.get("suitable", False))
        summary = str(result.get("summary", "")).strip() or "Vertex AI did not return a summary."

        skill_score = _clamp_score(float(result.get("skillScore", 0.0)))
        experience_score = _clamp_score(float(result.get("experienceScore", 0.0)))
        overall_score = _clamp_score(float(result.get("overallScore", 0.0)))
        logger.info(
            "Vertex suitability result received: suitable=%s, skill_score=%.2f, experience_score=%.2f, overall_score=%.2f",
            vertex_suitable,
            skill_score,
            experience_score,
            overall_score,
        )
        logger.info(
            "Baseline suitability result: suitable=%s, final_score=%.2f",
            baseline_response.suitable,
            baseline_response.final_score,
        )

        dataset_score = _calculate_dataset_score(request)
        final_score = round(
            overall_score if dataset_score is None else ((overall_score * 0.7) + (dataset_score * 0.3)),
            2,
        )
        suitable = final_score >= 0.6

        if vertex_suitable != suitable:
            logger.info(
                "Suitability decision adjusted by final score: vertex=%s, final=%s",
                vertex_suitable,
                suitable,
            )


        return SuitabilityResponse(
            suitable=suitable,
            summary=summary,
            breakdown=SuitabilityBreakdown(
                skillScore=round(skill_score, 2),
                experienceScore=round(experience_score, 2),
                overallScore=round(overall_score, 2),
            ),
            vertexScore=round(overall_score, 2),
            datasetScore=dataset_score,
            finalScore=final_score,
        )

    except Exception:
        logger.exception("Vertex suitability evaluation failed; falling back to baseline")
        return evaluate_suitability_baseline(request)
