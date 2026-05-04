import litellm
from litellm import embedding
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

from app.config import (
    LLM_EMBEDDING_MODEL,
)
from app.schemas import AiResultEvent, VectorGenCommand



RETRY_STRATEGY = retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=10),
    retry=retry_if_exception_type((
        litellm.exceptions.RateLimitError,
        litellm.exceptions.APIConnectionError,
        litellm.exceptions.Timeout
    ))
)

@RETRY_STRATEGY
def generate_embedding(text: str) -> list[float]:
    cleaned_text = text.strip()

    if not cleaned_text:
        raise ValueError("Input text for vector generation is empty.")

    response = embedding(
        model=LLM_EMBEDDING_MODEL,
        input=[cleaned_text],
    )

    if not response.data:
        raise ValueError("LiteLLM returned no embeddings.")

    emb_item = response.data[0]
    emb = emb_item["embedding"] if isinstance(emb_item, dict) else emb_item.embedding

    if not emb:
        raise ValueError("LiteLLM returned no embeddings.")

    return emb


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
