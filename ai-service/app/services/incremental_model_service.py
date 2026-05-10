import hashlib
import json
import logging
import re
import shutil
import threading
from collections import OrderedDict
from datetime import datetime, timezone
from pathlib import Path

logger = logging.getLogger(__name__)

FEATURE_KEYS = [
    "skill_overlap_ratio",
    "title_keyword_overlap",
    "experience_description_overlap",
]

FEATURE_COUNT_CAP = 5000
MIN_SAMPLES_TO_RECOMPUTE = 10

INCREMENTAL_STATS_FILE = (
    Path(__file__).resolve().parents[2] / "data_pipeline" / "output" / "incremental_stats.json"
)
MODEL_VERSION_DIR = (
    Path(__file__).resolve().parents[2] / "data_pipeline" / "output" / "models"
)
LEGACY_MODEL_FILE = (
    Path(__file__).resolve().parents[2] / "data_pipeline" / "output" / "baseline_model.json"
)

MAX_MEMORY_DEDUP = 10000


class DedupManager:
    """评分消息去重管理器。

    消息签名 = SHA256(messageId + jobId + resumeVersionId)
    内存 LRU 处理热数据，持久化文件处理冷启动恢复。
    """

    def __init__(self, stats_file: Path):
        self._memory: OrderedDict[str, bool] = OrderedDict()
        self._stats_file = stats_file
        self._lock = threading.Lock()
        self._load_persisted()

    def _load_persisted(self):
        """启动时从 incremental_stats.json 恢复已处理签名。"""
        if not self._stats_file.exists():
            return
        try:
            with self._stats_file.open("r", encoding="utf-8") as f:
                data = json.load(f)
            signatures = data.get("processed_signatures", [])
            for sig in signatures[-MAX_MEMORY_DEDUP:]:
                self._memory[sig] = True
        except Exception:
            logger.exception("Failed to load persisted dedup signatures")

    def is_processed(self, message_id: str, job_id: str, resume_version_id: str) -> bool:
        signature = hashlib.sha256(
            f"{message_id}:{job_id}:{resume_version_id}".encode()
        ).hexdigest()[:16]
        with self._lock:
            return signature in self._memory

    def mark_processed(self, message_id: str, job_id: str, resume_version_id: str):
        signature = hashlib.sha256(
            f"{message_id}:{job_id}:{resume_version_id}".encode()
        ).hexdigest()[:16]
        with self._lock:
            if signature in self._memory:
                self._memory.move_to_end(signature)
            else:
                self._memory[signature] = True
                while len(self._memory) > MAX_MEMORY_DEDUP:
                    self._memory.popitem(last=False)

    def get_persisted_signatures(self) -> list[str]:
        """返回应持久化到磁盘的签名列表（供 _save_stats 调用）。"""
        with self._lock:
            return list(self._memory.keys())


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
    """增量模型统计与权重重算服务。

    维护运行时增量统计（正负样本特征累加和与计数），
    定时触发权重重算并生成版本化模型文件。
    """

    def __init__(self):
        self._stats = self._load_stats()
        self._dedup = DedupManager(INCREMENTAL_STATS_FILE)
        self._ensure_model_dir()
        self._lock = threading.Lock()

    def _ensure_model_dir(self):
        MODEL_VERSION_DIR.mkdir(parents=True, exist_ok=True)

    def _load_stats(self) -> dict:
        if not INCREMENTAL_STATS_FILE.exists():
            return {
                "positive": {k: {"sum": 0.0, "count": 0} for k in FEATURE_KEYS},
                "negative": {k: {"sum": 0.0, "count": 0} for k in FEATURE_KEYS},
                "version": 0,
                "last_updated": None,
                "processed_signatures": [],
            }
        try:
            with INCREMENTAL_STATS_FILE.open("r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            logger.exception("Failed to load incremental stats, starting fresh")
            return {
                "positive": {k: {"sum": 0.0, "count": 0} for k in FEATURE_KEYS},
                "negative": {k: {"sum": 0.0, "count": 0} for k in FEATURE_KEYS},
                "version": 0,
                "last_updated": None,
                "processed_signatures": [],
            }

    def _save_stats(self):
        try:
            INCREMENTAL_STATS_FILE.parent.mkdir(parents=True, exist_ok=True)
            temp_file = INCREMENTAL_STATS_FILE.with_suffix(".tmp")
            self._stats["processed_signatures"] = self._dedup.get_persisted_signatures()
            with temp_file.open("w", encoding="utf-8") as f:
                json.dump(self._stats, f, indent=2)
            shutil.move(str(temp_file), str(INCREMENTAL_STATS_FILE))
        except Exception:
            logger.exception("Failed to save incremental stats")

    def _apply_soft_cap(self, target: str, key: str, new_value: float):
        """将新样本以软上限方式合并到统计中。

        当 count < CAP 时：正常累加。
        当 count >= CAP 时：旧统计先衰减，再合并新样本，
        保证模型始终对近期数据敏感。
        """
        stat = self._stats[target][key]
        current_sum = stat["sum"]
        current_count = stat["count"]

        if current_count >= FEATURE_COUNT_CAP:
            shrink_ratio = FEATURE_COUNT_CAP / (FEATURE_COUNT_CAP + 1)
            current_sum = current_sum * shrink_ratio
            current_count = current_count * shrink_ratio

        current_sum += new_value
        current_count += 1

        stat["sum"] = current_sum
        stat["count"] = current_count

    def update_statistics(self, score_payload: dict):
        """接收一次评分结果，提取特征并累加到对应类别（正/负样本）。"""
        message_id = score_payload.get("messageId", "")
        job_id = score_payload.get("jobId", "")
        resume_id = score_payload.get("resumeVersionId", "")

        if self._dedup.is_processed(message_id, job_id, resume_id):
            logger.info("Duplicate incremental message ignored: messageId=%s", message_id)
            return

        features = _extract_features(score_payload)

        is_positive = score_payload.get("suitable", False) and score_payload.get("finalScore", 0) >= 0.6
        target = "positive" if is_positive else "negative"

        with self._lock:
            for key in FEATURE_KEYS:
                self._apply_soft_cap(target, key, features.get(key, 0.0))

            self._stats["last_updated"] = datetime.now(timezone.utc).isoformat()
            self._dedup.mark_processed(message_id, job_id, resume_id)
            self._save_stats()

            # 触发阈值检查
            total_new = sum(self._stats["positive"][k]["count"] for k in FEATURE_KEYS)
            if total_new >= MIN_SAMPLES_TO_RECOMPUTE:
                logger.info(
                    "Incremental sample threshold reached (%d), triggering weight recomputation",
                    total_new,
                )
                self.recompute_weights()

    def recompute_weights(self) -> dict:
        """基于增量统计重新计算特征权重，生成新版本模型文件。"""
        with self._lock:
            positive_avgs = {}
            negative_avgs = {}
            normalizations = {}

            for key in FEATURE_KEYS:
                pos = self._stats["positive"][key]
                neg = self._stats["negative"][key]

                positive_avgs[key] = pos["sum"] / max(pos["count"], 1)
                negative_avgs[key] = neg["sum"] / max(neg["count"], 1)

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

            new_version = self._stats["version"] + 1

            model_artifact = {
                "model_type": "weighted_feature_baseline",
                "version": f"v{new_version}",
                "feature_weights": feature_weights,
                "normalization": normalizations,
                "bias": 0.0,
                "stats": {
                    "positive_counts": {k: self._stats["positive"][k]["count"] for k in FEATURE_KEYS},
                    "negative_counts": {k: self._stats["negative"][k]["count"] for k in FEATURE_KEYS},
                },
                "generated_at": datetime.now(timezone.utc).isoformat(),
            }

            # 原子写入新版本文件
            new_file = MODEL_VERSION_DIR / f"baseline_model_v{new_version}.json"
            temp_file = MODEL_VERSION_DIR / f".tmp_baseline_model_v{new_version}.json"

            with temp_file.open("w", encoding="utf-8") as f:
                json.dump({"model_artifact": model_artifact}, f, indent=2)
            shutil.move(str(temp_file), str(new_file))

            # 更新符号链接指向最新版本
            latest_link = MODEL_VERSION_DIR / "baseline_model_latest.json"
            if latest_link.exists() or latest_link.is_symlink():
                latest_link.unlink()
            latest_link.symlink_to(new_file.name)

            # 同时覆盖传统 baseline_model.json 保持兼容
            shutil.copy2(str(new_file), str(LEGACY_MODEL_FILE))

            # 更新统计版本号
            self._stats["version"] = new_version
            self._save_stats()

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
