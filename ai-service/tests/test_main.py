import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock

from app.main import app, initialize_mq, _start_mq_consumer_once
import app.main as main_module

client = TestClient(app)

def test_root():
    response = client.get("/")
    assert response.status_code == 200
    assert response.json() == {
        "service": "Resume Assistant AI",
        "version": "1.0.0",
        "status": "running",
    }

def test_health_check_healthy(monkeypatch):
    monkeypatch.setattr(main_module, "_mq_is_connected", True)
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"

def test_health_check_unhealthy(monkeypatch):
    monkeypatch.setattr(main_module, "_mq_is_connected", False)
    response = client.get("/health")
    assert response.status_code == 503
    assert response.json()["detail"] == "RabbitMQ consumer not connected"

def test_status():
    response = client.get("/api/status")
    assert response.status_code == 200
    assert response.json()["ready"] is True

@patch("app.main.create_connection")
@patch("app.main.setup_all_queues")
@patch("app.main.start_all_consumers")
def test_initialize_mq_success(mock_start, mock_setup, mock_create, monkeypatch):
    mock_conn = MagicMock()
    mock_channel = MagicMock()
    mock_conn.channel.return_value = mock_channel
    mock_create.return_value = mock_conn
    
    monkeypatch.setattr(main_module, "_mq_is_connected", False)
    initialize_mq()
    
    assert main_module._mq_is_connected is True
    mock_create.assert_called_once()
    mock_setup.assert_called_once_with(mock_channel)
    mock_start.assert_called_once_with(mock_channel)

@patch("app.main.create_connection")
def test_initialize_mq_failure(mock_create, monkeypatch):
    mock_create.side_effect = Exception("Connection failed")
    
    monkeypatch.setattr(main_module, "_mq_is_connected", True)
    with pytest.raises(Exception, match="Connection failed"):
        initialize_mq()
        
    assert main_module._mq_is_connected is False

@patch("threading.Thread")
def test_start_mq_consumer_once(mock_thread, monkeypatch):
    monkeypatch.setattr(main_module, "_mq_started", False)
    
    mock_thread_instance = MagicMock()
    mock_thread.return_value = mock_thread_instance
    
    _start_mq_consumer_once()
    
    assert main_module._mq_started is True
    mock_thread.assert_called_once()
    mock_thread_instance.start.assert_called_once()
    
    # Call again, should not start another thread
    _start_mq_consumer_once()
    mock_thread.assert_called_once() # Still 1

@patch("app.main.evaluate_suitability_with_vertex")
def test_evaluate_suitability(mock_eval):
    mock_eval.return_value = {
        "suitable": True,
        "summary": "Good fit",
        "breakdown": {"skillScore": 80, "experienceScore": 90, "overallScore": 85},
        "vertexScore": 85,
        "finalScore": 85
    }
    
    payload = {
        "resume": {
            "name": "John",
            "skills": ["Python"],
            "experience": []
        },
        "job": {
            "title": "Dev",
            "company": "Tech",
            "description": "Python dev",
            "requirements": []
        }
    }
    
    response = client.post("/api/v1/suitability", json=payload)
    assert response.status_code == 200
    assert response.json()["suitable"] is True
    mock_eval.assert_called_once()

@patch("app.main.find_job_matches")
def test_match_jobs(mock_match):
    mock_match.return_value = {
        "matches": [],
        "total": 0,
        "recallTime": 10,
        "rankTime": 20
    }
    
    payload = {
        "userId": "user-123",
        "query": "Python developer",
        "topK": 5
    }
    
    response = client.post("/api/v1/match", json=payload)
    assert response.status_code == 200
    assert response.json()["total"] == 0
    mock_match.assert_called_once()

@patch("app.main._start_mq_consumer_once")
def test_startup_event(mock_start):
    import asyncio
    from app.main import startup_event
    asyncio.run(startup_event())
    mock_start.assert_called_once()
