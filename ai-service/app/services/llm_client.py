import json
import logging
import re
import base64
from typing import Any

import litellm
from litellm import completion
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

from app.config import (
    LLM_TEXT_MODEL,
    LLM_VISION_MODEL,
    LLM_TEMPERATURE,
)

logger = logging.getLogger(__name__)



RETRY_STRATEGY = retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=10),
    retry=retry_if_exception_type((
        litellm.exceptions.RateLimitError,
        litellm.exceptions.APIConnectionError,
        litellm.exceptions.Timeout
    ))
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


@RETRY_STRATEGY
def _generate_text(model: str, messages: list[dict[str, Any]]) -> str:
    logger.debug("LLM request: model=%s, messages_count=%d", model, len(messages))
    try:
        response = completion(
            model=model,
            messages=messages,
            temperature=LLM_TEMPERATURE,
        )
    except Exception:
        logger.exception("LLM completion failed: model=%s", model)
        raise

    content = response.choices[0].message.content
    if not content:
        raise ValueError("LiteLLM returned an empty response.")

    logger.debug("LLM response: model=%s, content_length=%d", model, len(content))
    return content.strip()


def generate_json_from_text_prompt(prompt: str) -> dict[str, Any]:
    messages = [{"role": "user", "content": prompt}]
    raw_text = _generate_text(
        model=LLM_TEXT_MODEL,
        messages=messages,
    )
    json_text = _extract_json_text(raw_text)
    return json.loads(json_text)


@RETRY_STRATEGY
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
