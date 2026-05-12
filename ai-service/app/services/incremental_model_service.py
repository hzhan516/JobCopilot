import hashlib
import json
import logging
import re
import threading
from datetime import datetime, timezone

from app.infrastructure.object_storage import get_object_storage
from app.infrastructure.redis_client import get_redis

logger = logging.getLogger(__name__)

FEATURE_KEYS = [
    "skill_overlap_ratio",
    "title_keyword_overlap",
    "experience_description_overlap",
]

FEATURE_COUNT_CAP = 5000
MIN_SAMPLES_TO_RECOMPUTE = 10

REDIS_KEY_PREFIX = "ra:ai:incremental"
REDIS_STATS_KEY = f"{REDIS_KEY_PREFIX}:stats"
REDIS_DEDUP_SET = f"{REDIS_KEY_PREFIX}:dedup"
REDIS_MODEL_VERSION_KEY = f"{REDIS_KEY_PREFIX}:model_version"
MAX_DEDUP_SIZE = 100000

OBJECT_STORAGE_BUCKET = "resume-assistant-models"
LATEST_MODEL_KEY = "baseline_model_latest.json"

# Lua script for atomic soft-cap accumulation in Redis
# KEYS[1] = hash key, ARGV[1] = new_value, ARGV[2] = cap
LUA_SOFT_CAP = """
local sum = tonumber(redis.call('hget', KEYS[1], 'sum') or '0')
local count = tonumber(redis.call('hget', KEYS[1], 'count') or '0')
local cap = tonumber(ARGV[2])
local new_val = tonumber(ARGV[1])
if count >= cap then
    local ratio = cap / (cap + 1)
    sum = sum * ratio
    count = count * ratio
end
sum = sum + new_val
count = count + 1
redis.call('hset', KEYS[1], 'sum', tostring(sum))
redis.call('hset', KEYS[1], 'count', tostring(count))
return {tostring(sum), tostring(count)}
"""


def _signature(message_id: str, job_id: str, resume_version_id: str) -> str:
    return hashlib.sha256(
        f"{message_id}:{job_id}:{resume_version_id}".encode()
    ).hexdigest()[:16]


def _normalize_items(items: list[str]) -> set[str]:
    normalized: set[str] = set()
    for item in items:
        cleaned = item.strip().lower()
        if cleaned:
            normalized.add(cleaned)
    return normalized


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


def _extract_features(score_payload: dict) -> dict[str, float]:
    """从评分消息体中提取三个特征值。"""
    resume = score_payload.get("resume", {})
    job = score_payload.get("job", {})

    resume_skills = _normalize_items(resume.get("skills", []))
    job_requirements = _normalize_items(job.get("requirements", []))

    if job_requirements:
        skill_overlap_ratio = len(resume_skills & job_requirements) / len(job_requirements)
    else:
        skill_overlap_ratio = 0.0

    title_tokens = _tokenize_text(job.get("title", ""))
    title_keyword_overlap = len(resume_skills & title_tokens)

    resume_experience_text = _build_experience_text(resume.get("experience", []))
    job_text = " ".join([job.get("title", ""), job.get("description", "")])
    experience_description_overlap = len(
        _tokenize_text(resume_experience_text) & _tokenize_text(job_text)
    )

    return {
        "skill_overlap_ratio": skill_overlap_ratio,
        "title_keyword_overlap": float(title_keyword_overlap),
        "experience_description_overlap": float(experience_description_overlap),
    }


class IncrementalModelService:
    """增量模型统计与权重重算服务（Redis 分布式版）。

    维护运行时增量统计（正负样本特征累加和与计数）于 Redis Hash，
    使用 Redis Set 做跨实例去重，定时触发权重重算并写入对象存储。
    """

    def __init__(self):
        self._redis = get_redis()
        self._storage = get_object_storage()
        self._lock = threading.Lock()
        self._lua_sha: str | None = None

    def _ensure_lua_loaded(self):
        if self._lua_sha is None:
            self._lua_sha = self._redis.script_load(LUA_SOFT_CAP)

    def _stats_hash_key(self, target: str, feature_key: str) -> str:
        return f"{REDIS_STATS_KEY}:{target}:{feature_key}"

    def is_processed(self, message_id: str, job_id: str, resume_version_id: str) -> bool:
        sig = _signature(message_id, job_id, resume_version_id)
        return self._redis.sismember(REDIS_DEDUP_SET, sig)

    def mark_processed(self, message_id: str, job_id: str, resume_version_id: str):
        sig = _signature(message_id, job_id, resume_version_id)
        self._redis.sadd(REDIS_DEDUP_SET, sig)
        cardinality = self._redis.scard(REDIS_DEDUP_SET)
        if cardinality and cardinality > MAX_DEDUP_SIZE:
            self._redis.spop(REDIS_DEDUP_SET, cardinality - MAX_DEDUP_SIZE)

    def update_statistics(self, score_payload: dict):
        """接收一次评分结果，提取特征并累加到对应类别（正/负样本）。"""
        message_id = score_payload.get("messageId", "")
        job_id = score_payload.get("jobId", "")
        resume_id = score_payload.get("resumeVersionId", "")

        if self.is_processed(message_id, job_id, resume_id):
            logger.info("Duplicate incremental message ignored: messageId=%s", message_id)
            return

        features = _extract_features(score_payload)
        is_positive = score_payload.get("suitable", False) and score_payload.get("finalScore", 0) >= 0.6
        target = "positive" if is_positive else "negative"

        self._ensure_lua_loaded()
        pipe = self._redis.pipeline()
        for key in FEATURE_KEYS:
            value = features.get(key, 0.0)
            hash_key = self._stats_hash_key(target, key)
            pipe.evalsha(self._lua_sha, 1, hash_key, str(value), str(FEATURE_COUNT_CAP))
        pipe.execute()

        self.mark_processed(message_id, job_id, resume_id)
        self._redis.set(
            f"{REDIS_KEY_PREFIX}:last_updated",
            datetime.now(timezone.utc).isoformat(),
        )

        # 触发阈值检查
        total_new = sum(
            int(float(self._redis.hget(self._stats_hash_key("positive", k), "count") or 0))
            for k in FEATURE_KEYS
        )
        if total_new >= MIN_SAMPLES_TO_RECOMPUTE:
            logger.info("Incremental sample threshold reached (%d), triggering weight recomputation", total_new)
            threading.Thread(target=self.recompute_weights, daemon=True).start()

    def recompute_weights(self) -> dict:
        """基于增量统计重新计算特征权重，生成新版本模型文件并写入对象存储。"""
        with self._lock:
            positive_avgs = {}
            negative_avgs = {}
            normalizations = {}

            for key in FEATURE_KEYS:
                pos_sum = float(self._redis.hget(self._stats_hash_key("positive", key), "sum") or 0)
                pos_count = int(self._redis.hget(self._stats_hash_key("positive", key), "count") or 0)
                neg_sum = float(self._redis.hget(self._stats_hash_key("negative", key), "sum") or 0)
                neg_count = int(self._redis.hget(self._stats_hash_key("negative", key), "count") or 0)

                positive_avgs[key] = pos_sum / max(pos_count, 1)
                negative_avgs[key] = neg_sum / max(neg_count, 1)

                all_values = [positive_avgs[key], negative_avgs[key]]
                normalizations[key] = max(max(all_values), 1.0)

            raw_weights = {}
            for key in FEATURE_KEYS:
                diff = positive_avgs[key] - negative_avgs[key]
                raw_weights[key] = max(diff, 0.0)

            total_weight = sum(raw_weights.values())
            if total_weight == 0:
                feature_weights = {k: round(1 / len(FEATURE_KEYS), 4) for k in FEATURE_KEYS}
            else:
                feature_weights = {k: round(raw_weights[k] / total_weight, 4) for k in FEATURE_KEYS}

            new_version = int(self._redis.get(REDIS_MODEL_VERSION_KEY) or 0) + 1

            model_artifact = {
                "model_type": "weighted_feature_baseline",
                "version": f"v{new_version}",
                "feature_weights": feature_weights,
                "normalization": normalizations,
                "bias": 0.0,
                "stats": {
                    "positive_counts": {
                        k: int(self._redis.hget(self._stats_hash_key("positive", k), "count") or 0)
                        for k in FEATURE_KEYS
                    },
                    "negative_counts": {
                        k: int(self._redis.hget(self._stats_hash_key("negative", k), "count") or 0)
                        for k in FEATURE_KEYS
                    },
                },
                "generated_at": datetime.now(timezone.utc).isoformat(),
            }

            payload = json.dumps({"model_artifact": model_artifact}, indent=2).encode("utf-8")

            # 写入对象存储（版本化 + latest）
            self._storage.put_object(
                OBJECT_STORAGE_BUCKET,
                f"baseline_model_v{new_version}.json",
                payload,
            )
            self._storage.put_object(
                OBJECT_STORAGE_BUCKET,
                LATEST_MODEL_KEY,
                payload,
            )

            self._redis.set(REDIS_MODEL_VERSION_KEY, str(new_version))
            self._redis.set(
                f"{REDIS_KEY_PREFIX}:model_updated_at",
                datetime.now(timezone.utc).isoformat(),
            )

            # 广播缓存失效通知
            self._redis.publish("ra:ai:model_invalidate", str(new_version))

            logger.info(
                "Model weights recomputed: version=%s, weights=%s",
                model_artifact["version"],
                feature_weights,
            )

            # 通知 suitability_service 刷新缓存
            try:
                from app.services.suitability_service import invalidate_model_cache

                invalidate_model_cache()
            except Exception:
                logger.exception("Failed to invalidate model cache after weight recomputation")

            return model_artifact


# 全局单例
incremental_service = IncrementalModelService()
