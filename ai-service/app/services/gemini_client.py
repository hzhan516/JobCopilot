import base64
import json
import re
from typing import Any

import httpx

from app.config import GEMINI_API_KEY, GEMINI_TEXT_MODEL, GEMINI_VISION_MODEL

GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta"


def _extract_candidate_text(payload: dict[str, Any]) -> str:
    candidates = payload.get("candidates") or []
    if not candidates:
        raise ValueError("Gemini returned no candidates.")

    content = candidates[0].get("content") or {}
    parts = content.get("parts") or []

    texts: list[str] = []
    for part in parts:
        text = part.get("text")
        if text:
            texts.append(text)

    if not texts:
        raise ValueError("Gemini response did not contain text output.")

    return "\n".join(texts).strip()


def _extract_json_text(raw_text: str) -> str:
    cleaned = raw_text.strip()

    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
        cleaned = re.sub(r"\s*```$", "", cleaned)

    match = re.search(r"\{.*\}", cleaned, flags=re.DOTALL)
    if not match:
        raise ValueError("Gemini response did not contain a JSON object.")

    return match.group(0)


def _post_generate_content(model: str, parts: list[dict[str, Any]]) -> dict[str, Any]:
    if not GEMINI_API_KEY:
        raise ValueError("GEMINI_API_KEY is not configured.")

    url = f"{GEMINI_API_BASE}/models/{model}:generateContent"

    payload = {
        "contents": [
            {
                "parts": parts,
            }
        ]
    }

    headers = {
        "Content-Type": "application/json",
        "x-goog-api-key": GEMINI_API_KEY,
    }

    response = httpx.post(
        url,
        json=payload,
        headers=headers,
        timeout=60.0,
    )

    if response.status_code >= 400:
        raise ValueError(
            f"Gemini API request failed with status {response.status_code}: {response.text}"
        )

    return response.json()


def generate_json_from_text_prompt(prompt: str) -> dict[str, Any]:
    response_payload = _post_generate_content(
        model=GEMINI_TEXT_MODEL,
        parts=[
            {
                "text": prompt,
            }
        ],
    )

    raw_text = _extract_candidate_text(response_payload)
    json_text = _extract_json_text(raw_text)
    return json.loads(json_text)


def generate_json_from_image_prompt(
    prompt: str,
    image_bytes: bytes,
    mime_type: str = "image/png",
) -> dict[str, Any]:
    encoded_image = base64.b64encode(image_bytes).decode("utf-8")

    response_payload = _post_generate_content(
        model=GEMINI_VISION_MODEL,
        parts=[
            {
                "text": prompt,
            },
            {
                "inline_data": {
                    "mime_type": mime_type,
                    "data": encoded_image,
                }
            },
        ],
    )

    raw_text = _extract_candidate_text(response_payload)
    json_text = _extract_json_text(raw_text)
    return json.loads(json_text)
