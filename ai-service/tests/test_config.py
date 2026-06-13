import importlib
import os
import sys
from unittest.mock import patch

import pytest


def _reload_config():
    """Remove app.config from sys.modules so it can be freshly imported.
    从 sys.modules 移除 app.config 以触发重新导入和顶层校验。"""
    if "app.config" in sys.modules:
        del sys.modules["app.config"]


def test_config_raises_when_minio_missing_in_non_dev():
    """Should raise RuntimeError when MINIO env vars missing in non-dev / 非 dev 环境缺少 MINIO 环境变量时应抛出 RuntimeError"""
    env = {
        "ENV": "prod",
        "MINIO_ENDPOINT": "",
        "MINIO_ACCESS_KEY": "",
        "MINIO_SECRET_KEY": "",
        "INTERNAL_API_KEY": "secret",
    }
    with patch.dict(os.environ, env, clear=True):
        _reload_config()
        with pytest.raises(RuntimeError) as exc_info:
            importlib.import_module("app.config")
        assert (
            "MINIO_ENDPOINT, MINIO_ACCESS_KEY and MINIO_SECRET_KEY are required"
            in str(exc_info.value)
        )


def test_config_raises_when_internal_api_key_missing_in_non_dev():
    """Should raise RuntimeError when INTERNAL_API_KEY missing in non-dev / 非 dev 环境缺少 INTERNAL_API_KEY 时应抛出 RuntimeError"""
    env = {
        "ENV": "staging",
        "MINIO_ENDPOINT": "http://minio:9000",
        "MINIO_ACCESS_KEY": "key",
        "MINIO_SECRET_KEY": "secret",
        "INTERNAL_API_KEY": "",
    }
    with patch.dict(os.environ, env, clear=True):
        _reload_config()
        with pytest.raises(RuntimeError) as exc_info:
            importlib.import_module("app.config")
        assert "INTERNAL_API_KEY environment variable is required" in str(
            exc_info.value
        )


def test_config_accepts_dev_without_minio():
    """Should not raise in dev when MINIO vars missing / dev 环境缺少 MINIO 变量时不应抛出"""
    env = {
        "ENV": "dev",
        "MINIO_ENDPOINT": "",
        "MINIO_ACCESS_KEY": "",
        "MINIO_SECRET_KEY": "",
        "INTERNAL_API_KEY": "",
    }
    with patch.dict(os.environ, env, clear=True):
        _reload_config()
        # Should not raise
        app_config = importlib.import_module("app.config")
        assert app_config.ENV == "dev"
