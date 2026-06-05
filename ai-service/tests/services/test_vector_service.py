from types import SimpleNamespace
from unittest.mock import patch

import pytest

from app.config import (
    LLM_EMBEDDING_MODEL,
    LLM_EMBEDDING_MODEL_DIMENSION,
    LLM_REQUEST_TIMEOUT_SECONDS,
)
from app.services.vector_service import _model_supports_dimensions, generate_embedding


@patch("app.services.vector_service.litellm.embedding")
def test_generate_embedding_passes_configured_dimensions(mock_embedding):
    expected_embedding = [0.1] * LLM_EMBEDDING_MODEL_DIMENSION
    mock_embedding.return_value = SimpleNamespace(
        data=[SimpleNamespace(embedding=expected_embedding)]
    )

    result = generate_embedding("backend engineer")

    assert result == expected_embedding
    # Since we dynamically inject dimensions based on the model, we shouldn't explicitly assert 'dimensions'
    # if the default LLM_EMBEDDING_MODEL doesn't trigger it (e.g. gemini-embedding-001).
    mock_embedding.assert_called_once()
    call_kwargs = mock_embedding.call_args.kwargs
    assert call_kwargs["model"] == LLM_EMBEDDING_MODEL
    assert call_kwargs["input"] == ["backend engineer"]
    assert call_kwargs["timeout"] == LLM_REQUEST_TIMEOUT_SECONDS


@patch("app.services.vector_service.litellm.embedding")
def test_generate_embedding_rejects_wrong_dimension(mock_embedding):
    mock_embedding.return_value = SimpleNamespace(
        data=[{"embedding": [0.1] * (LLM_EMBEDDING_MODEL_DIMENSION - 1)}]
    )

    with pytest.raises(ValueError, match="Embedding dimension mismatch"):
        generate_embedding("backend engineer")


def test_gemini_embedding_models_do_not_pass_dimensions():
    assert _model_supports_dimensions("gemini/gemini-embedding-001") is False
    assert _model_supports_dimensions("gemini/gemini-embedding-2") is False


def test_openai_embedding_models_pass_dimensions():
    assert _model_supports_dimensions("text-embedding-3-small") is True
    assert _model_supports_dimensions("openai/text-embedding-3-large") is True
