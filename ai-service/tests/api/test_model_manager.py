import pytest
from unittest.mock import MagicMock, patch
from app.api.model_manager import ModelManager

@pytest.mark.asyncio
async def test_load_latest_no_meta(mock_minio_registry):
    mock_minio_registry.get_latest_meta.return_value = None
    manager = ModelManager()
    await manager.load_latest()
    assert manager._model is None

@pytest.mark.asyncio
async def test_predict_without_model():
    manager = ModelManager()
    with pytest.raises(RuntimeError, match="Model not loaded"):
        await manager.predict([[0.5, 0.5]])

# Note: We don't test actual LightGBM loading here to avoid binary dependencies in unit tests,
# but the failure path is tested.
