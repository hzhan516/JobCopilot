import json

import pytest

from app.services.incremental_model_service import (
    _extract_features,
    _normalize_items,
    _tokenize_text,
    _build_experience_text,
    IncrementalModelService,
    FEATURE_KEYS,
    REDIS_STATS_KEY,
    REDIS_DEDUP_SET,
    REDIS_MODEL_VERSION_KEY,
    OBJECT_STORAGE_BUCKET,
    LATEST_MODEL_KEY,
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


class TestIncrementalModelService:
    @pytest.fixture(autouse=True)
    def setup_service(self, mock_redis, mock_object_storage):
        self.service = IncrementalModelService()

    def test_update_statistics_positive_sample(self):
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
        self.service.update_statistics(payload)
        pos_count = self.service._redis.hget(
            f"{REDIS_STATS_KEY}:positive:skill_overlap_ratio", "count"
        )
        neg_count = self.service._redis.hget(
            f"{REDIS_STATS_KEY}:negative:skill_overlap_ratio", "count"
        )
        assert float(pos_count) == 1.0
        assert float(neg_count or 0) == 0.0

    def test_dedup_prevents_duplicate(self):
        payload = {
            "messageId": "msg-dup",
            "jobId": "job-dup",
            "resumeVersionId": "res-dup",
            "resume": {"skills": ["Python"], "experience": []},
            "job": {"title": "Job", "description": "Desc", "requirements": ["Python"]},
            "suitable": True,
            "finalScore": 0.85,
        }
        self.service.update_statistics(payload)
        self.service.update_statistics(payload)
        pos_count = self.service._redis.hget(
            f"{REDIS_STATS_KEY}:positive:skill_overlap_ratio", "count"
        )
        # 第二次应被去重忽略 / Second should be ignored by dedup
        assert float(pos_count) == 1.0

    def test_recompute_weights(self):
        # 手动填充统计 / Manually seed statistics
        for key in FEATURE_KEYS:
            self.service._redis.hset(
                f"{REDIS_STATS_KEY}:positive:{key}",
                mapping={"sum": "1.0", "count": "1"},
            )
            self.service._redis.hset(
                f"{REDIS_STATS_KEY}:negative:{key}",
                mapping={"sum": "0.0", "count": "1"},
            )

        result = self.service.recompute_weights()
        assert result["version"] == "v1"
        assert "feature_weights" in result

        # 验证对象存储写入 / Verify object storage writes
        latest = self.service._storage.get_object(OBJECT_STORAGE_BUCKET, LATEST_MODEL_KEY)
        assert latest is not None
        payload = json.loads(latest)
        assert payload["model_artifact"]["version"] == "v1"

        # 验证 Redis 版本号 / Verify Redis version
        version = self.service._redis.get(REDIS_MODEL_VERSION_KEY)
        assert version == "1"

    def test_soft_cap(self):
        key = f"{REDIS_STATS_KEY}:positive:skill_overlap_ratio"
        # 设置 count 超过 cap
        self.service._redis.hset(key, mapping={"sum": "5000.0", "count": "5000"})
        self.service._ensure_lua_loaded()
        self.service._redis.evalsha(
            self.service._lua_sha, 1, key, "0.5", str(5000)
        )
        count = float(self.service._redis.hget(key, "count"))
        # 衰减后 count 应略小于 5001
        assert count < 5001
        assert count > 5000
