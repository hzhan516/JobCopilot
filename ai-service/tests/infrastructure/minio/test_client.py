import json
import pytest
from botocore.exceptions import ClientError
from unittest.mock import patch, MagicMock
from app.infrastructure.minio.client import MinioModelRegistry
from app.config import MINIO_MODEL_BUCKET


@pytest.fixture
def mock_boto3_client():
    with patch("app.infrastructure.minio.client.boto3.client") as mock_client:
        mock_s3 = MagicMock()
        mock_client.return_value = mock_s3
        yield mock_s3


def test_init_ensures_bucket_exists(mock_boto3_client):
    # When head_bucket succeeds
    _ = MinioModelRegistry()
    mock_boto3_client.head_bucket.assert_called_once_with(Bucket=MINIO_MODEL_BUCKET)
    mock_boto3_client.create_bucket.assert_not_called()


def test_init_creates_bucket_if_missing(mock_boto3_client):
    # When head_bucket fails
    mock_boto3_client.head_bucket.side_effect = Exception("Not found")
    _ = MinioModelRegistry()
    mock_boto3_client.head_bucket.assert_called_once_with(Bucket=MINIO_MODEL_BUCKET)
    mock_boto3_client.create_bucket.assert_called_once_with(Bucket=MINIO_MODEL_BUCKET)


def test_upload_model(mock_boto3_client):
    registry = MinioModelRegistry()
    model_bytes = b"dummy_model_data"
    version = "v1.0"

    object_key = registry.upload_model(model_bytes, version)

    assert object_key == f"ranker_model_{version}.txt"
    mock_boto3_client.put_object.assert_called_once_with(
        Bucket=MINIO_MODEL_BUCKET, Key=object_key, Body=model_bytes
    )


def test_update_latest_meta(mock_boto3_client):
    registry = MinioModelRegistry()
    meta = {"version": "v1.0", "accuracy": 0.95}

    registry.update_latest_meta(meta)

    mock_boto3_client.put_object.assert_called_once_with(
        Bucket=MINIO_MODEL_BUCKET,
        Key="latest_meta.json",
        Body=json.dumps(meta, ensure_ascii=False).encode("utf-8"),
    )


def test_get_latest_meta_success(mock_boto3_client):
    registry = MinioModelRegistry()
    meta = {"version": "v1.0", "accuracy": 0.95}

    mock_response = {"Body": MagicMock()}
    mock_response["Body"].read.return_value = json.dumps(meta).encode("utf-8")
    mock_boto3_client.get_object.return_value = mock_response

    result = registry.get_latest_meta()

    assert result == meta
    mock_boto3_client.get_object.assert_called_once_with(
        Bucket=MINIO_MODEL_BUCKET, Key="latest_meta.json"
    )


def test_get_latest_meta_failure(mock_boto3_client):
    registry = MinioModelRegistry()
    mock_boto3_client.get_object.side_effect = Exception("Not found")

    result = registry.get_latest_meta()

    assert result is None
    mock_boto3_client.get_object.assert_called_once_with(
        Bucket=MINIO_MODEL_BUCKET, Key="latest_meta.json"
    )


def test_download_model(mock_boto3_client):
    registry = MinioModelRegistry()
    object_key = "ranker_model_v1.0.txt"
    dest_path = "/tmp/model.txt"

    registry.download_model(object_key, dest_path)

    mock_boto3_client.download_file.assert_called_once_with(
        MINIO_MODEL_BUCKET, object_key, dest_path
    )


def test_list_objects_returns_contents(mock_boto3_client):
    registry = MinioModelRegistry()
    mock_boto3_client.list_objects_v2.return_value = {
        "Contents": [
            {"Key": "ranker_model_v1.0.txt", "Size": 1024, "LastModified": "now"}
        ]
    }

    result = registry.list_objects(prefix="ranker_model_")

    assert len(result) == 1
    assert result[0]["Key"] == "ranker_model_v1.0.txt"
    mock_boto3_client.list_objects_v2.assert_called_once_with(
        Bucket=MINIO_MODEL_BUCKET, Prefix="ranker_model_"
    )


def test_list_objects_returns_empty_when_no_contents(mock_boto3_client):
    registry = MinioModelRegistry()
    mock_boto3_client.list_objects_v2.return_value = {}

    result = registry.list_objects(prefix="ranker_model_")

    assert result == []


def test_object_exists_when_present(mock_boto3_client):
    registry = MinioModelRegistry()
    mock_boto3_client.head_object.return_value = {"ContentLength": 1024}

    assert registry.object_exists("ranker_model_v1.0.txt") is True
    mock_boto3_client.head_object.assert_called_once_with(
        Bucket=MINIO_MODEL_BUCKET, Key="ranker_model_v1.0.txt"
    )


def test_object_exists_when_missing(mock_boto3_client):
    registry = MinioModelRegistry()
    mock_boto3_client.exceptions.ClientError = ClientError
    error_response = {"Error": {"Code": "404", "Message": "Not Found"}}
    mock_boto3_client.head_object.side_effect = ClientError(
        error_response, "HeadObject"
    )

    assert registry.object_exists("missing.txt") is False
