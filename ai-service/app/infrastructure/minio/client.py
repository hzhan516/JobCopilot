import json
import logging
from typing import Any
import boto3
from botocore.client import Config
from app.config import MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_MODEL_BUCKET

logger = logging.getLogger(__name__)

class MinioModelRegistry:
    def __init__(self):
        self.s3 = boto3.client(
            's3',
            endpoint_url=MINIO_ENDPOINT,
            aws_access_key_id=MINIO_ACCESS_KEY,
            aws_secret_access_key=MINIO_SECRET_KEY,
            config=Config(signature_version='s3v4'),
            region_name='us-east-1'
        )
        self._ensure_bucket_exists()

    def _ensure_bucket_exists(self):
        try:
            self.s3.head_bucket(Bucket=MINIO_MODEL_BUCKET)
        except Exception:
            logger.info(f"Creating bucket {MINIO_MODEL_BUCKET}")
            try:
                self.s3.create_bucket(Bucket=MINIO_MODEL_BUCKET)
            except Exception as e:
                logger.error(f"Failed to create bucket: {e}")

    def upload_model(self, model_bytes: bytes, version: str) -> str:
        object_key = f"ranker_model_{version}.txt"
        self.s3.put_object(Bucket=MINIO_MODEL_BUCKET, Key=object_key, Body=model_bytes)
        return object_key

    def update_latest_meta(self, meta: dict[str, Any]):
        self.s3.put_object(
            Bucket=MINIO_MODEL_BUCKET,
            Key="latest_meta.json",
            Body=json.dumps(meta, ensure_ascii=False).encode('utf-8')
        )

    def get_latest_meta(self) -> dict[str, Any] | None:
        try:
            response = self.s3.get_object(Bucket=MINIO_MODEL_BUCKET, Key="latest_meta.json")
            return json.loads(response['Body'].read().decode('utf-8'))
        except Exception as e:
            logger.warning(f"Could not load latest meta: {e}")
            return None

    def download_model(self, object_key: str, dest_path: str):
        self.s3.download_file(MINIO_MODEL_BUCKET, object_key, dest_path)
