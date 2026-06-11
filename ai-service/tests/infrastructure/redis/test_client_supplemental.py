"""Test Redis client module (supplemental).
Redis 客户端补充测试：覆盖连接中断降级、pipeline 错误、超时场景。
"""

from unittest.mock import AsyncMock, patch

import pytest

from app.infrastructure.redis.client import RedisBuffer
from app.config import REDIS_KEY_PREFIX

# ── Connection failure during append ───────────────────────


@pytest.mark.asyncio
async def test_redis_buffer_append_connection_failure():
    """When Redis connection fails during append, error should propagate.
    Redis 连接失败时 append 应传播异常。"""
    with patch("app.infrastructure.redis.client.get_redis_client") as mock_get:
        mock_client = AsyncMock()
        mock_client.lpush.side_effect = Exception("Connection refused")
        mock_get.return_value = mock_client

        buffer = RedisBuffer()
        with pytest.raises(Exception, match="Connection refused"):
            await buffer.append({"test": "data"})


# ── Drain with empty list ──────────────────────────────────


@pytest.mark.asyncio
async def test_redis_buffer_drain_empty():
    """When buffer is empty, drain should return empty list.
    Buffer 为空时 drain 应返回空列表。"""
    with patch("app.infrastructure.redis.client.get_redis_client") as mock_get:
        mock_client = AsyncMock()
        mock_pipe = AsyncMock()
        mock_client.pipeline.return_value = mock_pipe
        mock_pipe.execute.return_value = ([], None)
        mock_get.return_value = mock_client

        buffer = RedisBuffer()
        results = await buffer.drain()

        assert results == []
        mock_pipe.lrange.assert_called_once_with(
            REDIS_KEY_PREFIX + "feedback:buffer", 0, -1
        )
        mock_pipe.delete.assert_called_once_with(REDIS_KEY_PREFIX + "feedback:buffer")


# ── Drain with pipeline failure ────────────────────────────


@pytest.mark.asyncio
async def test_redis_buffer_drain_pipeline_failure():
    """When pipeline execute fails, error should propagate.
    Pipeline 执行失败时应传播异常。"""
    with patch("app.infrastructure.redis.client.get_redis_client") as mock_get:
        mock_client = AsyncMock()
        mock_pipe = AsyncMock()
        mock_client.pipeline.return_value = mock_pipe
        mock_pipe.execute.side_effect = Exception("Pipeline broken")
        mock_get.return_value = mock_client

        buffer = RedisBuffer()
        with pytest.raises(Exception, match="Pipeline broken"):
            await buffer.drain()


# ── Lock expiry ────────────────────────────────────────────


@pytest.mark.asyncio
async def test_redis_buffer_acquire_lock_with_custom_ttl():
    """Lock acquisition should respect custom TTL.
    锁获取应尊重自定义 TTL。"""
    with patch("app.infrastructure.redis.client.get_redis_client") as mock_get:
        mock_client = AsyncMock()
        mock_client.set.return_value = True
        mock_get.return_value = mock_client

        buffer = RedisBuffer()
        result = await buffer.acquire_lock("instance-123", ttl=3600)

        assert result is True
        mock_client.set.assert_called_once_with(
            REDIS_KEY_PREFIX + "model:retrain:lock", "instance-123", nx=True, ex=3600
        )


# ── Release lock with wrong instance ───────────────────────


@pytest.mark.asyncio
async def test_redis_buffer_release_lock_wrong_instance():
    """Releasing lock with wrong instance ID should not delete the lock.
    使用错误的实例 ID 释放锁不应删除锁。"""
    with patch("app.infrastructure.redis.client.get_redis_client") as mock_get:
        mock_client = AsyncMock()
        mock_client.eval.return_value = 0  # 0 keys deleted
        mock_get.return_value = mock_client

        buffer = RedisBuffer()
        await buffer.release_lock("wrong-instance")

        mock_client.eval.assert_called_once()


# ── Broadcast with connection failure ──────────────────────


@pytest.mark.asyncio
async def test_redis_buffer_broadcast_connection_failure():
    """When publish fails due to connection error, error should propagate.
    发布因连接错误失败时应传播异常。"""
    with patch("app.infrastructure.redis.client.get_redis_client") as mock_get:
        mock_client = AsyncMock()
        mock_client.publish.side_effect = Exception("Connection lost")
        mock_get.return_value = mock_client

        buffer = RedisBuffer()
        with pytest.raises(Exception, match="Connection lost"):
            await buffer.broadcast_reload("v1.0", "model.pkl")
