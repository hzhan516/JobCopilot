import json
import logging
import re
import threading
import time

from app.infrastructure.object_storage import MODEL_BUCKET, get_object_storage
from app.infrastructure.redis_client import get_redis
from app.schemas import SuitabilityBreakdown, SuitabilityRequest, SuitabilityResponse
from app.config import LLM_TEXT_MODEL
from app.services.llm_client import generate_json_from_text_prompt


logger = logging.getLogger(__name__)

OBJECT_STORAGE_BUCKET = MODEL_BUCKET
LATEST_MODEL_KEY = "baseline_model_latest.json"
REDIS_MODEL_VERSION_KEY = "ra:ai:incremental:model_version"


class ModelCache:
    """线程安全的分布式模型内存缓存。

    热路径（get）纯内存读取，零网络 I/O。
    版本失效通过 Redis Pub/Sub 即时通知 + 60 秒兜底轮询实现。
    模型数据从对象存储拉取，支持多实例一致性。
    """

    def __init__(self):
        self._artifact: dict | None = None
        self._version: int = 0
        self._lock = threading.RLock()
        self._redis = get_redis()
        self._storage = get_object_storage()
        self._pubsub = None

        # 订阅模型失效频道（在后台线程中延迟建立，避免导入时因 Redis 不可用而崩溃）
        threading.Thread(target=self._listen_invalidation, daemon=True).start()

        # 兜底：每 60 秒主动检查 Redis 版本号
        threading.Thread(target=self._periodic_refresh, daemon=True).start()

    def _listen_invalidation(self):
        while True:
            try:
                pubsub = self._redis.pubsub()
                pubsub.subscribe("ra:ai:model_invalidate")
                self._pubsub = pubsub
                for message in pubsub.listen():
                    if message["type"] == "message":
                        try:
                            new_version = int(message["data"])
                            with self._lock:
                                if new_version > self._version:
                                    self._version = 0
                                    logger.info("Model invalidation received: version=%d", new_version)
                        except (ValueError, TypeError):
                            pass
            except Exception:
                logger.exception("Pub/Sub listener encountered error, retrying in 5s")
            time.sleep(5)

    def _periodic_refresh(self):
        while True:
            time.sleep(60)
            try:
                redis_version = int(self._redis.get(REDIS_MODEL_VERSION_KEY) or 0)
                with self._lock:
                    if redis_version > self._version:
                        self._version = 0
                        logger.info("Periodic refresh triggered: new_version=%d", redis_version)
            except Exception:
                logger.exception("Periodic model version check failed")

    def get(self) -> dict | None:
        """热路径：纯内存读取，零网络 I/O。"""
        with self._lock:
            if self._version == 0:
                self._reload()
            return self._artifact

    def _reload(self):
        try:
            redis_version = int(self._redis.get(REDIS_MODEL_VERSION_KEY) or 0)
            data = self._storage.get_object(OBJECT_STORAGE_BUCKET, LATEST_MODEL_KEY)
            if data is None:
                logger.warning("Model object not found in storage: %s/%s", OBJECT_STORAGE_BUCKET, LATEST_MODEL_KEY)
                # 保留旧缓存，不降级到 None
                if self._artifact is not None:
                    self._version = redis_version if redis_version else 1
                return
            payload = json.loads(data)
            self._artifact = payload.get("model_artifact")
            self._version = redis_version if redis_version else 1
            logger.info("Model reloaded from object storage: version=%d", self._version)
        except Exception:
            logger.exception("Failed to load model from object storage")
            # 拉取失败时保留旧缓存

    def invalidate(self):
        with self._lock:
            self._version = 0


_model_cache = ModelCache()


def invalidate_model_cache():
    """使模型缓存失效，供 incremental_model_service 在权重重算后调用。"""
    _model_cache.invalidate()


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
    return _model_cache.get()


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


def _select_model_profile(artifact: dict, llm_model: str) -> tuple[dict, bool]:
    if artifact.get("model_type") != "adaptive_weighted_ensemble":
        return artifact, False

    min_samples = int(artifact.get("min_model_specific_samples", 10))
    by_model = artifact.get("by_llm_model") or {}
    model_profile = by_model.get(llm_model)
    if model_profile and int(model_profile.get("sample_count", 0)) >= min_samples:
        return model_profile, True

    return artifact.get("default", {}), True


def _calculate_dataset_score(
    request: SuitabilityRequest,
    llm_overall_score: float,
    llm_model: str,
) -> tuple[float | None, bool]:
    """Compute a data-driven suitability score if an incremental model artifact exists.
    数据集模型评分：加载增量训练产出的自适应权重模型，
    对 LLM 分数、语义相似度和文本重叠特征做归一化加权求和。"""
    artifact = _load_dataset_model_artifact()
    if not artifact:
        return None, False

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
        "llm_overall_score": _clamp_score(llm_overall_score),
        "semantic_match": _clamp_score(request.semantic_match or 0.0),
        "skill_overlap_ratio": skill_overlap_ratio,
        "title_keyword_overlap": float(title_keyword_overlap),
        "experience_description_overlap": float(experience_description_overlap),
    }

    profile, adaptive = _select_model_profile(artifact, llm_model)
    feature_weights = profile.get("feature_weights", {})
    normalization = profile.get("normalization", {})
    bias = float(profile.get("bias", 0.0))

    score = bias

    for key, value in raw_features.items():
        weight = float(feature_weights.get(key, 0.0))
        normalizer = float(normalization.get(key, 1.0)) or 1.0
        normalized_value = min(value / normalizer, 1.0)
        score += weight * normalized_value

    return round(_clamp_score(score), 2), adaptive


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


def evaluate_suitability_with_vertex(request: SuitabilityRequest) -> SuitabilityResponse:
    """Evaluate suitability using the configured LLM and optional incremental model scores.
    人岗匹配度评估主入口：优先使用 LLM 做深度语义评估；若 LLM 异常则降级到基线规则；
    有增量自适应模型时使用模型输出作为最终分数，否则兼容旧版 70/30 加权模型。"""
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

        dataset_score, uses_adaptive_ensemble = _calculate_dataset_score(
            request,
            overall_score,
            LLM_TEXT_MODEL,
        )
        final_score = round(
            overall_score if dataset_score is None
            else dataset_score if uses_adaptive_ensemble
            else ((overall_score * 0.7) + (dataset_score * 0.3)),
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
            llmModel=LLM_TEXT_MODEL,
        )

    except Exception:
        logger.exception("Vertex suitability evaluation failed; falling back to baseline")
        return evaluate_suitability_baseline(request)
