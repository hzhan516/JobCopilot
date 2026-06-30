"""Tests for AI service admin router endpoints."""
import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock


@pytest.fixture
def client():
    """Create test client with mocked model_manager to avoid MinIO/Redis deps."""
    with patch("app.api.model_manager.ModelManager.load_latest", MagicMock()), \
         patch("app.api.model_manager.ModelManager.watch_for_reloads", MagicMock()):
        from app.main import app
        with TestClient(app) as c:
            yield c


def test_admin_status_returns_version(client):
    response = client.get("/admin/status")
    assert response.status_code == 200
    data = response.json()
    assert data["service"] == "jobcopilot-ai-service"
    assert "version" in data
    assert "uptime_seconds" in data


def test_queue_stats_returns_queues(client):
    response = client.get("/admin/queue-stats")
    assert response.status_code == 200
    data = response.json()
    assert "queues" in data
    assert "ai.queue.job.parse" in data["queues"]


def test_model_info_no_model_loaded(client):
    """When no model is loaded, returns loaded=False."""
    response = client.get("/admin/model/info")
    assert response.status_code == 200
    data = response.json()
    assert data["loaded"] is False
    assert data["message"] == "No model loaded"


def test_model_history_returns_empty_list(client):
    response = client.get("/admin/model/history")
    assert response.status_code == 200
    assert response.json() == {"versions": []}


def test_config_reload_log_key(client):
    response = client.post("/admin/config/reload", json={"key": "log.aiServiceLevel", "value": "DEBUG"})
    assert response.status_code == 200
    assert response.json()["status"] == "reloaded"


def test_config_reload_ignores_unknown(client):
    response = client.post("/admin/config/reload", json={"key": "unknown.setting", "value": "x"})
    assert response.status_code == 200
    assert response.json()["status"] == "ignored"


def test_retrain_returns_not_implemented(client):
    response = client.post("/admin/model/retrain")
    assert response.status_code == 200
    assert response.json()["status"] == "not_implemented"


def test_rollback_returns_501(client):
    response = client.post("/admin/model/rollback?version=v1")
    assert response.status_code == 501


def test_purge_queue_returns_501(client):
    response = client.post("/admin/queue/purge/test-queue")
    assert response.status_code == 501
