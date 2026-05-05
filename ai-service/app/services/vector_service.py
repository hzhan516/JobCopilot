import litellm
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

# Embedding generation utilities for vector workflows.

from app.config import (
    LLM_EMBEDDING_MODEL,
    LLM_EMBEDDING_MODEL_DIMENSION,
    LLM_REQUEST_TIMEOUT_SECONDS,
)
from app.schemas import AiResultEvent, VectorGenCommand


# Shared retry policy for embedding calls.
RETRY_STRATEGY = retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=10),
    retry=retry_if_exception_type((
        litellm.exceptions.RateLimitError,
        litellm.exceptions.APIConnectionError,
        litellm.exceptions.Timeout
    ))
)

# Generate an embedding vector for the provided text.
@RETRY_STRATEGY
def generate_embedding(text: str) -> list[float]:
    cleaned_text = text.strip()

    if not cleaned_text:
        raise ValueError("Input text for vector generation is empty.")

    response = litellm.embedding(
        model=LLM_EMBEDDING_MODEL,
        input=[cleaned_text],
        dimensions=LLM_EMBEDDING_MODEL_DIMENSION,
        timeout=LLM_REQUEST_TIMEOUT_SECONDS,
    )

    if not response.data:
        raise ValueError("LiteLLM returned no embeddings.")

    emb_item = response.data[0]
    emb = emb_item["embedding"] if isinstance(emb_item, dict) else emb_item.embedding

    if not emb:
        raise ValueError("LiteLLM returned no embeddings.")

    if len(emb) != LLM_EMBEDDING_MODEL_DIMENSION:
        raise ValueError(
            "Embedding dimension mismatch: "
            f"expected {LLM_EMBEDDING_MODEL_DIMENSION}, got {len(emb)}"
        )

    return emb


# Generate embeddings and wrap them in an AI result event.
def process_vector(command: VectorGenCommand) -> AiResultEvent:
    emb = generate_embedding(command.text)

    return AiResultEvent(
        referenceId=command.reference_id,
        type="VECTOR_GEN",
        status="COMPLETED",
        data={
            "embedding": emb,
            "entityType": command.entity_type,
        },
        errorMessage=None,
        eventType=command.entity_type,
    )
