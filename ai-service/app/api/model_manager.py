import asyncio
import logging
from pathlib import Path
import lightgbm as lgb
from app.infrastructure.minio.client import MinioModelRegistry
from app.infrastructure.redis.client import get_redis_client
import json

logger = logging.getLogger(__name__)


class ModelManager:
    def __init__(self):
        self._model: lgb.Booster | None = None
        self._model_meta: dict = {}
        self._lock = asyncio.Lock()
        self.minio = MinioModelRegistry()
        self.redis = get_redis_client()

    async def load_latest(self):
        meta = self.minio.get_latest_meta()
        if not meta:
            logger.warning("No latest meta found in MinIO. Running without model.")
            return

        object_key = meta.get("model_filename")
        if not object_key:
            return

        tmp_path = f"/tmp/{object_key}"
        try:
            self.minio.download_model(object_key, tmp_path)
            async with self._lock:
                self._model = lgb.Booster(model_file=tmp_path)
                self._model_meta = meta
            logger.info(f"Loaded ranker model version {meta.get('version')}")
        except Exception as e:
            logger.error(f"Failed to load model from MinIO: {e}")
        finally:
            path = Path(tmp_path)
            if path.exists():
                path.unlink()

    async def predict(self, feature_matrix: list[list[float]]) -> list[float]:
        async with self._lock:
            if not self._model:
                raise RuntimeError("Model not loaded")
            scores = self._model.predict(feature_matrix, num_threads=2)
            return scores.tolist()

    async def watch_for_reloads(self):
        pubsub = self.redis.pubsub()
        await pubsub.subscribe("ai.model.reload")
        logger.info("Subscribed to ai.model.reload channel")

        async for message in pubsub.listen():
            if message["type"] != "message":
                continue
            try:
                payload = json.loads(message["data"])
                logger.info(f"Reload notification received: {payload}")
                await self.load_latest()
            except Exception as e:
                logger.error(f"Reload failed: {e}")


model_manager = ModelManager()


def get_model_manager() -> ModelManager:
    """Return the singleton ModelManager instance."""
    return model_manager
