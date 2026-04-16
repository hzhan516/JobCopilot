from app.schemas import AiResultEvent, VectorGenCommand


def generate_embedding(text: str) -> list[float]:
    cleaned_text = text.strip()

    if not cleaned_text:
        raise ValueError("Input text for vector generation is empty.")

    preview = cleaned_text[:10]
    vector = [float(ord(char) % 100) / 100.0 for char in preview]

    while len(vector) < 10:
        vector.append(0.0)

    return vector


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
