from unittest.mock import patch

from fastapi.testclient import TestClient

from app.main import app


class TestAdminRecomputeModel:
    def test_recompute_model_returns_410_deprecated(self):
        """Deprecated endpoint returns 410 / 废弃端点返回 410"""
        client = TestClient(app)
        response = client.post("/api/v1/admin/recompute-model")
        assert response.status_code == 410
        assert "Deprecated" in response.json()["detail"]

    def test_recompute_model_auth_required_when_not_dev(self):
        """Auth required in non-dev: 401 without key, 410 with valid key / 非 dev 环境鉴权：无 key 401，有 key 410"""
        client = TestClient(app)
        with patch("app.main.ENV", "prod"), patch(
            "app.main.INTERNAL_API_KEY", "secret-key"
        ):
            # No key → 401
            response = client.post("/api/v1/admin/recompute-model")
            assert response.status_code == 401

            # Valid key → 410 (endpoint itself is deprecated)
            response = client.post(
                "/api/v1/admin/recompute-model",
                headers={"X-Internal-API-Key": "secret-key"},
            )
            assert response.status_code == 410
            assert "Deprecated" in response.json()["detail"]
