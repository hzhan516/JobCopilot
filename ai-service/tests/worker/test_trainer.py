import pytest
from unittest.mock import patch, MagicMock, AsyncMock
from app.worker.scheduler.trainer import IncrementalTrainer

@pytest.mark.asyncio
async def test_trainer_skips_when_lock_fails(mock_redis_buffer, mock_internal_api, mock_minio_registry):
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=False)
    trainer = IncrementalTrainer()
    await trainer.try_retrain()
    
    mock_redis_buffer.drain.assert_not_called()
    mock_minio_registry.upload_model.assert_not_called()

@pytest.mark.asyncio
async def test_trainer_skips_insufficient_samples(mock_redis_buffer, mock_internal_api, mock_minio_registry):
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    # Return fewer than MIN_SAMPLES_FOR_RETRAIN (which is 10)
    mock_redis_buffer.drain = AsyncMock(return_value=[{"label": 1}] * 5)
    
    trainer = IncrementalTrainer()
    await trainer.try_retrain()
    
    # Check that it puts them back
    assert mock_redis_buffer.append.call_count == 5
    mock_internal_api.get_baseline_features.assert_not_called()

@pytest.mark.asyncio
async def test_trainer_successful_run(mock_redis_buffer, mock_internal_api, mock_minio_registry):
    mock_redis_buffer.acquire_lock = AsyncMock(return_value=True)
    
    # 15 new samples
    mock_redis_buffer.drain = AsyncMock(return_value=[
        {"label": 1, "features": {"semantic_match": 0.9}},
        {"label": 0, "features": {"semantic_match": 0.1}}
    ] * 8)
    
    # 5 baseline samples
    mock_internal_api.get_baseline_features.return_value = [
        {"label": 1, "features": {"semantic_match": 0.8}}
    ] * 5
    
    trainer = IncrementalTrainer()
    await trainer.try_retrain()
    
    # Should fetch baseline
    mock_internal_api.get_baseline_features.assert_called_once()
    
    # Should upload model
    mock_minio_registry.upload_model.assert_called_once()
    
    # Should update meta
    mock_minio_registry.update_latest_meta.assert_called_once()
    
    # Should broadcast
    mock_redis_buffer.broadcast_reload.assert_called_once()
    
    # Lock released
    mock_redis_buffer.release_lock.assert_called_once_with(trainer.instance_id)
