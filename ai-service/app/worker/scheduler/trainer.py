import asyncio
import logging
from datetime import datetime, timezone
import lightgbm as lgb
import numpy as np
from app.config import MIN_SAMPLES_FOR_RETRAIN, RETRAIN_INTERVAL_HOURS
from app.infrastructure.redis.client import RedisBuffer
from app.infrastructure.api_client.client import InternalApiClient
from app.infrastructure.minio.client import MinioModelRegistry
from app.domain.ml.features import FEATURE_COLUMNS, extract_features
from sklearn.metrics import roc_auc_score

logger = logging.getLogger(__name__)


class IncrementalTrainer:
    def __init__(self):
        self.redis_buffer = RedisBuffer()
        self.api_client = InternalApiClient()
        self.minio_registry = MinioModelRegistry()
        self.instance_id = "worker-1"

    async def run_scheduled(self):
        while True:
            await asyncio.sleep(RETRAIN_INTERVAL_HOURS * 3600)
            try:
                await self.try_retrain()
            except Exception as e:
                logger.exception(f"Scheduled retraining failed: {e}")

    async def try_retrain(self):
        if not await self.redis_buffer.acquire_lock(self.instance_id):
            logger.info("Retrain lock not acquired. Skipping.")
            return

        try:
            logger.info("Acquired retrain lock. Starting batch retraining...")
            new_samples = await self.redis_buffer.drain()

            if len(new_samples) < MIN_SAMPLES_FOR_RETRAIN:
                logger.info(
                    f"Insufficient samples ({len(new_samples)} < {MIN_SAMPLES_FOR_RETRAIN}). Skipping."
                )
                for sample in new_samples:
                    await self.redis_buffer.append(sample)
                return

            raw_baseline = await self.api_client.get_baseline_features_async()
            baseline_samples = []
            for item in raw_baseline:
                features = extract_features(
                    item, query="", resume_text=item.get("requirements", "")
                )
                baseline_samples.append({"label": 1, "features": features})

            all_samples = baseline_samples + new_samples

            X, y = self._build_matrix(all_samples)
            if len(X) == 0:
                return

            model, metrics = self._train_model(X, y)

            version = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
            model_str = model.model_to_string()
            object_key = self.minio_registry.upload_model(
                model_str.encode("utf-8"), version
            )

            meta = {
                "version": version,
                "model_filename": object_key,
                "feature_count": len(FEATURE_COLUMNS),
                "feature_columns": FEATURE_COLUMNS,
                "train_rows": len(all_samples),
                "metrics": metrics,
                "trained_at": datetime.now(timezone.utc).isoformat(),
            }
            self.minio_registry.update_latest_meta(meta)

            await self.redis_buffer.broadcast_reload(version, object_key)
            logger.info(f"Model saved and broadcasted: version={version}")

        finally:
            await self.redis_buffer.release_lock(self.instance_id)

    def _build_matrix(self, samples: list[dict]) -> tuple[np.ndarray, np.ndarray]:
        X, y = [], []
        for sample in samples:
            features = sample.get("features", {})
            row = [float(features.get(col, 0.0)) for col in FEATURE_COLUMNS]
            X.append(row)
            y.append(1 if sample.get("label", 0) > 0 else 0)
        return np.array(X), np.array(y)

    def _train_model(self, X: np.ndarray, y: np.ndarray):
        split_idx = int(len(X) * 0.8)
        if split_idx == 0:
            split_idx = len(X)

        X_train, X_val = X[:split_idx], X[split_idx:]
        y_train, y_val = y[:split_idx], y[split_idx:]

        train_data = lgb.Dataset(X_train, label=y_train)
        valid_sets = [train_data]
        if len(X_val) > 0:
            val_data = lgb.Dataset(X_val, label=y_val, reference=train_data)
            valid_sets.append(val_data)

        params = {
            "objective": "binary",
            "metric": "auc",
            "boosting_type": "gbdt",
            "verbose": -1,
            "seed": 42,
        }

        model = lgb.train(
            params,
            train_data,
            num_boost_round=50,
            valid_sets=valid_sets,
        )

        metrics = {"num_trees": int(model.num_trees())}
        if len(X_val) > 0:
            preds = model.predict(X_val)
            if len(np.unique(y_val)) > 1:
                metrics["auc"] = round(float(roc_auc_score(y_val, preds)), 4)

        return model, metrics
