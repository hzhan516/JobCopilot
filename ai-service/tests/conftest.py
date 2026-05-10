import json
from collections.abc import Iterator
from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient

# Patch Redis BEFORE importing any app modules that create singletons at import time
_redis_mock = MagicMock()
_redis_strings: dict[str, str] = {}
_redis_hashes: dict[str, dict[str, str]] = {}
_redis_sets: dict[str, set[str]] = {}
_redis_lua_scripts: dict[str, str] = {}
_redis_pubsub = MagicMock()


def _redis_get(key: str) -> str | None:
    return _redis_strings.get(key)


def _redis_set(key: str, value: str, **kwargs) -> None:
    _redis_strings[key] = value


def _redis_setex(key: str, seconds: int, value: str) -> None:
    _redis_strings[key] = value


def _redis_delete(*keys: str) -> int:
    count = 0
    for k in keys:
        if k in _redis_strings:
            del _redis_strings[k]
            count += 1
        if k in _redis_hashes:
            del _redis_hashes[k]
            count += 1
        if k in _redis_sets:
            del _redis_sets[k]
            count += 1
    return count


def _redis_hget(key: str, field: str) -> str | None:
    return _redis_hashes.get(key, {}).get(field)


def _redis_hset(key: str, **kwargs) -> int:
    if key not in _redis_hashes:
        _redis_hashes[key] = {}
    mapping = kwargs.get("mapping", {})
    for f, v in mapping.items():
        _redis_hashes[key][f] = v
    return len(mapping)


def _redis_hgetall(key: str) -> dict[str, str]:
    return dict(_redis_hashes.get(key, {}))


def _redis_hincrby(key: str, field: str, amount: int = 1) -> int:
    if key not in _redis_hashes:
        _redis_hashes[key] = {}
    current = int(_redis_hashes[key].get(field, 0))
    new_val = current + amount
    _redis_hashes[key][field] = str(new_val)
    return new_val


def _redis_sadd(key: str, *members: str) -> int:
    if key not in _redis_sets:
        _redis_sets[key] = set()
    before = len(_redis_sets[key])
    _redis_sets[key].update(members)
    return len(_redis_sets[key]) - before


def _redis_sismember(key: str, member: str) -> bool:
    return member in _redis_sets.get(key, set())


def _redis_scard(key: str) -> int:
    return len(_redis_sets.get(key, set()))


def _redis_spop(key: str, count: int = 1) -> list[str]:
    s = _redis_sets.get(key, set())
    popped = []
    for _ in range(min(count, len(s))):
        item = s.pop()
        popped.append(item)
    return popped


def _redis_publish(channel: str, message: str) -> int:
    return 0


def _redis_script_load(script: str) -> str:
    sha = f"sha:{hash(script) & 0xFFFFFFFF:08x}"
    _redis_lua_scripts[sha] = script
    return sha


def _redis_evalsha(sha: str, numkeys: int, *args) -> list:
    if sha in _redis_lua_scripts:
        key = args[0]
        new_val = float(args[1])
        cap = float(args[2])
        h = _redis_hashes.setdefault(key, {"sum": "0", "count": "0"})
        sum_val = float(h["sum"])
        count_val = float(h["count"])
        if count_val >= cap:
            ratio = cap / (cap + 1)
            sum_val *= ratio
            count_val *= ratio
        sum_val += new_val
        count_val += 1
        h["sum"] = str(sum_val)
        h["count"] = str(count_val)
        return [str(sum_val), str(count_val)]
    return []


def _redis_has_key(key: str) -> bool:
    return key in _redis_strings or key in _redis_hashes or key in _redis_sets


_redis_mock.get = _redis_get
_redis_mock.set = _redis_set
_redis_mock.setex = _redis_setex
_redis_mock.delete = _redis_delete
_redis_mock.hget = _redis_hget
_redis_mock.hset = _redis_hset
_redis_mock.hgetall = _redis_hgetall
_redis_mock.hincrby = _redis_hincrby
_redis_mock.sadd = _redis_sadd
_redis_mock.sismember = _redis_sismember
_redis_mock.scard = _redis_scard
_redis_mock.spop = _redis_spop
_redis_mock.publish = _redis_publish
_redis_mock.script_load = _redis_script_load
_redis_mock.evalsha = _redis_evalsha
_redis_mock.has_key = _redis_has_key
_redis_mock.pubsub = MagicMock(return_value=_redis_pubsub)

# Pipeline mock
_redis_pipeline_commands: list = []


def _redis_pipeline():
    pipe = MagicMock()
    _redis_pipeline_commands.clear()

    def _pipe_evalsha(sha, numkeys, *args):
        _redis_pipeline_commands.append(("evalsha", sha, numkeys, args))
        return pipe

    def _pipe_execute():
        results = []
        for cmd, sha, numkeys, args in _redis_pipeline_commands:
            if cmd == "evalsha":
                results.append(_redis_evalsha(sha, numkeys, *args))
        return results

    pipe.evalsha = _pipe_evalsha
    pipe.execute = _pipe_execute
    return pipe


_redis_mock.pipeline = _redis_pipeline

# Apply patch before any app imports
import app.infrastructure.redis_client as _redis_client_module
_redis_client_module._redis_client = _redis_mock
_redis_client_module.get_redis = lambda: _redis_mock

# Object storage patch
_storage_mock = MagicMock()
_storage_store: dict[str, bytes] = {}


def _storage_put(bucket: str, key: str, body: bytes) -> None:
    _storage_store[f"{bucket}/{key}"] = body if isinstance(body, bytes) else body.encode("utf-8")


def _storage_get(bucket: str, key: str) -> bytes | None:
    return _storage_store.get(f"{bucket}/{key}")


_storage_mock.put_object = _storage_put
_storage_mock.get_object = _storage_get

import app.infrastructure.object_storage as _storage_module
_storage_module._backend = _storage_mock
_storage_module.get_object_storage = lambda: _storage_mock

# Now safe to import app.main
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
def mock_redis(monkeypatch: pytest.MonkeyPatch) -> MagicMock:
    """Return the pre-configured in-memory Redis mock."""
    # Reset in-memory stores between tests
    _redis_strings.clear()
    _redis_hashes.clear()
    _redis_sets.clear()
    _redis_lua_scripts.clear()
    return _redis_mock


@pytest.fixture
def mock_object_storage(monkeypatch: pytest.MonkeyPatch) -> MagicMock:
    """Return the pre-configured in-memory object storage mock."""
    _storage_store.clear()
    return _storage_mock


@pytest.fixture
def mock_dependencies(mock_litellm: MagicMock, mock_httpx: tuple[MagicMock, MagicMock], mock_rabbitmq: tuple[MagicMock, MagicMock]) -> dict[str, object]:
    return {
        "litellm": mock_litellm,
        "httpx_sync": mock_httpx[0],
        "httpx_async": mock_httpx[1],
        "rabbitmq_connection": mock_rabbitmq[0],
        "rabbitmq_channel": mock_rabbitmq[1],
    }
