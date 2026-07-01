"""Tests for AI service admin router endpoints."""

import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock, AsyncMock
from datetime import datetime, timezone


@pytest.fixture
def client():
    """Create test client with mocked model_manager to avoid MinIO/Redis deps."""
    with patch("app.api.model_manager.ModelManager.load_latest", MagicMock()), patch(
        "app.api.model_manager.ModelManager.watch_for_reloads", MagicMock()
    ):
        from app.main import app

        with TestClient(app) as c:
            yield c


@pytest.fixture(autouse=True)
def mock_admin_minio(monkeypatch):
    """Provide a MinIO registry mock for all admin router tests."""
    mock = MagicMock()
    mock.list_objects.return_value = []
    mock.object_exists.return_value = False
    monkeypatch.setattr("app.api.admin_router.get_minio_client", lambda: mock)
    return mock


@pytest.fixture(autouse=True)
def mock_admin_redis(monkeypatch):
    """Provide an async Redis mock for all admin router tests."""
    mock = AsyncMock()
    monkeypatch.setattr("app.api.admin_router.get_redis_client", lambda: mock)
    return mock


@pytest.fixture(autouse=True)
def mock_admin_model_manager(monkeypatch):
    """Provide an async ModelManager mock for all admin router tests."""
    mock = MagicMock()
    mock.load_latest = AsyncMock()
    monkeypatch.setattr("app.api.admin_router.get_model_manager", lambda: mock)
    return mock


@pytest.fixture(autouse=True)
def mock_admin_trainer(monkeypatch):
    """Provide a trainer mock for all admin router tests."""
    mock = MagicMock()
    mock.try_retrain = AsyncMock(return_value=None)
    monkeypatch.setattr("app.api.admin_router.get_trainer", lambda: mock)
    return mock


def test_admin_status_returns_version(client):
    response = client.get("/admin/status")
    assert response.status_code == 200
    data = response.json()
    assert data["service"] == "jobcopilot-ai-service"
    assert "version" in data
    assert "uptime_seconds" in data


def test_queue_stats_returns_real_depths(client):
    """Queue stats should query RabbitMQ and return actual depths."""
    with patch("app.api.admin_router.pika.BlockingConnection") as mock_conn_cls:
        connection = MagicMock()
        channel = MagicMock()
        connection.channel.return_value = channel
        mock_conn_cls.return_value = connection

        mock_result = MagicMock()
        mock_result.method.message_count = 3
        mock_result.method.consumer_count = 1
        channel.queue_declare.return_value = mock_result

        response = client.get("/admin/queue-stats")
        assert response.status_code == 200
        data = response.json()
        assert "queues" in data
        assert data["queues"]["ai.queue.job.parse"]["depth"] == 3
        assert data["queues"]["ai.queue.job.parse"]["consumers"] == 1


def test_model_info_no_model_loaded(client):
    """When no model is loaded, returns loaded=False."""
    response = client.get("/admin/model/info")
    assert response.status_code == 200
    data = response.json()
    assert data["loaded"] is False
    assert data["message"] == "No model loaded"


def test_model_history_returns_empty_list(client, mock_admin_minio):
    response = client.get("/admin/model/history")
    assert response.status_code == 200
    assert response.json() == {"versions": []}


def test_model_history_returns_versions(client, mock_admin_minio):
    mock_admin_minio.list_objects.return_value = [
        {
            "Key": "ranker_model_20260101-120000.txt",
            "Size": 1024,
            "LastModified": datetime(2026, 1, 1, 12, 0, 0, tzinfo=timezone.utc),
        }
    ]
    response = client.get("/admin/model/history")
    assert response.status_code == 200
    data = response.json()
    assert len(data["versions"]) == 1
    assert data["versions"][0]["version"] == "20260101-120000"
    assert data["versions"][0]["key"] == "ranker_model_20260101-120000.txt"


def test_config_reload_log_key(client):
    response = client.post(
        "/admin/config/reload", json={"key": "log.aiServiceLevel", "value": "DEBUG"}
    )
    assert response.status_code == 200
    assert response.json()["status"] == "reloaded"


def test_config_reload_ignores_unknown(client):
    response = client.post(
        "/admin/config/reload", json={"key": "unknown.setting", "value": "x"}
    )
    assert response.status_code == 200
    assert response.json()["status"] == "ignored"


def test_retrain_skipped_when_no_data(client, mock_admin_trainer):
    response = client.post("/admin/model/retrain")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "skipped"
    assert "No new feedback data" in data["message"]


def test_retrain_success(client, mock_admin_trainer):
    mock_admin_trainer.try_retrain.return_value = {
        "version": "20260101-120000",
        "sample_count": 20,
        "metrics": {"auc": 0.9},
    }
    response = client.post("/admin/model/retrain")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "completed"
    assert data["new_version"] == "20260101-120000"
    assert data["samples_used"] == 20
    assert data["metrics"]["auc"] == 0.9


def test_rollback_not_found(client, mock_admin_minio):
    mock_admin_minio.object_exists.return_value = False
    mock_admin_minio.list_objects.return_value = []
    response = client.post("/admin/model/rollback", json={"version": "v99"})
    assert response.status_code == 404


def test_rollback_success(client, mock_admin_minio, mock_admin_redis, mock_admin_model_manager):
    mock_admin_minio.object_exists.return_value = True
    response = client.post(
        "/admin/model/rollback",
        json={"version": "ranker_model_20260101-120000.txt"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "rolled_back"
    assert data["version"] == "ranker_model_20260101-120000.txt"
    mock_admin_minio.update_latest_meta.assert_called_once()
    mock_admin_redis.publish.assert_called_once()
    mock_admin_model_manager.load_latest.assert_called_once()


def test_purge_queue_unknown_returns_400(client):
    response = client.post("/admin/queue/purge/not-allowed")
    assert response.status_code == 400


def test_purge_queue_success(client):
    with patch("app.api.admin_router.pika.BlockingConnection") as mock_conn_cls:
        connection = MagicMock()
        channel = MagicMock()
        connection.channel.return_value = channel
        mock_conn_cls.return_value = connection

        mock_result = MagicMock()
        mock_result.method.message_count = 7
        channel.queue_purge.return_value = mock_result

        response = client.post("/admin/queue/purge/ai.queue.job.parse")
        assert response.status_code == 200
        assert response.json() == {
            "status": "purged",
            "queue": "ai.queue.job.parse",
            "messages_removed": 7,
        }


def test_retry_dlq_success(client):
    with patch("app.api.admin_router.pika.BlockingConnection") as mock_conn_cls:
        connection = MagicMock()
        channel = MagicMock()
        connection.channel.return_value = channel
        mock_conn_cls.return_value = connection

        method1 = MagicMock()
        method1.delivery_tag = 1
        method2 = MagicMock()
        method2.delivery_tag = 2
        channel.basic_get.side_effect = [
            (method1, None, b"msg1"),
            (method2, None, b"msg2"),
            (None, None, None),
        ]

        response = client.post("/admin/queue/retry-dlq/ai.queue.job.parse")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"
        assert data["messages_retried"] == 2
        assert channel.basic_publish.call_count == 2
        assert channel.basic_ack.call_count == 2


def test_cache_flush_success(client, mock_admin_redis):
    mock_admin_redis.keys.side_effect = [
        ["ai:feedback:1", "ai:feedback:2"],
        [],
        ["ai:lock:1"],
    ]
    mock_admin_redis.delete.side_effect = [2, 1]

    response = client.post("/admin/cache/flush")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "flushed"
    assert data["keys_deleted"] == 3
