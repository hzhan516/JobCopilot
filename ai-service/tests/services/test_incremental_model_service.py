import json
import tempfile
from pathlib import Path
from unittest.mock import patch

import pytest

from app.services.incremental_model_service import (
    _extract_features,
    _normalize_items,
    _tokenize_text,
    _build_experience_text,
    DedupManager,
    IncrementalModelService,
    FEATURE_KEYS,
    INCREMENTAL_STATS_FILE,
    MODEL_VERSION_DIR,
)


class TestFeatureExtraction:
    def test_extract_features_basic(self):
        payload = {
            "resume": {
                "skills": ["Python", "AWS", "Kubernetes"],
                "experience": [
                    {"title": "Senior Engineer", "summary": "Built microservices", "company": "TechCorp"}
                ],
            },
            "job": {
                "title": "Software Engineer",
                "description": "We are looking for backend engineers",
                "requirements": ["Python", "AWS"],
            },
        }
        features = _extract_features(payload)
        assert "skill_overlap_ratio" in features
        assert "title_keyword_overlap" in features
        assert "experience_description_overlap" in features
        assert features["skill_overlap_ratio"] == 1.0  # 2/2 matched

    def test_extract_features_no_requirements(self):
        payload = {
            "resume": {"skills": ["Python"], "experience": []},
            "job": {"title": "Job", "description": "Desc", "requirements": []},
        }
        features = _extract_features(payload)
        assert features["skill_overlap_ratio"] == 0.0


class TestDedupManager:
    def test_is_processed_and_mark(self, tmp_path):
        stats_file = tmp_path / "stats.json"
        dedup = DedupManager(stats_file)
        assert not dedup.is_processed("msg1", "job1", "res1")
        dedup.mark_processed("msg1", "job1", "res1")
        assert dedup.is_processed("msg1", "job1", "res1")

    def test_persisted_signatures(self, tmp_path):
        stats_file = tmp_path / "stats.json"
        with stats_file.open("w") as f:
            json.dump({"processed_signatures": ["abc123"]}, f)
        dedup = DedupManager(stats_file)
        assert dedup.is_processed("any", "any", "any") is False  # 不同输入产生不同签名
        # 但 abc123 不在内存中因为输入不同


class TestIncrementalModelService:
    def test_update_statistics_positive_sample(self, tmp_path):
        stats_file = tmp_path / "incremental_stats.json"
        model_dir = tmp_path / "models"
        with patch.object(
            __import__("app.services.incremental_model_service", fromlist=["INCREMENTAL_STATS_FILE"]),
            "INCREMENTAL_STATS_FILE",
            stats_file,
        ):
            with patch.object(
                __import__("app.services.incremental_model_service", fromlist=["MODEL_VERSION_DIR"]),
                "MODEL_VERSION_DIR",
                model_dir,
            ):
                with patch.object(
                    __import__("app.services.incremental_model_service", fromlist=["LEGACY_MODEL_FILE"]),
                    "LEGACY_MODEL_FILE",
                    model_dir / "baseline_model.json",
                ):
                    service = IncrementalModelService()
                    payload = {
                        "messageId": "msg-1",
                        "jobId": "job-1",
                        "resumeVersionId": "res-1",
                        "resume": {
                            "skills": ["Python", "AWS"],
                            "experience": [{"title": "Dev", "summary": "Build apps", "company": "X"}],
                        },
                        "job": {
                            "title": "Python Developer",
                            "description": "Need Python and AWS",
                            "requirements": ["Python", "AWS"],
                        },
                        "suitable": True,
                        "finalScore": 0.85,
                    }
                    service.update_statistics(payload)
                    stats = service._stats
                    assert stats["positive"]["skill_overlap_ratio"]["count"] == 1
                    assert stats["negative"]["skill_overlap_ratio"]["count"] == 0

    def test_recompute_weights(self, tmp_path):
        stats_file = tmp_path / "incremental_stats.json"
        model_dir = tmp_path / "models"
        with patch.object(
            __import__("app.services.incremental_model_service", fromlist=["INCREMENTAL_STATS_FILE"]),
            "INCREMENTAL_STATS_FILE",
            stats_file,
        ):
            with patch.object(
                __import__("app.services.incremental_model_service", fromlist=["MODEL_VERSION_DIR"]),
                "MODEL_VERSION_DIR",
                model_dir,
            ):
                with patch.object(
                    __import__("app.services.incremental_model_service", fromlist=["LEGACY_MODEL_FILE"]),
                    "LEGACY_MODEL_FILE",
                    model_dir / "baseline_model.json",
                ):
                    service = IncrementalModelService()
                    # 手动填充统计
                    service._stats["positive"]["skill_overlap_ratio"] = {"sum": 1.0, "count": 1}
                    service._stats["negative"]["skill_overlap_ratio"] = {"sum": 0.0, "count": 1}
                    for key in FEATURE_KEYS:
                        if key != "skill_overlap_ratio":
                            service._stats["positive"][key] = {"sum": 1.0, "count": 1}
                            service._stats["negative"][key] = {"sum": 0.0, "count": 1}

                    result = service.recompute_weights()
                    assert result["version"] == "v1"
                    assert "feature_weights" in result
                    assert (model_dir / "baseline_model_v1.json").exists()
                    assert (model_dir / "baseline_model_latest.json").exists()

    def test_soft_cap(self, tmp_path):
        stats_file = tmp_path / "incremental_stats.json"
        model_dir = tmp_path / "models"
        with patch.object(
            __import__("app.services.incremental_model_service", fromlist=["INCREMENTAL_STATS_FILE"]),
            "INCREMENTAL_STATS_FILE",
            stats_file,
        ):
            with patch.object(
                __import__("app.services.incremental_model_service", fromlist=["MODEL_VERSION_DIR"]),
                "MODEL_VERSION_DIR",
                model_dir,
            ):
                with patch.object(
                    __import__("app.services.incremental_model_service", fromlist=["LEGACY_MODEL_FILE"]),
                    "LEGACY_MODEL_FILE",
                    model_dir / "baseline_model.json",
                ):
                    service = IncrementalModelService()
                    # 设置 count 超过 cap
                    service._stats["positive"]["skill_overlap_ratio"] = {"sum": 5000.0, "count": 5000}
                    service._apply_soft_cap("positive", "skill_overlap_ratio", 0.5)
                    # 衰减后 count 应略小于 5001
                    assert service._stats["positive"]["skill_overlap_ratio"]["count"] < 5001
                    assert service._stats["positive"]["skill_overlap_ratio"]["count"] > 5000
