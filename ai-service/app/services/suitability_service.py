import json
import logging

from app.schemas import SuitabilityBreakdown, SuitabilityRequest, SuitabilityResponse
from app.config import LLM_TEXT_MODEL
from app.services.llm_client import generate_json_from_text_prompt
from app.domain.ml.features import normalize_items

logger = logging.getLogger(__name__)


def _calculate_experience_score(experience_items: list[dict]) -> float:
    """Map experience item count to a coarse score using piecewise thresholds.
    经验粗打分：按条目数量分段映射，0 条给 0.2 避免完全归零，4 条及以上封顶 1.0，
    作为基线模型中快速评估候选人资历的启发式规则。"""
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
    """Compute a heuristic suitability score without any LLM call.
    基线规则评分：无需 LLM，仅通过技能集合交集与经验条目数快速估算匹配度，
    作为 LLM 失败或超时的兜底策略，保证服务可用性。"""
    resume_skills = normalize_items(request.resume.skills)
    job_requirements = normalize_items(request.job.requirements)

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
        llmModel=None,
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


def evaluate_suitability_with_vertex(
    request: SuitabilityRequest,
) -> SuitabilityResponse:
    """Evaluate suitability using the configured LLM and optional incremental model scores.
    人岗匹配度评估主入口：优先使用 LLM 做深度语义评估；若 LLM 异常则降级到基线规则；
    有增量自适应模型时使用模型输出作为最终分数，否则兼容旧版 70/30 加权模型。"""
    baseline_response = evaluate_suitability_baseline(request)
    prompt = _build_vertex_suitability_prompt(request)

    try:
        result = generate_json_from_text_prompt(prompt)

        vertex_suitable = bool(result.get("suitable", False))
        summary = (
            str(result.get("summary", "")).strip()
            or "Vertex AI did not return a summary."
        )

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

        final_score = round(overall_score, 2)
        suitable = final_score >= 0.6

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
            finalScore=final_score,
            llmModel=LLM_TEXT_MODEL,
        )

    except Exception:
        logger.exception(
            "Vertex suitability evaluation failed; falling back to baseline"
        )
        return evaluate_suitability_baseline(request)
