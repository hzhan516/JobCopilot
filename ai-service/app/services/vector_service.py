from google import genai
from google.genai import types

from app.config import (
    EMBEDDING_OUTPUT_DIMENSION,
    GEMINI_EMBEDDING_MODEL,
    GOOGLE_CLOUD_PROJECT,
    VERTEX_AI_LOCATION,
)
from app.schemas import AiResultEvent, VectorGenCommand


def _get_vertex_client() -> genai.Client:
    return genai.Client(
        vertexai=True,
        project=GOOGLE_CLOUD_PROJECT,
        location=VERTEX_AI_LOCATION,
    )


def generate_embedding(text: str) -> list[float]:
    cleaned_text = text.strip()

    if not cleaned_text:
        raise ValueError("Input text for vector generation is empty.")

    client = _get_vertex_client()
    response = client.models.embed_content(
        model=GEMINI_EMBEDDING_MODEL,
        contents=[cleaned_text],
        config=types.EmbedContentConfig(
            task_type="RETRIEVAL_DOCUMENT",
            output_dimensionality=EMBEDDING_OUTPUT_DIMENSION,
        ),
    )

    if not response.embeddings:
        raise ValueError("Vertex AI returned no embeddings.")

    embedding = response.embeddings[0].values
    if not embedding:
        raise ValueError("Vertex AI returned an empty embedding vector.")

    return [float(value) for value in embedding]


def process_vector(command: VectorGenCommand) -> AiResultEvent:
    embedding = generate_embedding(command.text)

    return AiResultEvent(
        referenceId=command.reference_id,
        type="VECTOR_GEN",
        status="COMPLETED",
        data={
            "embedding": embedding,
            "entityType": command.entity_type,
        },
        errorMessage=None,
        eventType=command.entity_type,
    )
