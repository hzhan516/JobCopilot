import json
import logging
import re
import base64
from typing import Any

import litellm
from litellm import completion
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

# LLM client helpers for text and image prompt execution with retries.

from app.config import (
    LLM_TEXT_MODEL,
    LLM_VISION_MODEL,
    LLM_TEMPERATURE,
    LLM_REQUEST_TIMEOUT_SECONDS,
)

logger = logging.getLogger(__name__)


# Shared retry policy for transient LLM failures.
RETRY_STRATEGY = retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=10),
    retry=retry_if_exception_type((
        litellm.exceptions.RateLimitError,
        litellm.exceptions.APIConnectionError,
        litellm.exceptions.Timeout
    ))
)

# Extract the JSON object portion from raw LLM output.
def _extract_json_text(raw_text: str) -> str:
    cleaned = raw_text.strip()

    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
        cleaned = re.sub(r"\s*```$", "", cleaned)

    match = re.search(r"\{.*\}", cleaned, flags=re.DOTALL)
    if not match:
        raise ValueError(f"LLM response did not contain a JSON object: {raw_text}")

    return match.group(0)


# Safely parse JSON text, handling illegal control characters from LLM output.
def _safe_json_loads(text: str) -> dict[str, Any]:
    """安全解析 JSON，处理 LLM 输出中字符串值内可能包含的非法控制字符
    Safely parse JSON, handling illegal control characters that may appear
    inside string values in LLM output."""
    # 第一层：先尝试 strict=False，允许字面量 \n 和 \r
    # Layer 1: try strict=False first, allowing literal \n and \r
    try:
        return json.loads(text, strict=False)
    except json.JSONDecodeError:
        logger.warning("JSON parse failed (strict=False), attempting repair")

    # 第二层：清理 JSON 字符串值内部的非法控制字符
    # Layer 2: sanitize illegal control characters inside JSON string values
    def _sanitize_string(match: re.Match) -> str:
        s = match.group(0)
        result = []
        i = 0
        while i < len(s):
            if s[i] == "\\" and i + 1 < len(s):
                # 保留已转义序列（如 \\n、\\"）
                # Preserve already-escaped sequences (e.g. \\n, \\")
                result.append(s[i])
                result.append(s[i + 1])
                i += 2
            elif ord(s[i]) < 32:
                # 将非法控制字符替换为对应的转义序列或空格
                # Replace illegal control chars with escapes or space
                if s[i] == "\n":
                    result.append("\\n")
                elif s[i] == "\r":
                    result.append("\\r")
                elif s[i] == "\t":
                    result.append("\\t")
                else:
                    result.append(" ")
                i += 1
            else:
                result.append(s[i])
                i += 1
        return "".join(result)

    # 只匹配双引号字符串（含转义字符），避免破坏 JSON 结构
    # Match only double-quoted strings (with escapes) to avoid breaking JSON structure
    sanitized = re.sub(r'"(?:\\.|[^"\\])*"', _sanitize_string, text)

    try:
        return json.loads(sanitized, strict=False)
    except json.JSONDecodeError as e:
        logger.error("JSON repair failed: %s, text=%r", e, text[:500])
        raise


# Execute a text-only LLM completion with retries.
@RETRY_STRATEGY
def _generate_text(model: str, messages: list[dict[str, Any]]) -> str:
    logger.debug("LLM request: model=%s, messages_count=%d", model, len(messages))
    try:
        response = completion(
            model=model,
            messages=messages,
            temperature=LLM_TEMPERATURE,
            timeout=LLM_REQUEST_TIMEOUT_SECONDS,
        )
    except Exception:
        logger.exception("LLM completion failed: model=%s", model)
        raise

    content = response.choices[0].message.content
    if not content:
        raise ValueError("LiteLLM returned an empty response.")

    logger.debug("LLM response: model=%s, content_length=%d", model, len(content))
    return content.strip()


# Generate a JSON dict from a text prompt.
def generate_json_from_text_prompt(prompt: str) -> dict[str, Any]:
    messages = [{"role": "user", "content": prompt}]
    raw_text = _generate_text(
        model=LLM_TEXT_MODEL,
        messages=messages,
    )
    json_text = _extract_json_text(raw_text)
    return _safe_json_loads(json_text)


# Generate a JSON dict from a text+image prompt.
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
    return _safe_json_loads(json_text)
