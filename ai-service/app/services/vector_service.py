from litellm import embedding

from app.config import (
    LLM_EMBEDDING_MODEL,
)
from app.schemas import AiResultEvent, VectorGenCommand


def generate_embedding(text: str) -> list[float]:
    cleaned_text = text.strip()

    if not cleaned_text:
        raise ValueError("Input text for vector generation is empty.")

    response = embedding(
        model=LLM_EMBEDDING_MODEL,
        input=[cleaned_text],
    )

    if not response.data or not response.data[0].embedding:
        raise ValueError("LiteLLM returned no embeddings.")

    return response.data[0].embedding


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
