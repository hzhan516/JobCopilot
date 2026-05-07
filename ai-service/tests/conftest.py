import json
from collections.abc import Iterator
from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient

import app.main as main_module


@pytest.fixture(autouse=True)
def configure_test_environment(monkeypatch: pytest.MonkeyPatch) -> Iterator[None]:
    """Pin environment variables and suppress real MQ startup for the entire test session.
    全局测试环境配置：固定环境变量并屏蔽真实的 MQ 启动，确保测试可重复且无外网依赖。"""
    monkeypatch.setenv("RABBITMQ_HOST", "localhost")
    monkeypatch.setenv("RABBITMQ_PORT", "5672")
    monkeypatch.setenv("RABBITMQ_USERNAME", "guest")
    monkeypatch.setenv("RABBITMQ_PASSWORD", "guest")
    monkeypatch.setenv("VERTEX_PROJECT_ID", "test-project")
    monkeypatch.setenv("VERTEX_LOCATION", "us-central1")
    monkeypatch.setenv("LLM_TEXT_MODEL", "mock/text-model")
    monkeypatch.setenv("LLM_VISION_MODEL", "mock/vision-model")
    monkeypatch.setenv("LLM_EMBEDDING_MODEL", "mock/embedding-model")
    monkeypatch.setenv("LLM_EMBEDDING_MODEL_DIMENSION", "1536")
    monkeypatch.setenv("LLM_TEMPERATURE", "0")

    monkeypatch.setattr(main_module, "_mq_started", False)
    monkeypatch.setattr(main_module, "_mq_is_connected", True)
    monkeypatch.setattr(main_module, "_start_mq_consumer_once", lambda: None)
    monkeypatch.setattr(main_module, "initialize_mq", lambda: None)
    yield


@pytest.fixture
def test_client() -> Iterator[TestClient]:
    with TestClient(main_module.app) as client:
        yield client


@pytest.fixture
def mock_litellm(monkeypatch: pytest.MonkeyPatch) -> MagicMock:
    """Provide a mocked LiteLLM completion that returns deterministic JSON.
    提供确定性返回的 LiteLLM mock：固定 JSON 输出，使 LLM 相关测试不依赖真实模型。"""
    mock_response = MagicMock()
    mock_choice = MagicMock()
    mock_message = MagicMock()
    mock_message.content = json.dumps({"ok": True})
    mock_choice.message = mock_message
    mock_response.choices = [mock_choice]

    completion_mock = MagicMock(return_value=mock_response)
    monkeypatch.setattr("app.services.llm_client.completion", completion_mock)
    monkeypatch.setattr("litellm.completion", completion_mock, raising=False)
    return completion_mock


@pytest.fixture
def mock_httpx(monkeypatch: pytest.MonkeyPatch) -> tuple[MagicMock, MagicMock]:
    """Stub both sync and async httpx clients with a default success response.
    同时 mock 同步与异步 httpx 客户端：覆盖 AI 服务内所有 HTTP 调用场景。"""
    response = MagicMock()
    response.status_code = 200
    response.json.return_value = {}
    response.text = "{}"
    response.raise_for_status.return_value = None

    sync_client = MagicMock()
    sync_client.__enter__.return_value = sync_client
    sync_client.__exit__.return_value = None
    sync_client.request.return_value = response
    sync_client.get.return_value = response
    sync_client.post.return_value = response

    async_client = MagicMock()
    async_client.__aenter__.return_value = async_client
    async_client.__aexit__.return_value = None
    async_client.request.return_value = response
    async_client.get.return_value = response
    async_client.post.return_value = response

    monkeypatch.setattr("httpx.Client", MagicMock(return_value=sync_client), raising=False)
    monkeypatch.setattr("httpx.AsyncClient", MagicMock(return_value=async_client), raising=False)
    return sync_client, async_client


@pytest.fixture
def mock_rabbitmq(monkeypatch: pytest.MonkeyPatch) -> tuple[MagicMock, MagicMock]:
    """Stub RabbitMQ connection and channel for MQ-related tests.
    mock RabbitMQ 连接与频道：避免测试期间建立真实 TCP 连接。"""
    channel = MagicMock()
    connection = MagicMock()
    connection.channel.return_value = channel

    monkeypatch.setattr("app.main.create_connection", MagicMock(return_value=connection), raising=False)
    monkeypatch.setattr("app.main.setup_all_queues", MagicMock(), raising=False)
    monkeypatch.setattr("app.main.start_all_consumers", MagicMock(), raising=False)
    monkeypatch.setattr("app.mq.consumer.create_connection", MagicMock(return_value=connection), raising=False)
    monkeypatch.setattr("app.mq.consumer.setup_all_queues", MagicMock(), raising=False)
    monkeypatch.setattr("app.mq.consumer.start_all_consumers", MagicMock(), raising=False)

    return connection, channel


@pytest.fixture
def mock_dependencies(mock_litellm: MagicMock, mock_httpx: tuple[MagicMock, MagicMock], mock_rabbitmq: tuple[MagicMock, MagicMock]) -> dict[str, object]:
    return {
        "litellm": mock_litellm,
        "httpx_sync": mock_httpx[0],
        "httpx_async": mock_httpx[1],
        "rabbitmq_connection": mock_rabbitmq[0],
        "rabbitmq_channel": mock_rabbitmq[1],
    }
