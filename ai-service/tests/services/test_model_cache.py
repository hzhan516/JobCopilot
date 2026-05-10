import json
import tempfile
from pathlib import Path

from app.services.suitability_service import ModelCache, _get_current_model_file, invalidate_model_cache


class TestModelCache:
    def test_loads_artifact_when_file_exists(self):
        with tempfile.TemporaryDirectory() as tmp:
            legacy = Path(tmp) / "baseline_model.json"
            with legacy.open("w") as f:
                json.dump({"model_artifact": {"version": "v1", "feature_weights": {"a": 0.5}}}, f)

            cache = ModelCache()
            cache._model_file = legacy
            cache._mtime = 0.0
            result = cache.get()
            assert result is not None
            assert result["version"] == "v1"

    def test_returns_none_when_file_missing(self):
        cache = ModelCache()
        # get() 会重新调用 _get_current_model_file()，所以需要 mock
        from unittest.mock import patch
        with patch("app.services.suitability_service._get_current_model_file", return_value=Path("/nonexistent/path/model.json")):
            result = cache.get()
        assert result is None

    def test_invalidate_resets_mtime(self):
        from app.services.suitability_service import _model_cache
        _model_cache._mtime = 12345.0
        invalidate_model_cache()
        assert _model_cache._mtime == 0.0


class TestGetCurrentModelFile:
    def test_prefers_latest_when_exists(self, tmp_path):
        latest = tmp_path / "baseline_model_latest.json"
        legacy = tmp_path / "baseline_model.json"
        latest.write_text("{}")
        legacy.write_text("{}")

        from unittest.mock import patch
        with patch("app.services.suitability_service.LATEST_MODEL_FILE", latest):
            with patch("app.services.suitability_service.BASELINE_MODEL_FILE", legacy):
                result = _get_current_model_file()
                assert result == latest

    def test_falls_back_to_legacy_when_latest_missing(self, tmp_path):
        legacy = tmp_path / "baseline_model.json"
        legacy.write_text("{}")

        from unittest.mock import patch
        with patch("app.services.suitability_service.LATEST_MODEL_FILE", tmp_path / "missing.json"):
            with patch("app.services.suitability_service.BASELINE_MODEL_FILE", legacy):
                result = _get_current_model_file()
                assert result == legacy
