import json
import pytest
from unittest.mock import AsyncMock, patch
from app.infrastructure.redis.client import RedisBuffer, get_redis_client
from app.config import REDIS_KEY_PREFIX

@pytest.fixture
def mock_redis():
    with patch("app.infrastructure.redis.client.get_redis_client") as mock_get:
        mock_client = AsyncMock()
        mock_get.return_value = mock_client
        yield mock_client

@pytest.mark.asyncio
async def test_redis_buffer_append(mock_redis):
    buffer = RedisBuffer()
    item = {"test": "data"}
    
    await buffer.append(item)
    
    mock_redis.lpush.assert_called_once_with(
        REDIS_KEY_PREFIX + "feedback:buffer", 
        json.dumps(item, ensure_ascii=False)
    )

@pytest.mark.asyncio
async def test_redis_buffer_drain(mock_redis):
    buffer = RedisBuffer()
    
    from unittest.mock import MagicMock
    mock_pipe = MagicMock()
    mock_pipe.execute = AsyncMock()
    mock_redis.pipeline = MagicMock(return_value=mock_pipe)
    
    # Mock the execute return value: (results, _)
    mock_pipe.execute.return_value = (
        [json.dumps({"item": 1}), json.dumps({"item": 2}), "invalid json"],
        None
    )
    
    results = await buffer.drain()
    
    mock_redis.pipeline.assert_called_once()
    mock_pipe.lrange.assert_called_once_with(REDIS_KEY_PREFIX + "feedback:buffer", 0, -1)
    mock_pipe.delete.assert_called_once_with(REDIS_KEY_PREFIX + "feedback:buffer")
    mock_pipe.execute.assert_called_once()
    
    assert len(results) == 2
    assert results[0] == {"item": 1}
    assert results[1] == {"item": 2}

@pytest.mark.asyncio
async def test_redis_buffer_acquire_lock(mock_redis):
    buffer = RedisBuffer()
    mock_redis.set.return_value = True
    
    result = await buffer.acquire_lock("instance-123", ttl=1800)
    
    mock_redis.set.assert_called_once_with(
        REDIS_KEY_PREFIX + "model:retrain:lock", "instance-123", nx=True, ex=1800
    )
    assert result is True

@pytest.mark.asyncio
async def test_redis_buffer_release_lock(mock_redis):
    buffer = RedisBuffer()
    
    await buffer.release_lock("instance-123")
    
    mock_redis.eval.assert_called_once()
    args = mock_redis.eval.call_args[0]
    assert "if redis.call" in args[0]
    assert args[1] == 1
    assert args[2] == REDIS_KEY_PREFIX + "model:retrain:lock"
    assert args[3] == "instance-123"

@pytest.mark.asyncio
async def test_redis_buffer_broadcast_reload(mock_redis):
    buffer = RedisBuffer()
    
    await buffer.broadcast_reload("v1.0", "model.pkl")
    
    expected_payload = json.dumps({"version": "v1.0", "object_key": "model.pkl"})
    mock_redis.publish.assert_called_once_with("ai.model.reload", expected_payload)

@pytest.mark.asyncio
async def test_redis_buffer_close(mock_redis):
    buffer = RedisBuffer()
    await buffer.close()
    mock_redis.close.assert_called_once()

def test_get_redis_client():
    with patch("app.infrastructure.redis.client.redis.Redis") as mock_redis_class:
        client = get_redis_client()
        mock_redis_class.assert_called_once()
        assert client == mock_redis_class.return_value
