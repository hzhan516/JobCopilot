from types import SimpleNamespace
from unittest.mock import patch

import pytest

from app.config import EMBEDDING_OUTPUT_DIMENSION, LLM_EMBEDDING_MODEL
from app.services.vector_service import generate_embedding


@patch("app.services.vector_service.embedding")
def test_generate_embedding_passes_configured_dimensions(mock_embedding):
    expected_embedding = [0.1] * EMBEDDING_OUTPUT_DIMENSION
    mock_embedding.return_value = SimpleNamespace(
        data=[SimpleNamespace(embedding=expected_embedding)]
    )

    result = generate_embedding("backend engineer")

    assert result == expected_embedding
    mock_embedding.assert_called_once_with(
        model=LLM_EMBEDDING_MODEL,
        input=["backend engineer"],
        dimensions=EMBEDDING_OUTPUT_DIMENSION,
    )


@patch("app.services.vector_service.embedding")
def test_generate_embedding_rejects_wrong_dimension(mock_embedding):
    mock_embedding.return_value = SimpleNamespace(
        data=[{"embedding": [0.1] * (EMBEDDING_OUTPUT_DIMENSION - 1)}]
    )

    with pytest.raises(ValueError, match="Embedding dimension mismatch"):
        generate_embedding("backend engineer")
