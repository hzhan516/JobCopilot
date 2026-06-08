import json
import logging
import re
import base64
import threading
from dataclasses import dataclass
from typing import Any

import litellm
from litellm import completion
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

from app.config import (
    LLM_TEXT_MODEL,
    LLM_VISION_MODEL,
    LLM_TEMPERATURE,
    LLM_REQUEST_TIMEOUT_SECONDS,
    LLM_MAX_TOKENS,
)

logger = logging.getLogger(__name__)


class LlmJsonParseError(ValueError):
    """Raised when an LLM response cannot be parsed as the requested JSON object."""

    def __init__(
        self,
        message: str,
        *,
        raw_text: str,
        json_text: str | None = None,
        repair_raw_text: str | None = None,
        original_error: Exception | None = None,
        repair_error: Exception | None = None,
    ) -> None:
        super().__init__(message)
        self.raw_text = raw_text
        self.json_text = json_text
        self.repair_raw_text = repair_raw_text
        self.original_error = original_error
        self.repair_error = repair_error


@dataclass(frozen=True)
class JsonGenerationResult:
    """Parsed JSON object plus telemetry about the model-output repair path."""

    data: dict[str, Any]
    raw_text: str
    json_text: str
    repaired: bool = False
    repair_raw_text: str | None = None

# 全局并发锁：防止 FastAPI 的 I/O 并发瞬间打爆 Vertex AI 导致 429
# Global Semaphore: Threading bounded semaphore to cap concurrent LLM requests
MAX_CONCURRENT_LLM_REQUESTS = 5
_llm_semaphore = threading.BoundedSemaphore(MAX_CONCURRENT_LLM_REQUESTS)

# Exponential backoff for transient LLM failures (rate limits, connection drops).
# 指数退避重试：针对 LLM 服务商的瞬时限流或网络抖动，避免请求堆积放大故障。
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
    """Strip markdown fences and extract the first JSON object from raw LLM output.
    清洗 LLM 原始输出：去除 markdown 代码块标记并提取首个 JSON 对象，兼容多种模型输出格式。"""
    cleaned = raw_text.strip()

    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
        cleaned = re.sub(r"\s*```$", "", cleaned)

    start = cleaned.find("{")
    if start == -1:
        raise ValueError(f"LLM response did not contain a JSON object: {raw_text}")

    depth = 0
    in_string = False
    escaped = False

    for index in range(start, len(cleaned)):
        char = cleaned[index]

        if escaped:
            escaped = False
            continue

        if char == "\\" and in_string:
            escaped = True
            continue

        if char == '"':
            in_string = not in_string
            continue

        if in_string:
            continue

        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return cleaned[start:index + 1]
            if depth < 0:
                raise ValueError(f"LLM response contained invalid JSON braces: {cleaned[start:index + 1]}")

    extracted = cleaned[start:]
    raise ValueError(f"Extracted JSON is incomplete: braces mismatch in {extracted[:200]}")

def _safe_json_loads(text: str) -> dict[str, Any]:
    """Safely parse JSON, handling illegal control characters that may appear inside string values in LLM output.
    安全解析 JSON：LLM 可能在字符串值中输出字面量控制字符（如换行符），导致标准 json.loads 失败。
    采用双层修复策略：先尝试 strict=False 宽容解析，失败后再对双引号字符串内的非法控制字符做精确替换。"""
    try:
        return json.loads(text, strict=False)
    except json.JSONDecodeError:
        logger.warning("JSON parse failed (strict=False), attempting repair")

    def _sanitize_string(match: re.Match) -> str:
        s = match.group(0)
        result = []
        i = 0
        while i < len(s):
            if s[i] == "\\" and i + 1 < len(s):
                # Preserve already-escaped sequences (e.g. \\n, \\") to avoid double-escaping.
                # 保留已转义序列，防止二次转义破坏原有结构。
                result.append(s[i])
                result.append(s[i + 1])
                i += 2
            elif ord(s[i]) < 32:
                # Replace raw control characters with their escaped equivalents or space.
                # 将原始控制字符替换为对应转义序列或空格，使其符合 JSON 规范。
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

    # Match only double-quoted strings (including escaped chars) to avoid breaking JSON structure.
    # 仅匹配双引号字符串（含转义字符），避免误替换 JSON 结构符号。
    sanitized = re.sub(r'"(?:\\.|[^"\\])*"', _sanitize_string, text)

    try:
        return json.loads(sanitized, strict=False)
    except json.JSONDecodeError as e:
        logger.error("JSON repair failed: %s, text=%r", e, text[:500])
        raise


def _parse_json_object_from_text(raw_text: str) -> tuple[dict[str, Any], str]:
    """Extract and parse one JSON object from raw LLM text without applying model repair."""
    json_text = _extract_json_text(raw_text)
    parsed = _safe_json_loads(json_text)
    if not isinstance(parsed, dict):
        raise ValueError(f"LLM JSON response must be an object, got {type(parsed).__name__}")
    return parsed, json_text


@RETRY_STRATEGY
def _generate_text(model: str, messages: list[dict[str, Any]]) -> str:
    """Execute a text-only LLM completion with retries and empty-response guard.
    执行文本补全：带重试机制与空响应兜底，防止下游因空字符串导致解析异常。"""
    logger.debug("LLM request: model=%s, messages_count=%d", model, len(messages))
    try:
        with _llm_semaphore:
            response = completion(
                model=model,
                messages=messages,
                temperature=LLM_TEMPERATURE,
                max_tokens=LLM_MAX_TOKENS,
                timeout=LLM_REQUEST_TIMEOUT_SECONDS,
            )
    except Exception:
        logger.exception("LLM completion failed: model=%s", model)
        raise

    if not response.choices:
        raise ValueError("LiteLLM returned no choices.")
    choice = response.choices[0]
    finish_reason = getattr(choice, "finish_reason", None)
    if finish_reason == "length":
        raise ValueError(
            f"LLM response was truncated due to token limit (finish_reason=length). "
            f"Consider increasing LLM_MAX_TOKENS (current={LLM_MAX_TOKENS})."
        )

    content = choice.message.content
    if not content:
        raise ValueError("LiteLLM returned an empty response.")

    logger.debug("LLM response: model=%s, content_length=%d", model, len(content))
    return content.strip()


def generate_json_from_text_prompt(prompt: str) -> dict[str, Any]:
    """Generate a structured JSON dict from a text prompt through the LLM pipeline.
    从文本 prompt 生成结构化 JSON：组合文本补全、JSON 提取与容错解析，封装完整的 LLM-to-dict 链路。"""
    messages = [{"role": "user", "content": prompt}]
    raw_text = _generate_text(
        model=LLM_TEXT_MODEL,
        messages=messages,
    )
    parsed, _ = _parse_json_object_from_text(raw_text)
    return parsed


def _build_json_repair_prompt(raw_text: str, parse_error: Exception, context: str | None = None) -> str:
    """Build a constrained prompt that converts malformed model output into valid JSON only."""
    context_section = f"\nExpected JSON contract:\n{context.strip()}\n" if context and context.strip() else ""
    return f"""
Convert the malformed model output below into one valid JSON object.
Return JSON only. Do not add markdown fences or explanatory text.
Preserve the user's visible answer as much as possible.
If a field is missing, use a safe empty default.
{context_section}
Parse error:
{parse_error}

Malformed output:
{raw_text[:12000]}
""".strip()


def generate_json_from_text_prompt_with_repair(
    prompt: str,
    *,
    repair_context: str | None = None,
) -> JsonGenerationResult:
    """Generate a JSON object and make one model-assisted repair attempt before failing.

    This is intended for user-facing flows like conversation where a valid natural-language
    answer can often be recovered from malformed JSON. Strict extraction flows should keep
    using generate_json_from_text_prompt().
    """
    messages = [{"role": "user", "content": prompt}]
    raw_text = _generate_text(
        model=LLM_TEXT_MODEL,
        messages=messages,
    )

    parse_error: Exception | None = None
    try:
        parsed, json_text = _parse_json_object_from_text(raw_text)
        return JsonGenerationResult(data=parsed, raw_text=raw_text, json_text=json_text)
    except Exception as first_error:
        parse_error = first_error
        logger.warning(
            "LLM JSON parse failed; attempting model repair: error=%s, raw_text_length=%d",
            first_error,
            len(raw_text),
        )

    repair_prompt = _build_json_repair_prompt(raw_text, parse_error, repair_context)
    repair_raw_text: str | None = None
    try:
        repair_raw_text = _generate_text(
            model=LLM_TEXT_MODEL,
            messages=[{"role": "user", "content": repair_prompt}],
        )
        parsed, json_text = _parse_json_object_from_text(repair_raw_text)
        logger.info(
            "LLM JSON repair succeeded: original_length=%d, repair_length=%d",
            len(raw_text),
            len(repair_raw_text),
        )
        return JsonGenerationResult(
            data=parsed,
            raw_text=raw_text,
            json_text=json_text,
            repaired=True,
            repair_raw_text=repair_raw_text,
        )
    except Exception as repair_error:
        logger.error(
            "LLM JSON repair failed: first_error=%s, repair_error=%s, raw_text_prefix=%r",
            parse_error,
            repair_error,
            raw_text[:500],
        )
        raise LlmJsonParseError(
            "LLM JSON response could not be parsed or repaired.",
            raw_text=raw_text,
            repair_raw_text=repair_raw_text,
            original_error=parse_error,
            repair_error=repair_error,
        ) from repair_error


@RETRY_STRATEGY
def generate_json_from_image_prompt(
    prompt: str,
    image_bytes: bytes,
    mime_type: str = "image/png",
) -> dict[str, Any]:
    """Generate a structured JSON dict from a text+image prompt using the vision model.
    从图文 prompt 生成结构化 JSON：将图片编码为 data URI 后送入 vision 模型，复用与文本模型相同的重试与解析链路。"""
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
    parsed = _safe_json_loads(json_text)
    if not isinstance(parsed, dict):
        raise ValueError(f"LLM JSON response must be an object, got {type(parsed).__name__}")
    return parsed
