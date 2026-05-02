import json
import re
import base64
from typing import Any

from litellm import completion

from app.config import (
    LLM_TEXT_MODEL,
    LLM_VISION_MODEL,
)


def _extract_json_text(raw_text: str) -> str:
    cleaned = raw_text.strip()

    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
        cleaned = re.sub(r"\s*```$", "", cleaned)

    match = re.search(r"\{.*\}", cleaned, flags=re.DOTALL)
    if not match:
        raise ValueError(f"LLM response did not contain a JSON object: {raw_text}")

    return match.group(0)


def _generate_text(model: str, messages: list[dict[str, Any]]) -> str:
    response = completion(
        model=model,
        messages=messages,
        temperature=0.1,
    )

    content = response.choices[0].message.content
    if not content:
        raise ValueError("LiteLLM returned an empty response.")

    return content.strip()


def generate_json_from_text_prompt(prompt: str) -> dict[str, Any]:
    messages = [{"role": "user", "content": prompt}]
    raw_text = _generate_text(
        model=LLM_TEXT_MODEL,
        messages=messages,
    )
    json_text = _extract_json_text(raw_text)
    return json.loads(json_text)


def generate_json_from_image_prompt(
    prompt: str,
    image_bytes: bytes,
    mime_type: str = "image/png",
) -> dict[str, Any]:
    base64_image = base64.b64encode(image_bytes).decode("utf-8")
    image_url = f"data:{mime_type};base64,{base64_image}"
    
    messages = [
        {
            "role": "user",
            "content": [
                {"type": "text", "text": prompt},
                {
                    "type": "image_url",
                    "image_url": {"url": image_url}
                }
            ]
        }
    ]
    
    raw_text = _generate_text(
        model=LLM_VISION_MODEL,
        messages=messages,
    )
    json_text = _extract_json_text(raw_text)
    return json.loads(json_text)
