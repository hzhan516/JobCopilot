"""Test ModelManager module.
模型管理器测试：覆盖加载、预测、热重载等核心路径。
"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.api.model_manager import ModelManager

# ── load_latest ────────────────────────────────────────────


@pytest.mark.asyncio
async def test_load_latest_with_valid_meta():
    """When MinIO has a valid meta record, model should be downloaded and loaded.
    MinIO 存在有效元数据时，应下载并加载模型。"""
    with patch("app.api.model_manager.MinioModelRegistry") as mock_registry_cls:
        mock_registry = MagicMock()
        mock_registry.get_latest_meta.return_value = {
            "version": "v1.0",
            "accuracy": 0.95,
            "model_filename": "ranker_model_v1.0.txt",
        }
        mock_registry.download_model.return_value = None
        mock_registry_cls.return_value = mock_registry

        with patch("app.api.model_manager.lgb.Booster") as mock_booster_cls:
            mock_model = MagicMock()
            mock_booster_cls.return_value = mock_model

            manager = ModelManager()
            await manager.load_latest()

            assert manager._model is mock_model
            mock_registry.get_latest_meta.assert_called_once()
            mock_registry.download_model.assert_called_once_with(
                "ranker_model_v1.0.txt", "/tmp/ranker_model_v1.0.txt"
            )


@pytest.mark.asyncio
async def test_load_latest_no_meta():
    """When no meta exists, model should remain None.
    无元数据时，模型应保持为 None。"""
    with patch("app.api.model_manager.MinioModelRegistry") as mock_registry_cls:
        mock_registry = MagicMock()
        mock_registry.get_latest_meta.return_value = None
        mock_registry_cls.return_value = mock_registry

        manager = ModelManager()
        await manager.load_latest()

        assert manager._model is None


@pytest.mark.asyncio
async def test_load_latest_download_failure():
    """When download fails, model should remain None and error logged.
    下载失败时，模型应保持为 None 并记录错误。"""
    with patch("app.api.model_manager.MinioModelRegistry") as mock_registry_cls:
        mock_registry = MagicMock()
        mock_registry.get_latest_meta.return_value = {
            "version": "v1.0",
            "model_filename": "ranker_model_v1.0.txt",
        }
        mock_registry.download_model.side_effect = Exception("S3 down")
        mock_registry_cls.return_value = mock_registry

        manager = ModelManager()
        await manager.load_latest()

        assert manager._model is None


@pytest.mark.asyncio
async def test_load_latest_missing_model_filename():
    """When meta lacks model_filename, model should not be loaded.
    元数据缺少 model_filename 时不应加载模型。"""
    with patch("app.api.model_manager.MinioModelRegistry") as mock_registry_cls:
        mock_registry = MagicMock()
        mock_registry.get_latest_meta.return_value = {
            "version": "v1.0",
        }
        mock_registry_cls.return_value = mock_registry

        manager = ModelManager()
        await manager.load_latest()

        assert manager._model is None
        mock_registry.download_model.assert_not_called()


# ── predict ────────────────────────────────────────────────


@pytest.mark.asyncio
async def test_predict_with_loaded_model():
    """When model is loaded, predict should return scores.
    模型已加载时，predict 应返回分数列表。"""
    with patch("app.api.model_manager.MinioModelRegistry") as mock_registry_cls:
        mock_registry = MagicMock()
        mock_registry.get_latest_meta.return_value = None
        mock_registry_cls.return_value = mock_registry

        manager = ModelManager()
        mock_model = MagicMock()
        mock_result = MagicMock()
        mock_result.tolist.return_value = [0.8, 0.3]
        mock_model.predict.return_value = mock_result
        manager._model = mock_model

        result = await manager.predict([[0.1] * 8, [0.2] * 8])

        assert result == [0.8, 0.3]
        mock_model.predict.assert_called_once()


@pytest.mark.asyncio
async def test_predict_without_model():
    """When no model is loaded, predict should raise RuntimeError.
    无模型时，predict 应抛出 RuntimeError。"""
    manager = ModelManager()

    with pytest.raises(RuntimeError, match="Model not loaded"):
        await manager.predict([[0.1] * 8])


# ── watch_for_reloads ────────────────────────────────────────


@pytest.mark.asyncio
async def test_watch_for_reloads_loads_on_message():
    """When reload message received, should call load_latest.
    收到重载消息时应调用 load_latest。"""
    with patch("app.api.model_manager.MinioModelRegistry") as mock_registry_cls:
        mock_registry = MagicMock()
        mock_registry.get_latest_meta.return_value = None
        mock_registry_cls.return_value = mock_registry

        manager = ModelManager()
        mock_pubsub = AsyncMock()
        mock_pubsub.subscribe = AsyncMock()
        mock_pubsub.listen = MagicMock(
            return_value=async_iter(
                [
                    {"type": "subscribe", "channel": "ai.model.reload"},
                    {"type": "message", "data": '{"version": "v2.0"}'},
                ]
            )
        )
        manager.redis.pubsub = MagicMock(return_value=mock_pubsub)

        with patch.object(manager, "load_latest", AsyncMock()) as mock_load:
            # Run watch_for_reloads for a short time then cancel
            import asyncio

            task = asyncio.create_task(manager.watch_for_reloads())
            await asyncio.sleep(0.1)
            task.cancel()
            try:
                await task
            except asyncio.CancelledError:
                pass

            mock_load.assert_called_once()


# Helper for async iterator
async def async_iter(items):
    for item in items:
        yield item
