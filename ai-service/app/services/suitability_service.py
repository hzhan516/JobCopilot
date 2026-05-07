import json
import logging
import re
from pathlib import Path

from app.schemas import SuitabilityBreakdown, SuitabilityRequest, SuitabilityResponse
from app.services.llm_client import generate_json_from_text_prompt


BASELINE_MODEL_FILE = Path(__file__).resolve().parents[2] / "data_pipeline" / "output" / "baseline_model.json"
logger = logging.getLogger(__name__)


def _normalize_items(items: list[str]) -> set[str]:
    normalized: set[str] = set()

    for item in items:
        cleaned = item.strip().lower()
        if cleaned:
            normalized.add(cleaned)

    return normalized


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


def _load_dataset_model_artifact() -> dict | None:
    if not BASELINE_MODEL_FILE.exists():
        return None

    with BASELINE_MODEL_FILE.open("r", encoding="utf-8") as file:
        payload = json.load(file)

    return payload.get("model_artifact")


def _tokenize_text(text: str) -> set[str]:
    tokens = re.findall(r"[a-zA-Z]+", text.lower())
    return {token for token in tokens if len(token) >= 3}


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


def _calculate_dataset_score(request: SuitabilityRequest) -> float | None:
    """Compute a data-driven suitability score if an offline baseline model artifact exists.
    数据集模型评分：加载离线训练产出的加权特征基线模型，
    对技能重叠率、标题关键词重叠、经验描述重叠三个维度做归一化加权求和，
    在 LLM 不可用时提供可解释的降级评分。"""
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


def evaluate_suitability_baseline(request: SuitabilityRequest) -> SuitabilityResponse:
    """Compute a heuristic suitability score without any LLM call.
    基线规则评分：无需 LLM，仅通过技能集合交集与经验条目数快速估算匹配度，
    作为 LLM 失败或超时的兜底策略，保证服务可用性。"""
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
    """Evaluate suitability using Vertex AI LLM and ensemble with baseline/dataset scores.
    人岗匹配度评估主入口：优先使用 LLM 做深度语义评估；若 LLM 异常则降级到基线规则；
    最终得分融合 LLM 评分（70%）与离线数据集模型评分（30%），兼顾准确性与可解释性。"""
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
