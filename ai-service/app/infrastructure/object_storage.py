"""Object storage abstraction for model artifacts and shared files.

Supports local filesystem and MinIO/S3-compatible backends.
Local mode writes to a configurable base path; MinIO mode uses boto3.
"""

import os
from pathlib import Path
from typing import Protocol

logger = __import__("logging").getLogger(__name__)

STORAGE_TYPE = os.getenv("MODEL_STORAGE_TYPE") or os.getenv("STORAGE_TYPE", "local")
LOCAL_STORAGE_BASE_PATH = (
    os.getenv("MODEL_STORAGE_BASE_PATH")
    or os.getenv("LOCAL_STORAGE_BASE_PATH")
    or "/app/model-artifacts"
)

# Model artifacts are stored under a dedicated subdirectory
MODEL_BUCKET = os.getenv("MODEL_BUCKET", "resume-assistant-models")


class StorageBackend(Protocol):
    def put_object(self, bucket: str, key: str, body: bytes) -> None: ...
    def get_object(self, bucket: str, key: str) -> bytes | None: ...


class LocalBackend:
    """Local filesystem backend: maps bucket to subdirectory under base path."""

    def __init__(self, base_path: str):
        self._base = Path(base_path)

    def put_object(self, bucket: str, key: str, body: bytes) -> None:
        target = self._base / bucket / key
        target.parent.mkdir(parents=True, exist_ok=True)
        tmp = target.with_suffix(".tmp")
        tmp.write_bytes(body)
        tmp.rename(target)
        logger.debug("LocalBackend put_object: %s/%s", bucket, key)

    def get_object(self, bucket: str, key: str) -> bytes | None:
        target = self._base / bucket / key
        if not target.exists():
            return None
        return target.read_bytes()


class MinioBackend:
    """MinIO/S3-compatible backend using boto3."""

    def __init__(self):
        import boto3
        from botocore.config import Config

        endpoint = os.getenv("MINIO_ENDPOINT", "http://minio:9000")
        access_key = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
        secret_key = os.getenv("MINIO_SECRET_KEY", "minioadmin")

        self._s3 = boto3.client(
            "s3",
            endpoint_url=endpoint,
            aws_access_key_id=access_key,
            aws_secret_access_key=secret_key,
            config=Config(signature_version="s3v4"),
        )

    def put_object(self, bucket: str, key: str, body: bytes) -> None:
        self._s3.put_object(Bucket=bucket, Key=key, Body=body)
        logger.debug("MinioBackend put_object: %s/%s", bucket, key)

    def get_object(self, bucket: str, key: str) -> bytes | None:
        try:
            response = self._s3.get_object(Bucket=bucket, Key=key)
            return response["Body"].read()
        except self._s3.exceptions.NoSuchKey:
            return None
        except Exception:
            logger.exception("MinioBackend get_object failed: %s/%s", bucket, key)
            return None


_backend: StorageBackend | None = None


def get_object_storage() -> StorageBackend:
    """Return the singleton storage backend based on STORAGE_TYPE."""
    global _backend
    if _backend is None:
        if STORAGE_TYPE == "minio" or STORAGE_TYPE == "s3":
            _backend = MinioBackend()
        else:
            _backend = LocalBackend(LOCAL_STORAGE_BASE_PATH)
    return _backend
