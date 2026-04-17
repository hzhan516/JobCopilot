import json

from app.schemas import SuitabilityBreakdown, SuitabilityRequest, SuitabilityResponse
from app.services.gemini_client import generate_json_from_text_prompt


def _normalize_items(items: list[str]) -> set[str]:
    normalized: set[str] = set()

    for item in items:
        cleaned = item.strip().lower()
        if cleaned:
            normalized.add(cleaned)

    return normalized


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

def _clamp_score(value: float) -> float:
    return max(0.0, min(1.0, value))


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


def evaluate_suitability_with_vertex(request: SuitabilityRequest) -> SuitabilityResponse:
    baseline_response = evaluate_suitability_baseline(request)
    prompt = _build_vertex_suitability_prompt(request)

    try:
        result = generate_json_from_text_prompt(prompt)
        print(f"Vertex suitability raw result: {result}")
        print(f"Baseline suitability result: {baseline_response.model_dump(by_alias=True)}")

        suitable = bool(result.get("suitable", False))
        summary = str(result.get("summary", "")).strip() or "Vertex AI did not return a summary."

        skill_score = _clamp_score(float(result.get("skillScore", 0.0)))
        experience_score = _clamp_score(float(result.get("experienceScore", 0.0)))
        overall_score = _clamp_score(float(result.get("overallScore", 0.0)))

        return SuitabilityResponse(
            suitable=suitable,
            summary=summary,
            breakdown=SuitabilityBreakdown(
                skillScore=round(skill_score, 2),
                experienceScore=round(experience_score, 2),
                overallScore=round(overall_score, 2),
            ),
            vertexScore=round(overall_score, 2),
            datasetScore=None,
            finalScore=round(overall_score, 2),
        )

    except Exception as exc:
        print(f"Vertex suitability evaluation failed: {exc}")
        return evaluate_suitability_baseline(request)


