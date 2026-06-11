import os
import sys
from unittest.mock import patch

import pytest


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
        # Force reload of config module so the top-level checks re-run
        if "app.config" in sys.modules:
            del sys.modules["app.config"]
        with patch.dict(sys.modules, {}):
            with patch.object(sys, "path", sys.path):
                try:
                    pass
                except RuntimeError as e:
                    assert (
                        "MINIO_ENDPOINT, MINIO_ACCESS_KEY and MINIO_SECRET_KEY are required"
                        in str(e)
                    )
                    return
    pytest.fail("Expected RuntimeError was not raised")


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
        if "app.config" in sys.modules:
            del sys.modules["app.config"]
        try:
            pass
        except RuntimeError as e:
            assert "INTERNAL_API_KEY environment variable is required" in str(e)
            return
    pytest.fail("Expected RuntimeError was not raised")


def test_config_accepts_dev_without_minio():
    """Should not raise in dev when MINIO vars missing / dev 环境缺少 MINIO 变量时不应抛出"""
    env = {
        "ENV": "dev",
        "INTERNAL_API_KEY": "",
    }
    with patch.dict(os.environ, env, clear=True):
        if "app.config" in sys.modules:
            del sys.modules["app.config"]
        # Should not raise
        import app.config

        assert app.config.ENV == "dev"
