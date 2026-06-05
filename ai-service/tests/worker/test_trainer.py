"""Test incremental trainer module.
增量训练器测试：覆盖锁竞争、样本不足、训练失败、MinIO 失败、API 失败等场景。
"""
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.worker.scheduler.trainer import IncrementalTrainer


# ── Lock acquisition failure ─────────────────────────────

@pytest.mark.asyncio
async def test_trainer_skips_when_lock_fails(mock_redis_buffer, mock_internal_api, mock_minio_registry):
    """When lock is held by another instance, training should be skipped.
    当锁被其他实例持有时，应跳过训练。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=False)
    trainer = IncrementalTrainer()
    await trainer.try_retrain()

    mock_redis_buffer.drain.assert_not_called()
    mock_minio_registry.upload_model.assert_not_called()


# ── Insufficient samples ─────────────────────────────────

@pytest.mark.asyncio
async def test_trainer_skips_insufficient_samples(mock_redis_buffer, mock_internal_api, mock_minio_registry):
    """When fewer than MIN_SAMPLES_FOR_RETRAIN samples, they should be put back.
    样本不足时应将数据放回队列。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    mock_redis_buffer.drain = AsyncMock(return_value=[{"label": 1}] * 5)

    trainer = IncrementalTrainer()
    await trainer.try_retrain()

    assert mock_redis_buffer.append.call_count == 5
    mock_internal_api.get_baseline_features_async.assert_not_called()


# ── Successful training run ──────────────────────────────

@pytest.mark.asyncio
async def test_trainer_successful_run(mock_redis_buffer, mock_internal_api, mock_minio_registry):
    """Full successful run should fetch baseline, train, upload, and broadcast.
    完整成功运行应获取基线、训练、上传、广播。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    mock_redis_buffer.drain = AsyncMock(return_value=[
        {"label": 1, "features": {"semantic_match": 0.9}},
        {"label": 0, "features": {"semantic_match": 0.1}},
    ] * 8)
    mock_internal_api.get_baseline_features_async = AsyncMock(return_value=[
        {"label": 1, "features": {"semantic_match": 0.8}}
    ] * 5)

    trainer = IncrementalTrainer()
    await trainer.try_retrain()

    mock_internal_api.get_baseline_features_async.assert_called_once()
    mock_minio_registry.upload_model.assert_called_once()
    mock_minio_registry.update_latest_meta.assert_called_once()
    mock_redis_buffer.broadcast_reload.assert_called_once()
    mock_redis_buffer.release_lock.assert_called_once_with(trainer.instance_id)


# ── Baseline API failure ─────────────────────────────────

@pytest.mark.asyncio
async def test_trainer_handles_baseline_api_failure(mock_redis_buffer, mock_internal_api, mock_minio_registry):
    """When baseline API fails, training should abort and lock released.
    基线 API 失败时应中止训练并释放锁。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    mock_redis_buffer.drain = AsyncMock(return_value=[
        {"label": 1, "features": {"semantic_match": 0.9}},
    ] * 15)
    mock_internal_api.get_baseline_features_async = AsyncMock(side_effect=Exception("Backend down"))

    trainer = IncrementalTrainer()
    await trainer.try_retrain()

    mock_minio_registry.upload_model.assert_not_called()
    mock_redis_buffer.release_lock.assert_called_once()


# ── LightGBM training failure ──────────────────────────────

@pytest.mark.asyncio
async def test_trainer_handles_training_failure(mock_redis_buffer, mock_internal_api, mock_minio_registry):
    """When LightGBM training fails, no model should be uploaded.
    LightGBM 训练失败时不应上传模型。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    mock_redis_buffer.drain = AsyncMock(return_value=[
        {"label": 1, "features": {"semantic_match": 0.9}},
    ] * 15)
    mock_internal_api.get_baseline_features_async = AsyncMock(return_value=[])

    with patch("app.worker.scheduler.trainer.lgb.train") as mock_train:
        mock_train.side_effect = Exception("Invalid data format")

        trainer = IncrementalTrainer()
        await trainer.try_retrain()

    mock_minio_registry.upload_model.assert_not_called()
    mock_redis_buffer.release_lock.assert_called_once()


# ── MinIO upload failure ───────────────────────────────────

@pytest.mark.asyncio
async def test_trainer_handles_minio_upload_failure(mock_redis_buffer, mock_internal_api, mock_minio_registry):
    """When MinIO upload fails, meta should not be updated.
    MinIO 上传失败时不应更新元数据。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    mock_redis_buffer.drain = AsyncMock(return_value=[
        {"label": 1, "features": {"semantic_match": 0.9}},
    ] * 15)
    mock_internal_api.get_baseline_features_async = AsyncMock(return_value=[])
    mock_minio_registry.upload_model.side_effect = Exception("S3 quota exceeded")

    with patch("app.worker.scheduler.trainer.lgb.train") as mock_train:
        mock_model = MagicMock()
        mock_model.model_to_string.return_value = "model_bytes"
        mock_train.return_value = mock_model

        trainer = IncrementalTrainer()
        await trainer.try_retrain()

    mock_minio_registry.update_latest_meta.assert_not_called()
    mock_redis_buffer.broadcast_reload.assert_not_called()
    mock_redis_buffer.release_lock.assert_called_once()


# ── Meta update failure ────────────────────────────────────

@pytest.mark.asyncio
async def test_trainer_handles_meta_update_failure(mock_redis_buffer, mock_internal_api, mock_minio_registry):
    """When meta update fails after upload, error should be logged but lock released.
    元数据更新失败时应记录错误但释放锁。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    mock_redis_buffer.drain = AsyncMock(return_value=[
        {"label": 1, "features": {"semantic_match": 0.9}},
    ] * 15)
    mock_internal_api.get_baseline_features_async = AsyncMock(return_value=[])
    mock_minio_registry.update_latest_meta.side_effect = Exception("Meta write failed")

    with patch("app.worker.scheduler.trainer.lgb.train") as mock_train:
        mock_model = MagicMock()
        mock_model.model_to_string.return_value = "model_bytes"
        mock_train.return_value = mock_model

        trainer = IncrementalTrainer()
        await trainer.try_retrain()

    mock_minio_registry.upload_model.assert_called_once()
    mock_redis_buffer.broadcast_reload.assert_not_called()
    mock_redis_buffer.release_lock.assert_called_once()
