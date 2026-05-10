import json
from unittest.mock import MagicMock

import pytest

from app.services.suitability_service import ModelCache, invalidate_model_cache, _model_cache


class TestModelCache:
    @pytest.fixture(autouse=True)
    def setup_cache(self, mock_redis, mock_object_storage):
        # Reset singleton state for each test
        _model_cache._artifact = None
        _model_cache._version = 0
        self.cache = ModelCache()

    def test_loads_artifact_when_object_exists(self, mock_redis, mock_object_storage):
        artifact = {"version": "v1", "feature_weights": {"a": 0.5}}
        payload = json.dumps({"model_artifact": artifact}).encode("utf-8")
        mock_object_storage.put_object("resume-assistant-models", "baseline_model_latest.json", payload)
        mock_redis.set("ra:ai:incremental:model_version", "1")

        result = self.cache.get()
        assert result is not None
        assert result["version"] == "v1"

    def test_returns_none_when_object_missing(self, mock_redis, mock_object_storage):
        mock_redis.set("ra:ai:incremental:model_version", "1")
        result = self.cache.get()
        # 当对象存储无数据时应返回 None / Should return None when no data in storage
        assert result is None

    def test_invalidate_resets_version(self):
        self.cache._version = 5
        self.cache.invalidate()
        assert self.cache._version == 0

    def test_uses_memory_cache_on_second_get(self, mock_redis, mock_object_storage):
        artifact = {"version": "v2", "feature_weights": {"b": 0.8}}
        payload = json.dumps({"model_artifact": artifact}).encode("utf-8")
        mock_object_storage.put_object("resume-assistant-models", "baseline_model_latest.json", payload)
        mock_redis.set("ra:ai:incremental:model_version", "2")

        # First get triggers load
        result1 = self.cache.get()
        # Second get should be from memory (no extra storage calls)
        result2 = self.cache.get()
        assert result1 == result2
        assert result1["version"] == "v2"

    def test_reload_on_version_change(self, mock_redis, mock_object_storage):
        artifact_v1 = {"version": "v1", "feature_weights": {"a": 0.5}}
        payload_v1 = json.dumps({"model_artifact": artifact_v1}).encode("utf-8")
        mock_object_storage.put_object("resume-assistant-models", "baseline_model_latest.json", payload_v1)
        mock_redis.set("ra:ai:incremental:model_version", "1")

        result1 = self.cache.get()
        assert result1["version"] == "v1"

        # Simulate version bump + new artifact
        artifact_v2 = {"version": "v2", "feature_weights": {"a": 0.9}}
        payload_v2 = json.dumps({"model_artifact": artifact_v2}).encode("utf-8")
        mock_object_storage.put_object("resume-assistant-models", "baseline_model_latest.json", payload_v2)
        mock_redis.set("ra:ai:incremental:model_version", "2")

        # Invalidate to force reload
        self.cache.invalidate()
        result2 = self.cache.get()
        assert result2["version"] == "v2"
