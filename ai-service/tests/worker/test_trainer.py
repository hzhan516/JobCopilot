"""Test incremental trainer module.
增量训练器测试：覆盖锁竞争、样本不足、训练失败、MinIO 失败、API 失败等场景。
"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.schemas import BaselineFeature
from app.worker.scheduler.trainer import IncrementalTrainer

# ── Lock acquisition failure ─────────────────────────────


@pytest.mark.asyncio
async def test_trainer_skips_when_lock_fails(
    mock_redis_buffer, mock_internal_api, mock_minio_registry
):
    """When lock is held by another instance, training should be skipped.
    当锁被其他实例持有时，应跳过训练。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=False)
    trainer = IncrementalTrainer()
    await trainer.try_retrain()

    mock_redis_buffer.drain.assert_not_called()
    mock_minio_registry.upload_model.assert_not_called()


# ── Insufficient samples ─────────────────────────────────


@pytest.mark.asyncio
async def test_trainer_skips_insufficient_samples(
    mock_redis_buffer, mock_internal_api, mock_minio_registry
):
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
async def test_trainer_successful_run(
    mock_redis_buffer, mock_internal_api, mock_minio_registry
):
    """Full successful run should fetch baseline, train, upload, and broadcast.
    完整成功运行应获取基线、训练、上传、广播。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    mock_redis_buffer.drain = AsyncMock(
        return_value=[
            {"label": 1, "features": {"semantic_match": 0.9}},
            {"label": 0, "features": {"semantic_match": 0.1}},
        ]
        * 8
    )
    mock_internal_api.get_baseline_features_async = AsyncMock(
        return_value=[
            BaselineFeature(
                job_id=f"baseline-{i}",
                title=f"Job {i}",
                description="test description",
            )
            for i in range(5)
        ]
    )

    trainer = IncrementalTrainer()
    await trainer.try_retrain()

    mock_internal_api.get_baseline_features_async.assert_called_once()
    mock_minio_registry.upload_model.assert_called_once()
    mock_minio_registry.update_latest_meta.assert_called_once()
    mock_redis_buffer.broadcast_reload.assert_called_once()
    mock_redis_buffer.release_lock.assert_called_once_with(trainer.instance_id)


# ── Baseline API failure ─────────────────────────────────


@pytest.mark.asyncio
async def test_trainer_handles_baseline_api_failure(
    mock_redis_buffer, mock_internal_api, mock_minio_registry
):
    """When baseline API fails, training should abort and lock released.
    基线 API 失败时应中止训练并释放锁。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    mock_redis_buffer.drain = AsyncMock(
        return_value=[
            {"label": 1, "features": {"semantic_match": 0.9}},
        ]
        * 10
    )
    mock_internal_api.get_baseline_features_async = AsyncMock(
        side_effect=RuntimeError("API unavailable")
    )

    trainer = IncrementalTrainer()
    with pytest.raises(RuntimeError, match="API unavailable"):
        await trainer.try_retrain()

    mock_minio_registry.upload_model.assert_not_called()
    mock_redis_buffer.release_lock.assert_called_once()


# ── Training failure ───────────────────────────────────────


@pytest.mark.asyncio
async def test_trainer_handles_training_failure(
    mock_redis_buffer, mock_internal_api, mock_minio_registry
):
    """When LightGBM training fails, lock should be released.
    LightGBM 训练失败时应释放锁。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    mock_redis_buffer.drain = AsyncMock(
        return_value=[
            {"label": 1, "features": {}},
        ]
        * 10
    )
    mock_internal_api.get_baseline_features_async = AsyncMock(
        return_value=[
            BaselineFeature(
                job_id=f"baseline-{i}",
                title=f"Job {i}",
                description="test",
            )
            for i in range(5)
        ]
    )

    # Force _train_model to raise to simulate a training failure
    trainer = IncrementalTrainer()
    trainer._train_model = MagicMock(side_effect=RuntimeError("LightGBM crash"))
    with pytest.raises(RuntimeError, match="LightGBM crash"):
        await trainer.try_retrain()

    mock_minio_registry.upload_model.assert_not_called()
    mock_redis_buffer.release_lock.assert_called_once()


# ── MinIO upload failure ────────────────────────────────────


@pytest.mark.asyncio
async def test_trainer_handles_minio_upload_failure(
    mock_redis_buffer, mock_internal_api, mock_minio_registry
):
    """When MinIO upload fails, lock should be released.
    MinIO 上传失败时应释放锁。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    mock_redis_buffer.drain = AsyncMock(
        return_value=[
            {"label": 1, "features": {"semantic_match": 0.9}},
        ]
        * 10
    )
    mock_internal_api.get_baseline_features_async = AsyncMock(
        return_value=[
            BaselineFeature(
                job_id=f"baseline-{i}",
                title=f"Job {i}",
                description="test",
            )
            for i in range(5)
        ]
    )
    mock_minio_registry.upload_model.side_effect = Exception("S3 quota exceeded")

    trainer = IncrementalTrainer()
    try:
        await trainer.try_retrain()
    except Exception:
        pass

    mock_redis_buffer.release_lock.assert_called_once()


# ── Meta update failure ──────────────────────────────────────


@pytest.mark.asyncio
async def test_trainer_handles_meta_update_failure(
    mock_redis_buffer, mock_internal_api, mock_minio_registry
):
    """When meta update fails, lock should still be released.
    Meta 更新失败时仍应释放锁。"""
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    mock_redis_buffer.drain = AsyncMock(
        return_value=[
            {"label": 1, "features": {"semantic_match": 0.9}},
        ]
        * 10
    )
    mock_internal_api.get_baseline_features_async = AsyncMock(
        return_value=[
            BaselineFeature(
                job_id=f"baseline-{i}",
                title=f"Job {i}",
                description="test",
            )
            for i in range(5)
        ]
    )
    mock_minio_registry.update_latest_meta.side_effect = Exception(
        "Meta write failed"
    )

    trainer = IncrementalTrainer()
    try:
        await trainer.try_retrain()
    except Exception:
        pass

    mock_redis_buffer.release_lock.assert_called_once()
