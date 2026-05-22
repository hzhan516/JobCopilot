from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

from app.main import app


class TestAdminRecomputeModel:
    def test_recompute_model_success(self):
        client = TestClient(app)
        response = client.post("/api/v1/admin/recompute-model")
        assert response.status_code == 200
        assert response.json()["status"] == "ok"
        assert "Deprecated" in response.json()["message"]

    def test_recompute_model_with_internal_api_key(self):
        client = TestClient(app)
        with patch("app.main.INTERNAL_API_KEY", "secret-key"):
            # 无 key 时应 401
            response = client.post("/api/v1/admin/recompute-model")
            assert response.status_code == 401

            # 有 key 时应 200
            response = client.post(
                "/api/v1/admin/recompute-model",
                headers={"X-Internal-API-Key": "secret-key"},
            )
            assert response.status_code == 200
