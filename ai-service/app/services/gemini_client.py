import json
import re
from typing import Any

from google import genai
from google.genai import types

from app.config import (
    GEMINI_TEXT_MODEL,
    GEMINI_VISION_MODEL,
    GOOGLE_CLOUD_PROJECT,
    VERTEX_AI_LOCATION,
)


def _get_vertex_client() -> genai.Client:
    return genai.Client(
        vertexai=True,
        project=GOOGLE_CLOUD_PROJECT,
        location=VERTEX_AI_LOCATION,
    )


def _extract_json_text(raw_text: str) -> str:
    cleaned = raw_text.strip()

    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
        cleaned = re.sub(r"\s*```$", "", cleaned)

    match = re.search(r"\{.*\}", cleaned, flags=re.DOTALL)
    if not match:
        raise ValueError(f"Gemini response did not contain a JSON object: {raw_text}")

    return match.group(0)


def _generate_text(model: str, parts: list[Any]) -> str:
    client = _get_vertex_client()

    response = client.models.generate_content(
        model=model,
        contents=parts,
        config=types.GenerateContentConfig(
            temperature=0.1,
        ),
    )

    if not response.text:
        raise ValueError("Vertex AI returned an empty response.")

    return response.text.strip()


def generate_json_from_text_prompt(prompt: str) -> dict[str, Any]:
    raw_text = _generate_text(
        model=GEMINI_TEXT_MODEL,
        parts=[prompt],
    )
    json_text = _extract_json_text(raw_text)
    return json.loads(json_text)


def generate_json_from_image_prompt(
    prompt: str,
    image_bytes: bytes,
    mime_type: str = "image/png",
) -> dict[str, Any]:
    raw_text = _generate_text(
        model=GEMINI_VISION_MODEL,
        parts=[
            prompt,
            types.Part.from_bytes(data=image_bytes, mime_type=mime_type),
        ],
    )
    json_text = _extract_json_text(raw_text)
    return json.loads(json_text)
